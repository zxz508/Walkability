package com.example.myapp.HomeFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.myapp.R;

public class CommunityFragment extends Fragment {

    public CommunityFragment() {
        // 必须的空构造函数
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 使用一个包含 ScrollView 或 RecyclerView 的布局
        return inflater.inflate(R.layout.fragment_community, container, false);
    }
}
