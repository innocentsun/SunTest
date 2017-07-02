package com.sun.test.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sun.test.R;

import java.util.ArrayList;

/**
 * Created by sunhzchen on 2016/8/31.
 * 只有一个文本的listitem
 */
public class SimpleTextViewAdapter extends BaseAdapter {

    private ArrayList<String> mData;
    private Context mContext;

    public SimpleTextViewAdapter(Context context, ArrayList<String> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_text_view, parent, false);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.listitem_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.textView.setText(mData.get(position));
        return convertView;
    }

    public static class ViewHolder {
        TextView textView;
    }
}
