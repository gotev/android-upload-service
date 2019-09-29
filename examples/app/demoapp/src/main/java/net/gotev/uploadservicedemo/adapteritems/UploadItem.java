package net.gotev.uploadservicedemo.adapteritems;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.gotev.recycleradapter.AdapterItem;
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
    public void bind(boolean firstTime, @NonNull UploadItem.Holder holder) {
        holder.image.setImageResource(icons[mType]);
        holder.title.setText(mTitle);
        holder.subtitle.setText(mSubtitle);
    }

    public static class Holder extends ButterKnifeViewHolder {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.subtitle)
        TextView subtitle;

        @BindView(R.id.image)
        ImageView image;

        public Holder(View itemView) {
            super(itemView);
        }

        @OnClick(R.id.remove)
        public void onRemoveItem() {
            UploadItem item = (UploadItem) getAdapterItem();

            if (item != null) {
                item.mDelegate.onRemoveUploadItem(getAdapterPosition());
            }
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
    public String diffingId() {
        return UploadItem.class.getName() + mTitle + mSubtitle;
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
