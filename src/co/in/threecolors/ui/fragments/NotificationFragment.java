package co.in.threecolors.ui.fragments;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import co.in.threecolors.R;
import co.in.threecolors.ui.HomeActivity;
import co.in.threecolors.ui.util.UIUtils;

/**
 * A fragment used in {@link HomeActivity} that shows info, notification a
 * generic message
 */
public class NotificationFragment extends Fragment implements LoaderCallbacks<Cursor> {

	private ViewGroup mRootView;
	private Cursor mMessagesCursor;
	@SuppressWarnings("unused")
	private LayoutInflater mInflater;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mInflater = inflater;
		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_notifications, container);
		refresh();
		return mRootView;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getActivity().getContentResolver().unregisterContentObserver(mObserver);
	}

	private void refresh() {
		mRootView.removeAllViews();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (getActivity() == null) {
			return;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mMessagesCursor = null;
	}

	public class MessageAdapter extends PagerAdapter {

		@Override
		public Object instantiateItem(ViewGroup pager, int position) {
			return null;
		}

		@SuppressWarnings("unused")
		private final OnClickListener messageClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = (String) v.getTag();
				if (!TextUtils.isEmpty(url)) {
					UIUtils.safeOpenLink(getActivity(), new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				}
			}
		};

		@Override
		public void destroyItem(ViewGroup pager, int position, Object view) {
			pager.removeView((View) view);
		}

		@Override
		public int getCount() {
			return mMessagesCursor.getCount();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	}

	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() == null) {
				return;
			}

		}
	};
}
