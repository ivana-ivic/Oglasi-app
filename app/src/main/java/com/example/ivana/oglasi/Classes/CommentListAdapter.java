package com.example.ivana.oglasi.Classes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ivana.oglasi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivana on 5/2/2017.
 */

public class CommentListAdapter extends ArrayAdapter {
    List list = new ArrayList();

    public CommentListAdapter(Context context, int resource) {
        super(context, resource);
    }

    static class DataHandler {
        TextView mUser;
        TextView mTime;
        TextView mText;
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
        CommentListAdapter.DataHandler handler;
        if(convertView==null){
            LayoutInflater inflater=(LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.comments_list_item,parent,false);
            handler=new CommentListAdapter.DataHandler();
            handler.mUser=(TextView) row.findViewById(R.id.comment_user);
            handler.mTime=(TextView) row.findViewById(R.id.comment_time);
            handler.mText=(TextView) row.findViewById(R.id.comment_text);
            row.setTag(handler);
        }
        else{
            handler=(CommentListAdapter.DataHandler)row.getTag();
        }

        CommentListItemData itemData;
        itemData = (CommentListItemData)this.getItem(position);
        handler.mUser.setText(itemData.getCommentUser());
        handler.mTime.setText(itemData.getCommentTime());
        handler.mText.setText(itemData.getCommentText());

        return row;
    }
}
