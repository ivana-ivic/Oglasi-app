package com.example.ivana.oglasi.Classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ivana.oglasi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivana on 2/5/2017.
 */


public class HomeListAdapter extends ArrayAdapter {

    List list = new ArrayList();

    public HomeListAdapter(Context context, int resource) {
        super(context, resource);
    }

    static class DataHandler {
        TextView mTitle;
        TextView mText;
        ImageView mImage;
        TextView mId;
    }

    @Override
    public void add(Object object) {
        super.add(object);
        list.add(object);
    }

    @Override
    public int getCount() {
        return this.list.size();
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        return this.list.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row;
        row=convertView;
        DataHandler handler;
        if(convertView==null){
            LayoutInflater inflater=(LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.home_list_item,parent,false);
            handler=new DataHandler();
            handler.mTitle=(TextView) row.findViewById(R.id.ad_title);
            handler.mText=(TextView) row.findViewById(R.id.ad_text);
            handler.mImage=(ImageView)row.findViewById(R.id.ad_img);
            handler.mId=(TextView)row.findViewById(R.id.ad_id);
            row.setTag(handler);
        }
        else{
            handler=(DataHandler)row.getTag();
        }

        HomeListItemData itemData;
        itemData = (HomeListItemData)this.getItem(position);
        handler.mTitle.setText(itemData.getAdTitle());
        handler.mText.setText(itemData.getAdText());
        handler.mImage.setImageBitmap(itemData.getAdImage());
        handler.mId.setText(itemData.getAdId());

        return row;
    }
}