package com.sun.test.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sun.test.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sunhzchen on 2016/8/25.
 * 简单版RecyclerView
 */
public class NormalRecyclerViewAdapter extends RecyclerView.Adapter<NormalRecyclerViewAdapter.CardViewHolder> implements View.OnClickListener {

    private Context mContext;
    private ArrayList<HashMap<String, Object>> mData;
    private OnRecyclerViewItemClickListener mListener;

    public interface OnRecyclerViewItemClickListener {
        void onItemClick(View view, HashMap<String, Object> data);
    }

    public NormalRecyclerViewAdapter(Context context, ArrayList<HashMap<String, Object>> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.listitem_card_view, viewGroup, false);
        CardViewHolder cardViewHolder = new CardViewHolder(view);
        view.setOnClickListener(NormalRecyclerViewAdapter.this);
        return cardViewHolder;
    }

    @Override
    public void onBindViewHolder(CardViewHolder cardViewHolder, int i) {
        cardViewHolder.mName.setText((String)mData.get(i).get("name"));
        cardViewHolder.mPhone.setText((String)mData.get(i).get("phone"));
        cardViewHolder.itemView.setTag(mData.get(i));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClick(v, (HashMap<String, Object>) v.getTag());
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
            mPhone.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.tv_cardview_phone) {
                Log.e("TAG", "phone = " + ((TextView) v).getText());
            }
        }
    }
}
