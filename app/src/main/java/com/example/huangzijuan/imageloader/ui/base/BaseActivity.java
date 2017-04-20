package com.example.huangzijuan.imageloader.ui.base;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    // 运行时权限
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static PermissionListener mPermissionListener;

    protected <T> T findView(int id) {
        return (T) findViewById(id);
    }

    public static void requestRunTimePermission(@NonNull String[] permissions, @NonNull PermissionListener permissionListener) {
        mPermissionListener = permissionListener;
        Activity topActivity = ActivityCollector.getTopActivity();
        if (topActivity == null) {
            return;
        }

        List<String> requestPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(topActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(permission);
            }
        }

        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(topActivity,
                    requestPermissions.toArray(new String[requestPermissions.size()]), PERMISSIONS_REQUEST_CODE);
        } else {
            permissionListener.granted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    List<String> deniedPermissions = new ArrayList<>();
                    for (int i = 0; i < grantResults.length; i++) {
                        String permission = permissions[i];
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED && !TextUtils.isEmpty(permission)) {
                            deniedPermissions.add(permission);
                        }
                    }

                    if (!deniedPermissions.isEmpty()) {
                        mPermissionListener.denied(deniedPermissions);
                    } else {
                        mPermissionListener.granted();
                    }
                }
                break;
            default:
                break;
        }
    }
}
