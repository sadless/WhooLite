package com.younggeon.whoolite;

import android.app.Application;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

/**
 * Created by sadless on 2015. 9. 28..
 */
public class WhooLiteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        if (oldVersion < 1) {
                            realm.getSchema().get("FrequentItem")
                                    .addField("searchKeyword", String.class);
                        }
                    }
                }).build());
        WhooLiteNetwork.setContextForNetworking(getApplicationContext());
    }
}
