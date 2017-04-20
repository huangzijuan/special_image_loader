package com.example.huangzijuan.imageloader.ui.base;

import android.support.annotation.NonNull;

import java.util.List;

public interface PermissionListener {
    void granted();
    void denied(@NonNull List<String> deniedPermissions);
}
