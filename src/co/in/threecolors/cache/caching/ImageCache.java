package co.in.threecolors.cache.caching;

import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentManager;
import co.in.threecolors.ui.util.Utility;

/**
 * This class holds bitmaps in caches (memory and disk).
 */
public class ImageCache extends ObjectCache<Bitmap> {

    private static final String TAG = ImageCache.class.getSimpleName();

    /** Disk cache directory name */
    protected static final String IMAGE_DISK_CACHE_DIR = "ImageCache";

    /** Compression settings when writing images to disk cache */
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 75;

    public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
    public int compressQuality = DEFAULT_COMPRESS_QUALITY;

    /**
     * Creating a new ImageCache object using the specified parameters.
     * 
     * @param cacheParams
     *            The cache parameters to use to initialize the cache
     */
    public ImageCache(FileCacheParams cacheParams) {
        super(cacheParams);
    }

    /**
     * Creating a new ImageCache object using the default parameters.
     * 
     * @param context
     *            The context to use
     */
    public ImageCache(Context context) {
        super(context, IMAGE_DISK_CACHE_DIR);
    }

    /**
     * Find and return an existing ObjectCache stored in a
     * {@link RetainFragment}, if not found a new one is created using the
     * supplied parameters and saved to a {@link RetainFragment}.
     * 
     * @param fragmentManager
     *            The fragment manager to use when dealing with the retained
     *            fragment.
     * @param cacheParams
     *            The cache parameters to use if creating the ImageCache
     * @return An existing retained ImageCache object or a new one if one did
     *         not exist
     */
    public static ImageCache getCache(FragmentManager fragmentManager, FileCacheParams cacheParams) {

        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager, TAG);

        // See if we already have an imageCache stored in RetainFragment
        ImageCache imageCache = (ImageCache) mRetainFragment.getObject();

        // No existing FileCache, create one and store it in RetainFragment
        if (imageCache == null) {
            imageCache = new ImageCache(cacheParams);
            mRetainFragment.setObject(imageCache);
        }

        return imageCache;
    }

    @Override
    public Bitmap getSpecificObjectFromDiskCache(InputStream inputStream) {
        if (inputStream != null) {
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return bitmap;
        }
        return null;
    }

    @Override
    public void addSpecificObjectToCache(Bitmap bitmap, OutputStream out) {
        bitmap.compress(compressFormat, compressQuality, out);
    }

    @Override
    public int getFileSize(Bitmap bitmap) {
        if (Utility.hasHoneycombMR1()) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

}
