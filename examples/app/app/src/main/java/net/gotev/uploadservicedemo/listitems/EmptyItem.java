package net.gotev.uploadservicedemo.listitems;

import android.view.View;
import android.widget.TextView;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapterNotifier;
import net.gotev.uploadservicedemo.R;
import net.gotev.uploadservicedemo.views.ButterKnifeViewHolder;

import butterknife.BindView;

/**
 * @author Aleksandar Gotev
 */

public class EmptyItem extends AdapterItem<EmptyItem.Holder> {

    @Override
    public int getLayoutId() {
        return R.layout.item_empty;
    }

    @Override
    protected void bind(Holder holder) {

    }

    public static class Holder extends ButterKnifeViewHolder {

        @BindView(R.id.textView)
        TextView textView;

        public Holder(View itemView, RecyclerAdapterNotifier adapter) {
            super(itemView, adapter);
        }
    }
}
