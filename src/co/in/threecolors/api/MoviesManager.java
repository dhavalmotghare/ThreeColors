/**
 * 
 */
package co.in.threecolors.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.*;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.List;

/**
 * @author dhavalmotghare@gmail.com
 * 
 */
public class MoviesManager {

    /** LOG TAG */
    private static final String TAG = MoviesManager.class.getSimpleName();

    public static final int TYPE_POPULAR_MOVIES = 1;
    public static final int TYPE_UPCOMING_MOVIES = 2;
    public static final int TYPE_NOW_PLAYING_MOVIES = 3;

    /** Shared preferences */
    private static final String PREFS_FILE = "three_colors_settings";
    public static final String KEY_SERVICE_STATUS = "status";

    /** API Key */
    private static final String API_KEY = "5a1a77e2eba8984804586122754f969f";
    private static TheMovieDbApi tmdb;

    /** Languages */
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_DEFAULT = LANGUAGE_ENGLISH;

    /** Shared Preferences settings */
    private SharedPreferences settings;

    /**
     * Private constructor for single reference
     * 
     */
    private MoviesManager() {

    }

    public void initializeTMDBApi() {
        try {
            tmdb = new TheMovieDbApi(API_KEY);
            TmdbConfiguration tmdbConfig = tmdb.getConfiguration();
            Log.i(TAG, "Configuration " + tmdbConfig);
            Log.i(TAG, "Base URL" + StringUtils.isNotBlank(tmdbConfig.getBaseUrl()));
            Log.i(TAG, "Backdrop sizes" + tmdbConfig.getBackdropSizes().size());
            Log.i(TAG, "Poster sizes" + tmdbConfig.getPosterSizes().size());
            Log.i(TAG, "Profile sizes" + tmdbConfig.getProfileSizes().size());
        } catch (MovieDbException e) {
            e.printStackTrace();
        }
    }

    /**
     * Holder pattern for single instance
     * 
     * @author dhavalmotghare@gmail.com
     * 
     */
    private static class LazyHolder {
        private static MoviesManager instance = new MoviesManager();
    }

    /**
     * Static get instance method for getting the instance
     * 
     * @return TwitterManager
     */
    public static MoviesManager getInstance() {
        return LazyHolder.instance;
    }

    @SuppressLint("WorldReadableFiles")
    public void setPreferences(Context context) {
        this.settings = context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_WORLD_READABLE);
    }

    /**
     * Set the updating service status
     * 
     * @param value
     */
    public void setServiceStatus(int value) {
        if (settings != null) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(KEY_SERVICE_STATUS, value);
            editor.commit();
        }
    }

    /**
     * Get the service status
     * 
     * @return int
     */
    public int getServiceStatus() {
        int value = -1;
        if (settings != null) {
            value = settings.getInt(KEY_SERVICE_STATUS, -1);
        }
        return value;
    }

    /**
     * Search a movie
     */
    public List<MovieDb> searchMovie(String name) throws MovieDbException {
        return tmdb.searchMovie(name, 0, LANGUAGE_DEFAULT, true, 0);
    }

    /**
     * Get movie info
     */
    public MovieDb getMovieInfo(int id) throws MovieDbException {
        MovieDb result = tmdb.getMovieInfo(id, LANGUAGE_DEFAULT);
        Log.i(TAG, "get movie info - " + result.getOriginalTitle());
        return result;
    }

    /**
     * Get MovieCasts
     */
    public List<Person> getMovieCasts(int id) throws MovieDbException {
        Log.i(TAG, "Get Movie Casts");
        List<Person> people = tmdb.getMovieCasts(id);
        Log.i(TAG, "Cast Size - " + people.size());
        return people;
    }

    /**
     * Get Movie Images
     */
    public List<Artwork> getMovieImages(int id) throws MovieDbException {
        Log.i(TAG, "Get Movie Images");
        List<Artwork> result = tmdb.getMovieImages(id, LANGUAGE_DEFAULT);
        Log.i(TAG, "Artwork found - " + result.isEmpty());
        return result;
    }

    /**
     * Generate the full image URL from the size and image path
     * 
     * @param imagePath
     * @param requiredSize
     * @throws MovieDbException
     */
    public URL createImageUrl(String imagePath, String size) throws MovieDbException {
        URL url = null;
        try {
            url = tmdb.createImageUrl(imagePath, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * Get Movie Release Info
     */
    public List<ReleaseInfo> getMovieReleaseInfo(int id) throws MovieDbException {
        Log.i(TAG, "Get Movie Release Info");
        List<ReleaseInfo> result = tmdb.getMovieReleaseInfo(id, LANGUAGE_DEFAULT);
        Log.i(TAG, "Release information present -" + result.isEmpty());
        return result;
    }

    /**
     * Get Latest Movie
     */
    public MovieDb getLatestMovie() throws MovieDbException {
        Log.i(TAG, "Get Latest Movie");
        MovieDb result = tmdb.getLatestMovie();
        return result;
    }

    /**
     * Get Now Playing Movies
     */
    public List<MovieDb> getNowPlayingMovies() throws MovieDbException {
        Log.i(TAG, "Get Now Playing Movies");
        List<MovieDb> results = tmdb.getNowPlayingMovies(LANGUAGE_DEFAULT, 0);
        Log.i(TAG, "Now playing movies found " + !results.isEmpty());
        return results;
    }

    /**
     * Get Popular Movie List
     */
    public List<MovieDb> getPopularMovieList() throws MovieDbException {
        Log.i(TAG, "Get Popular Movie List");
        List<MovieDb> results = tmdb.getPopularMovieList(LANGUAGE_DEFAULT, 0);
        Log.i(TAG, "Popular movies found " + !results.isEmpty());
        return results;
    }

    /**
     * Get Top Rated Movies
     */
    public List<MovieDb> getTopRatedMovies() throws MovieDbException {
        Log.i(TAG, "Get Top Rated Movies");
        List<MovieDb> results = tmdb.getTopRatedMovies(LANGUAGE_DEFAULT, 0);
        Log.i(TAG, "Ttop rated movies found " + !results.isEmpty());
        return results;
    }

    /**
     * Get Upcoming movies
     */
    public List<MovieDb> getUpcoming() throws MovieDbException {
        Log.i(TAG, "getUpcoming");
        List<MovieDb> results = tmdb.getUpcoming(LANGUAGE_DEFAULT, 0);
        Log.i(TAG, "Upcoming movies found" + !results.isEmpty());
        return results;
    }

    /**
     * Search Keyword
     */
    public List<Keyword> SearchKeyword(String keyword) throws Exception {
        Log.i(TAG, "Search Keyword");
        List<Keyword> result = tmdb.searchKeyword(keyword, 0);
        Log.i(TAG, "Keywords found" + (result == null));
        Log.i(TAG, "Keywords found" + (result.size() > 0));
        return result;
    }

}
