package com.alexbbb.uploadservice.demo.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.alexbbb.uploadservice.FileToUpload;
import com.alexbbb.uploadservice.demo.R;
import com.squareup.picasso.Picasso;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Divish on 1/15/2015.
 */
public class ImageGridAdapter extends BaseAdapter{
    private Context context;
    ArrayList<FileToUpload> fileToUploads;

    public ImageGridAdapter(Context context , ArrayList<FileToUpload> fileToUploads) {
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
            convertView = LayoutInflater.from(context).inflate(R.layout.image_item, null);
        }
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        TextView progressBar = (TextView) convertView.findViewById(R.id.tv_progress);

        FileToUpload fileToUpload =  fileToUploads.get(i);
        Picasso.with(context).load(fileToUpload.getFile()).into(imageView);

        progressBar.setText(fileToUpload.getProgress()+ "%");



        return convertView;
    }
}
