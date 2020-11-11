package net.gotev.uploadservice.persistence

interface Persistable {
    fun toPersistableData(): PersistableData

    interface Creator<T> {
        fun createFromPersistableData(data: PersistableData): T
    }
}
