/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.adapter.EmotionsAdapter;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.MultiPartRequest;
import org.catnut.support.TweetImageSpan;
import org.catnut.util.CatnutUtils;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * 发微博
 *
 * @author longkai
 */
public class ComposeTweetActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

	public static final String TAG = "ComposeTweetActivity";

	// app specifics
	private CatnutApp mApp;
	private EasyTracker mTracker;

//	private boolean mKeyboardShown = false; // 软键盘是否显示

	// customized actionbar widgets
	private View mCustomizedBar;
	private TextView mTextCounter;
	private View mSender; // 发送触发按钮
	private View mProgressor; // 发送进度条


	// widgets
	private SlidingPaneLayout mSlidingPaneLayout;
	private GridView mEmotions;
	private ActionBar mActionBar;
	private GridView mPhotos; // 待上传的图片
	private List<Uri> mUris;
	private ArrayAdapter<Uri> mAdapter;

	// str
	private String mTitle;
	private String mEmotionTitle;
	private int mImageThumbSize;

	private ImageView mAvatar;
	private TextView mScreenName;
	private EditText mText;

	// listeners
	private Response.Listener<JSONObject> listener;
	private Response.ErrorListener errorListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose);
		mApp = CatnutApp.getTingtingApp();
		mActionBar = getActionBar();

		mTitle = getString(R.string.compose);
		mEmotionTitle = getString(R.string.add_emotions);
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);

		injectLayout();
		injectActionBar();
		injectListener();

		mActionBar.setIcon(R.drawable.ic_title_compose);
		mActionBar.setTitle(mTitle);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);

		if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
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
		mApp.getRequestQueue().cancelAll(TAG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.compose, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!mSlidingPaneLayout.isOpen()) {
			mSlidingPaneLayout.openPane();
		}
		switch (item.getItemId()) {
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			case R.id.action_gallery:
				// todo: 暂时只支持上传一张图片，因为没有高级权限Orz
				if (mUris != null && mUris.size() > 0) {
					Toast.makeText(this, getString(R.string.only_one_pic_hint), Toast.LENGTH_LONG).show();
				} else {
					startActivityForResult(new Intent(Intent.ACTION_PICK).setType("image/*"), 1);
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_send:
				sendTweet();
				break;
			case R.id.action_discovery:
				int cursor = mText.getSelectionStart();
				mText.getText().append("##");
				mText.setSelection(cursor + 1);
				mText.requestFocus();
				break;
			default:
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (CatnutUtils.hasLength(mText) || (mUris != null && mUris.size() > 0)) {
					abort();
					return true; // deal it
				}
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	// 确定是否放弃已在编辑的内容
	private void abort() {
		new AlertDialog.Builder(this)
				.setMessage(getString(R.string.abort_existing_tweet_alert))
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						navigateUpTo(getIntent());
					}
				})
				.show();
	}

	private void injectLayout() {
		// for panel
		mSlidingPaneLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);
		mEmotions = (GridView) findViewById(R.id.emotions);
		mEmotions.setAdapter(new EmotionsAdapter(this));
		mEmotions.setOnItemClickListener(this);
		mSlidingPaneLayout.setPanelSlideListener(new SliderListener());
		mSlidingPaneLayout.openPane();
		mSlidingPaneLayout.getViewTreeObserver().addOnGlobalLayoutListener(new FirstLayoutListener());
		// for tweet
		mAvatar = (ImageView) findViewById(R.id.avatar);
		mScreenName = (TextView) findViewById(R.id.screen_name);
		mText = (EditText) findViewById(R.id.text);
		// set data to layout...
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mApp.getImageLoader().get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
							ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
					mScreenName.setText("@" + cursor.getString(cursor.getColumnIndex(User.screen_name)));
				}
				cursor.close();
			}
		}.startQuery(0, null,
				CatnutProvider.parse(User.MULTIPLE, mApp.getAccessToken().uid),
				new String[]{User.avatar_large, User.screen_name}, null, null, null);
		// other stuffs...
		mText.addTextChangedListener(this);
	}

	private void injectActionBar() {
		mActionBar.setDisplayShowCustomEnabled(true);
		mCustomizedBar = LayoutInflater.from(this).inflate(R.layout.customized_actionbar, null);
		mTextCounter = (TextView) mCustomizedBar.findViewById(R.id.text_counter);
		mSender = mCustomizedBar.findViewById(R.id.action_send);
		mProgressor = mCustomizedBar.findViewById(android.R.id.progress);
		mActionBar.setCustomView(mCustomizedBar, new ActionBar.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
	}

	private void injectListener() {
		listener = new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				mSender.setVisibility(View.VISIBLE);
				mProgressor.setVisibility(View.GONE);
				// delete posted text and thumbs
				mText.setText(null);
				if (mUris != null) {
					mUris.clear();
					mAdapter.notifyDataSetChanged();
				}
				Toast.makeText(ComposeTweetActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
			}
		};
		errorListener = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				mSender.setVisibility(View.VISIBLE);
				mProgressor.setVisibility(View.GONE);
				Log.e(TAG, "post tweet error!", error);
				WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
				Toast.makeText(ComposeTweetActivity.this, weiboAPIError.error, Toast.LENGTH_SHORT).show();
			}
		};
		mCustomizedBar.findViewById(R.id.action_discovery).setOnClickListener(this);
		mCustomizedBar.findViewById(R.id.action_mention).setOnClickListener(this);
		mCustomizedBar.findViewById(R.id.action_send).setOnClickListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && data != null) {
			if (mPhotos == null) {
				ViewStub viewStub = (ViewStub) findViewById(R.id.view_stub);
				mPhotos = (GridView) viewStub.inflate();
				mUris = new LinkedList<Uri>();
				mAdapter = new ThumbsAdapter(this, mUris);
				mPhotos.setAdapter(mAdapter);
				// 长按删除之
				mPhotos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
						mUris.remove(position);
						mAdapter.notifyDataSetChanged();
						return true;
					}
				});
				// 单击直接查看
				mPhotos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						startActivity(new Intent(Intent.ACTION_VIEW, mUris.get(position)));
					}
				});
			}
			mUris.add(data.getData());
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// no-op
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// no-op
	}

	@Override
	public void afterTextChanged(Editable s) {
		int count = 140 - mText.length();
		mTextCounter.setText(String.valueOf(count));
		if (count >= 10) {
			// def color
			mTextCounter.setTextColor(getResources().getColor(android.R.color.white));
		} else if (count <= 0) { // in fact, never lt 0
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
		} else {
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
		}
	}

	private void sendTweet() {
		if (!CatnutUtils.hasLength(mText)) {
			Toast.makeText(this, R.string.require_not_empty, Toast.LENGTH_SHORT).show();
			return; // stop here
		}
		// 防止多次提交
		mSender.setVisibility(View.GONE);
		mProgressor.setVisibility(View.VISIBLE);
		if (mUris != null && mUris.size() > 0) { // 有图片的
			mApp.getRequestQueue().add(new MultiPartRequest(
					this,
					TweetAPI.upload(mText.getText().toString(), 0, null, mUris, 0f, 0f, null, null),
					new StatusProcessor.SingleTweetProcessor(Status.HOME),
					listener,
					errorListener
			)).setTag(TAG);
		} else {
			mApp.getRequestQueue().add(new CatnutRequest(
					this,
					TweetAPI.update(mText.getText().toString(), 0, null, 0f, 0f, null, null),
					new StatusProcessor.SingleTweetProcessor(Status.HOME),
					listener,
					errorListener
			)).setTag(TAG);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// 插入到编辑框里
		int cursor = mText.getSelectionStart();
		mText.getText().insert(cursor, CatnutUtils.text2Emotion(this, TweetImageSpan.EMOTION_KEYS[position]));
		// focus
		mText.requestFocus();
	}

	private class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {
		@Override
		public void onPanelOpened(View panel) {
			ComposeTweetActivity.this.onPanelOpened();
		}

		@Override
		public void onPanelClosed(View panel) {
			ComposeTweetActivity.this.onPanelClosed();
		}
	}

	private class FirstLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
		@Override
		public void onGlobalLayout() {
			onFirstLayout();
			mSlidingPaneLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		}
	}

	private void onPanelClosed() {
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		mActionBar.setTitle(mEmotionTitle);
	}

	private void onPanelOpened() {
		mActionBar.setHomeButtonEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setTitle(mTitle);
		mText.requestFocus();
	}

	private void onFirstLayout() {
		if (mSlidingPaneLayout.isSlideable() && !mSlidingPaneLayout.isOpen()) {
			onPanelClosed();
		} else {
			onPanelOpened();
		}
	}

	private class ThumbsAdapter extends ArrayAdapter<Uri> {

		public ThumbsAdapter(Context context, List<Uri> uris) {
			super(context, 0, uris);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView photo;
			if (convertView == null) {
				photo = new ImageView(ComposeTweetActivity.this);
				photo.setBackgroundResource(R.drawable.overflow_dropdown_light);
				photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				photo = (ImageView) convertView;
			}
			// load image efficiently
			Picasso.with(getContext())
					.load(getItem(position))
					.resize(mImageThumbSize, mImageThumbSize)
					.centerCrop()
					.into(photo);
			return photo;
		}
	}
}
