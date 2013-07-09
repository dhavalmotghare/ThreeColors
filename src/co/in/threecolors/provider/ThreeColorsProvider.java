package co.in.threecolors.provider;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

/**
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class ThreeColorsProvider extends ContentProvider {

	/** Log tag */
	private static final String TAG = ThreeColorsProvider.class.getSimpleName();
	private static final String DATABASE_NAME = "3CDatabase";

	/** Database constants */
	private static final int DATABASE_VERSION = 1;
	private static final String MOVIE_TABLE_NAME = "movies";

	public static final String AUTHORITY = "co.in.threecolors.provider.3CProvider";

	/** URI */
	private static final int MOVIE = 1;
	private static final int MOVIE_COUNT = 2;
	private static final int MOVIE_ID = 3;

	/** Database helper reference */
	private DatabaseHelper databaseHelper;
	private static HashMap<String, String> moviesProjections;

	private static final UriMatcher sUriMatcher;

	/** Stream table columns */
	public static final String MOVIE_TITLE = "movie_title";
	public static final String MOVIE_ORIGINAL_TITLE = "movie_original_title";
	public static final String MOVIE_BACKDROP_PATH = "movie_backdrop_path";
	public static final String MOVIE_POPULARITY = "movie_popularity";//float
	public static final String MOVIE_POSTER_PATH = "movie_poster_path";
	public static final String MOVIE_ADULT = "movie_adult";//boolean
	public static final String MOVIE_BUDGET = "movie_budget";//long
	public static final String MOVIE_HOME_PAGE = "movie_home_page";
	public static final String MOVIE_IMDB_ID = "movie_imdb_id";
	public static final String MOVIE_OVERVIEW = "movie_overview";
	public static final String MOVIE_REVENUE = "movie_revenue";//long
	public static final String MOVIE_RUNTIME = "movie_runtime";//int
	public static final String MOVIE_TAGLINE = "movie_tagline";
	public static final String MOVIE_VOTE_COUNT = "movie_vote_count";//int
	public static final String MOVIE_STATUS = "movie_status";
	public static final String MOVIE_VOTE_AVERAGE = "movie_vote_average";//float
	public static final String MOVIE_RELEASE_DATE = "movie_release_date";
	public static final String MOVIE_TYPE_POPULAR = "movie_type_popular";
	public static final String MOVIE_TYPE_UPCOMING = "movie_type_upcoming";
	public static final String MOVIE_TYPE_NOW_SHOWING = "movie_type_now_showing";
	public static final String MOVIE_MY_MOVIE = "my_movie";
	public static final String CREATED_DATE = "created_date";
	public static final String UPDATE_DATE = "update_date";
	public static final String URL = "Url";

	/** Table columns index */
	public static final int MOVIE_TITLE_INDEX = 1;
	public static final int MOVIE_ORIGINAL_TITLE_INDEX = 2;
	public static final int MOVIE_BACKDROP_PATH_INDEX = 3;
	public static final int MOVIE_POPULARITY_INDEX = 4;
	public static final int MOVIE_POSTER_PATH_INDEX = 5;
	public static final int MOVIE_ADULT_INDEX = 6;
	public static final int MOVIE_BUDGET_INDEX = 7;
	public static final int MOVIE_HOME_PAGE_INDEX = 8;
	public static final int MOVIE_IMDB_ID_INDEX = 9;
	public static final int MOVIE_OVERVIEW_INDEX = 10;
	public static final int MOVIE_REVENUE_INDEX = 11;
	public static final int MOVIE_RUNTIME_INDEX = 12;
	public static final int MOVIE_TAGLINE_INDEX = 13;
	public static final int MOVIE_VOTE_COUNT_INDEX = 14;
	public static final int MOVIE_STATUS_INDEX = 15;
	public static final int MOVIE_VOTE_AVERAGE_INDEX = 16;
	public static final int RELEASE_DATE_INDEX = 17;
	public static final int MOVIE_TYPE_POPULAR_INDEX = 18;
	public static final int MOVIE_TYPE_UPCOMING_INDEX = 19;
	public static final int MOVIE_TYPE_NOW_SHOWING_INDEX = 20;
	public static final int MOVIE_MY_MOVIE_INDEX = 21;
	public static final int CREATED_DATE_INDEX = 22;
	public static final int UPDATE_DATE_INDEX = 23;
	public static final int URL_INDEX = 24;

	public static final String FULL = "full";

	/**
	 * Stream table
	 * 
	 */
	public static final class Movie implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + MOVIE_TABLE_NAME);
		public static final String DEFAULT_SORT_ORDER = " _ID DESC";

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.co.in.threecolors.movies";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.co.in.threecolors.movie";
	}

	/**
	 * Database helper for 3C.
	 * 
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		private static final String TAG = DatabaseHelper.class.getSimpleName();
		private SQLiteDatabase mDatabase;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			return super.getWritableDatabase();
		}

		@Override
		public synchronized SQLiteDatabase getReadableDatabase() {
			return super.getReadableDatabase();
		}

		@Override
		public synchronized void close() {
			super.close();
			if (mDatabase != null && mDatabase.isOpen()) {
				mDatabase.close();
				mDatabase = null;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Creating tables");
			db.execSQL("CREATE TABLE " + MOVIE_TABLE_NAME + " (" + Movie._ID
					+ " INTEGER PRIMARY KEY," 
					+ MOVIE_TITLE + " TEXT,"
					+ MOVIE_ORIGINAL_TITLE + " TEXT,"
					+ MOVIE_BACKDROP_PATH + " TEXT,"
					+ MOVIE_POPULARITY + " REAL,"
					+ MOVIE_POSTER_PATH + " TEXT,"
					+ MOVIE_ADULT + " INTEGER,"
					+ MOVIE_BUDGET + " INTEGER,"
					+ MOVIE_HOME_PAGE + " TEXT,"
					+ MOVIE_IMDB_ID + " TEXT,"
					+ MOVIE_OVERVIEW + " TEXT,"
					+ MOVIE_REVENUE + " REAL,"
					+ MOVIE_RUNTIME + " TEXT,"
					+ MOVIE_TAGLINE + " TEXT,"
					+ MOVIE_VOTE_COUNT + " INTEGER,"
					+ MOVIE_STATUS + " TEXT,"
					+ MOVIE_VOTE_AVERAGE + " REAL,"
					+ MOVIE_RELEASE_DATE + " INTEGER,"
					+ MOVIE_TYPE_POPULAR + " INTEGER,"
					+ MOVIE_TYPE_NOW_SHOWING + " INTEGER,"
					+ MOVIE_TYPE_UPCOMING + " INTEGER,"
					+ MOVIE_MY_MOVIE + " INTEGER,"
					+ CREATED_DATE + " INTEGER," 
					+ UPDATE_DATE + " INTEGER,"
					+ URL + " TEXT" + ");");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + MOVIE_TABLE_NAME);

			onCreate(db);
		}

	}

	/**
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		databaseHelper = new DatabaseHelper(getContext());
		return (databaseHelper == null) ? false : true;
	}

	/**
	 * Delete a record from the database.
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 *      java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case MOVIE:
			count = db.delete(MOVIE_TABLE_NAME, selection, selectionArgs);
			break;

		case MOVIE_ID:
			String entityID = uri.getPathSegments().get(1);
			count = db.delete(MOVIE_TABLE_NAME, Movie._ID
					+ "="
					+ entityID
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * Insert a new record into the database.
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 *      android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		ContentValues values;
		long rowId;
		Long now = System.currentTimeMillis();
		SQLiteDatabase db = databaseHelper.getWritableDatabase();

		String table;
		String nullColumnHack;
		Uri contentUri;

		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		switch (sUriMatcher.match(uri)) {
		case MOVIE:
			table = MOVIE_TABLE_NAME;
			nullColumnHack = MOVIE_TITLE;
			contentUri = Movie.CONTENT_URI;
			/**
			 * Add default values for missed required fields
			 */
			if (values.containsKey(CREATED_DATE) == false)
				values.put(CREATED_DATE, now);
			if (values.containsKey(UPDATE_DATE) == false)
				values.put(UPDATE_DATE, now);
			if (values.containsKey(MOVIE_TITLE) == false)
				values.put(MOVIE_TITLE, "");
			if (values.containsKey(MOVIE_ORIGINAL_TITLE) == false)
				values.put(MOVIE_ORIGINAL_TITLE, "");
			if (values.containsKey(MOVIE_BACKDROP_PATH) == false)
				values.put(MOVIE_BACKDROP_PATH, "");
			if (values.containsKey(MOVIE_POPULARITY) == false)
				values.put(MOVIE_POPULARITY, 0.0);
			if (values.containsKey(MOVIE_POSTER_PATH) == false)
				values.put(MOVIE_POSTER_PATH, "");
			if (values.containsKey(MOVIE_ADULT) == false)
				values.put(MOVIE_ADULT, false);
			if (values.containsKey(MOVIE_BUDGET) == false)
				values.put(MOVIE_BUDGET, 0.0);
			if (values.containsKey(MOVIE_HOME_PAGE) == false)
				values.put(MOVIE_HOME_PAGE, "");
			if (values.containsKey(MOVIE_IMDB_ID) == false)
				values.put(MOVIE_IMDB_ID, "");
			if (values.containsKey(MOVIE_OVERVIEW) == false)
				values.put(MOVIE_OVERVIEW, "");
			if (values.containsKey(MOVIE_REVENUE) == false)
				values.put(MOVIE_REVENUE, 0.0);
			if (values.containsKey(MOVIE_RUNTIME) == false)
				values.put(MOVIE_RUNTIME, 0);
			if (values.containsKey(MOVIE_TAGLINE) == false)
				values.put(MOVIE_TAGLINE, "");
			if (values.containsKey(MOVIE_VOTE_COUNT) == false)
				values.put(MOVIE_VOTE_COUNT, 0);
			if (values.containsKey(MOVIE_STATUS) == false)
				values.put(MOVIE_STATUS, "");
			if (values.containsKey(MOVIE_VOTE_AVERAGE) == false)
				values.put(MOVIE_VOTE_AVERAGE, 0.0);
			if (values.containsKey(URL) == false)
				values.put(URL, "");
			if (values.containsKey(MOVIE_RELEASE_DATE) == false)
				values.put(MOVIE_RELEASE_DATE, 0);
			if (values.containsKey(MOVIE_TYPE_POPULAR) == false)
                values.put(MOVIE_TYPE_POPULAR, 0);
			if (values.containsKey(MOVIE_TYPE_NOW_SHOWING) == false)
                values.put(MOVIE_TYPE_NOW_SHOWING, 0);
			if (values.containsKey(MOVIE_TYPE_UPCOMING) == false)
                values.put(MOVIE_TYPE_UPCOMING, 0);
			if (values.containsKey(MOVIE_MY_MOVIE) == false)
                values.put(MOVIE_MY_MOVIE, 0);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		rowId = db.insert(table, nullColumnHack, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Get a cursor to the database
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 *      java.lang.String[], java.lang.String, java.lang.String[],
	 *      java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String sql = "";

		int matchedCode = sUriMatcher.match(uri);
		switch (matchedCode) {
		case MOVIE:
			qb.setTables(MOVIE_TABLE_NAME);
			qb.setProjectionMap(moviesProjections);
			break;

		case MOVIE_COUNT:
			sql = "SELECT count(*) FROM " + MOVIE_TABLE_NAME;
			if (selection != null && selection.length() > 0) {
				sql += " WHERE " + selection;
			}
			break;

		case MOVIE_ID:
			qb.setTables(MOVIE_TABLE_NAME);
			qb.setProjectionMap(moviesProjections);
			qb.appendWhere(Movie._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI \"" + uri
					+ "\"; matchedCode=" + matchedCode);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (matchedCode) {
			case MOVIE:
			case MOVIE_ID:
				orderBy = Movie.DEFAULT_SORT_ORDER;
				break;

			case MOVIE_COUNT:
				orderBy = "";
				break;

			default:
				throw new IllegalArgumentException("Unknown URI \"" + uri
						+ "\"; matchedCode=" + matchedCode);
			}
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor c = null;
		boolean logQuery = Log.isLoggable(TAG, Log.VERBOSE);
		try {
			if (sql.length() > 0) {
				c = db.rawQuery(sql, selectionArgs);
			} else {
				c = qb.query(db, projection, selection, selectionArgs, null,
						null, orderBy);
			}
		} catch (Exception e) {
			logQuery = true;
			Log.e(TAG, "Database query failed");
			e.printStackTrace();
		}

		if (logQuery) {
			if (sql.length() > 0) {
				Log.v(TAG, "query, SQL=\"" + sql + "\"");
				if (selectionArgs != null && selectionArgs.length > 0) {
					Log.v(TAG,
							"; selectionArgs=" + Arrays.toString(selectionArgs));
				}
			} else {
				Log.v(TAG,
						"query, uri=" + uri + "; projection="
								+ Arrays.toString(projection));
				Log.v(TAG, "; selection=" + selection);
				Log.v(TAG, "; selectionArgs=" + Arrays.toString(selectionArgs)
						+ "; sortOrder=" + sortOrder);
				Log.v(TAG, "; qb.getTables=" + qb.getTables() + "; orderBy="
						+ orderBy);
			}
		}

		if (c != null) {
			// Tell the cursor what Uri to watch, so it knows when its source
			// data changes
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	/**
	 * Update a record in the database
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 *      android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case MOVIE:
			count = db.update(MOVIE_TABLE_NAME, values, selection,
					selectionArgs);
			break;

		case MOVIE_ID:
			String entityID = uri.getPathSegments().get(1);
			count = db.update(MOVIE_TABLE_NAME, values, Movie._ID
					+ "="
					+ entityID
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI \"" + uri + "\"");
		}
		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	// Static Definitions for UriMatcher and Projection Maps
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, MOVIE_TABLE_NAME, MOVIE);
		sUriMatcher.addURI(AUTHORITY, MOVIE_TABLE_NAME + "/#", MOVIE_ID);
		sUriMatcher.addURI(AUTHORITY, MOVIE_TABLE_NAME + "/count", MOVIE_COUNT);

		moviesProjections = new HashMap<String, String>();
		moviesProjections.put(Movie._ID, Movie._ID);
		moviesProjections.put(MOVIE_TITLE, MOVIE_TITLE);
		moviesProjections.put(MOVIE_ORIGINAL_TITLE, MOVIE_ORIGINAL_TITLE);
		moviesProjections.put(MOVIE_ADULT, MOVIE_ADULT);
		moviesProjections.put(MOVIE_BACKDROP_PATH, MOVIE_BACKDROP_PATH);
		moviesProjections.put(MOVIE_BUDGET, MOVIE_BUDGET);
		moviesProjections.put(MOVIE_HOME_PAGE, MOVIE_HOME_PAGE);
		moviesProjections.put(MOVIE_IMDB_ID, MOVIE_IMDB_ID);
		moviesProjections.put(MOVIE_OVERVIEW, MOVIE_OVERVIEW);
		moviesProjections.put(MOVIE_POPULARITY, MOVIE_POPULARITY);
		moviesProjections.put(MOVIE_POSTER_PATH, MOVIE_POSTER_PATH);
		moviesProjections.put(MOVIE_REVENUE, MOVIE_REVENUE);
		moviesProjections.put(MOVIE_RUNTIME, MOVIE_RUNTIME);
		moviesProjections.put(MOVIE_STATUS, MOVIE_STATUS);
		moviesProjections.put(MOVIE_TAGLINE, MOVIE_TAGLINE);
		moviesProjections.put(MOVIE_VOTE_AVERAGE, MOVIE_VOTE_AVERAGE);
		moviesProjections.put(MOVIE_VOTE_COUNT, MOVIE_VOTE_COUNT);
		moviesProjections.put(MOVIE_RELEASE_DATE, MOVIE_RELEASE_DATE);
		moviesProjections.put(MOVIE_TYPE_POPULAR, MOVIE_TYPE_POPULAR);
		moviesProjections.put(MOVIE_TYPE_NOW_SHOWING, MOVIE_TYPE_NOW_SHOWING);
		moviesProjections.put(MOVIE_TYPE_UPCOMING, MOVIE_TYPE_UPCOMING);
		moviesProjections.put(MOVIE_MY_MOVIE, MOVIE_MY_MOVIE);
		moviesProjections.put(UPDATE_DATE, UPDATE_DATE);
		moviesProjections.put(CREATED_DATE, CREATED_DATE);
		moviesProjections.put(URL, URL);
	}

	/**
	 * Get MIME type of the content, used for the supplied Uri
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case MOVIE:
		case MOVIE_COUNT:
			return Movie.CONTENT_TYPE;

		case MOVIE_ID:
			return Movie.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

}
