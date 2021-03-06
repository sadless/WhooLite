package com.younggeon.whoolite.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.BuildConfig;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.activity.WhooingLoginActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.databinding.FragmentWhooingLoginBinding;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by sadless on 2015. 9. 23..
 */
public class WhooingLoginActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<JSONObject> {
    private static final int LOADER_ID_REQUEST_TOKEN = 1;
    private static final int LOADER_ID_REQUEST_ACCESS_TOKEN = 2;

    private static final String URI_AUTHORIZE = "https://whooing.com/app_auth/authorize";

    private static final String INSTANCE_STATE_TOKEN = "token";
    private static final String INSTANCE_STATE_FAILED = "failed";
    private static final String INSTANCE_STATE_PIN = "pin";

    private static WebView sLoginWebView;
    private static ViewGroup sOldRootView;

    private ObservableField<String> mToken;
    private ObservableBoolean mFailed;
    private ObservableField<String> mPin;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentWhooingLoginBinding binding = DataBindingUtil.bind(inflater.inflate(R.layout.fragment_whooing_login, container, false));

        if (savedInstanceState == null) {
            sLoginWebView = binding.loginWebView;
            sLoginWebView.getSettings().setJavaScriptEnabled(true);
            mToken = new ObservableField<>(null);
            mFailed = new ObservableBoolean(false);
            mPin = new ObservableField<>(null);
        } else {
            ViewGroup rootView = (ViewGroup)binding.getRoot();

            rootView.removeView(binding.loginWebView);
            sOldRootView.removeView(sLoginWebView);
            rootView.addView(sLoginWebView, 0);
            mToken = new ObservableField<>(savedInstanceState.getString(INSTANCE_STATE_TOKEN));
            mFailed = new ObservableBoolean(savedInstanceState.getBoolean(INSTANCE_STATE_FAILED));
            mPin = new ObservableField<>(savedInstanceState.getString(INSTANCE_STATE_PIN));
        }
        binding.setFragment(this);
        binding.setToken(mToken);
        binding.setFailed(mFailed);
        binding.setPin(mPin);
        sLoginWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String pin;

                if ((pin = uri.getQueryParameter(WhooingKeyValues.PIN)) != null) {
                    mPin.set(pin);

                    RequestAccessTokenLoader requestAccessTokenLoader
                            = RequestAccessTokenLoader.castLoader(
                                getLoaderManager()
                                        .initLoader(LOADER_ID_REQUEST_ACCESS_TOKEN, null, WhooingLoginActivityFragment.this));

                    requestAccessTokenLoader.token = mToken.get();
                    requestAccessTokenLoader.pin = pin;
                    requestAccessTokenLoader.forceLoad();

                    return true;
                }

                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().removeAllCookies(null);
        } else {
            CookieManager.getInstance().removeAllCookie();
        }
        if (getLoaderManager().getLoader(LOADER_ID_REQUEST_ACCESS_TOKEN) == null) {
            Loader requestTokenLoader = getLoaderManager().initLoader(LOADER_ID_REQUEST_TOKEN, null, this);

            if (!requestTokenLoader.isStarted()) {
                requestTokenLoader.forceLoad();
            }
        } else {
            getLoaderManager().initLoader(LOADER_ID_REQUEST_ACCESS_TOKEN, null, this);
        }
        sOldRootView = (ViewGroup)binding.getRoot();

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(INSTANCE_STATE_TOKEN, mToken.get());
        outState.putBoolean(INSTANCE_STATE_FAILED, mFailed.get());
        outState.putString(INSTANCE_STATE_PIN, mPin.get());
    }

    @Override
    public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_REQUEST_TOKEN: {
                return new RequestTokenLoader(getActivity());
            }
            case LOADER_ID_REQUEST_ACCESS_TOKEN: {
                return new RequestAccessTokenLoader(getActivity());
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
        switch (loader.getId()) {
            case LOADER_ID_REQUEST_TOKEN: {
                if (data == null) {
                    mFailed.set(true);
                    getLoaderManager().destroyLoader(LOADER_ID_REQUEST_TOKEN);
                } else if (sLoginWebView.getUrl() == null) {
                    String token = data.optString(WhooingKeyValues.TOKEN);

                    mToken.set(token);

                    Uri uri = Uri.parse(URI_AUTHORIZE).buildUpon()
                            .appendQueryParameter(WhooingKeyValues.TOKEN, token).build();

                    sLoginWebView.loadUrl(uri.toString());
                }
                break;
            }
            case LOADER_ID_REQUEST_ACCESS_TOKEN: {
                if (data == null) {
                    mFailed.set(true);
                    getLoaderManager().destroyLoader(LOADER_ID_REQUEST_ACCESS_TOKEN);
                } else {
                    Intent intent = new Intent();
                    String apiKeyFormat = WhooingKeyValues.APP_ID + "=" + BuildConfig.WHOOING_APP_ID + "," +
                            WhooingKeyValues.TOKEN + "=" + data.optString(WhooingKeyValues.TOKEN) + "," +
                            WhooingKeyValues.SIGNATURE + "=" + sha1(BuildConfig.WHOOING_APP_SECRET + "|" +
                            data.optString(WhooingKeyValues.TOKEN_SECRET)) + "," +
                            WhooingKeyValues.NONCE + "=" + getContext().getString(R.string.app_name) + "," +
                            WhooingKeyValues.TIMESTAMP + "=%d";

                    intent.putExtra(WhooingLoginActivity.EXTRA_API_KEY_FORMAT, apiKeyFormat);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                }
                break;
            }
            default:
        }
    }

    @Override
    public void onLoaderReset(Loader<JSONObject> loader) {
    }

    public boolean webViewGoBack() {
        boolean retVal = sLoginWebView.canGoBack();

        if (retVal) {
            sLoginWebView.goBack();
        }

        return retVal;
    }

    public void retryClicked() {
        mFailed.set(false);

        Loader requestTokenLoader = getLoaderManager().getLoader(LOADER_ID_REQUEST_TOKEN);

        if (requestTokenLoader != null) {
            getLoaderManager().initLoader(LOADER_ID_REQUEST_ACCESS_TOKEN, null, WhooingLoginActivityFragment.this).forceLoad();
        } else {
            getLoaderManager().initLoader(LOADER_ID_REQUEST_TOKEN, null, WhooingLoginActivityFragment.this).forceLoad();
        }
    }

    private String sha1(String str) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
            byte[] result = mDigest.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();

            for (byte b : result) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

            return null;
        }
    }

    public static class RequestTokenLoader extends AsyncTaskLoader<JSONObject> {
        private static final String URI_REQUEST_TOKEN = "https://whooing.com/app_auth/request_token";

        public RequestTokenLoader(Context context) {
            super(context);
        }

        @Override
        public JSONObject loadInBackground() {
            Uri uri = Uri.parse(URI_REQUEST_TOKEN).buildUpon()
                    .appendQueryParameter(WhooingKeyValues.APP_ID, "" + BuildConfig.WHOOING_APP_ID)
                    .appendQueryParameter(WhooingKeyValues.APP_SECRET, BuildConfig.WHOOING_APP_SECRET).build();
            RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

            WhooLiteNetwork.requestQueue.add(new JsonObjectRequest(Request.Method.GET,
                    uri.toString(),
                    null,
                    requestFuture,
                    requestFuture));
            try {
                return requestFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();

                return null;
            }
        }
    }

    public static class RequestAccessTokenLoader extends AsyncTaskLoader<JSONObject> {
        private static final String URI_REQUEST_ACCESS_TOKEN = "https://whooing.com/app_auth/access_token";

        public String token;
        public String pin;

        public RequestAccessTokenLoader(Context context) {
            super(context);
        }

        @Override
        public JSONObject loadInBackground() {
            Uri uri = Uri.parse(URI_REQUEST_ACCESS_TOKEN).buildUpon()
                    .appendQueryParameter(WhooingKeyValues.APP_ID, "" + BuildConfig.WHOOING_APP_ID)
                    .appendQueryParameter(WhooingKeyValues.APP_SECRET, BuildConfig.WHOOING_APP_SECRET)
                    .appendQueryParameter(WhooingKeyValues.TOKEN, token)
                    .appendQueryParameter(WhooingKeyValues.PIN, pin).build();
            RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

            WhooLiteNetwork.requestQueue.add(new JsonObjectRequest(Request.Method.GET,
                    uri.toString(),
                    null,
                    requestFuture,
                    requestFuture));
            try {
                return requestFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();

                return null;
            }
        }

        public static RequestAccessTokenLoader castLoader(Loader loader) {
            return (RequestAccessTokenLoader)loader;
        }
    }
}
