package com.younggeon.whoolite.whooing.loader;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.constant.PreferenceKeys;

/**
 * Created by sadless on 2016. 1. 17..
 */
public abstract class WhooingBaseLoader extends AsyncTaskLoader<Integer> {
    public static final String ARG_CURSOR_INDEX = "cursor_index";

    public Bundle args;

    protected int mMethod;
    protected RequestFuture<String> mRequestFuture;
    protected String mApiKeyFormat;

    public WhooingBaseLoader(Context context, int method, Bundle args) {
        super(context);

        this.mMethod = method;
        this.args = args;
        mRequestFuture = RequestFuture.newFuture();
        mApiKeyFormat = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PreferenceKeys.API_KEY_FORMAT, null);
    }

    public static WhooingBaseLoader castLoader(Loader loader) {
        return (WhooingBaseLoader) loader;
    }
}
