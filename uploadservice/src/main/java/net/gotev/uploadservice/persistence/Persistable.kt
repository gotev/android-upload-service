package net.gotev.uploadservice.persistence

interface Persistable {
    fun asPersistableData(): PersistableData

    interface Creator<T> {
        fun createFromPersistableData(data: PersistableData): T
    }
}