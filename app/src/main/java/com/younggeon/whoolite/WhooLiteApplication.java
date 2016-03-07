package com.younggeon.whoolite;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.younggeon.whoolite.constant.PreferenceKeys;

import java.util.UUID;

/**
 * Created by sadless on 2015. 9. 28..
 */
public class WhooLiteApplication extends Application {
    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        WhooLiteNetwork.setContextForNetworking(getApplicationContext());
        getDefaultTracker();
    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String id = prefs.getString(PreferenceKeys.ID_FOR_TRACKER, null);

            if (id == null) {
                id = UUID.randomUUID().toString();
                prefs.edit().putString(PreferenceKeys.ID_FOR_TRACKER, id).apply();
            }
            mTracker = analytics.newTracker(R.xml.ga_config);
            mTracker.set("&uid", id);
        }

        return mTracker;
    }
}
