/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.ProfileFragment;
import org.catnut.metadata.User;
import org.catnut.util.Constants;

/**
 * 用户信息界面
 *
 * @author longkai
 */
public class ProfileActivity extends Activity implements FragmentManager.OnBackStackChangedListener {

	private CatnutApp mApp;
	private EasyTracker mTracker;

	private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			mApp = CatnutApp.getTingtingApp();
			if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
				mTracker = EasyTracker.getInstance(this);
			}
			String screenName = getIntent().getStringExtra(User.screen_name);
			long uid = getIntent().getLongExtra(Constants.ID, 0L);
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.addOnBackStackChangedListener(this);
			fragmentManager
					.beginTransaction()
					.replace(android.R.id.content, ProfileFragment.getFragment(uid, screenName))
					.commit();
			ActionBar bar = getActionBar();
			bar.setDisplayShowHomeEnabled(false);
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setTitle(screenName);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.activityStart(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mTracker != null) {
			mTracker.activityStop(this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.navigateUpTo(getIntent());
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackStackChanged() {
		invalidateOptionsMenu();
	}

	public void flipCard(Fragment fragment, String tag, boolean backStack) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction
				.setCustomAnimations(
						R.animator.card_flip_right_in, R.animator.card_flip_right_out,
						R.animator.card_flip_left_in, R.animator.card_flip_left_out)
				.replace(android.R.id.content, fragment, tag);
		if (backStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commit();
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}
}