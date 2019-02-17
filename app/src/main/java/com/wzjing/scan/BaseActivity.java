package com.wzjing.scan;

import android.annotation.SuppressLint;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    @IdRes
    protected int getContainer() {
        return 0;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getSupportFragmentManager() != null) {
            for (Fragment fragment : getSupportFragmentManager ().getFragments()) {
                if (fragment instanceof BaseFragment) {
                    if (((BaseFragment)fragment).onBackPressed()) {
                        break;
                    }
                }
            }
        }
    }
}
