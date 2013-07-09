package co.in.threecolors.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import co.in.threecolors.R;
import co.in.threecolors.api.LocalMovies;
import co.in.threecolors.api.MoviesManager;
import co.in.threecolors.provider.ThreeColorsProvider;
import co.in.threecolors.provider.ThreeColorsProvider.Movie;
import co.in.threecolors.ui.HomeActivity;
import co.in.threecolors.ui.util.Utility;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.model.MovieDb;

/**
 * Service to manage the download and update of movies.
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class MovieFetcherService extends Service {

    /** LOG TAG */
    private static final String TAG = MovieFetcherService.class.getSimpleName();

    public static final String MOVIE_UPDATE_INTENT = "movie_update";
    public static final String MOVIE_UPDATE_INTENT_URI = "content://co.in.threecolors.service/";

    /** Service states */
    public static final int STATUS_UPDATING = 0;
    public static final int STATUS_LASTUPDATE_FAILED = 1;
    public static final int STATUS_IDLE = 2;
    public static final int STATUS_UNKNOWN = -1;

    private int newMovies;
    private Uri mContentUri;
    private ContentResolver mContentResolver;
    private MoviesManager moviesManager;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onCreate()
     */
    public void onCreate() {
        super.onCreate();
        MoviesManager.getInstance().setServiceStatus(STATUS_IDLE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (MoviesManager.getInstance().getServiceStatus() == STATUS_UPDATING) {
            return START_STICKY;
        }

        new Thread() {
            public void run() {
                try {
                    if (Utility.networkAvailable(getApplicationContext())) {
                        MoviesManager.getInstance().initializeTMDBApi();
                        MoviesManager.getInstance().setServiceStatus(STATUS_UPDATING);
                        try {
                            loadLocalMovies();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            loadTimeline(MoviesManager.TYPE_UPCOMING_MOVIES);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            loadTimeline(MoviesManager.TYPE_NOW_PLAYING_MOVIES);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            loadTimeline(MoviesManager.TYPE_POPULAR_MOVIES);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        MoviesManager.getInstance().setServiceStatus(STATUS_IDLE);
                        Log.d(TAG, " Unable to connect, No wifi available ");
                    }
                } catch (Exception e) {
                    MoviesManager.getInstance().setServiceStatus(STATUS_LASTUPDATE_FAILED);
                    e.printStackTrace();
                    Log.d(TAG, " Something went wrong while syncing " + e.toString());
                }
            };
        }.start();
        return START_STICKY;
    }

    /**
     * Helper method to get the movie manager instance
     * 
     * @return MoviesManager
     */
    private MoviesManager getMoviesManager() {
        if (moviesManager == null) {
            moviesManager = MoviesManager.getInstance();
        }
        return moviesManager;
    }

    /**
     * Load time line for the supplied time-line type
     * 
     * @param timelineType
     * @return boolean
     * @throws MovieDbException
     */
    public boolean loadTimeline(int timelineType) throws MovieDbException {
        boolean operationStatus = false;
        newMovies = 0;

        mContentResolver = getApplicationContext().getContentResolver();
        List<MovieDb> results = null;
        switch (timelineType) {
        case MoviesManager.TYPE_NOW_PLAYING_MOVIES:
            mContentUri = ThreeColorsProvider.Movie.CONTENT_URI;
            results = getMoviesManager().getNowPlayingMovies();
            break;
        case MoviesManager.TYPE_POPULAR_MOVIES:
            mContentUri = ThreeColorsProvider.Movie.CONTENT_URI;
            results = getMoviesManager().getPopularMovieList();
            break;
        case MoviesManager.TYPE_UPCOMING_MOVIES:
            mContentUri = ThreeColorsProvider.Movie.CONTENT_URI;
            results = getMoviesManager().getUpcoming();
            break;
        default:
            Log.e(TAG, "Type not supported " + timelineType);
            break;
        }
        if (results != null) {
            operationStatus = true;
            try {
                for (MovieDb movie : results) {
                    insertMovie(movie, timelineType);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (newMovies > 0) {
            mContentResolver.notifyChange(mContentUri, null);
        }
        sendBroadcast();

        MoviesManager.getInstance().setServiceStatus(STATUS_IDLE);
        return operationStatus;
    }

    /**
     * Load local movies
     * 
     * @param timelineType
     * @return
     * @throws MovieDbException
     */
    public boolean loadLocalMovies() throws MovieDbException {
        boolean operationStatus = false;

        mContentResolver = getApplicationContext().getContentResolver();
        List<MovieDb> results = new ArrayList<MovieDb>();
        
        LocalMovies localMovies = new LocalMovies();
        try {
            ArrayList<String> movies = localMovies.getLocalMovies("pune");
            System.out.println(movies);
        } catch (XmlPullParserException xml) {
            xml.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (results != null) {
            operationStatus = true;
            try {
                for (MovieDb movie : results) {
                    insertMovie(movie, MoviesManager.TYPE_NOW_PLAYING_MOVIES);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (newMovies > 0) {
            mContentResolver.notifyChange(mContentUri, null);
        }
        sendBroadcast();

        MoviesManager.getInstance().setServiceStatus(STATUS_IDLE);
        return operationStatus;
    }

    /**
     * Clear All Notifications
     */
    public void clearNotification(int type) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(type);
    }

    /**
     * Display a notification if users movies are about to be released
     * 
     * @param type
     * @param Movie
     */
    @SuppressWarnings("deprecation")
    public void sendNotification(int type,Movie movie) {

        final int icon = R.drawable.ic_launcher;
        final String ns = Context.NOTIFICATION_SERVICE;
        final CharSequence tickerText = getString(R.string.app_name);

        final long when = System.currentTimeMillis();

        final Context context = getApplicationContext();
        CharSequence contentTitle = "";
        CharSequence contentText = "";

        if (contentTitle == null || contentTitle.equals("") || contentText == null || contentText.equals(""))
            return;

        final Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
        notificationIntent.putExtra("Refresh", true);
        final PendingIntent contentIntent = PendingIntent
                .getActivity(getApplicationContext(), 0, notificationIntent, 0);

        final Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.notify(type, notification);
    }

    /**
     * Send a broadcast that we are done updating, it could either be a
     * successful update or a failed update
     * 
     */
    private void sendBroadcast() {
        Uri uri = Uri.parse(MOVIE_UPDATE_INTENT_URI);
        Intent intent = new Intent(MOVIE_UPDATE_INTENT, uri);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Log.d(TAG, "Broadcasting tweet intent! ");
        getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Insert a movie
     * 
     * @param MovieDB
     * @param type
     * @return
     * @throws JSONException
     * @throws SQLiteConstraintException
     */
    public Uri insertMovie(MovieDb movie, int type) throws JSONException, SQLiteConstraintException {
        ContentValues values = new ContentValues();

        Uri movieUri = ContentUris.withAppendedId(mContentUri, movie.getId());

        values.put(ThreeColorsProvider.Movie._ID, movie.getId());
        values.put(ThreeColorsProvider.MOVIE_TITLE, movie.getTitle());
        values.put(ThreeColorsProvider.MOVIE_ORIGINAL_TITLE, movie.getOriginalTitle());
        values.put(ThreeColorsProvider.MOVIE_BACKDROP_PATH, getCompeleteURL(movie.getBackdropPath(), "w780"));
        values.put(ThreeColorsProvider.MOVIE_POPULARITY, movie.getPopularity());
        values.put(ThreeColorsProvider.MOVIE_POSTER_PATH, getCompeleteURL(movie.getPosterPath(), "w154"));
        values.put(ThreeColorsProvider.MOVIE_ADULT, movie.isAdult());
        values.put(ThreeColorsProvider.MOVIE_BUDGET, movie.getBudget());
        values.put(ThreeColorsProvider.MOVIE_HOME_PAGE, movie.getHomepage());
        values.put(ThreeColorsProvider.MOVIE_IMDB_ID, movie.getImdbID());
        values.put(ThreeColorsProvider.MOVIE_OVERVIEW, movie.getOverview());
        values.put(ThreeColorsProvider.MOVIE_REVENUE, movie.getRevenue());
        values.put(ThreeColorsProvider.MOVIE_RUNTIME, movie.getRuntime());
        values.put(ThreeColorsProvider.MOVIE_TAGLINE, movie.getTagline());
        values.put(ThreeColorsProvider.MOVIE_VOTE_COUNT, movie.getVoteCount());
        values.put(ThreeColorsProvider.MOVIE_STATUS, movie.getStatus());
        values.put(ThreeColorsProvider.MOVIE_VOTE_AVERAGE, movie.getVoteAverage());
        values.put(ThreeColorsProvider.URL, "");
        values.put(ThreeColorsProvider.MOVIE_RELEASE_DATE, movie.getReleaseDate());

        try {
            switch (type) {
            case MoviesManager.TYPE_NOW_PLAYING_MOVIES:
                values.put(ThreeColorsProvider.MOVIE_TYPE_NOW_SHOWING, true);
                break;
            case MoviesManager.TYPE_POPULAR_MOVIES:
                values.put(ThreeColorsProvider.MOVIE_TYPE_POPULAR, true);
                break;
            case MoviesManager.TYPE_UPCOMING_MOVIES:
                values.put(ThreeColorsProvider.MOVIE_TYPE_UPCOMING, true);
                break;
            }

        } catch (Exception e) {
            Log.e(TAG, "insert movie: " + e.toString());
        }

        if ((mContentResolver.update(movieUri, values, null, null)) == 0) {
            // There was no such row so add new one
            mContentResolver.insert(mContentUri, values);
            newMovies++;
        }
        return movieUri;
    }

    private String getCompeleteURL(String imgPath, String size) {
        URL url = null;
        try {
            url = MoviesManager.getInstance().createImageUrl(imgPath, size);
        } catch (Exception e) {
        }
        if (url == null || TextUtils.isEmpty(url.toString())) {
            return imgPath;
        } else {
            return url.toString();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MoviesManager.getInstance().setServiceStatus(STATUS_IDLE);
    }

}
