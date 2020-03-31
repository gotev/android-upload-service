package net.gotev.uploadservice.data

data class TaskData(
        val status: TaskStatus,
        val info: UploadInfo,
        val config: UploadNotificationStatusConfig
)