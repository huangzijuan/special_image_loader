package com.example.huangzijuan.imageloader.bean;

public class FolderBean {
    private String dir;
    private String firstImagePath;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setFirstImagePath(String path) {
        firstImagePath = path;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }
}
