package net.gotev.uploadservicedemo.utils;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;

import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_FILE;
import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_HEADER;
import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_PARAMETER;

/**
 * @author Aleksandar Gotev
 */

public class UploadItemUtils implements UploadItem.Delegate {

    public interface ForEachDelegate {
        void onHeader(UploadItem item);
        void onParameter(UploadItem item);
        void onFile(UploadItem item);
    }

    private RecyclerAdapter mAdapter;

    public UploadItemUtils(RecyclerAdapter adapter) {
        mAdapter = adapter;
    }

    public final void forEach(ForEachDelegate delegate) {
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            AdapterItem adapterItem = mAdapter.getItemAtPosition(i);

            if (adapterItem != null && adapterItem.getClass().getClass() == UploadItem.class.getClass()) {
                UploadItem uploadItem = (UploadItem) adapterItem;

                switch (uploadItem.getType()) {
                    case TYPE_HEADER:
                        delegate.onHeader(uploadItem);
                        break;

                    case TYPE_PARAMETER:
                        delegate.onParameter(uploadItem);
                        break;

                    case TYPE_FILE:
                        delegate.onFile(uploadItem);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onRemoveUploadItem(int position) {
        mAdapter.removeItemAtPosition(position);
    }

    public void add(UploadItem item) {
        mAdapter.addOrUpdate(item);
        mAdapter.sort(true, null);
    }

    public void addHeader(String headerName, String headerValue) {
        add(new UploadItem(TYPE_HEADER, headerName, headerValue, this));
    }

    public void addParameter(String paramName, String paramValue) {
        add(new UploadItem(TYPE_PARAMETER, paramName, paramValue, this));
    }

    public void addFile(String paramName, String filePath) {
        add(new UploadItem(TYPE_FILE, paramName, filePath, this));
    }

}
