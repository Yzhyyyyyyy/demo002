package com.example.demo002

import androidx.room.*

// ══════════════════════════════════════════════
//  Task 表
// ══════════════════════════════════════════════
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id        : Int     = 0,
    val title     : String,
    val note      : String  = "",
    val dueDate   : String? = null,   // LocalDate.toString()
    val startTime : String? = null,   // LocalTime.toString()
    val endTime   : String? = null,   // LocalTime.toString()
    val priority  : String  = "MEDIUM",
    val isDone    : Boolean = false,
    val tagLabels : String  = "",     // 逗号分隔的预设标签 label
    val location  : String  = ""      // 地点信息
)

// ══════════════════════════════════════════════
//  SubTask 表
// ══════════════════════════════════════════════
@Entity(
    tableName  = "subtasks",
    foreignKeys = [ForeignKey(
        entity        = TaskEntity::class,
        parentColumns = ["id"],
        childColumns  = ["taskId"],
        onDelete      = ForeignKey.CASCADE   // 删主任务时子任务自动删除
    )],
    indices = [Index("taskId")]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id     : Int     = 0,
    val taskId : Int,                  // 外键
    val title  : String,
    val isDone : Boolean = false,
    val dueDate: String? = null,
    val sortOrder: Int   = 0           // 保存拖拽排序
)

// ══════════════════════════════════════════════
//  Room 查询结果：Task + 其子任务列表
// ══════════════════════════════════════════════
data class TaskWithSubTasks(
    @Embedded val task    : TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subTasks: List<SubTaskEntity>
)

// ══════════════════════════════════════════════
//  Entity ↔ 业务模型 转换扩展函数
// ══════════════════════════════════════════════

fun TaskWithSubTasks.toTask(): Task {
    val tagList = if (task.tagLabels.isBlank()) emptyList()
    else task.tagLabels.split(",").mapNotNull { label ->
        PRESET_TAGS.find { it.label == label }
    }
    return Task(
        id        = task.id,
        title     = task.title,
        note      = task.note,
        dueDate   = task.dueDate?.let { java.time.LocalDate.parse(it) },
        startTime = task.startTime?.let { java.time.LocalTime.parse(it) },
        endTime   = task.endTime?.let { java.time.LocalTime.parse(it) },
        priority  = Priority.valueOf(task.priority),
        isDone    = task.isDone,
        tags      = tagList,
        // subTasks  = subTasks
        //     .sortedBy { it.sortOrder }
        //     .map { it.toSubTask() }, // 暂时屏蔽子任务功能
        location  = task.location
    )
}

fun SubTaskEntity.toSubTask() = SubTask(
    id      = id,
    title   = title,
    isDone  = isDone,
    dueDate = dueDate?.let { java.time.LocalDate.parse(it) }
)

fun Task.toEntity() = TaskEntity(
    id        = id,
    title     = title,
    note      = note,
    dueDate   = dueDate?.toString(),
    startTime = startTime?.toString(),
    endTime   = endTime?.toString(),
    priority  = priority.name,
    isDone    = isDone,
    tagLabels = tags.joinToString(",") { it.label },
    location  = location
)

fun SubTask.toEntity(taskId: Int, sortOrder: Int) = SubTaskEntity(
    id        = id,
    taskId    = taskId,
    title     = title,
    isDone    = isDone,
    dueDate   = dueDate?.toString(),
    sortOrder = sortOrder
)