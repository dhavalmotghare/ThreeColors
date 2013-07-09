package co.in.threecolors.cache.caching;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;
import co.in.threecolors.ui.util.Utility;

/**
 * Abstract class with generic functions for object caching, implementation for
 * handling specific objects is delegated to concrete classes. Example - for
 * handling bitmap objects implement the
 * getSpecificObjectFromDiskCache(InputStream inputStream) and
 * addSpecificObjectToCache(T object, OutputStream out) for conversion to and
 * from bitmaps.
 * 
 * @author dhavalmotghare@gmail.com
 * 
 * @param <T>
 *            - The type of object to cache.
 */
public abstract class ObjectCache<T> {
    private static final String TAG = ObjectCache.class.getSimpleName();

    /** Default disk cache size */
    protected static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    protected static final int MAX_MEM_CACHE_SIZE = 20 * 1024 * 1024; // 20MB

    /** Default memory cache size as a percent of device memory class */
    protected static final float DEFAULT_MEM_CACHE_PERCENT = 0.15f;

    /** Default disk cache directory name */
    protected static final String DEFAULT_DISK_CACHE_DIR = "ObjectCache";

    protected static final int DISK_CACHE_INDEX = 0;

    protected DiskLruCache mDiskLruCache;
    protected LruCache<String, T> mMemoryCache;
    protected FileCacheParams mCacheParams;
    protected final Object mDiskCacheLock = new Object();
    protected boolean mDiskCacheStarting = true;

    /**
     * Create a new ObjectCache object using the specified parameters.
     * 
     * @param cacheParams
     *            The cache parameters to use to initialize the cache
     */
    protected ObjectCache(FileCacheParams cacheParams) {
        init(cacheParams);
    }

    /**
     * Create a new ObjectCache object using the default parameters.
     * 
     * @param context
     *            The context to use
     */
    protected ObjectCache(Context context) {
        init(new FileCacheParams(context));
    }

    /**
     * Create a new ObjectCache object using the passed cacheDir name and rest
     * default parameters.
     * 
     * @param context
     *            The context to use
     * @param cacheDir
     *            Cache directory name
     */
    protected ObjectCache(Context context, String cacheDir) {
        init(new FileCacheParams(context, cacheDir));
    }

    /**
     * Initialize the cache, providing all parameters.
     * 
     * @param cacheParams
     *            The cache parameters to initialize the cache
     */
    private void init(FileCacheParams cacheParams) {
        mCacheParams = cacheParams;

        // Set up memory cache
        if (mCacheParams.memoryCacheEnabled) {
            Log.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");
            mMemoryCache = new LruCache<String, T>(mCacheParams.memCacheSize) {
                /**
                 * Measure item size in kilobytes
                 */
                @Override
                protected int sizeOf(String key, T object) {
                    final int fileSize = getFileSize(object) / 1024;
                    return fileSize == 0 ? 1 : fileSize;
                }
            };
        }

        /**
         * By default the disk cache is not initialized here as it should be
         * initialized on a separate thread due to disk access.
         */
        if (cacheParams.initDiskCacheOnCreate) {
            // Set up disk cache
            initDiskCache();
        }
    }

    /**
     * Initializes the disk cache. Note that this includes disk access so this
     * should not be executed on the main/UI thread. By default an ObjectCache
     * does not initialize the disk cache when it is created, instead you should
     * call initDiskCache() to initialize it on a background thread.
     */
    public void initDiskCache() {
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                File diskCacheDir = mCacheParams.diskCacheDir;
                if (mCacheParams.diskCacheEnabled && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs();
                    }
                    if (getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
                        try {
                            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, mCacheParams.diskCacheSize);
                            Log.d(TAG, "Disk cache initialized");
                        } catch (final IOException e) {
                            mCacheParams.diskCacheDir = null;
                            Log.e(TAG, "initDiskCache - " + e);
                        }
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }

    /**
     * Adds a Object to both memory and disk cache.
     * 
     * @param data
     *            Unique identifier for the object to store
     * @param object
     *            The object to store
     */
    public void addObjectToCache(String data, T object) {
        if (data == null || object == null) {
            return;
        }

        // Add to memory cache
        if (mMemoryCache != null && mMemoryCache.get(data) == null) {
            mMemoryCache.put(data, object);
        }

        synchronized (mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            addSpecificObjectToCache(object, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "addObjectToCache - " + e);
                } catch (Exception e) {
                    Log.e(TAG, "addObjectToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * Adds a Object to both memory and disk cache.
     * 
     * @param data
     *            Unique identifier for the file to store
     * @param object
     *            The object to store
     */
    public abstract void addSpecificObjectToCache(T object, OutputStream out);

    /**
     * Get from memory cache.
     * 
     * @param data
     *            Unique identifier for which item to get
     * @return The Object if found in cache, null otherwise
     */
    public T getObjectFromMemCache(String data) {
        if (mMemoryCache != null) {
            final T object = mMemoryCache.get(data);
            if (object != null) {
                Log.d(TAG, "Memory cache hit");
                return object;
            }
        }
        return null;
    }

    /**
     * Get from disk cache.
     * 
     * @param data
     *            Unique identifier for which item to get
     * @return The object if found in cache, null otherwise
     */
    public T getObjectFromDiskCache(String data) {
        final String key = hashKeyForDisk(data);
        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        Log.d(TAG, "Disk cache hit");
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            final T object = getSpecificObjectFromDiskCache(inputStream);
                            return object;
                        }
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    }

    public abstract T getSpecificObjectFromDiskCache(InputStream inputStream);

    /**
     * Clears both the memory and disk cache associated with this ObjectCache
     * object. Note that this includes disk access so this should not be
     * executed on the main/UI thread.
     */
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            Log.d(TAG, "Memory cache cleared");
        }

        synchronized (mDiskCacheLock) {
            mDiskCacheStarting = true;
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.delete();
                    Log.d(TAG, "Disk cache cleared");
                } catch (IOException e) {
                    Log.e(TAG, "clearCache - " + e);
                }
                mDiskLruCache = null;
                initDiskCache();
            }
        }
    }

    /**
     * Flushes the disk cache associated with this ObjectCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void flush() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache.flush();
                    Log.d(TAG, "Disk cache flushed");
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    /**
     * Closes the disk cache associated with this ObjectCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void close() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    if (!mDiskLruCache.isClosed()) {
                        mDiskLruCache.close();
                        mDiskLruCache = null;
                        Log.d(TAG, "Disk cache closed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close - " + e);
                }
            }
        }
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class FileCacheParams {
        public int memCacheSize;
        public File diskCacheDir;
        public boolean memoryCacheEnabled = true;
        public boolean diskCacheEnabled = true;
        public boolean clearDiskCacheOnStart = false;
        public boolean initDiskCacheOnCreate = false;
        public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;

        public FileCacheParams(Context context) {
            init(getDiskCacheDir(context, DEFAULT_DISK_CACHE_DIR));
        }

        public FileCacheParams(Context context, String uniqueName) {
            init(getDiskCacheDir(context, uniqueName));
        }

        public FileCacheParams(File diskCacheDir) {
            init(diskCacheDir);
        }

        private void init(File diskCacheDir) {
            setMemCacheSizePercent(DEFAULT_MEM_CACHE_PERCENT);
            this.diskCacheDir = diskCacheDir;
        }

        /**
         * Sets the memory cache size based on a percentage of the max available
         * VM memory. Throws exception {@link IllegalArgumentException} if
         * percent is < 0.05 or > .8. memCacheSize is stored in kilobytes
         * instead of bytes as this will eventually be passed to construct a
         * LruCache which takes an integer in its constructor.
         * 
         * 
         * @param percent
         *            Percent of memory class to use to size memory cache
         */
        public void setMemCacheSizePercent(float percent) {
            if (percent < 0.05f || percent > 0.8f) {
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                        + "between 0.05 and 0.8 (inclusive)");
            }
            memCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
        }

    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     * 
     * @param context
     *            The context to use
     * @param uniqueName
     *            A unique directory name to append to the cache dir
     * @return The cache directory
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {
        /**
         * Check if media is mounted or storage is built-in, if so, try and use
         * external cache dir otherwise use internal cache dir
         */
        final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() : context.getCacheDir()
                .getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Get the size in bytes of a file.
     * 
     * @param object
     * @return size in bytes
     */
    public abstract int getFileSize(T object);

    /**
     * Check if external storage is built-in or removable.
     * 
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (Utility.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     * 
     * @param context
     *            The context to use
     * @return The external cache directory
     */
    @TargetApi(8)
    public static File getExternalCacheDir(Context context) {
        if (Utility.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        /**
         * Before FROYO we need to construct the external cache directory
         * ourselves
         */
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * Check how much usable space is available at a given path.
     * 
     * @param path
     *            The path to check
     * @return The space available in bytes
     */
    @TargetApi(9)
    public static long getUsableSpace(File path) {
        if (Utility.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Locate an existing instance of this Fragment or if not found, create and
     * add it using FragmentManager.
     * 
     * @param fm
     *            The FragmentManager manager to use.
     * @return The existing instance of the Fragment or the new instance if just
     *         created.
     */
    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm, String tag) {
        // Check to see if we have retained the worker fragment.
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(tag);

        // If not retained or first time running, we need to create and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, tag).commitAllowingStateLoss();
        }

        return mRetainFragment;
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over
     * configuration changes. It will be used to retain the FileCache object.
     */
    public static class RetainFragment extends Fragment {
        private Object mObject;

        /**
         * Empty constructor as per the Fragment documentation
         */
        public RetainFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        /**
         * Store a single object in this Fragment.
         * 
         * @param object
         *            The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }

        /**
         * Get the stored object.
         * 
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }
    }

}
