package co.in.threecolors.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import co.in.threecolors.R;


/**
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class PopUpActivity extends Activity {

	public static final String TAG = PopUpActivity.class.getSimpleName();

	private Interpolator mInterpolator = new DecelerateInterpolator(1.2f);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isFinishing()) {
			return;
		}
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_dailog_pop);
		setTitle("");
		
	}

	@Override
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

	public Interpolator getInterpolator() {
		return mInterpolator;
	}

	public void setInterpolator(Interpolator i) {
		mInterpolator = i;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

}
