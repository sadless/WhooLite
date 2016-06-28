package com.younggeon.whoolite;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by sadless on 2015. 9. 28..
 */
public class WhooLiteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());
        WhooLiteNetwork.setContextForNetworking(getApplicationContext());
    }
}
