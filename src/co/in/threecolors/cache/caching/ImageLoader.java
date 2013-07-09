package co.in.threecolors.cache.caching;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;
import co.in.threecolors.BuildConfig;
import co.in.threecolors.ui.util.Utility;

/**
 * ImageLoader deals with fetching the bitmap from either the cache or loading
 * it over HTTP and then setting this into the passed ImageView. Uses background
 * threads to fetch/cache bitmaps. Pass a placeholder image (if needed) to be
 * used while the image is being fetched.
 * 
 * @author dhavalmotghare@gmail.com
 */
public class ImageLoader {
    private static final String TAG = ImageLoader.class.getSimpleName();

    private static final int TASK_CLEAR_CACHE = 0;
    private static final int TASK_FLUSH_CACHE = 1;
    private static final int TASK_CLOSE_CACHE = 2;
    private static final int TASK_INIT_CACHE = 3;

    private static final String HTTP_CACHE_DIR = "http";

    private static final int DEFAULT_IMAGE_HEIGHT = 1024;
    private static final int DEFAULT_IMAGE_WIDTH = 1024;

    private static final int MAX_THUMBNAIL_BYTES = 70 * 1024; // 70KB
    private static final int HTTP_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int IO_BUFFER_SIZE_BYTES = 4 * 1024; // 4KB

    private static final int DISK_CACHE_INDEX = 0;
    private static final int FADE_IN_TIME = 200;

    /**
     * Create an ImageLoader specifying max image loading width/height.
     * 
     * @param context
     * @param imageWidth
     * @param imageHeight
     */
    public ImageLoader(Context context, int imageWidth, int imageHeight) {
        mResources = context.getResources();
        init(context, imageWidth, imageHeight);
    }

    /**
     * Create an ImageLoader using defaults.
     * 
     * @param context
     */
    public ImageLoader(Context context) {
        mResources = context.getResources();
        init(context, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    private void init(Context context, int imageWidth, int imageHeight) {
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
        mHttpCacheDir = ImageCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
        if (!mHttpCacheDir.exists()) {
            mHttpCacheDir.mkdirs();
        }
    }

    public void loadThumbnailImage(String key, ImageView imageView, Bitmap loadingBitmap) {
        loadImage(new ImageData(key, ImageData.IMAGE_TYPE_THUMBNAIL), imageView, loadingBitmap);
    }

    public void loadThumbnailImage(String key, ImageView imageView, int resId) {
        loadImage(new ImageData(key, ImageData.IMAGE_TYPE_THUMBNAIL), imageView, resId);
    }

    public void loadImage(String key, ImageView imageView, Bitmap loadingBitmap) {
        loadImage(new ImageData(key, ImageData.IMAGE_TYPE_NORMAL), imageView, loadingBitmap);
    }

    public void loadImage(String key, ImageView imageView, int resId) {
        loadImage(new ImageData(key, ImageData.IMAGE_TYPE_NORMAL), imageView, resId);
    }

    /**
     * Loads the specified image into an ImageView. A memory and disk cache will
     * be used if an ImageCache has been set using ImageLoader#addImageCache. If
     * the image is found in the memory cache, it is set immediately, otherwise
     * an AsyncTask will be created to asynchronously load the bitmap.
     * 
     * @param data
     *            The URL of the image to download.
     * @param imageView
     *            The ImageView to bind the fetched image to.
     * @param resId
     *            Resource of placeholder bitmap while the image loads.
     */
    private void loadImage(ImageData data, ImageView imageView, int resId) {
        if (!loadingBitmaps.containsKey(resId)) {
            // Store the loaded bitmap, so we decode it only once.
            loadingBitmaps.put(resId, BitmapFactory.decodeResource(mResources, resId));
        }
        loadImage(data, imageView, loadingBitmaps.get(resId));
    }

    /**
     * Load the specified image into an ImageView. A memory and disk cache will
     * be used if an ImageCache has been set using ImageLoader#addImageCache. If
     * the image is found in the memory cache, it is set immediately, otherwise
     * an AsyncTask will be created to asynchronously load the bitmap.
     * 
     * @param data
     *            The URL of the image to download.
     * @param imageView
     *            The ImageView to bind the fetched image to.
     */
    public void loadImage(ImageData data, ImageView imageView, Bitmap loadingBitmap) {
        if (data == null)
            return;

        Bitmap bitmap = null;

        if (mImageCache != null) {
            bitmap = mImageCache.getObjectFromMemCache(String.valueOf(data));
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
            imageView.setImageBitmap(bitmap);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final ReferenceDrawable asyncDrawable = new ReferenceDrawable(mResources, loadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            if (Utility.hasHoneycomb()) {
                // On HC+ we execute on a dual thread executor.
                task.executeOnExecutor(Utility.getDualThreadExecutor(2), data);
            } else {
                // Otherwise PRE-HC the default is a thread pool executor
                task.execute(data);
            }
        }
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is
     * running.
     * 
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    /**
     * Adds an ImageCache to this worker in the background (to prevent disk
     * access on UI thread).
     * 
     * @param fragmentManager
     *            The FragmentManager to initialize and add the cache
     * @param cacheParams
     *            The cache parameters to use
     */
    public void addImageCache(FragmentManager fragmentManager, ObjectCache.FileCacheParams cacheParams) {
        mImageCacheParams = cacheParams;
        setImageCache(ImageCache.getCache(fragmentManager, mImageCacheParams));
        new CacheAsyncTask().execute(TASK_INIT_CACHE);
    }

    /**
     * Adds an ImageCache to this worker in the background (to prevent disk
     * access on UI thread) using default cache parameters.
     * 
     * @param fragmentActivity
     *            The FragmentActivity to initialize and add the cache
     */
    public void addImageCache(FragmentActivity fragmentActivity) {
        addImageCache(fragmentActivity.getSupportFragmentManager(), new ObjectCache.FileCacheParams(fragmentActivity));
    }

    /**
     * Sets the ImageCache object to use with this ImageWorker. Usually you will
     * not need to call this directly, instead use ImageLoader#addImageCache
     * which will create and add the ImageCache object in a background thread
     * (to ensure no disk access on the main/UI thread).
     * 
     * @param imageCache
     */
    public void setImageCache(ImageCache imageCache) {
        mImageCache = imageCache;
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the
     * background thread.
     */
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    /**
     * Setting this to true will signal the working tasks to exit processing at
     * the next chance. This helps finish up pending work when the activity is
     * no longer in the foreground and completing the tasks is no longer useful.
     * 
     * @param exitTasksEarly
     */
    public void exitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    /**
     * Cancels any pending work attached to the provided ImageView.
     * 
     * @param imageView
     */
    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapWorkerTask.mData);
            }
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mData;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView
     *            Any imageView
     * @return Retrieve the currently active work task (if any) associated with
     *         this imageView. null if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof ReferenceDrawable) {
                final ReferenceDrawable referenceDrawable = (ReferenceDrawable) drawable;
                return referenceDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work
     * is in progress. Contains a reference to the actual worker task, so that
     * it can be stopped if a new binding is required, and makes sure that only
     * the last started worker process can bind its result, independently of the
     * finish order.
     */
    private static class ReferenceDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public ReferenceDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Called when the processing is complete and the final bitmap should be set
     * on the ImageView.
     * 
     * @param imageView
     * @param bitmap
     */
    @SuppressWarnings({ "deprecation" })
    protected void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            // Use TransitionDrawable to fade in
            final TransitionDrawable td = new TransitionDrawable(new Drawable[] {
                    new ColorDrawable(android.R.color.transparent), new BitmapDrawable(mResources, bitmap) });
            // no inspection deprecation
            imageView.setBackgroundDrawable(imageView.getDrawable());
            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * Pause any ongoing background work. This can be used as a temporary
     * measure to improve performance. For example background work could be
     * paused when a ListView or GridView is being scrolled. If work is paused,
     * be sure setPauseWork(false) is called again before your fragment or
     * activity is destroyed (for example during
     * {@link android.app.Activity#onPause()}), or there is a risk the
     * background thread will never finish.
     */
    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer) params[0]) {
            case TASK_CLEAR_CACHE:
                clearCacheInternal();
                break;
            case TASK_INIT_CACHE:
                initDiskCacheInternal();
                break;
            case TASK_FLUSH_CACHE:
                flushCacheInternal();
                break;
            case TASK_CLOSE_CACHE:
                closeCacheInternal();
                break;
            }
            return null;
        }
    }

    protected void initDiskCacheInternal() {
        if (mImageCache != null) {
            mImageCache.initDiskCache();
        }
        initHttpDiskCache();
    }

    protected void clearCacheInternal() {
        if (mImageCache != null) {
            mImageCache.clearCache();
        }

        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null && !mHttpDiskCache.isClosed()) {
                try {
                    mHttpDiskCache.delete();
                    Log.d(TAG, "HTTP cache cleared");
                } catch (IOException e) {
                    Log.e(TAG, "clearCacheInternal - " + e);
                }
                mHttpDiskCache = null;
                mHttpDiskCacheStarting = true;
                initHttpDiskCache();
            }
        }
    }

    protected void flushCacheInternal() {
        if (mImageCache != null) {
            mImageCache.flush();
        }

        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    mHttpDiskCache.flush();
                    Log.d(TAG, "HTTP cache flushed");
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    protected void closeCacheInternal() {
        if (mImageCache != null) {
            mImageCache.close();
            mImageCache = null;
        }

        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    if (!mHttpDiskCache.isClosed()) {
                        mHttpDiskCache.close();
                        mHttpDiskCache = null;
                        Log.d(TAG, "HTTP cache closed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "closeCacheInternal - " + e);
                }
            }
        }
    }

    public void clearCache() {
        new CacheAsyncTask().execute(TASK_CLEAR_CACHE);
    }

    public void flushCache() {
        new CacheAsyncTask().execute(TASK_FLUSH_CACHE);
    }

    public void closeCache() {
        new CacheAsyncTask().execute(TASK_CLOSE_CACHE);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTask background thread.
     * 
     * @param key
     *            The key to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(String key, int type) {
        Log.d(TAG, "processBitmap - " + key);

        if (type == ImageData.IMAGE_TYPE_NORMAL) {
            /** Process a regular, full sized bitmap */
            return processNormalBitmap(key);
        } else if (type == ImageData.IMAGE_TYPE_THUMBNAIL) {
            /** Process a smaller, thumb-nail bitmap */
            return processThumbnailBitmap(key);
        }
        return null;
    }

    protected Bitmap processBitmap(Object key) {
        final ImageData imageData = (ImageData) key;
        return processBitmap(imageData.mKey, imageData.mType);
    }

    /**
     * Download and resize a normal sized remote bitmap from a HTTP URL using a
     * HTTP cache.
     * 
     * @param urlString
     *            The URL of the image to download
     * @return The scaled bitmap
     */
    private Bitmap processNormalBitmap(String urlString) {
        final String key = ImageCache.hashKeyForDisk(urlString);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        DiskLruCache.Snapshot snapshot;
        synchronized (mHttpDiskCacheLock) {
            // Wait for disk cache to initialize
            while (mHttpDiskCacheStarting) {
                try {
                    mHttpDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }

            if (mHttpDiskCache != null) {
                try {
                    snapshot = mHttpDiskCache.get(key);
                    if (snapshot == null) {
                        Log.d(TAG, "processBitmap, not found in http cache, downloading...");
                        DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
                        if (editor != null) {
                            if (downloadUrlToStream(urlString, editor.newOutputStream(DISK_CACHE_INDEX))) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        snapshot = mHttpDiskCache.get(key);
                    }
                    if (snapshot != null) {
                        fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                        fileDescriptor = fileInputStream.getFD();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "processBitmap - " + e);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "processBitmap - " + e);
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        Bitmap bitmap = null;
        if (fileDescriptor != null) {
            bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, mImageWidth, mImageHeight);
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
            }
        }
        return bitmap;
    }

    /**
     * Download a thumb-nail sized remote bitmap from a HTTP URL. No HTTP
     * caching is done ImageCache that this eventually gets passed to will do
     * it's own disk caching.
     * 
     * @param urlString
     *            The URL of the image to download
     * @return The bitmap
     */
    private Bitmap processThumbnailBitmap(String urlString) {
        final byte[] bitmapBytes = downloadBitmapToMemory(urlString, MAX_THUMBNAIL_BYTES);
        if (bitmapBytes != null) {
            return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        }
        return null;
    }

    /**
     * Download a bitmap from a URL, write it to a disk and return the File
     * pointer. This implementation uses a simple disk cache.
     * 
     * @param urlString
     *            The URL to fetch
     * @param maxBytes
     *            The maximum number of bytes to read before returning null to
     *            protect against OutOfMemory exceptions.
     * @return A File pointing to the fetched bitmap
     */
    public static byte[] downloadBitmapToMemory(String urlString, int maxBytes) {
        Log.d(TAG, "downloadBitmapToMemory - downloading - " + urlString);

        HttpURLConnection urlConnection = null;
        ByteArrayOutputStream out = null;
        InputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE_BYTES);
            out = new ByteArrayOutputStream(IO_BUFFER_SIZE_BYTES);

            final byte[] buffer = new byte[128];
            int total = 0;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                total += bytesRead;
                if (total > maxBytes) {
                    return null;
                }
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmapToMemory - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (final IOException e) {
            }
        }
        return null;
    }

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     * 
     * @param urlString
     *            The URL to fetch
     * @param outputStream
     *            The outputStream to write to
     * @return true if successful, false otherwise
     */
    public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE_BYTES);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE_BYTES);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
            }
        }
        return false;
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested
     * width and height.
     * 
     * @param fileDescriptor
     *            The file descriptor to read from
     * @param reqWidth
     *            The requested width of the resulting bitmap
     * @param reqHeight
     *            The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    /**
     * Calculate an inSampleSize for use in a
     * {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This
     * implementation calculates the closest inSampleSize that will result in
     * the final decoded bitmap having a width and height equal to or larger
     * than the requested width and height. This implementation does not ensure
     * a power of 2 is returned for inSampleSize which can be faster when
     * decoding but results in a larger bitmap which isn't as useful for caching
     * purposes.
     * 
     * @param options
     *            An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth
     *            The requested width of the resulting bitmap
     * @param reqHeight
     *            The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    private void initHttpDiskCache() {
        if (!mHttpCacheDir.exists()) {
            mHttpCacheDir.mkdirs();
        }
        synchronized (mHttpDiskCacheLock) {
            if (ImageCache.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE) {
                try {
                    mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1, HTTP_CACHE_SIZE);
                    Log.d(TAG, "HTTP cache initialized");
                } catch (IOException e) {
                    mHttpDiskCache = null;
                }
            }
            mHttpDiskCacheStarting = false;
            mHttpDiskCacheLock.notifyAll();
        }
    }

    /**
     * AsyncTask that will load the image either from the disk-cache or download
     * it.
     * 
     * @author dhaval.motghare@autodesk.com
     */
    public class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
        public BitmapWorkerTask(ImageView imageView) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(Object... params) {
            Log.d(TAG, "doInBackground - starting work");

            mData = params[0];
            final String dataString = String.valueOf(mData);
            Bitmap bitmap = null;

            // Wait here if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = mImageCache.getObjectFromDiskCache(dataString);
            }

            if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = processBitmap(params[0]);
            }

            /**
             * If the bitmap was processed and the image cache is available,
             * then add the processed bitmap to the cache for future use.
             */
            if (bitmap != null && mImageCache != null) {
                mImageCache.addObjectToCache(dataString, bitmap);
            }

            Log.d(TAG, "doInBackground - finished work");
            return bitmap;
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // if cancel was called on this task or the "exit early" flag is set
            // then we're done
            if (isCancelled() || mExitTasksEarly) {
                bitmap = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (bitmap != null && imageView != null) {
                Log.d(TAG, "onPostExecute - setting bitmap");
                setImageBitmap(imageView, bitmap);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the
         * ImageView's task still points to this task as well. Returns null
         * otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = mImageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = ImageLoader.getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }

        private Object mData;
        private final WeakReference<ImageView> mImageViewReference;

    }

    private static class ImageData {
        public static final int IMAGE_TYPE_THUMBNAIL = 0;
        public static final int IMAGE_TYPE_NORMAL = 1;
        public String mKey;
        public int mType;

        public ImageData(String key, int type) {
            mKey = key;
            mType = type;
        }

        @Override
        public String toString() {
            return mKey;
        }
    }

    protected ImageCache mImageCache;
    protected ObjectCache.FileCacheParams mImageCacheParams;

    protected Bitmap mLoadingBitmap;
    protected boolean mFadeInBitmap = true;
    protected boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();
    private final Hashtable<Integer, Bitmap> loadingBitmaps = new Hashtable<Integer, Bitmap>(2);

    protected Resources mResources;
    private boolean mExitTasksEarly = false;

    protected int mImageWidth;
    protected int mImageHeight;
    private File mHttpCacheDir;
    private DiskLruCache mHttpDiskCache;
    private boolean mHttpDiskCacheStarting = true;
    private final Object mHttpDiskCacheLock = new Object();

}
