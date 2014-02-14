/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 用户时间线
 *
 * @author longkai
 */
public class UserTimeLineFragment extends TimelineFragment {

	private static final String TAG = "UserTimeLineFragment";

	private long uid;
	private String nick;

	@Override
	protected void fetchTweetsFromCloud(boolean isRefresh, long offset) {
		int size = super.getDefaultFetchSize();
		CatnutAPI api = isRefresh
			? TweetAPI.userTimeline(uid, 0, 0, size, 0, 0, 0, 0)
			: TweetAPI.userTimeline(uid, 0, offset, size, 0, 0, 0, 0);
		mRequestQueue.add(new CatnutRequest(
			mActivity,
			api,
			new StatusProcessor.TweetsProcessor(),
			isRefresh ? refreshSuccessListener : loadMoreSuccessListener,
			isRefresh ? refreshFailListener : loadMoreFailListener
		)).setTag(TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uid = getArguments().getLong(Constants.ID, 0L);
		nick = getArguments().getString(User.screen_name, null);
		// 判断是否是当前授权用户的时间线
		if (nick != null) {
			mAdapter = new TweetAdapter(mActivity, nick);
		} else {
			mAdapter = new TweetAdapter(mActivity, mActivity.getDefaultUserNick());
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(
			nick == null || nick.equals(mActivity.getDefaultUserNick()) ?
				getText(R.string.my_timeline) : nick
		);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mPref.getBoolean(PrefFragment.AUTO_FETCH_ON_START, true)) {
			mPullToRefreshLayout.setRefreshing(true);
			fetchTweetsFromCloud(true, 0);
		}

		getListView().addFooterView(mLoadMore);
		setEmptyText(mActivity.getString(R.string.no_tweets));
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int size = super.getDefaultFetchSize();
		StringBuilder where = new StringBuilder(Status.uid).append("=").append(uid);
		String limit = String.valueOf((mCurPage + 1) * size);
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			where.append(" and ").append(Status.columnText).append(" like ").append(CatnutUtils.like(mCurFilter));
			limit = null;
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}
		CursorLoader loader = CatnutUtils.getCursorLoader(
			mActivity,
			CatnutProvider.parse(Status.MULTIPLE),
			new String[]{
				BaseColumns._ID,
				Status.columnText,
				Status.thumbnail_pic,
				Status.comments_count,
				Status.reposts_count,
				Status.attitudes_count,
				Status.source,
				Status.created_at,
			},
			where.toString(),
			null,
			Status.TABLE,
			null,
			"_id desc",
			limit
		);
		return loader;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PrefFragment.TWEET_FONT_SIZE)
			|| key.equals(PrefFragment.CUSTOMIZE_TWEET_FONT)
			|| key.equals(PrefFragment.SHOW_TWEET_THUMBS)) {
			Log.d(TAG, "pref change, the user timeline fragment needs update!");
			// 应用新的偏好
			mAdapter.swapCursor(null);
			if (nick != null) {
				mAdapter = new TweetAdapter(mActivity, nick);
			} else {
				mAdapter = new TweetAdapter(mActivity, mActivity.getDefaultUserNick());
			}
			setListAdapter(mAdapter);
		}
	}
}
