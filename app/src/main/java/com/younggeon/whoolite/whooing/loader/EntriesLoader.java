package com.younggeon.whoolite.whooing.loader;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.Loader;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
//                        Uri useCountUri = WhooingProvider.getFrequentItemUseCountUri(sectionId,
//                                slotNumber,
//                                frequentItemId);
//                        Cursor c = getContext().getContentResolver()
//                                .query(useCountUri,
//                                        new String[]{FrequentItemUseCount.COLUMN_USE_COUNT},
//                                        null,
//                                        null,
//                                        null);
//
//                        if (c != null) {
//                            ContentValues cv = new ContentValues();
//
//                            if (c.moveToFirst()) {
//                                cv.put(FrequentItemUseCount.COLUMN_USE_COUNT, c.getInt(0) + 1);
//                                getContext().getContentResolver()
//                                        .update(useCountUri,
//                                                cv,
//                                                null,
//                                                null);
//                            } else {
//                                cv.put(FrequentItemUseCount.COLUMN_USE_COUNT, 1);
//                                getContext().getContentResolver()
//                                        .insert(useCountUri, cv);
//                            }
//                            c.close();
//                        }
                    }
                    resultCode = result.optInt(WhooingKeyValues.CODE);
                    if (resultCode == WhooingKeyValues.SUCCESS) {
                        ContentValues cv = new ContentValues();

                        cv.put(Entries.COLUMN_ENTRY_ID, result.optLong(WhooingKeyValues.ENTRY_ID));
                        cv.put(Entries.COLUMN_ENTRY_DATE, result.optDouble(WhooingKeyValues.ENTRY_DATE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, result.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_ID, result.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, result.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_ID, result.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                        cv.put(Entries.COLUMN_TITLE, result.optString(WhooingKeyValues.ITEM_TITLE));
                        cv.put(Entries.COLUMN_MONEY, result.optDouble(WhooingKeyValues.MONEY));
                        cv.put(Entries.COLUMN_MEMO, result.optString(WhooingKeyValues.MEMO));
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
                int[] cursorIndex = args.getIntArray(ARG_CURSOR_INDEX);

                if (cursorIndex == null) {
                    Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();
                    long entryId = args.getLong(WhooingKeyValues.ENTRY_ID);

                    builder.appendPath(entryId + ".json");
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.DELETE,
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
                    Cursor c = getContext().getContentResolver().query(WhooingProvider.getEntriesUri(sectionId),
                            null,
                            null,
                            null,
                            null);

                    if (c == null) {
                        return -1;
                    }
                    c.moveToPosition(cursorIndex[0]);

                    ArrayList<Long> itemIds = new ArrayList<>();
                    long id = c.getLong(Entries.COLUMN_INDEX_ENTRY_ID);

                    itemIds.add(id);

                    String itemIdsPath = "" + id;
                    Uri.Builder builder = Uri.parse(URI_ENTRIES).buildUpon();

                    for (int i = 1; i < cursorIndex.length; i++) {
                        c.moveToPosition(cursorIndex[i]);
                        id = c.getLong(Entries.COLUMN_INDEX_ENTRY_ID);
                        itemIds.add(id);
                        itemIdsPath += "," + id;
                    }
                    builder.appendEncodedPath(itemIdsPath + ".json");
                    c.close();
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                            builder.build().toString(),
                            mRequestFuture,
                            mRequestFuture,
                            mApiKeyFormat));
                    try {
                        JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));
                        int resultCode = result.optInt(WhooingKeyValues.CODE);

                        if (resultCode == WhooingKeyValues.SUCCESS) {
                            String itemIdArray = "('" + itemIds.get(0) + "'";

                            for (int i = 1; i < itemIds.size(); i++) {
                                itemIdArray += ",'" + itemIds.get(i) + "'";
                            }
                            itemIdArray += ")";
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
                        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                        JSONArray entries = result.optJSONArray(WhooingKeyValues.RESULT);
                        String ids = null;
                        Uri entriesUri = WhooingProvider.getEntriesUri(sectionId);

                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject entry = entries.optJSONObject(i);
                            long entryId = entry.optLong(WhooingKeyValues.ENTRY_ID);
                            Uri entryItemUri = WhooingProvider.getEntryItemUri(sectionId, entryId);
                            Cursor c = getContext().getContentResolver().query(
                                    entryItemUri,
                                    null,
                                    null,
                                    null,
                                    null);

                            if (c != null) {
                                ContentValues cv = new ContentValues();
                                String title = entry.optString(WhooingKeyValues.ITEM_TITLE);
                                double money = entry.optDouble(WhooingKeyValues.MONEY);
                                String leftAccountType = entry.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE);
                                String leftAccountId = entry.optString(WhooingKeyValues.LEFT_ACCOUNT_ID);
                                String rightAccountType = entry.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE);
                                String rightAccountId = entry.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID);
                                String memo = entry.optString(WhooingKeyValues.MEMO);
                                double entryDate = entry.optDouble(WhooingKeyValues.ENTRY_DATE);

                                if (c.moveToFirst()) {
                                    if (!c.getString(Entries.COLUMN_INDEX_TITLE).equals(title)) {
                                        cv.put(Entries.COLUMN_TITLE, title);
                                    }
                                    if (Math.abs(c.getDouble(Entries.COLUMN_INDEX_MONEY) - money) > WhooingKeyValues.EPSILON) {
                                        cv.put(Entries.COLUMN_MONEY, money);
                                    }
                                    if (!c.getString(Entries.COLUMN_INDEX_LEFT_ACCOUNT_TYPE).equals(leftAccountType)) {
                                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, leftAccountType);
                                    }
                                    if (!c.getString(Entries.COLUMN_INDEX_LEFT_ACCOUNT_ID).equals(leftAccountId)) {
                                        cv.put(Entries.COLUMN_LEFT_ACCOUNT_ID, leftAccountId);
                                    }
                                    if (!c.getString(Entries.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE).equals(rightAccountType)) {
                                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, rightAccountType);
                                    }
                                    if (!c.getString(Entries.COLUMN_INDEX_RIGHT_ACCOUNT_ID).equals(rightAccountId)) {
                                        cv.put(Entries.COLUMN_RIGHT_ACCOUNT_ID, rightAccountId);
                                    }
                                    if (!c.getString(Entries.COLUMN_INDEX_MEMO).equals(memo)) {
                                        cv.put(Entries.COLUMN_MEMO, memo);
                                    }
                                    if (Math.abs(c.getDouble(Entries.COLUMN_INDEX_ENTRY_DATE) - entryDate) > WhooingKeyValues.EPSILON) {
                                        cv.put(Entries.COLUMN_ENTRY_DATE, entryDate);
                                    }
                                    if (cv.size() > 0) {
                                        operations.add(ContentProviderOperation.newUpdate(entryItemUri)
                                                .withValues(cv).build());
                                    }
                                } else {
                                    cv.put(Entries.COLUMN_ENTRY_ID, entryId);
                                    cv.put(Entries.COLUMN_TITLE, title);
                                    cv.put(Entries.COLUMN_MONEY, money);
                                    cv.put(Entries.COLUMN_LEFT_ACCOUNT_TYPE, leftAccountType);
                                    cv.put(Entries.COLUMN_LEFT_ACCOUNT_ID, leftAccountId);
                                    cv.put(Entries.COLUMN_RIGHT_ACCOUNT_TYPE, rightAccountType);
                                    cv.put(Entries.COLUMN_RIGHT_ACCOUNT_ID, rightAccountId);
                                    cv.put(Entries.COLUMN_MEMO, memo);
                                    cv.put(Entries.COLUMN_ENTRY_DATE, entryDate);
                                    operations.add(ContentProviderOperation.newInsert(entriesUri)
                                            .withValues(cv).build());
                                }
                                c.close();
                            }
                            if (ids == null) {
                                ids = "('" + sectionId + "'";
                            } else {
                                ids += ",'" + sectionId + "'";
                            }
                        }
                        if (ids != null) {
                            ids += ")";
                            operations.add(ContentProviderOperation.newDelete(entriesUri)
                                    .withSelection(Sections.COLUMN_SECTION_ID + " NOT IN " + ids, null)
                                    .build());
                        }
                        if (operations.size() > 0) {
                            getContext().getContentResolver().applyBatch(getContext().getString(R.string.whooing_authority),
                                    operations);
                        }
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException |
                        RemoteException | OperationApplicationException e) {
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
