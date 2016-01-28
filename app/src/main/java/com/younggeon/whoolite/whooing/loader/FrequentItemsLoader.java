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
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class FrequentItemsLoader extends WhooingBaseLoader {
    private static final String URI_FREQUENT_ITEMS = "https://whooing.com/api/frequent_items";

    public static final String ARG_OLD_SLOT = "old_slot";
    public static final String ARG_NEW_SLOT = "new_slot";

    public FrequentItemsLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        switch (mMethod) {
            case Request.Method.DELETE: {
                String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                int[] cursorIndex = args.getIntArray(ARG_CURSOR_INDEX);

                if (cursorIndex == null) {
                    int slotNumber = args.getInt(FrequentItems.COLUMN_SLOT_NUMBER);
                    String itemId = args.getString(WhooingKeyValues.ITEM_ID);

                    return deleteFrequentItem(sectionId, slotNumber, itemId);
                } else {
                    Cursor c = getContext().getContentResolver().query(WhooingProvider.getFrequentItemsUri(sectionId),
                            null,
                            null,
                            null,
                            null);

                    if (c == null) {
                        return -1;
                    }

                    HashMap<Integer, ArrayList<String>> slotItemIdsMap = new HashMap<>();
                    HashMap<Integer, ArrayList<Integer>> slotIndexMap = new HashMap<>();

                    for (int i = 0; i < cursorIndex.length; i++) {
                        if (cursorIndex[i] >= 0) {
                            c.moveToPosition(cursorIndex[i]);

                            int slotNumber = c.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER);
                            ArrayList<String> itemIds = slotItemIdsMap.get(slotNumber);
                            ArrayList<Integer> index = slotIndexMap.get(slotNumber);

                            if (itemIds == null) {
                                itemIds = new ArrayList<>();
                                index = new ArrayList<>();
                                slotItemIdsMap.put(slotNumber, itemIds);
                                slotIndexMap.put(slotNumber, index);
                            }
                            itemIds.add(c.getString(FrequentItems.COLUMN_INDEX_ITEM_ID));
                            index.add(i);
                        }
                    }
                    c.close();

                    int resultCode = -1;

                    for (int slotNumber : slotItemIdsMap.keySet()) {
                        ArrayList<String> itemIds = slotItemIdsMap.get(slotNumber);
                        Uri.Builder builder = Uri.parse(URI_FREQUENT_ITEMS).buildUpon();
                        String itemIdsPath = itemIds.get(0);

                        for (int i = 1; i < itemIds.size(); i++) {
                            itemIdsPath += "," + itemIds.get(i);
                        }
                        builder.appendPath("slot" + slotNumber)
                                .appendEncodedPath(itemIdsPath)
                                .appendPath(sectionId + ".json");
                        WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.DELETE,
                                builder.build().toString(),
                                mRequestFuture,
                                mRequestFuture,
                                mApiKeyFormat));
                        try {
                            JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                            resultCode = result.optInt(WhooingKeyValues.CODE);
                            if (resultCode == WhooingKeyValues.SUCCESS) {
                                ArrayList<Integer> index = slotIndexMap.get(slotNumber);
                                String itemIdArray = "('" + itemIds.get(0) + "'";

                                for (int i = 1; i < itemIds.size(); i++) {
                                    itemIdArray += ",'" + itemIds.get(i) + "'";
                                }
                                itemIdArray += ")";
                                getContext().getContentResolver().delete(WhooingProvider.getFrequentItemsUri(sectionId),
                                        FrequentItems.COLUMN_ITEM_ID + " IN " + itemIdArray + " AND " +
                                                FrequentItems.COLUMN_SLOT_NUMBER + " = ?",
                                        new String[]{"" + slotNumber});
                                for (int i : index) {
                                    cursorIndex[i] = -1;
                                }
                                args.putIntArray(ARG_CURSOR_INDEX, cursorIndex);
                            }
                        } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                            e.printStackTrace();

                            return -1;
                        }
                    }

                    return resultCode;
                }
            }
            case Request.Method.POST: {
                String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                int[] cursorIndex = args.getIntArray(ARG_CURSOR_INDEX);
                int slotNumber = args.getInt(FrequentItems.COLUMN_SLOT_NUMBER);
                int resultCode = -1;

                if (cursorIndex == null) {
                    args.remove(FrequentItems.COLUMN_SLOT_NUMBER);
                    resultCode = postFrequentItem((Bundle) args.clone(), slotNumber);
                    args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);

                    return resultCode;
                } else {
                    Cursor c = getContext().getContentResolver().query(WhooingProvider.getEntriesUri(sectionId),
                            null,
                            null,
                            null,
                            null);
                    Bundle args = new Bundle();

                    if (c == null) {
                        return -1;
                    }
                    for (int position : cursorIndex) {
                        c.moveToPosition(position);
                        args.clear();
                        args.putString(WhooingKeyValues.SECTION_ID, sectionId);
                        args.putString(WhooingKeyValues.ITEM_TITLE, c.getString(Entries.COLUMN_INDEX_TITLE));
                        args.putString(WhooingKeyValues.MONEY, "" + c.getDouble(Entries.COLUMN_INDEX_MONEY));
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE,
                                c.getString(Entries.COLUMN_INDEX_LEFT_ACCOUNT_TYPE));
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID,
                                c.getString(Entries.COLUMN_INDEX_LEFT_ACCOUNT_ID));
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE,
                                c.getString(Entries.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE));
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID,
                                c.getString(Entries.COLUMN_INDEX_RIGHT_ACCOUNT_ID));
                        resultCode = postFrequentItem(args, slotNumber);
                        mRequestFuture = RequestFuture.newFuture();
                    }
                    c.close();

                    return resultCode;
                }
            }
            case Request.Method.PUT: {
                int oldSlot = args.getInt(ARG_OLD_SLOT);
                int newSlot = args.getInt(ARG_NEW_SLOT);
                String itemId = args.getString(WhooingKeyValues.ITEM_ID);
                Uri.Builder builder = Uri.parse(URI_FREQUENT_ITEMS).buildUpon();
                int resultCode;

                args.remove(ARG_OLD_SLOT);
                args.remove(ARG_NEW_SLOT);
                args.remove(WhooingKeyValues.ITEM_ID);
                if (oldSlot == newSlot) {
                    builder.appendPath("slot" + newSlot)
                            .appendPath(itemId + ".json");
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
                            ContentValues cv = new ContentValues();
                            JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);

                            cv.put(FrequentItems.COLUMN_TITLE, resultItem.optString(WhooingKeyValues.ITEM_TITLE));
                            cv.put(FrequentItems.COLUMN_MONEY, resultItem.optDouble(WhooingKeyValues.MONEY));
                            cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE,
                                    resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                            cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_ID,
                                    resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                            cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE,
                                    resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                            cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_ID,
                                    resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                            getContext().getContentResolver().update(
                                    WhooingProvider.getFrequentItemUri(args.getString(WhooingKeyValues.SECTION_ID),
                                            oldSlot,
                                            itemId), cv, null, null);
                        }
                    } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                        resultCode = -1;
                    }
                } else {
                    String sectionId = args.getString(WhooingKeyValues.SECTION_ID);

                    resultCode = deleteFrequentItem(sectionId, oldSlot, itemId);
                    if (resultCode == WhooingKeyValues.SUCCESS) {
                        mRequestFuture = RequestFuture.newFuture();
                        resultCode = postFrequentItem((Bundle) args.clone(), newSlot);
                    }
                }
                args.putInt(ARG_OLD_SLOT, oldSlot);
                args.putInt(ARG_NEW_SLOT, newSlot);
                args.putString(WhooingKeyValues.ITEM_ID, itemId);

                return resultCode;
            }
            case Request.Method.GET: {
                String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                Uri.Builder builder = Uri.parse(URI_FREQUENT_ITEMS + ".json_array").buildUpon();

                builder.appendQueryParameter(WhooingKeyValues.SECTION_ID, sectionId);
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        builder.build().toString(),
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat,
                        args));

                int resultCode;

                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                        JSONObject frequentItems = result.optJSONObject(WhooingKeyValues.RESULT);
                        Iterator<String> keys = frequentItems.keys();
                        Uri frequentItemsUri = WhooingProvider.getFrequentItemsUri(sectionId);
                        int sortOrder = 0;
                        int slotNumber = 1;

                        while (keys.hasNext()) {
                            JSONArray itemsInSlot = frequentItems.optJSONArray(keys.next());
                            String ids = null;

                            for (int i = 0; i < itemsInSlot.length(); i++, sortOrder++) {
                                JSONObject frequentItem = itemsInSlot.optJSONObject(i);
                                String itemId = frequentItem.optString(WhooingKeyValues.ITEM_ID);
                                Uri frequentItemUri = WhooingProvider.getFrequentItemUri(sectionId,
                                        slotNumber,
                                        itemId);
                                Cursor c = getContext().getContentResolver().query(
                                        frequentItemUri,
                                        null,
                                        null,
                                        null,
                                        null);

                                if (c != null) {
                                    ContentValues cv = new ContentValues();
                                    String title = frequentItem.optString(WhooingKeyValues.ITEM_TITLE);
                                    double money = frequentItem.optDouble(WhooingKeyValues.MONEY);
                                    String leftAccountType = frequentItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE);
                                    String leftAccountId = frequentItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID);
                                    String rightAccountType = frequentItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE);
                                    String rightAccountId = frequentItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID);

                                    if (c.moveToFirst()) {
                                        if (!c.getString(FrequentItems.COLUMN_INDEX_TITLE).equals(title)) {
                                            cv.put(FrequentItems.COLUMN_TITLE, title);
                                        }
                                        if (Math.abs(c.getDouble(FrequentItems.COLUMN_INDEX_MONEY) - money) > WhooingKeyValues.EPSILON) {
                                            cv.put(FrequentItems.COLUMN_MONEY, money);
                                        }
                                        if (!c.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_TYPE).equals(leftAccountType)) {
                                            cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE, leftAccountType);
                                        }
                                        if (!c.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_ID).equals(leftAccountId)) {
                                            cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_ID, leftAccountId);
                                        }
                                        if (!c.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE).equals(rightAccountType)) {
                                            cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE, rightAccountType);
                                        }
                                        if (!c.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_ID).equals(rightAccountId)) {
                                            cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_ID, rightAccountId);
                                        }
                                        if (c.getInt(FrequentItems.COLUMN_INDEX_SORT_ORDER) != sortOrder) {
                                            cv.put(FrequentItems.COLUMN_SORT_ORDER, sortOrder);
                                        }
                                        if (cv.size() > 0) {
                                            operations.add(ContentProviderOperation.newUpdate(frequentItemUri)
                                                    .withValues(cv).build());
                                        }
                                    } else {
                                        cv.put(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);
                                        cv.put(FrequentItems.COLUMN_ITEM_ID, itemId);
                                        cv.put(FrequentItems.COLUMN_TITLE, title);
                                        cv.put(FrequentItems.COLUMN_MONEY, money);
                                        cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE, leftAccountType);
                                        cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_ID, leftAccountId);
                                        cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE, rightAccountType);
                                        cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_ID, rightAccountId);
                                        cv.put(FrequentItems.COLUMN_SORT_ORDER, sortOrder);
                                        operations.add(ContentProviderOperation.newInsert(frequentItemsUri)
                                                .withValues(cv).build());
                                    }
                                    c.close();
                                }
                                if (ids == null) {
                                    ids = "('" + itemId + "'";
                                } else {
                                    ids += ",'" + itemId + "'";
                                }
                            }
                            if (ids != null) {
                                ids += ")";
                                operations.add(ContentProviderOperation.newDelete(frequentItemsUri)
                                        .withSelection(FrequentItems.COLUMN_SECTION_ID + " = ? AND " +
                                                        FrequentItems.COLUMN_SLOT_NUMBER + " = ? AND " +
                                                        FrequentItems.COLUMN_ITEM_ID + " NOT IN " + ids,
                                                new String[]{sectionId, "" + slotNumber})
                                        .build());
                            }
                            slotNumber++;
                        }
                        if (operations.size() > 0) {
                            getContext().getContentResolver().applyBatch(getContext().getString(R.string.whooing_authority),
                                    operations);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException |
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

        return null;
    }

    public static FrequentItemsLoader castLoader(Loader loader) {
        return (FrequentItemsLoader) loader;
    }

    private int deleteFrequentItem(String sectionId, int slotNumber, String itemId) {
        Uri.Builder builder = Uri.parse(URI_FREQUENT_ITEMS).buildUpon();
        int resultCode;

        builder.appendPath("slot" + slotNumber)
                .appendPath(itemId)
                .appendPath(sectionId + ".json");
        WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.DELETE,
                builder.build().toString(),
                mRequestFuture,
                mRequestFuture,
                mApiKeyFormat));

        try {
            JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

            resultCode = result.optInt(WhooingKeyValues.CODE);
            if (resultCode == WhooingKeyValues.SUCCESS) {
                getContext().getContentResolver().delete(WhooingProvider.getFrequentItemUri(sectionId,
                        slotNumber,
                        itemId), null, null);
            }
        } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            resultCode = -1;
        }

        return resultCode;
    }

    private int postFrequentItem(Bundle args, int slotNumber) {
        Uri saveUri = Uri.parse(URI_FREQUENT_ITEMS).buildUpon()
                .appendPath("slot" + slotNumber + ".json").build();
        int resultCode;

        WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.POST,
                saveUri.toString(),
                mRequestFuture,
                mRequestFuture,
                mApiKeyFormat,
                args));
        try {
            JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

            resultCode = result.optInt(WhooingKeyValues.CODE);
            if (resultCode == WhooingKeyValues.SUCCESS) {
                JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);

                if (resultItem != null) {
                    ContentValues cv = new ContentValues();

                    cv.put(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);
                    cv.put(FrequentItems.COLUMN_ITEM_ID, resultItem.optString(WhooingKeyValues.ITEM_ID));
                    cv.put(FrequentItems.COLUMN_TITLE, resultItem.optString(WhooingKeyValues.ITEM_TITLE));
                    cv.put(FrequentItems.COLUMN_MONEY, resultItem.optDouble(WhooingKeyValues.MONEY));
                    cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE,
                            resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
                    cv.put(FrequentItems.COLUMN_LEFT_ACCOUNT_ID,
                            resultItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
                    cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE,
                            resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
                    cv.put(FrequentItems.COLUMN_RIGHT_ACCOUNT_ID,
                            resultItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
                    getContext().getContentResolver().insert(WhooingProvider.getFrequentItemsUri(
                                    args.getString(WhooingKeyValues.SECTION_ID)),
                            cv);
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
            e.printStackTrace();

            resultCode = -1;
        }

        return resultCode;
    }
}
