package net.gotev.uploadservicedemo.adapteritems

import android.view.View
import android.widget.TextView
import net.gotev.recycleradapter.AdapterItem
import net.gotev.recycleradapter.RecyclerAdapterViewHolder
import net.gotev.uploadservicedemo.R

class EmptyItem(private val text: String) : AdapterItem<EmptyItem.Holder>(text) {
    override fun getLayoutId() = R.layout.item_empty

    override fun bind(firstTime: Boolean, holder: Holder) {
        holder.textView.text = text
    }

    class Holder(itemView: View) : RecyclerAdapterViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }
}
