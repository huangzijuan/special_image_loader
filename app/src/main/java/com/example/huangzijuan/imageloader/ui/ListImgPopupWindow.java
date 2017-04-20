package com.example.huangzijuan.imageloader.ui;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.huangzijuan.imageloader.R;
import com.example.huangzijuan.imageloader.bean.FolderBean;
import com.example.huangzijuan.imageloader.util.ImageLoader;

import java.util.List;

public class ListImgPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mDatas;

    private OnDirSelectListener mListener;

    public interface OnDirSelectListener {
        void onSelect(FolderBean folderBean);
    }

    public void setOnDirSelectListener(OnDirSelectListener mListener) {
        this.mListener = mListener;
    }

    public ListImgPopupWindow(Context context, List<FolderBean> mDatas) {
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_albumset, null);

        this.mDatas = mDatas;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(context);
        initEvent();
    }

    private void initView(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.list_dir);
        mListView.setAdapter(new ListDirAdapter(context, mDatas));
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mListener != null) {
                    mListener.onSelect(mDatas.get(position));
                }
            }
        });
    }

    /*
    * 计算popupWindow的宽度和高度
    */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater mInflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(Context context, List<FolderBean> datas) {
            super(context, 0, datas);

            mInflater = LayoutInflater.from(context);
            mDatas = datas;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.popup_albumset_item, parent, false);

                holder.mImg = (ImageView) convertView.findViewById(R.id.dir_item_image);
                holder.mDirName = (TextView) convertView.findViewById(R.id.dir_item_name);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.dir_item_count);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FolderBean bean = getItem(position);
            holder.mImg.setImageResource(R.drawable.picture_no);

            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(bean.getFirstImagePath(), holder.mImg);
            holder.mDirName.setText(bean.getName());
            holder.mDirCount.setText(String.valueOf(bean.getCount()));
            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}
