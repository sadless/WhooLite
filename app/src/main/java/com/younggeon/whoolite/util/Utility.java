package com.younggeon.whoolite.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.WelcomeActivity;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.provider.WhooingProvider;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
                        }).create().show();
                return false;
            }
            default: {
                return true;
            }
        }
    }

    public static void logout(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(PreferenceKeys.API_KEY_FORMAT)
                .remove(PreferenceKeys.CURRENT_SECTION_ID).apply();
        context.getContentResolver().delete(WhooingProvider.getSectionsUri(),
                null,
                null);
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
}
