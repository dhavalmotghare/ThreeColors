
package co.in.threecolors.ui.util;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;
import co.in.threecolors.BuildConfig;
import co.in.threecolors.cache.caching.ImageLoader;

/**
 * An assortment of UI helpers.
 */
public class UIUtils {
	
	private static final int BRIGHTNESS_THRESHOLD = 130;

    public static void preferPackageForIntent(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                intent.setPackage(packageName);
                break;
            }
        }
    }

    public static ImageLoader getImageLoader(final FragmentActivity activity) {
        // The ImageLoader takes care of loading remote images into our ImageView
        ImageLoader loader = new ImageLoader(activity);
        loader.addImageCache(activity);
        return loader;
    }


    /**
     * Calculate whether a color is light or dark, based on a commonly known
     * brightness formula.
     *
     * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
     */
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }


    private static final long sAppLoadTime = System.currentTimeMillis();

    public static long getCurrentTime(final Context context) {
        if (BuildConfig.DEBUG) {
            return context.getSharedPreferences("mock_data", Context.MODE_PRIVATE)
                    .getLong("mock_current_time", System.currentTimeMillis())
                    + System.currentTimeMillis() - sAppLoadTime;
        } else {
            return System.currentTimeMillis();
        }
    }

    public static void safeOpenLink(Context context, Intent linkIntent) {
        try {
            context.startActivity(linkIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT) .show();
        }
    }

    @SuppressWarnings("rawtypes")
	private static final Class[] sPhoneActivities = new Class[]{

    };

    @SuppressWarnings("rawtypes")
	private static final Class[] sTabletActivities = new Class[]{

    };

    public static void enableDisableActivities(final Context context) {
        boolean isHoneycombTablet = isHoneycombTablet(context);
        PackageManager pm = context.getPackageManager();

        // Enable/disable phone activities
        for (@SuppressWarnings("rawtypes") Class a : sPhoneActivities) {
            pm.setComponentEnabledSetting(new ComponentName(context, a),
                    isHoneycombTablet
                            ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }

        // Enable/disable tablet activities
        for (@SuppressWarnings("rawtypes") Class a : sTabletActivities) {
            pm.setComponentEnabledSetting(new ComponentName(context, a),
                    isHoneycombTablet
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setActivatedCompat(View view, boolean activated) {
        if (hasHoneycomb()) {
            view.setActivated(activated);
        }
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are in lined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return hasHoneycomb() && isTablet(context);
    }
    
}
