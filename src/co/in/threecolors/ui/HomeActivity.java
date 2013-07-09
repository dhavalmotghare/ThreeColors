package co.in.threecolors.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;
import co.in.threecolors.R;
import co.in.threecolors.api.MoviesManager;
import co.in.threecolors.service.MovieFetcherService;
import co.in.threecolors.ui.fragments.MyMoviesFragment;
import co.in.threecolors.ui.fragments.NowPlayingMoviesFragment;
import co.in.threecolors.ui.fragments.PopularMoviesFragment;
import co.in.threecolors.ui.fragments.UpComingMoviesFragment;
import co.in.threecolors.ui.util.UIUtils;

import static co.in.threecolors.ui.util.LogUtils.LOGD;

/**
 * The landing screen for the application, once the user has logged in.
 * 
 * <p>
 * This activity uses different layouts to present its various fragments,
 * depending on the device configuration. {@link NowPlayingMoviesFragment} and
 * {@link MyMoviesFragment} are always available to the user.
 * 
 * <p>
 * On phone-size screens, the two fragments are represented by {@link ActionBar}
 * tabs, and are held inside a {@link ViewPager} to allow horizontal swiping.
 * 
 * <p>
 * On tablets, the two fragments are always visible and are presented as either
 * two panes (landscape) or a grid (portrait).
 */
public class HomeActivity extends BaseActivity implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

    public static final String TAG = HomeActivity.class.getSimpleName();
    
    public static final int PAGE_POPULAR_MOVIES = 0;
    public static final int PAGE_NOW_SHOWING_MOVIES = 1;
    public static final int PAGE_UPCOMING_MOVIES = 2;

    @SuppressWarnings("unused")
    private UpComingMoviesFragment upComingMoviesFragment;
    @SuppressWarnings("unused")
    private PopularMoviesFragment popularMoviesFragment;
    
    private NowPlayingMoviesFragment nowPlayingMoviesFragment;
    private MyMoviesFragment myMoviesFragment;
    

    private ViewPager mViewPager;
    private Menu mOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        UIUtils.enableDisableActivities(this);
        setContentView(R.layout.activity_home);

        FragmentManager fm = getSupportFragmentManager();

        mViewPager = (ViewPager) findViewById(R.id.pager);
        String homeScreenLabel;
        if (mViewPager != null) {

            // Phone setup
            mViewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_r);
            mViewPager.setPageMargin(2);

            final ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab().setText(R.string.title_popular).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_now_showing).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_upcoming).setTabListener(this));

            homeScreenLabel = getString(R.string.app_name);
            gotToPage(PAGE_NOW_SHOWING_MOVIES);

        } else {
            nowPlayingMoviesFragment = (NowPlayingMoviesFragment) fm.findFragmentById(R.id.fragment_now_playing);
            upComingMoviesFragment = (UpComingMoviesFragment) fm.findFragmentById(R.id.fragment_upcoming_movies);

            homeScreenLabel = "Home";
        }

        View homeIcon = findViewById(android.R.id.home);
        ((View) homeIcon.getParent()).setVisibility(View.GONE);
        getActionBar().setHomeButtonEnabled(false);
        getActionBar().setTitle("");

        LOGD("Tracker", homeScreenLabel);

        // Sync data on load
        if (savedInstanceState == null) {
            //triggerRefresh();
        }

    }
    
    private void gotToPage(int position){
        getActionBar().setSelectedNavigationItem(position);
        mViewPager.setCurrentItem(position);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void onClick(View v) {
        Intent intent = new Intent(this, PopUpActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {
    }

    @Override
    public void onPageSelected(int position) {
        getActionBar().setSelectedNavigationItem(position);

        int titleId = -1;
        switch (position) {
        case PAGE_POPULAR_MOVIES:
            titleId = R.string.title_popular;
            break;
        case PAGE_NOW_SHOWING_MOVIES:
            titleId = R.string.title_now_showing;
            break;
        case PAGE_UPCOMING_MOVIES:
            titleId = R.string.title_upcoming;
            break;
        }

        String title = getString(titleId);
        LOGD("Tracker", title);

    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /**
         * Since the pager fragments don't have known tags or IDs, the only way
         * to persist the reference is to use putFragment/getFragment. Remember,
         * we're not persisting the exact Fragment instance. This mechanism
         * simply gives us a way to persist access to the 'current' fragment
         * instance for the given fragment (which changes across orientation
         * changes).
         * 
         * The outcome of all this is that the "Refresh" menu button refreshes
         * the stream across orientation changes.
         */
        if (myMoviesFragment != null) {
            getSupportFragmentManager().putFragment(outState, "now_playing_fragment", nowPlayingMoviesFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (nowPlayingMoviesFragment == null) {
            nowPlayingMoviesFragment = (NowPlayingMoviesFragment) getSupportFragmentManager().getFragment(
                    savedInstanceState, "now_playing_fragment");
        }
    }

    private class HomePagerAdapter extends FragmentPagerAdapter {
        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case PAGE_POPULAR_MOVIES:
                return (popularMoviesFragment = new PopularMoviesFragment());

            case PAGE_NOW_SHOWING_MOVIES:
                return (nowPlayingMoviesFragment = new NowPlayingMoviesFragment());

            case PAGE_UPCOMING_MOVIES:
                return (upComingMoviesFragment = new UpComingMoviesFragment());

            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.home, menu);
        setupSearchMenuItem(menu);

        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupSearchMenuItem(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_refresh:
            triggerRefresh();
            return true;

        case R.id.menu_search:
            if (!UIUtils.hasHoneycomb()) {
                startSearch(null, false, Bundle.EMPTY, false);
                return true;
            }
            break;

        case R.id.menu_about:
            return true;

        case android.R.id.home:
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerRefresh() {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        refreshData();
    }

    /**
     * Either we are launching the application or the user has asked us to
     * refresh the data
     * 
     */
    private void refreshData() {
        int status = MoviesManager.getInstance().getServiceStatus();
        if (status != MovieFetcherService.STATUS_UPDATING) {
            Intent serviceIntent = new Intent(this, MovieFetcherService.class);
            getApplicationContext().startService(serviceIntent);
            showMessage("Updating Please wait");
        }
    }

    /**
     * Show a toast for the message
     * 
     * @param message
     */
    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
        // TODO Auto-generated method stub

    }

}
