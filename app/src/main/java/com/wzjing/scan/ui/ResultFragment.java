package com.wzjing.scan.ui;

import android.os.Bundle;

import com.wzjing.scan.BaseFragment;

public class ResultFragment extends BaseFragment {

    private static final String ARG_CONTENT = "argument_content";

    public static ResultFragment newInstance(String content) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_CONTENT, content);
        ResultFragment instance = new ResultFragment();
        instance.setArguments(arguments);
        return instance;
    }

}
