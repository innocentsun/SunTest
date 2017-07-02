package com.sun.test.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sun.test.R;

import java.util.ArrayList;

public class MainActivity extends CommonActivity {

    private ArrayList<ActivityClass> mListData;

    @Override
    protected void enterAnimation() {
    }

    @Override
    protected void exitAnimation() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
    }

    private void initData() {
        mListData = new ArrayList<>();
        mListData.add(new ActivityClass("Hello World", HelloWorldActivity.class));
        mListData.add(new ActivityClass("ScratchView", ScratchViewActivity.class));
        mListData.add(new ActivityClass("RecycleView", RecyclerViewActivity.class));
        mListData.add(new ActivityClass("ONAEnterTips", ONAEnterTipsActivity.class));
        mListData.add(new ActivityClass("TestSharedPreferences", TestSharedPreferencesActivity
                .class));

    }

    private void initView() {
        BaseAdapter adapter = new BaseAdapter() {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout
                            .listitem_main, null);
                    holder.initView(convertView);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                holder.setView(getItem(position));
                return convertView;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public String getItem(int position) {
                return mListData.get(position).mTitle;
            }

            @Override
            public int getCount() {
                return mListData.size();
            }
        };

        ListView listView = (ListView) findViewById(R.id.listView_mainActivity);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> itemClick(position));
    }

    private void itemClick(int position) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, mListData.get(position).mClass);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ActivityClass {
        private String mTitle;
        private Class<?> mClass;

        private ActivityClass(String title, Class<?> className) {
            mTitle = title;
            mClass = className;
        }
    }

    private static class ViewHolder {
        private TextView mListItemName;

        private void initView(View view) {
            mListItemName = (TextView) view.findViewById(R.id.list_item_name);
        }

        private void setView(String name) {
            mListItemName.setText(name);
        }
    }

}
