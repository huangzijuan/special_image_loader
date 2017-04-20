package com.example.huangzijuan.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ImageLoader {
    private static ImageLoader sInstance;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String, Bitmap> mLruCache;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;

    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;

    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    public enum Type {
        LIFO, FIFO;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        // 后台轮询线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // 线程池去取一个任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /*
    * 从任务队列取出一个方法
    */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (sInstance == null) {
            synchronized (ImageLoader.class) {
                if (sInstance == null) {
                    sInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return sInstance;
    }

    /*
    * 根据path为imageview设置图片
    */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);

        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // 获取得到的图片，为Imageview回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;

                    //将path与getTag存储路径进行比较
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreshBitmap(bm, imageView, path);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    // 1.获得图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    // 2.压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);
                    // 3.把图片加入到缓存
                    addBitmapToLruCache(path, bm);

                    refreshBitmap(bm, imageView, path);

                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(Bitmap bm, ImageView imageView, String path) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.imageView = imageView;
        holder.path = path;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /*
    * 将图片加入到LruCache
    */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /*
    * 根据imageview获取适当的压缩的宽和高
    */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetric = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        //int width = (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getWidth());
        int width = imageView.getWidth(); //获取imageview的实际宽度
        if (width <= 0) {
            width = lp.width;
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, "mMaxWidth"); //检查最大值
        }
        if (width <= 0) {
            width = displayMetric.widthPixels;
        }

        int height = imageView.getHeight(); //获取imageview的实际高度
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight"); //检查最大值
        }
        if (height <= 0) {
            height = displayMetric.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /*
    * 通过反射获取imageview的某个属性值
    */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);

        try {
            if (mPoolThreadHandler == null)
                mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    /*
    * 根据图片显示的宽高对图片进行压缩
    */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /*
    * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
    */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);

        }
        return inSampleSize;
    }

    private class ImageSize {
        int width;
        int height;
    }

    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
