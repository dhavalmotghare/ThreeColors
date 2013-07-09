package co.in.threecolors.ui.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import co.in.threecolors.R;
import co.in.threecolors.cache.caching.ImageLoader;
import co.in.threecolors.provider.ThreeColorsProvider;
import co.in.threecolors.ui.adapters.MovielistAdapter;
import co.in.threecolors.ui.util.UIUtils;

/**
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class MyMoviesFragment extends Fragment implements AbsListView.OnScrollListener, LoaderManager.LoaderCallbacks<Cursor> {

	public static final String TAG = MyMoviesFragment.class.getSimpleName();
	private static final int MOVIE_LOADER_ID = 0;

	private MovielistAdapter movielistAdapter;
	private ViewGroup mRootView;

	private ImageLoader mImageLoader;
	private GridView mGridView;
	private View emptyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mImageLoader = UIUtils.getImageLoader(getActivity());
		movielistAdapter = new MovielistAdapter(getActivity(), mImageLoader);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(R.layout.my_movies_layout, container, false);
		mGridView = (GridView) mRootView.findViewById(R.id.my_movies_gridView);
		emptyView = mRootView.findViewById(R.id.empty_message);
		//mGridView.setAdapter(movielistAdapter);
		return mRootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageLoader.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageLoader.closeCache();
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	public void refresh(String newQuery) {
		refresh(true);
	}

	public void refresh() {
		refresh(false);
	}

	public void refresh(boolean forceRefresh) {
		if (!forceRefresh) {
			return;
		}
		movielistAdapter.notifyDataSetInvalidated();
	}

	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mImageLoader.setPauseWork(true);
		} else {
			mImageLoader.setPauseWork(false);
		}
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
	    
		return new CursorLoader(getActivity(), ThreeColorsProvider.Movie.CONTENT_URI, null, null, null,
				ThreeColorsProvider.Movie.DEFAULT_SORT_ORDER);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in.

		movielistAdapter.swapCursor(data);

		// The list should now be shown.
		mGridView.setVisibility(View.VISIBLE);
		emptyView.setVisibility(View.GONE);
		
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.
		movielistAdapter.swapCursor(null);
	}

}
