package net.gotev.uploadservicedemo.adapteritems;

import android.content.Context;
import android.support.annotation.StringRes;
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

    private @StringRes int text;

    public EmptyItem(@StringRes int textResource) {
        text = textResource;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_empty;
    }

    @Override
    protected void bind(Holder holder) {
        Context ctx = holder.textView.getContext();
        holder.textView.setText(ctx.getString(text));
    }

    public static class Holder extends ButterKnifeViewHolder {

        @BindView(R.id.textView)
        TextView textView;

        public Holder(View itemView, RecyclerAdapterNotifier adapter) {
            super(itemView, adapter);
        }
    }
}
