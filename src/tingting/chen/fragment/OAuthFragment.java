/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import tingting.chen.R;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.metadata.WeiboAPIError;
import tingting.chen.ui.MainActivity;
import tingting.chen.util.Manifest;

/**
 * 使用Oauth2的方式获取新浪微博的认证
 *
 * @author longkai
 */
public class OAuthFragment extends Fragment {

	private static final String TAG = "OAuthFragment";

	private WebView mWebView;
	private ActionBar mActionBar;
	private TingtingApp mApp;
	private RequestQueue mRequestQueue;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = TingtingApp.getTingtingApp();
		mRequestQueue = mApp.getRequestQueue();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set actionbar
		mActionBar = getActivity().getActionBar();
		setHasOptionsMenu(true);

		// set the auth webview
		mWebView = new WebView(getActivity());
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mWebView.setLayoutParams(params);

		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
			String code = Uri.parse(url).getQueryParameter("code");
			Log.d(TAG, "the auth code is " + code);
			String accessTokenUri = Manifest.getAccessTokenUri(code);
			mRequestQueue.add(new JsonObjectRequest(
					Request.Method.POST,
					accessTokenUri,
					null,
					new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {
							Log.i(TAG, "auth success with result: " + response.toString());
							mApp.saveAccessToken(response);
							Toast.makeText(getActivity(), getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
							startActivity(new Intent(getActivity(), MainActivity.class));
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.wtf(TAG, "auth fail!", error);
							WeiboAPIError e = WeiboAPIError.fromVolleyError(error);
							ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(null, e.error);
							fragment.show(getFragmentManager(), null);
						}
					}
			)).setTag(TAG);
			return true;
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return mWebView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// clear cache
		WebSettings settings = mWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.setSaveFormData(false);
		mWebView.loadUrl(Manifest.getOAuthUri());

		// set actionbar title
		mActionBar.setTitle(getString(R.string.oauth));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refresh = menu.add(Menu.NONE, R.id.refresh, Menu.NONE, getString(R.string.refresh));
		refresh.setIcon(R.drawable.navigation_refresh_light);
		refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				mWebView.reload();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		mWebView.clearCache(true);
		super.onDestroy();
	}
}
