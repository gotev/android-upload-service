package net.gotev.uploadservicedemo.utils

import net.gotev.recycleradapter.RecyclerAdapter
import net.gotev.uploadservicedemo.adapteritems.UploadItem

/**
 * @author Aleksandar Gotev
 */
class UploadItemUtils(private val adapter: RecyclerAdapter) : UploadItem.Delegate {
    interface ForEachDelegate {
        fun onHeader(item: UploadItem)
        fun onParameter(item: UploadItem)
        fun onFile(item: UploadItem)
    }

    fun forEach(delegate: ForEachDelegate) {
        for (i in 0 until adapter.itemCount) {
            val adapterItem = adapter.getItemAtPosition(i)
            if (adapterItem != null && adapterItem.javaClass.javaClass == UploadItem::class.java.javaClass) {
                val uploadItem = adapterItem as UploadItem
                when (uploadItem.type) {
                    UploadItem.Type.Header -> delegate.onHeader(uploadItem)
                    UploadItem.Type.Parameter -> delegate.onParameter(uploadItem)
                    UploadItem.Type.File -> delegate.onFile(uploadItem)
                }
            }
        }
    }

    override fun onRemoveUploadItem(position: Int) {
        adapter.removeItemAtPosition(position)
    }

    private fun add(item: UploadItem) {
        adapter.addOrUpdate(item)
        adapter.sort(true, null)
    }

    fun addHeader(headerName: String, headerValue: String) {
        add(UploadItem(UploadItem.Type.Header, headerName, headerValue, this))
    }

    fun addParameter(paramName: String, paramValue: String) {
        add(UploadItem(UploadItem.Type.Parameter, paramName, paramValue, this))
    }

    fun addFile(paramName: String, filePath: String) {
        add(UploadItem(UploadItem.Type.File, paramName, filePath, this))
    }
}
