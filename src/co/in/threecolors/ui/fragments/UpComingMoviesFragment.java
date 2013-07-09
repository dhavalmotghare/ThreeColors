package co.in.threecolors.ui.fragments;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.GridView;
import android.widget.ListView;
import co.in.threecolors.R;
import co.in.threecolors.provider.ThreeColorsProvider;
import co.in.threecolors.ui.adapters.MovielistAdapter;
import co.in.threecolors.ui.util.UIUtils;

public class UpComingMoviesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback {

    public static final String TAG = UpComingMoviesFragment.class.getSimpleName();

    private View emptyText;
    private GridView mGridView;
    private ViewGroup mRootView;
    private boolean mScrollToNow;
    private ActionMode mActionMode;
    private MovielistAdapter movieListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        movieListAdapter = new MovielistAdapter(getActivity(), UIUtils.getImageLoader(getActivity()));

        if (savedInstanceState == null) {
            mScrollToNow = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.movies_view_layout, container, false);
        emptyText = mRootView.findViewById(R.id.empty_message);
        mGridView = (GridView) mRootView.findViewById(R.id.my_movies_gridView);
        mGridView.setAdapter(movieListAdapter);
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(0);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().getContentResolver().registerContentObserver(ThreeColorsProvider.Movie.CONTENT_URI, true,
                mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String[] PROJECTION = new String[] { ThreeColorsProvider.Movie._ID, ThreeColorsProvider.MOVIE_TITLE,
                ThreeColorsProvider.MOVIE_BACKDROP_PATH, ThreeColorsProvider.MOVIE_POSTER_PATH,
                ThreeColorsProvider.MOVIE_OVERVIEW, ThreeColorsProvider.MOVIE_RELEASE_DATE };
        String where = ThreeColorsProvider.MOVIE_TYPE_UPCOMING + "=1";
        return new CursorLoader(getActivity(), ThreeColorsProvider.Movie.CONTENT_URI, PROJECTION, where, null,
                ThreeColorsProvider.Movie.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (cursor.getCount() <= 0) {
            emptyText.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            return;
        }

        int firstNowPosition = GridView.INVALID_POSITION;
        movieListAdapter.changeCursor(cursor);

        if (mScrollToNow && firstNowPosition != ListView.INVALID_POSITION) {
            mScrollToNow = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        movieListAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean handled = false;
        switch (item.getItemId()) {

        }
        mActionMode.finish();
        return handled;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

}
