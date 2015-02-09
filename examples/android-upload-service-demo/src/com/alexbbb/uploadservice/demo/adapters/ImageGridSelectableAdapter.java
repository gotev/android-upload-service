package com.alexbbb.uploadservice.demo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.alexbbb.uploadservice.FileToUpload;
import com.alexbbb.uploadservice.demo.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Divish on 1/15/2015.
 */
public class ImageGridSelectableAdapter extends BaseAdapter{
    private Context context;
    List<String> fileToUploads;

    public ImageGridSelectableAdapter(Context context, List<String> fileToUploads) {
        this.context = context;
        this.fileToUploads = fileToUploads;
    }

    @Override
    public int getCount() {
        return fileToUploads.size();
    }

    @Override
    public Object getItem(int i) {
        return fileToUploads.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.image_selectable_item, null);
        }
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);


        String fileToUpload =  fileToUploads.get(i);
        Picasso.with(context).load(new File(fileToUpload)).into(imageView);




        return convertView;
    }
}
