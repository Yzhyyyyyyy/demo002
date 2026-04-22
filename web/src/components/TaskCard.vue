<template>
  <div class="task-card" :class="{ done: task.isDone }" @click="$emit('edit', task)">
    <div class="card-inner">
      <!-- 优先级色条 -->
      <div class="priority-bar" :style="{ background: priorityColor }"></div>

      <!-- 内容区 -->
      <div class="card-content">
        <div class="card-main">
          <div class="card-left">
            <!-- 完成按钮 -->
            <button class="check-btn" :class="{ checked: task.isDone }" @click.stop="$emit('toggle', task)">
              <svg v-if="task.isDone" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </button>

            <div class="text-area">
              <span class="task-title" :class="{ 'line-through': task.isDone }">{{ task.title }}</span>
              <span v-if="task.note" class="task-note">{{ task.note }}</span>

              <!-- 日期时间信息 -->
              <div v-if="task.dueDate" class="meta-row">
                <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                <span :class="{ overdue: isOverdue }">{{ formatDate(task.dueDate) }}</span>
                <span v-if="isOverdue" class="overdue-label">已逾期</span>
              </div>

              <div v-if="task.startTime || task.endTime" class="meta-row">
                <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span>{{ formatTimeRange(task.startTime, task.endTime) }}</span>
              </div>

              <div v-if="task.location" class="meta-row">
                <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
                <span>{{ task.location }}</span>
              </div>
            </div>
          </div>

          <!-- 优先级标签 -->
          <div class="priority-tag" :style="{ color: priorityColor, background: priorityColor + '18' }">
            {{ priorityLabel }}
          </div>
        </div>

        <!-- 标签 -->
        <div v-if="task.tags && task.tags.length > 0" class="tags-row">
          <span
            v-for="tag in task.tags"
            :key="tag"
            class="tag"
            :style="tagStyle(tag)"
          >{{ tag }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { PRESET_TAGS, PRIORITIES } from '../utils/leancloud'

const props = defineProps({
  task: { type: Object, required: true }
})

defineEmits(['edit', 'toggle', 'delete'])

const priorityColor = computed(() => PRIORITIES[props.task.priority]?.color || '#FFB347')
const priorityLabel = computed(() => PRIORITIES[props.task.priority]?.label || '普通')

const isOverdue = computed(() => {
  if (!props.task.dueDate || props.task.isDone) return false
  const d = new Date(props.task.dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  d.setHours(0, 0, 0, 0)
  return d < today
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  d.setHours(0, 0, 0, 0)
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)

  if (d.getTime() === today.getTime()) return '今天'
  if (d.getTime() === tomorrow.getTime()) return '明天'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatTimeRange(start, end) {
  let s = ''
  if (start) s += start.substring(0, 5)
  if (start && end) s += ' - '
  if (end) s += end.substring(0, 5)
  return s
}

function tagStyle(label) {
  const preset = PRESET_TAGS.find(t => t.label === label)
  if (preset) {
    return {
      color: preset.color,
      background: preset.color + '18'
    }
  }
  return { color: '#94a3b8', background: '#f1f5f9' }
}
</script>

<style scoped>
.task-card {
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.task-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}

.task-card.done {
  opacity: 0.6;
}

.card-inner {
  display: flex;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.priority-bar {
  width: 4px;
  border-radius: 0;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
  padding: 14px 16px;
}

.card-main {
  display: flex;
  gap: 12px;
}

.card-left {
  display: flex;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.check-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1.5px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  transition: all 0.2s;
}

.check-btn.checked {
  background: #34d399;
  border-color: #34d399;
}

.text-area {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.task-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  word-break: break-word;
}

.task-title.line-through {
  text-decoration: line-through;
  color: #94a3b8;
}

.task-note {
  font-size: 12px;
  color: #b0bec5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.meta-row .overdue {
  color: #ff6b6b;
  font-weight: 700;
}

.overdue-label {
  color: #ff6b6b;
  font-weight: 700;
  font-size: 10px;
  margin-left: 4px;
}

.priority-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 9px;
  border-radius: 8px;
  white-space: nowrap;
  flex-shrink: 0;
  align-self: flex-start;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding-left: 32px;
}

.tag {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
}
</style>
