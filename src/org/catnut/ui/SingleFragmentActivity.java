/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.DraftFragment;
import org.catnut.fragment.FavoriteFragment;
import org.catnut.fragment.MyRelationshipFragment;
import org.catnut.fragment.PhotoViewerFragment;
import org.catnut.fragment.PrefFragment;
import org.catnut.fragment.UserTimelineFragment;
import org.catnut.metadata.User;
import org.catnut.util.Constants;

/**
 * helper activity, just show a single fragment
 *
 * @author longkai
 */
public class SingleFragmentActivity extends Activity {

	public static final String TAG = "SingleFragmentActivity";
	public static final int PREF = 0;
	public static final int PHOTO_VIEWER = 1;
	public static final int DRAFT = 2;
	public static final int USER_TWEETS = 3;
	public static final int FRIENDS = 4; // 可以为关注或者粉丝
	public static final int FAVORITES = 5;

	private EasyTracker mTracker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			int which = getIntent().getIntExtra(TAG, PREF);
			Fragment fragment = null;
			switch (which) {
				case PREF:
					fragment = PrefFragment.getFragment();
					break;
				case PHOTO_VIEWER:
					getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
					String picUrl = getIntent().getStringExtra(Constants.PIC);
					fragment = PhotoViewerFragment.getFragment(picUrl);
					break;
				case DRAFT:
					fragment = DraftFragment.getFragment();
					break;
				case FAVORITES:
					fragment = FavoriteFragment.getFragment();
					break;
				case FRIENDS:
					fragment = MyRelationshipFragment.getFragment(getIntent().getBooleanExtra(User.following, true));
					break;
				case USER_TWEETS:
					long id = getIntent().getLongExtra(Constants.ID, 0L);
					String screenName = getIntent().getStringExtra(User.screen_name);
					fragment = UserTimelineFragment.getFragment(id, screenName);
					break;
				default:
					// get out!
					navigateUpTo(getIntent());
					break;
			}
			ft.replace(android.R.id.content, fragment).commit();
		}
		if (CatnutApp.getTingtingApp().getPreferences()
				.getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUpTo(getIntent());
				break;
			default:
				break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * helper method for obtaining a intent which will go to this activity
	 */
	public static Intent getIntent(Context context, int which) {
		Intent intent = new Intent(context, SingleFragmentActivity.class);
		intent.putExtra(TAG, which);
		return intent;
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
}