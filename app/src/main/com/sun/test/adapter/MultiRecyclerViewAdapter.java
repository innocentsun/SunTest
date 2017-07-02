package com.sun.test.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sun.test.R;
import com.sun.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sunhzchen on 2016/8/25.
 * 复杂版RecyclerView
 */
public class MultiRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private static final int TYPE_CARDVIEW = 0;
    private static final int TYPE_TEXTVIEW = 1;

    private Context mContext;
    private ArrayList<HashMap<String, Object>> mData;
    private OnRecyclerViewItemClickListener mListener;

    public interface OnRecyclerViewItemClickListener {
        void onItemClick(View view, HashMap<String, String> data);
    }

    public MultiRecyclerViewAdapter(Context context, ArrayList<HashMap<String, Object>> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public int getItemViewType(int position) {
        return (int)(mData.get(position).get("type")) == Constants.ZERO ? TYPE_CARDVIEW : TYPE_TEXTVIEW;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view;
        RecyclerView.ViewHolder viewHolder;
        if(i == TYPE_CARDVIEW){
            view = LayoutInflater.from(mContext).inflate(R.layout.listitem_card_view, viewGroup, false);
            viewHolder = new CardViewHolder(view);
        }else {
            view = LayoutInflater.from(mContext).inflate(R.layout.listitem_text_view, viewGroup, false);
            viewHolder = new TextViewHolder(view);
        }
        view.setOnClickListener(this);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if(viewHolder instanceof CardViewHolder){
            ((CardViewHolder)viewHolder).mName.setText((String)mData.get(i).get("name"));
            ((CardViewHolder)viewHolder).mPhone.setText((String)mData.get(i).get("phone"));
        }else if(viewHolder instanceof TextViewHolder){
            ((TextViewHolder)viewHolder).mText.setText((String)mData.get(i).get("name"));
        }
        viewHolder.itemView.setTag(mData.get(i));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClick(v, (HashMap<String, String>) v.getTag());
        }
    }

    public void setItemClickListener(OnRecyclerViewItemClickListener listener) {
        mListener = listener;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mName;
        private TextView mPhone;

        public CardViewHolder(View view) {
            super(view);
            mName = (TextView) view.findViewById(R.id.tv_cardview_name);
            mPhone = (TextView) view.findViewById(R.id.tv_cardview_phone);
            mPhone.setOnClickListener(CardViewHolder.this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.tv_cardview_phone) {
                Log.e("TAG", "phone = " + ((TextView) v).getText());
            }
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mText;

        public TextViewHolder(View view) {
            super(view);
            mText = (TextView) view.findViewById(R.id.listitem_text);
            mText.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.listitem_text) {
                Log.e("TAG", "text = " + ((TextView) v).getText());
            }
        }
    }
}
