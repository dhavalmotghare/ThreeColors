package co.in.threecolors.ui.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.RelativeLayout.LayoutParams;

public class MarginAnimation extends Animation {

	private int mEnd;
	private int mStart;
	private int mChange;
	private View mView;

	public MarginAnimation(View v, int marginStart, int marginEnd, Interpolator i) {
		mView = v;
		mStart = marginStart;
		mEnd = marginEnd;
		mChange = mEnd - mStart;
		setInterpolator(i);
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {

		float change = mChange * interpolatedTime;
		LayoutParams lp = (LayoutParams) mView.getLayoutParams();
		lp.setMargins(0,-(int) (mStart + change),0, (int) (mStart + change));
		mView.setLayoutParams(lp);

		super.applyTransformation(interpolatedTime, t);
	}

}
