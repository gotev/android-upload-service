package net.gotev.uploadservicedemo.adapteritems

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import net.gotev.recycleradapter.AdapterItem
import net.gotev.recycleradapter.RecyclerAdapterViewHolder
import net.gotev.uploadservicedemo.R

class UploadItem(
    val type: Type,
    val title: String,
    val subtitle: String,
    private val delegate: Delegate
) : AdapterItem<UploadItem.Holder>(type.toString() + title + subtitle) {
    interface Delegate {
        fun onRemoveUploadItem(position: Int)
    }

    enum class Type {
        Header,
        Parameter,
        File
    }

    private val icons = intArrayOf(
        R.drawable.ic_dehaze,
        R.drawable.ic_code,
        R.drawable.ic_description
    )

    override fun getLayoutId() = R.layout.item_upload

    override fun bind(firstTime: Boolean, holder: Holder) {
        holder.image.setImageResource(icons[type.ordinal])
        holder.title.text = title
        holder.subtitle.text = subtitle
    }

    class Holder(itemView: View) : RecyclerAdapterViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val image: ImageView = itemView.findViewById(R.id.image)

        init {
            itemView.findViewById<ImageView>(R.id.remove).setOnClickListener {
                (getAdapterItem() as? UploadItem)?.delegate?.onRemoveUploadItem(adapterPosition)
            }
        }
    }
}
