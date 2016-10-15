package com.younggeon.whoolite.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.younggeon.whoolite.BuildConfig;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.WelcomeActivity;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.realm.Account;
import com.younggeon.whoolite.realm.Entry;
import com.younggeon.whoolite.realm.FrequentItem;
import com.younggeon.whoolite.realm.Section;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by sadless on 2015. 10. 19..
 */
public class Utility {
    public static boolean checkResultCodeWithAlert(final Context context, int code) {
        switch (code) {
            case 405: {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.token_expired)
                        .setMessage(R.string.token_expired_message)
                        .setPositiveButton(R.string.re_login, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                logout(context);
                            }
                        }).setCancelable(false)
                        .create().show();
                return false;
            }
            default: {
                return true;
            }
        }
    }

    public static void logout(Context context) {
        Realm realm = Realm.getDefaultInstance();

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(PreferenceKeys.API_KEY_FORMAT)
                .remove(PreferenceKeys.CURRENT_SECTION_ID)
                .remove(context.getString(R.string.pref_key_show_slot_numbers)).apply();
        realm.removeAllChangeListeners();
        realm.beginTransaction();
        realm.delete(Section.class);
        realm.delete(Account.class);
        realm.delete(FrequentItem.class);
        realm.commitTransaction();
        realm.close();
        context.sendBroadcast(new Intent(Actions.FINISH));
        context.startActivity(new Intent(context, WelcomeActivity.class));
    }

    public static SimpleDateFormat getDateFormatFromWhooingDateFormat(String whooingDateFormat) {
        String dateFormatString = "";

        for (int i = 0; i < whooingDateFormat.length(); i++) {
            if (i > 0) {
                dateFormatString += "-";
            }
            switch (whooingDateFormat.charAt(i)) {
                case 'Y': {
                    dateFormatString += "yyyy";
                    break;
                }
                case 'M': {
                    dateFormatString += "MM";
                    break;
                }
                case 'D': {
                    dateFormatString += "dd";
                    break;
                }
            }
        }
        dateFormatString += " E";

        return new SimpleDateFormat(dateFormatString, Locale.getDefault());
    }

    public static void setAdView(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(BuildConfig.TEST_DEVICE)
                .build();

        adView.loadAd(adRequest);
        adView.setVisibility(View.VISIBLE);
    }

    public static RealmResults<Entry> getDuplicateEntries(Realm realm, Bundle args) {
        String memo = args.getString(WhooingKeyValues.MEMO);
        long entryId = args.getLong(WhooingKeyValues.ENTRY_ID, -1);

        if (memo == null) {
            memo = "";
        }

        RealmQuery<Entry> query = realm.where(Entry.class).equalTo("sectionId", args.getString(WhooingKeyValues.SECTION_ID))
                .equalTo("entryDate", Integer.parseInt(args.getString(WhooingKeyValues.ENTRY_DATE)))
                .equalTo("title", args.getString(WhooingKeyValues.ITEM_TITLE))
                .equalTo("leftAccountType", args.getString(WhooingKeyValues.LEFT_ACCOUNT_TYPE))
                .equalTo("leftAccountId", args.getString(WhooingKeyValues.LEFT_ACCOUNT_ID))
                .equalTo("rightAccountType", args.getString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE))
                .equalTo("rightAccountId", args.getString(WhooingKeyValues.RIGHT_ACCOUNT_ID))
                .equalTo("memo", memo);

        if (entryId >= 0) {
            query = query.notEqualTo("entryId", entryId);
        }

        return query.findAll();
    }
}
