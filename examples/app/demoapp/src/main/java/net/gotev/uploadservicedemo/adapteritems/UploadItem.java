package net.gotev.uploadservicedemo.adapteritems;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapterNotifier;
import net.gotev.uploadservicedemo.R;
import net.gotev.uploadservicedemo.views.ButterKnifeViewHolder;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author Aleksandar Gotev
 */

public class UploadItem extends AdapterItem<UploadItem.Holder> {

    public interface Delegate {
        void onRemoveUploadItem(int position);
    }

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_PARAMETER = 1;
    public static final int TYPE_FILE = 2;

    private static final String KEY_EVENT = "event";
    private static final String EVENT_REMOVE = "remove";

    private int mType;
    private String mTitle;
    private String mSubtitle;
    private Delegate mDelegate;

    private int[] icons = new int[]{
            R.drawable.ic_dehaze,
            R.drawable.ic_code,
            R.drawable.ic_description
    };

    public UploadItem(int type, String title, String subtitle, Delegate delegate) {
        mType = type;
        mTitle = title;
        mSubtitle = subtitle;
        mDelegate = delegate;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_upload;
    }

    @Override
    protected void bind(Holder holder) {
        holder.image.setImageResource(icons[mType]);
        holder.title.setText(mTitle);
        holder.subtitle.setText(mSubtitle);
    }

    @Override
    public boolean onEvent(int position, Bundle data) {
        if (data != null && EVENT_REMOVE.equals(data.getString(KEY_EVENT)))
            mDelegate.onRemoveUploadItem(position);

        return false;
    }

    public static class Holder extends ButterKnifeViewHolder {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.subtitle)
        TextView subtitle;

        @BindView(R.id.image)
        ImageView image;

        public Holder(View itemView, RecyclerAdapterNotifier adapter) {
            super(itemView, adapter);
        }

        @OnClick(R.id.remove)
        public void onRemoveItem() {
            Bundle data = new Bundle();
            data.putString(KEY_EVENT, EVENT_REMOVE);

            sendEvent(data);
        }
    }

    @Override
    public int compareTo(@NonNull AdapterItem otherItem) {
        if (otherItem.getClass() != getClass())
            return -1;

        UploadItem other = (UploadItem) otherItem;

        if (mType > other.mType)
            return 1;

        if (mType < other.mType)
            return -1;

        return mTitle.compareTo(other.mTitle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadItem)) return false;

        UploadItem that = (UploadItem) o;

        return that.mTitle.equals(mTitle);

    }

    @Override
    public int hashCode() {
        int result = mTitle.hashCode();
        result = 31 * result + mSubtitle.hashCode();
        return result;
    }

    public int getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubtitle() {
        return mSubtitle;
    }
}
