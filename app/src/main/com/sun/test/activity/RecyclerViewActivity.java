package com.sun.test.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.sun.test.R;
import com.sun.test.adapter.MultiRecyclerViewAdapter;
import com.sun.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sunhzchen on 2017/1/4.
 * RecyclerView测试
 */

public class RecyclerViewActivity extends CommonActivity {

    private ArrayList<HashMap<String, Object>> mData;
    private RecyclerView mRecycleView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        initView();
        initData();
        initAdapter();
    }

    private void initView() {
        mRecycleView = (RecyclerView) findViewById(R.id.recycler_view);
    }

    private void initData() {
        mData = new ArrayList<>();
        HashMap<String, Object> hashMapOne = new HashMap<>();
        hashMapOne.put("type", Constants.ONE);
        hashMapOne.put("name", "sun");
        hashMapOne.put("phone", "1234");
        HashMap<String, Object> hashMapTwo = new HashMap<>();
        hashMapTwo.put("type", Constants.ZERO);
        hashMapTwo.put("name", "ada");
        hashMapTwo.put("phone", "2134");
        HashMap<String, Object> hashMapThree = new HashMap<>();
        hashMapThree.put("type", Constants.ONE);
        hashMapThree.put("name", "hope");
        hashMapThree.put("phone", "3214");
        HashMap<String, Object> hashMapFour = new HashMap<>();
        hashMapFour.put("type", Constants.ZERO);
        hashMapFour.put("name", "owen");
        hashMapFour.put("phone", "4321");
        mData.add(hashMapOne);
        mData.add(hashMapTwo);
        mData.add(hashMapThree);
        mData.add(hashMapFour);
    }

    private void initAdapter() {
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        //只有一种类型的item
//        NormalRecyclerViewAdapter adapter = new NormalRecyclerViewAdapter(RecyclerViewActivity
// .this, mData);
//        mRecycleView.setAdapter(adapter);
//        adapter.setItemClickListener(new NormalRecyclerViewAdapter
// .OnRecyclerViewItemClickListener() {
//            @Override
//            public void onItemClick(View view, HashMap<String, String> data) {
//                Toast.makeText(RecyclerViewActivity.this, "name = " + data.get("name") + ";
// phone = " + data.get("phone"), Toast.LENGTH_SHORT).show();
//            }
//        });

        //多种类型的item
        MultiRecyclerViewAdapter adapter = new MultiRecyclerViewAdapter(this, mData);
        mRecycleView.setAdapter(adapter);
        adapter.setItemClickListener((view, data) -> Toast.makeText(RecyclerViewActivity.this, "name = " + data.get("name") + ";phone " +
                "= " + data.get("phone"), Toast.LENGTH_SHORT).show());
    }
}
