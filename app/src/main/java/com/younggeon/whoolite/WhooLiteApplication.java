package com.younggeon.whoolite;

import android.app.Application;

/**
 * Created by sadless on 2015. 9. 28..
 */
public class WhooLiteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        WhooLiteNetwork.setContextForNetworking(getApplicationContext());
    }
}
