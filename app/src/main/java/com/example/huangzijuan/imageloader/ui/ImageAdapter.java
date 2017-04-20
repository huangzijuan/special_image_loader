package com.example.huangzijuan.imageloader.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.huangzijuan.imageloader.R;
import com.example.huangzijuan.imageloader.util.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
    private static Set<String> mSelected = new HashSet<>();
    private String mDirPath;
    private List<String> mImgPaths;
    private LayoutInflater mInflater;
    private int mScreenWidth;

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mDirPath = dirPath;
        this.mImgPaths = mDatas;
        mInflater = LayoutInflater.from(context);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gridview, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.item_image);
            viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.item_select);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // 重置状态
        viewHolder.mImg.setImageResource(R.drawable.picture_no);
        viewHolder.mSelect.setImageResource(R.drawable.albumset_unselected);
        viewHolder.mImg.setColorFilter(null);
        viewHolder.mImg.setMaxWidth(mScreenWidth / 3);

        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(
                mDirPath + "/" + mImgPaths.get(position), viewHolder.mImg);

        final String filePath = mDirPath + "/" + mImgPaths.get(position);
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelected.contains(filePath)) {
                    mSelected.remove(filePath);
                    viewHolder.mImg.setColorFilter(null);
                    viewHolder.mSelect.setImageResource(R.drawable.albumset_unselected);
                } else {
                    mSelected.add(filePath);
                    viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.mSelect.setImageResource(R.drawable.albumset_selected);
                }
                //notifyDataSetChanged();
            }
        });

        if (mSelected.contains(filePath)) {
            viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.mSelect.setImageResource(R.drawable.albumset_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        private ImageView mImg;
        private ImageButton mSelect;
    }
}