package co.in.threecolors.ui.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {

    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final int IO_BUFFER_SIZE = 8 * 1024;

    public static int dipsToPixels(Context context, int dips) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dips * scale + 0.5f);
    }

    public static int pixelstoDips(Context context, int pixels) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pixels / scale);
    }

    public static double getStatusbarHeight(Context c) {
        return FloatMath.ceil(25 * c.getResources().getDisplayMetrics().density);
    }

    /***
     * Gets the top status bar height. If the device has a bottom status bar,
     * returns 0
     * 
     * @return int the size of the top status bar
     */
    public static int getTopStatusBarHeight(Resources r, WindowManager m) {
        int result = 0;

        if (!Utility.isStatusBarAtTop(m))
            return result;

        int resourceId = r.getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0) {
            result = r.getDimensionPixelSize(resourceId);
        }

        return result;
    }

    /***
     * The idea here is that devices at sw600 and up and between honeycomb and
     * jelly bean have the combined bottom status bar and no top bar.
     * 
     * @return boolean whether or not the device has a status bar at the top.
     */
    public static boolean isStatusBarAtTop(WindowManager m) {

        DisplayMetrics dm = new DisplayMetrics();
        m.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        if (width >= 600 || height >= 600) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                return false;
            }
        }

        return true;
    }

    /**
     * Used to decide whether a WIFI network is available.
     * 
     * @param context
     * @return true: WIFI network is available. false: Wifi network is not
     *         available.
     */
    public static boolean networkAvailable(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null) {
            // Only support download over WiFi
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                return info.isConnected();
            } else {
                return true;// doesn't work on emulator so returning true
            }
        }
        return true;// doesn't work on emulator so returning true
    }

    /**
     * Get a JSONObject for the passed string
     * 
     * @param response
     * @return JSONObject
     */
    public static JSONObject getJsonObject(String response) {
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /**
     * Convert to integer
     * 
     * @param value
     * @return int
     */
    public static int convertToInt(String value) {
        int v = 0;
        try {
            v = Integer.parseInt(value);
        } catch (Exception e) {

        }
        return v;
    }

    /**
     * Deletes the contents of directory. Throws an IOException if any file
     * could not be deleted, or if directory is not a readable directory.
     */
    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before FROYO we need to construct the external cache directory
        // ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    };

    public static Executor getDualThreadExecutor(int noOfThreads) {
        return Executors.newFixedThreadPool(noOfThreads, sThreadFactory);
    }

}
