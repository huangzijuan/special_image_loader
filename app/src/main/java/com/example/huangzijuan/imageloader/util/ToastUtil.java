package com.example.huangzijuan.imageloader.util;

import android.text.TextUtils;
import android.widget.Toast;

import com.example.huangzijuan.imageloader.App;


public class ToastUtil {
    public static void show(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(App.getInstance(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(App.getInstance(), msg, Toast.LENGTH_LONG).show();
    }
}
