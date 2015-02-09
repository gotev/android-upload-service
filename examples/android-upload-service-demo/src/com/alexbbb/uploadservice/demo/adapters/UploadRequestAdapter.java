package com.alexbbb.uploadservice.demo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.demo.R;
import com.alexbbb.uploadservice.demo.views.ExpandableHeightGridView;

import java.util.ArrayList;

/**
 * Created by Divish on 1/15/2015.
 */
public class UploadRequestAdapter extends BaseAdapter{
    private Context context;
    ArrayList<UploadRequest> uploadRequests;

    public UploadRequestAdapter(Context context, ArrayList<UploadRequest> uploadRequests) {
        this.context = context;
        this.uploadRequests = uploadRequests;
    }

    @Override
    public int getCount() {
        return uploadRequests.size();
    }

    @Override
    public Object getItem(int i) {
        return uploadRequests.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.upload_request_item, null);
        }
        UploadRequest uploadRequest = (UploadRequest) getItem(i);
        TextView name = (TextView) convertView.findViewById(R.id.request_id);
        ExpandableHeightGridView gridView = (ExpandableHeightGridView) convertView.findViewById(R.id.gridView);
        name.setText(uploadRequest.getUploadId());
        ImageGridAdapter imageGridAdapter = new ImageGridAdapter(context, uploadRequest.getFilesToUpload() );
        gridView.setAdapter(imageGridAdapter);

        if (gridView.isExpanded() == false) {
            gridView.setExpanded(true);
        }

        return convertView;
    }
}
