<template>
  <div class="task-list-page">
    <!-- 日期选择器 -->
    <div class="week-selector">
      <button class="nav-arrow" @click="changeWeek(-1)">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <div class="week-days">
        <button
          v-for="(day, idx) in weekDates"
          :key="idx"
          :class="['day-cell', { selected: isSameDay(day, selectedDate), today: isSameDay(day, today) }]"
          @click="selectedDate = day"
        >
          <span class="day-label">{{ cnDays[idx] }}</span>
          <span class="day-num">{{ day.getDate() }}</span>
          <span v-if="hasTaskOn(day)" class="dot"></span>
        </button>
      </div>
      <button class="nav-arrow" @click="changeWeek(1)">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>

    <!-- 今日按钮 -->
    <div v-if="!isSameDay(selectedDate, today)" class="today-bar">
      <button class="btn-today" @click="goToday">回到今天</button>
    </div>

    <!-- 任务列表 -->
    <div class="task-sections">
      <!-- 待办 -->
      <div v-if="pendingTasks.length > 0" class="section">
        <div class="section-header">
          <span class="section-title">待办</span>
          <span class="section-count">{{ pendingTasks.length }}</span>
        </div>
        <div class="task-cards">
          <TaskCard
            v-for="task in pendingTasks"
            :key="task.id"
            :task="task"
            @toggle="toggleTask"
            @edit="editTask"
            @delete="deleteTask"
          />
        </div>
      </div>

      <!-- 已完成 -->
      <div v-if="doneTasks.length > 0" class="section">
        <div class="section-header completed">
          <span class="section-title">已完成</span>
          <span class="section-count">{{ doneTasks.length }}</span>
        </div>
        <div class="task-cards">
          <TaskCard
            v-for="task in doneTasks"
            :key="task.id"
            :task="task"
            @toggle="toggleTask"
            @edit="editTask"
            @delete="deleteTask"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredTasks.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p class="empty-title">这一天还没有任务</p>
        <p class="empty-hint">点击右下角 + 添加新任务</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../stores/task'
import TaskCard from '../components/TaskCard.vue'

const router = useRouter()
const taskStore = useTaskStore()

const today = new Date()
today.setHours(0, 0, 0, 0)

const cnDays = ['一', '二', '三', '四', '五', '六', '日']
const selectedDate = ref(new Date(today))
const weekStart = ref(getMonday(new Date(today)))

onMounted(() => {
  if (!taskStore.tasks.length) {
    taskStore.loadTasks()
  }
})

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1)
  date.setDate(diff)
  date.setHours(0, 0, 0, 0)
  return date
}

const weekDates = computed(() => {
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart.value)
    d.setDate(d.getDate() + i)
    return d
  })
})

function changeWeek(delta) {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + delta * 7)
  weekStart.value = d
}

function goToday() {
  selectedDate.value = new Date(today)
  weekStart.value = getMonday(new Date(today))
}

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
}

function hasTaskOn(date) {
  return taskStore.tasks.some(t => {
    if (!t.dueDate) return false
    const taskDate = new Date(t.dueDate)
    return isSameDay(taskDate, date)
  })
}

function parseDateStr(str) {
  if (!str) return null
  const d = new Date(str)
  return isNaN(d.getTime()) ? null : d
}

const filteredTasks = computed(() => {
  return taskStore.tasks.filter(t => {
    if (t.deleted) return false
    const taskDate = parseDateStr(t.dueDate)
    if (!taskDate) return false
    return isSameDay(taskDate, selectedDate.value)
  }).sort((a, b) => {
    const pa = a.priority === 'HIGH' ? 0 : a.priority === 'MEDIUM' ? 1 : 2
    const pb = b.priority === 'HIGH' ? 0 : b.priority === 'MEDIUM' ? 1 : 2
    return pa - pb
  })
})

const pendingTasks = computed(() => filteredTasks.value.filter(t => !t.isDone))
const doneTasks = computed(() => filteredTasks.value.filter(t => t.isDone))

async function toggleTask(task) {
  await taskStore.toggleTaskCompletion(task.id)
}

function editTask(task) {
  router.push(`/task/edit/${task.id}`)
}

async function deleteTask(task) {
  if (confirm(`确定要删除任务「${task.title}」吗？`)) {
    await taskStore.deleteTaskById(task.id)
  }
}
</script>

<style scoped>
.task-list-page {
  padding-top: 4px;
}

.week-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.nav-arrow {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #fff;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.week-days {
  flex: 1;
  display: flex;
  background: #fff;
  border-radius: 14px;
  padding: 8px 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f5;
}

.day-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 2px;
  border-radius: 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.day-cell.selected {
  background: #1a1a2e;
}

.day-cell.today:not(.selected) {
  background: rgba(108, 92, 231, 0.1);
}

.day-label {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
}

.day-cell.selected .day-label {
  color: rgba(255, 255, 255, 0.8);
}

.day-num {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
}

.day-cell.selected .day-num {
  color: #fff;
}

.day-cell.today:not(.selected) .day-num {
  color: #6C5CE7;
}

.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fb7185;
}

.day-cell.selected .dot {
  background: #fff;
}

.today-bar {
  margin-bottom: 12px;
  text-align: center;
}

.btn-today {
  padding: 6px 16px;
  border-radius: 10px;
  border: none;
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.task-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 1px;
}

.section-header.completed .section-title {
  color: #94a3b8;
}

.section-count {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 20px;
  background: rgba(26, 26, 46, 0.08);
  color: #1a1a2e;
}

.section-header.completed .section-count {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}

.task-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: #94a3b8;
  margin: 0;
}

.empty-hint {
  font-size: 13px;
  color: #b0bec5;
  margin: 6px 0 0;
}

@media (max-width: 768px) {
  .week-days {
    padding: 6px 2px;
  }
  .day-num {
    font-size: 15px;
  }
}
</style>
