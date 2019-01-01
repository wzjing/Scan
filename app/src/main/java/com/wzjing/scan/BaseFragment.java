package com.wzjing.scan;

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {

    private final int PERMISSION_REQUEST_CODE = 0x101;
    private Runnable postAction;
    private String[] requestPermissions;

    protected void withPermission(Runnable action, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermission = true;
            for (String permission : permissions) {
                hasPermission &= ActivityCompat.checkSelfPermission(requireContext(), permission)
                        == PackageManager.PERMISSION_GRANTED;
            }
            if (hasPermission) {
                action.run();
            } else {
                requestPermissions = permissions;
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        } else {
            action.run();
        }
    }

    protected boolean onBackPressed() {
        return false;
    }

    protected void navigateTo(Fragment fragment) {
        if (getFragmentManager() != null) {
            if (getFragmentManager().getFragments().size() > 0) {
                if (getActivity() instanceof BaseActivity) {
                    getFragmentManager().beginTransaction().replace(
                            ((BaseActivity) getActivity()).getContainer(),
                            fragment,
                            fragment.getClass().getSimpleName());
                }
            }
        }
    }

    protected void popBack() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean requestResult = true;
            for (int grantResult : grantResults) {
                requestResult &= grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (requestResult && postAction != null) {
                postAction.run();
                postAction = null;
            } else {
                popBack();
            }
        }
    }
}
