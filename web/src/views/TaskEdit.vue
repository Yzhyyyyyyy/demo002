<template>
  <div class="task-edit-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <h1 class="page-title">{{ isNew ? '新建任务' : '编辑任务' }}</h1>
      <button class="save-btn" @click="handleSave" :disabled="saving">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div class="form-body">
      <!-- 标题 -->
      <div class="field">
        <input
          v-model="form.title"
          class="title-input"
          type="text"
          placeholder="任务标题"
          maxlength="100"
        />
      </div>

      <!-- 优先级 -->
      <div class="field">
        <label class="field-label">优先级</label>
        <div class="priority-picker">
          <button
            v-for="(p, key) in priorities"
            :key="key"
            :class="['priority-option', { active: form.priority === key }]"
            :style="form.priority === key ? { background: p.color + '18', color: p.color, borderColor: p.color } : {}"
            @click="form.priority = key"
          >{{ p.label }}</button>
        </div>
      </div>

      <!-- 日期 -->
      <div class="field">
        <label class="field-label">日期</label>
        <input v-model="form.dueDate" class="date-input" type="date" />
      </div>

      <!-- 时间 -->
      <div class="field">
        <label class="field-label">时间段</label>
        <div class="time-row">
          <input v-model="form.startTime" class="time-input" type="time" />
          <span class="time-sep">—</span>
          <input v-model="form.endTime" class="time-input" type="time" />
        </div>
      </div>

      <!-- 提醒 -->
      <div class="field">
        <label class="field-label">提醒</label>
        <div class="chip-row">
          <button
            v-for="opt in reminderOptions"
            :key="opt.value"
            :class="['chip', { active: form.reminderOffset === opt.value }]"
            @click="form.reminderOffset = opt.value"
          >{{ opt.label }}</button>
        </div>
      </div>

      <!-- 位置 -->
      <div class="field">
        <label class="field-label">位置</label>
        <input v-model="form.location" class="text-input" type="text" placeholder="添加地点" />
      </div>

      <!-- 标签 -->
      <div class="field">
        <label class="field-label">标签</label>
        <div class="chip-row">
          <button
            v-for="tag in presetTags"
            :key="tag.label"
            :class="['chip tag-chip', { active: form.tags.includes(tag.label) }]"
            :style="form.tags.includes(tag.label) ? { background: tag.color + '18', color: tag.color, borderColor: tag.color } : {}"
            @click="toggleTag(tag.label)"
          >{{ tag.label }}</button>
        </div>
      </div>

      <!-- 备注 -->
      <div class="field">
        <label class="field-label">备注</label>
        <textarea
          v-model="form.note"
          class="note-input"
          placeholder="添加备注..."
          rows="3"
        ></textarea>
      </div>

      <!-- 删除按钮 -->
      <div v-if="!isNew" class="field delete-field">
        <button class="delete-btn" @click="handleDelete">删除任务</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTaskStore } from '../stores/task'
import { PRIORITIES, PRESET_TAGS } from '../utils/leancloud'

const router = useRouter()
const route = useRoute()
const taskStore = useTaskStore()

const priorities = PRIORITIES
const presetTags = PRESET_TAGS

const isNew = computed(() => route.name === 'TaskCreate')
const saving = ref(false)

const form = ref({
  title: '',
  note: '',
  dueDate: todayStr(),
  startTime: '',
  endTime: '',
  priority: 'MEDIUM',
  location: '',
  tags: [],
  reminderOffset: null,
  isDone: false
})

const reminderOptions = [
  { label: '无', value: null },
  { label: '5分钟前', value: 5 },
  { label: '15分钟前', value: 15 },
  { label: '30分钟前', value: 30 },
  { label: '1小时前', value: 60 }
]

function todayStr() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

onMounted(async () => {
  if (!isNew.value) {
    const taskId = route.params.id
    const task = taskStore.tasks.find(t => t.id === taskId)
    if (task) {
      form.value = {
        title: task.title || '',
        note: task.note || '',
        dueDate: task.dueDate ? task.dueDate.substring(0, 10) : '',
        startTime: task.startTime || '',
        endTime: task.endTime || '',
        priority: task.priority || 'MEDIUM',
        location: task.location || '',
        tags: task.tags || [],
        reminderOffset: task.reminderOffset ?? null,
        isDone: task.isDone || false
      }
    } else {
      // 任务可能在 store 还没加载完成，尝试加载
      await taskStore.loadTasks()
      const loadedTask = taskStore.tasks.find(t => t.id === taskId)
      if (loadedTask) {
        form.value = {
          title: loadedTask.title || '',
          note: loadedTask.note || '',
          dueDate: loadedTask.dueDate ? loadedTask.dueDate.substring(0, 10) : '',
          startTime: loadedTask.startTime || '',
          endTime: loadedTask.endTime || '',
          priority: loadedTask.priority || 'MEDIUM',
          location: loadedTask.location || '',
          tags: loadedTask.tags || [],
          reminderOffset: loadedTask.reminderOffset ?? null,
          isDone: loadedTask.isDone || false
        }
      }
    }
  }
})

function toggleTag(label) {
  const idx = form.value.tags.indexOf(label)
  if (idx >= 0) {
    form.value.tags.splice(idx, 1)
  } else {
    form.value.tags.push(label)
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    alert('请输入任务标题')
    return
  }
  saving.value = true
  try {
    const taskData = {
      ...form.value,
      id: isNew.value ? undefined : route.params.id
    }
    await taskStore.saveTask(taskData)
    router.push('/')
  } catch (e) {
    alert('保存失败: ' + e.message)
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (confirm('确定要删除这个任务吗？')) {
    await taskStore.deleteTaskById(route.params.id)
    router.push('/')
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.task-edit-page {
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0 20px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: #fff;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-title {
  flex: 1;
  font-size: 20px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0;
}

.save-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #6C5CE7, #a855f7);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 0.5px;
}

.title-input {
  padding: 14px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  font-size: 18px;
  font-weight: 600;
  outline: none;
  background: #fafbfc;
  transition: border-color 0.2s;
}

.title-input:focus {
  border-color: #6C5CE7;
  background: #fff;
}

.text-input {
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  background: #fafbfc;
  transition: border-color 0.2s;
}

.text-input:focus {
  border-color: #6C5CE7;
  background: #fff;
}

.date-input, .time-input {
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  background: #fafbfc;
  transition: border-color 0.2s;
}

.date-input:focus, .time-input:focus {
  border-color: #6C5CE7;
  background: #fff;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-sep {
  color: #cbd5e1;
  font-weight: 700;
}

.priority-picker {
  display: flex;
  gap: 10px;
}

.priority-option {
  padding: 8px 18px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  background: #fafbfc;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 7px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  background: #fafbfc;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.chip.active {
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
  border-color: #6C5CE7;
}

.tag-chip.active {
  border-color: currentColor;
}

.note-input {
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  background: #fafbfc;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.note-input:focus {
  border-color: #6C5CE7;
  background: #fff;
}

.delete-field {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.delete-btn {
  width: 100%;
  padding: 12px;
  background: #fef2f2;
  color: #ef4444;
  border: 1.5px solid #fecaca;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.delete-btn:hover {
  background: #fee2e2;
}
</style>
