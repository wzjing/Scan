package com.wzjing.scan.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wzjing.scan.BaseFragment;
import com.wzjing.scan.R;

public class ResultFragment extends BaseFragment {

    private static final String ARG_CONTENT = "argument_content";

    public static ResultFragment newInstance(String content) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_CONTENT, content);
        ResultFragment instance = new ResultFragment();
        instance.setArguments(arguments);
        return instance;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}
