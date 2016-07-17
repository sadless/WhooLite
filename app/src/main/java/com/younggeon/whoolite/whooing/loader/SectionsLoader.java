package com.younggeon.whoolite.whooing.loader;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.realm.Section;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by sadless on 2016. 1. 26..
 */
public class SectionsLoader extends WhooingBaseLoader {
    private static final String URI_SECTIONS = "https://whooing.com/api/sections";

    public SectionsLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        switch (mMethod) {
            case Request.Method.GET: {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                int resultCode;

                if (prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null) == null) {
                    Uri.Builder builder = Uri.parse(URI_SECTIONS).buildUpon();

                    builder.appendPath("default.json");
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                            builder.build().toString(),
                            mRequestFuture,
                            mRequestFuture,
                            mApiKeyFormat));

                    try {
                        JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                        if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                            JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);

                            prefs.edit()
                                    .putString(PreferenceKeys.CURRENT_SECTION_ID,
                                            resultItem.optString(WhooingKeyValues.SECTION_ID)).apply();
                        } else {
                            return resultCode;
                        }
                    } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();

                        return -1;
                    }
                    mRequestFuture = RequestFuture.newFuture();
                }
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        URI_SECTIONS + ".json_array",
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat));
                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                        JSONArray sections = result.optJSONArray(WhooingKeyValues.RESULT);
                        Realm realm = Realm.getDefaultInstance();
                        ArrayList<Section> objects = new ArrayList<>();
                        RealmQuery<Section> query = realm.where(Section.class);

                        for (int i = 0; i < sections.length(); i++) {
                            JSONObject section = sections.optJSONObject(i);
                            Section object = new Section();
                            String sectionId = section.optString(WhooingKeyValues.SECTION_ID);

                            object.setSectionId(sectionId);
                            object.setTitle(section.optString(WhooingKeyValues.TITLE));
                            object.setMemo(section.optString(WhooingKeyValues.MEMO));
                            object.setCurrency(section.optString(WhooingKeyValues.CURRENCY));
                            object.setDateFormat(section.optString(WhooingKeyValues.DATE_FORMAT));
                            object.setSortOrder(i);
                            objects.add(object);
                            query = query.notEqualTo("sectionId", sectionId);
                        }

                        RealmResults<Section> willDeleteSections = query.findAll();
                        
                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(objects);
                        willDeleteSections.deleteAllFromRealm();
                        realm.commitTransaction();
                        realm.close();
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();

                    resultCode = -1;
                }

                return resultCode;
            }
            default: {
                return null;
            }
        }
    }
}
