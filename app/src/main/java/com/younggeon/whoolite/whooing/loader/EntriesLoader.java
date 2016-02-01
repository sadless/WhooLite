package com.younggeon.whoolite.whooing.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.android.volley.Request;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by sadless on 2015. 11. 8..
 */
public class EntriesLoader extends WhooingBaseLoader {
    private static final String URI_ENTRIES = "https://whooing.com/api/entries";

    public static final String ARG_SLOT_NUMBER = "slot_number";

    public EntriesLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        String sectionId = args.getString(WhooingKeyValues.SECTION_ID);

        switch (mMethod) {
            case Request.Method.POST: {
                Uri.Builder builder = Uri.parse(URI_ENTRIES + ".json").buildUpon();
                String frequentItemId = args.getString(WhooingKeyValues.ITEM_ID);
                int slotNumber = args.getInt(ARG_SLOT_NUMBER, -1);
                int resultCode;

                args.remove(WhooingKeyValues.ITEM_ID);
                args.remove(ARG_SLOT_NUMBER);
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        builder.build().toString(),
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat,
                        args));
                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if (slotNumber > 0) {
                        Uri frequentItemUri = WhooingProvider.getFrequentItemUri(sectionId,
                                slotNumber,
                                frequentItemId);
                        Cursor c = getContext().getContentResolver()
                                .query(frequentItemUri,
                                        new String[]{FrequentItems.COLUMN_USE_COUNT, FrequentItems.COLUMN_LAST_USE_TIME},
                                        null,
                                        null,
                                        null);

                        if (c != null) {
                            ContentValues cv = new ContentValues();

                            c.moveToFirst();
                            cv.put(FrequentItems.COLUMN_USE_COUNT, c.getInt(0) + 1);
                            cv.put(FrequentItems.COLUMN_LAST_USE_TIME, (new Date()).getTime());
                            getContext().getContentResolver().update(frequentItemUri,
                                    cv,
                                    null,
                                    null);
                            c.close();
                        }
                    }
                    resultCode = result.optInt(WhooingKeyValues.CODE);
                    if (resultCode == WhooingKeyValues.SUCCESS) {
                        JSONObject resultItem = result.optJSONArray(WhooingKeyValues.RESULT).optJSONObject(0);
                        ContentValues cv = new ContentValues();

                        cv.put(Entries.COLUMN_ENTRY_ID, resultItem.optLong(WhooingKeyValues.ENTRY_ID));
                        cv.put(Entries.COLUMN_ENTRY_DATE, resultItem.optDouble(WhooingKeyValues.ENTRY_DATE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_ID, resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_ID, resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_TITLE, resultItem.optString(WhooingKeyValues.ITEM_TITLE));
                        cv.put(Entries.COLUMN_MONEY, resultItem.optDouble(WhooingKeyValues.MONEY));
                        cv.put(Entries.COLUMN_MEMO, resultItem.optString(WhooingKeyValues.MEMO));
                        getContext().getContentResolver()
                                .insert(WhooingProvider.getEntriesUri(sectionId),
                                        cv);
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                    e.printStackTrace();

                    resultCode = -1;
                }
                args.putString(WhooingKeyValues.ITEM_ID, frequentItemId);
                args.putInt(ARG_SLOT_NUMBER, slotNumber);

                return resultCode;
            }
            case Request.Method.DELETE: {
                ArrayList<String> selectedItems = args.getStringArrayList(ARG_SELECTED_ITEMS);

                if (selectedItems == null) {
                    Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();
                    long entryId = args.getLong(WhooingKeyValues.ENTRY_ID);

                    builder.appendPath(entryId + ".json");
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                            builder.build().toString(),
                            mRequestFuture,
                            mRequestFuture,
                            mApiKeyFormat));

                    try {
                        JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));
                        int resultCode = result.optInt(WhooingKeyValues.RESULT);

                        if (resultCode == WhooingKeyValues.SUCCESS) {
                            getContext().getContentResolver().delete(WhooingProvider.getEntryItemUri(sectionId, entryId),
                                    null,
                                    null);
                        }

                        return resultCode;
                    } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();

                        return -1;
                    }
                } else {
                    String itemIdsPath = "" + selectedItems.get(0);
                    Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();

                    for (int i = 1; i < selectedItems.size(); i++) {
                        itemIdsPath += "," + selectedItems.get(i);
                    }
                    builder.appendEncodedPath(itemIdsPath + ".json");
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                            builder.build().toString(),
                            mRequestFuture,
                            mRequestFuture,
                            mApiKeyFormat));
                    try {
                        JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));
                        int resultCode = result.optInt(WhooingKeyValues.CODE);

                        if (resultCode == WhooingKeyValues.SUCCESS) {
                            String itemIdArray = "(" + itemIdsPath + ")";

                            getContext().getContentResolver().delete(WhooingProvider.getEntriesUri(sectionId),
                                    Entries.COLUMN_ENTRY_ID + " IN " + itemIdArray,
                                    null);
                        }

                        return resultCode;
                    } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                        e.printStackTrace();

                        return -1;
                    }
                }
            }
            case Request.Method.PUT: {
                Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();
                int resultCode;
                long entryId = args.getLong(WhooingKeyValues.ENTRY_ID);

                builder.appendPath(entryId + ".json");
                args.remove(WhooingKeyValues.ENTRY_ID);
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        builder.build().toString(),
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat,
                        args));

                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    resultCode = result.optInt(WhooingKeyValues.CODE);
                    if (resultCode == WhooingKeyValues.SUCCESS) {
                        JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);
                        ContentValues cv = new ContentValues();

                        cv.put(Entries.COLUMN_ENTRY_DATE, resultItem.optDouble(WhooingKeyValues.ENTRY_DATE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_ID, resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_ID, resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_TITLE, resultItem.optString(WhooingKeyValues.ITEM_TITLE));
                        cv.put(Entries.COLUMN_MONEY, resultItem.optDouble(WhooingKeyValues.MONEY));
                        cv.put(Entries.COLUMN_MEMO, resultItem.optString(WhooingKeyValues.MEMO));
                        getContext().getContentResolver().update(WhooingProvider
                                        .getEntryItemUri(args.getString(WhooingKeyValues.SECTION_ID), entryId),
                                cv, null, null);
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                    resultCode = -1;
                }
                args.putLong(WhooingKeyValues.ENTRY_ID, entryId);

                return resultCode;
            }
            case Request.Method.GET: {
                Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();

                builder.appendPath("latest.json")
                        .appendQueryParameter(WhooingKeyValues.SECTION_ID, sectionId);
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        builder.build().toString(),
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat));

                int resultCode;

                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                        JSONArray entries = result.optJSONArray(WhooingKeyValues.RESULT);
                        ContentValues[] values = new ContentValues[entries.length()];

                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject entry = entries.optJSONObject(i);

                            values[i] = new ContentValues();
                            values[i].put(Entries.COLUMN_ENTRY_ID, entry.optLong(WhooingKeyValues.ENTRY_ID));
                            values[i].put(Entries.COLUMN_TITLE, entry.optString(WhooingKeyValues.ITEM_TITLE));
                            values[i].put(Entries.COLUMN_MONEY, entry.optDouble(WhooingKeyValues.MONEY));
                            values[i].put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, entry.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                            values[i].put(Entries.COLUMN_LEFT_ACCOUNT_ID, entry.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                            values[i].put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, entry.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                            values[i].put(Entries.COLUMN_RIGHT_ACCOUNT_ID, entry.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                            values[i].put(Entries.COLUMN_MEMO, entry.optString(WhooingKeyValues.MEMO));
                            values[i].put(Entries.COLUMN_ENTRY_DATE, entry.optDouble(WhooingKeyValues.ENTRY_DATE));
                        }
                        getContext().getContentResolver().bulkInsert(WhooingProvider.getEntriesUri(sectionId),
                                values);
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();

                    resultCode = -1;
                }

                return resultCode;
            }
            default: {
                break;
            }
        }

        return -1;
    }

    public static EntriesLoader castLoader(Loader loader) {
        return (EntriesLoader) loader;
    }
}
