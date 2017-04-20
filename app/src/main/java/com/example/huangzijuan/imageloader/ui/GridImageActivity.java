package com.example.huangzijuan.imageloader.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.huangzijuan.imageloader.R;
import com.example.huangzijuan.imageloader.bean.FolderBean;
import com.example.huangzijuan.imageloader.ui.base.ActivityCollector;
import com.example.huangzijuan.imageloader.ui.base.BaseActivity;
import com.example.huangzijuan.imageloader.ui.base.PermissionListener;
import com.example.huangzijuan.imageloader.util.ToastUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GridImageActivity extends BaseActivity {
    private static final int DATA_LOADED = 0x110;

    private TextView mDirName;
    private TextView mDirCount;
    private RelativeLayout mBottomContainer;
    private GridView mGridView;
    private List<String> mImgs;
    private ImageAdapter mImgAdapter;

    private File mCurrentDir;
    private int mMaxCount = 300;

    private List<FolderBean> mFolderBeans = new ArrayList<>();
    private ProgressDialog mProgressDialog;

    private ListImgPopupWindow mDirPopupWindow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();
                data2View();
                initDirPopupWindow();
            }
        }
    };

    private void initDirPopupWindow() {
        mDirPopupWindow = new ListImgPopupWindow(this, mFolderBeans);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        mDirPopupWindow.setOnDirSelectListener(new ListImgPopupWindow.OnDirSelectListener() {
            @Override
            public void onSelect(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String filename) {
                        if (filename.endsWith(".jpg") ||
                                filename.endsWith(".jpeg") ||
                                filename.endsWith(".png")) {
                            return true;
                        }
                        return false;
                    }
                }));
                mImgAdapter = new ImageAdapter(GridImageActivity.this, mImgs,
                        mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);

                mDirCount.setText(String.valueOf(mImgs.size()));
                mDirName.setText(folderBean.getName());
                mDirPopupWindow.dismiss();
            }
        });
    }

    /*
    * 内容区域变亮
    */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    /*
    * 内容区域变暗
    */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_image);
        ActivityCollector.addActivity(this);

        requestRunTimePermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                new PermissionListener() {
                    @Override
                    public void granted() {
                        initView();
                        initDatas();
                        initEvent();
                    }

                    @Override
                    public void denied(@NonNull List<String> deniedPermissions) {
                        ToastUtil.show("您禁用了权限" + deniedPermissions);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    private void initView() {
        mGridView = findView(R.id.grid_view);
        mDirName = findView(R.id.dir_name);
        mDirCount = findView(R.id.dir_count);
        mBottomContainer = findView(R.id.bottom_container);
    }

    private void initEvent() {
        mBottomContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                mDirPopupWindow.showAsDropDown(mBottomContainer, 0, 0);
                lightOff();
            }
        });
    }

    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);
        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    /*
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载……");

        new Thread() {
            @Override
            public void run() {
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = GridImageActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImageUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or " +
                                MediaStore.Images.Media.MIME_TYPE + "= ?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths = new HashSet<>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }

                    String dirPath = parentFile.getAbsolutePath();

                    FolderBean folderBean = new FolderBean();
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImagePath(path);
                    }

                    if (parentFile.list() == null) {
                        continue;
                    }

                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String filename) {
                            if (filename.endsWith(".jpg") ||
                                    filename.endsWith(".jpeg") ||
                                    filename.endsWith(".png")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;

                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }

                cursor.close();
                // 通知handler扫描图片完成
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();
    }
}
