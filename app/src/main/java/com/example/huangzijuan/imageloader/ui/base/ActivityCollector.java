package com.example.huangzijuan.imageloader.ui.base;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {
    public static List<BaseActivity> activityList = new ArrayList<BaseActivity>();

    public static void addActivity(BaseActivity activity) {
        activityList.add(activity);
    }

    public static void removeActivity(BaseActivity activity) {
        activityList.remove(activity);
    }

    public static Activity getTopActivity() {
        if (activityList.isEmpty()) {
            return null;
        } else {
            return activityList.get(activityList.size() - 1);
        }
    }

}
