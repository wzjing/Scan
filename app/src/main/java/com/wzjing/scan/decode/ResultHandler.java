package com.wzjing.scan.decode;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;

public class ResultHandler extends Handler {

    public static final int RESULT_FAIL = 0;
    public static final int RESULT_SUCCESS = 1;
    public Callback mCb;

    public void setCallback(Callback cb) {
        mCb = cb;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case RESULT_FAIL:
                if (mCb != null) mCb.onDecoded(false, null);
                break;
            case RESULT_SUCCESS:
                if (mCb != null) mCb.onDecoded(true, msg.obj.toString());
                break;
        }
    }

    public interface Callback {
        void onDecoded(boolean success, @Nullable String result);
    }
}
