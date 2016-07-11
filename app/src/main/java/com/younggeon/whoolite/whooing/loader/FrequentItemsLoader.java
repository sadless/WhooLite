package com.younggeon.whoolite.whooing.loader;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.realm.Entry;
import com.younggeon.whoolite.realm.FrequentItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class FrequentItemsLoader extends WhooingBaseLoader {
    private static final String URI_FREQUENT_ITEMS = "https://whooing.com/api/frequent_items";

    public static final String ARG_OLD_SLOT = "old_slot";
    public static final String ARG_NEW_SLOT = "new_slot";
    public static final String ARG_SEARCH_KEYWORD = "search_keyword";

    private int mUseCount;
    private long mLastUseTime;
    private String mSearchKeyword;

    public FrequentItemsLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        switch (mMethod) {
            case Request.Method.DELETE: {
                String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                ArrayList<String> selectedItems = args.getStringArrayList(ARG_SELECTED_ITEMS);

                if (selectedItems == null) {
                    int slotNumber = args.getInt(FrequentItems.COLUMN_SLOT_NUMBER);
                    String itemId = args.getString(WhooingKeyValues.ITEM_ID);

                    return deleteFrequentItem(sectionId, slotNumber, itemId);
                } else {
                    HashMap<String, ArrayList<String>> slotItemIdsMap = new HashMap<>();
                    HashMap<String, ArrayList<Integer>> slotIndexMap = new HashMap<>();

                    for (int i = 0; i < selectedItems.size(); i++) {
                        if (!TextUtils.isEmpty(selectedItems.get(i))) {
                            String[] slotNumberAndItemId = selectedItems.get(i).split(":");
                            String slotNumber = slotNumberAndItemId[0];
                            ArrayList<String> itemIds = slotItemIdsMap.get(slotNumber);
                            ArrayList<Integer> index = slotIndexMap.get(slotNumber);

                            if (itemIds == null) {
                                itemIds = new ArrayList<>();
                                index = new ArrayList<>();
                                slotItemIdsMap.put(slotNumber, itemIds);
                                slotIndexMap.put(slotNumber, index);
                            }
                            itemIds.add(slotNumberAndItemId[1]);
                            index.add(i);
                        }
                    }

                    int resultCode = -1;

                    for (String slotNumber : slotItemIdsMap.keySet()) {
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
                                Realm realm = Realm.getDefaultInstance();
                                RealmQuery<FrequentItem> query = realm.where(FrequentItem.class)
                                        .equalTo("sectionId", sectionId)
                                        .equalTo("slotNumber", Integer.parseInt(slotNumber));

                                query.beginGroup().equalTo("itemId", itemIds.get(0));
                                ArrayList<Integer> index = slotIndexMap.get(slotNumber);

                                for (int i = 1; i < itemIds.size(); i++) {
                                    query.or().equalTo("itemId", itemIds.get(i));
                                }
                                query.endGroup();

                                RealmResults<FrequentItem> deletedItems = query.findAll();

                                realm.beginTransaction();
                                deletedItems.deleteAllFromRealm();
                                realm.commitTransaction();
                                realm.close();
                                for (int i : index) {
                                    selectedItems.set(i, "");
                                }
                                args.putStringArrayList(ARG_SELECTED_ITEMS, selectedItems);
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
                ArrayList<String> selectedItems = args.getStringArrayList(ARG_SELECTED_ITEMS);
                int slotNumber = args.getInt(FrequentItems.COLUMN_SLOT_NUMBER);
                int resultCode = -1;

                if (selectedItems == null) {
                    args.remove(FrequentItems.COLUMN_SLOT_NUMBER);
                    resultCode = postFrequentItem((Bundle) args.clone(), slotNumber);
                    args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);

                    return resultCode;
                } else {
                    Realm realm = Realm.getDefaultInstance();
                    for (String entryId : selectedItems) {
                        Entry entry = realm.where(Entry.class).equalTo("sectionId", sectionId)
                                .equalTo("entryId", Long.parseLong(entryId)).findFirst();

                        if (entry == null) {
                            continue;
                        }
                        args.clear();
                        args.putString(WhooingKeyValues.SECTION_ID, sectionId);
                        args.putString(WhooingKeyValues.ITEM_TITLE, entry.getTitle());
                        args.putString(WhooingKeyValues.MONEY, "" + entry.getMoney());
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, entry.getLeftAccountType());
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, entry.getLeftAccountId());
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, entry.getRightAccountType());
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, entry.getRightAccountId());
                        resultCode = postFrequentItem(args, slotNumber);
                        mRequestFuture = RequestFuture.newFuture();
                    }
                    realm.close();

                    return resultCode;
                }
            }
            case Request.Method.PUT: {
                int oldSlot = args.getInt(ARG_OLD_SLOT);
                int newSlot = args.getInt(ARG_NEW_SLOT);
                String itemId = args.getString(WhooingKeyValues.ITEM_ID);
                Uri.Builder builder = Uri.parse(URI_FREQUENT_ITEMS).buildUpon();
                int resultCode;
                String searchKeyword = args.getString(ARG_SEARCH_KEYWORD);

                args.remove(ARG_OLD_SLOT);
                args.remove(ARG_NEW_SLOT);
                args.remove(WhooingKeyValues.ITEM_ID);
                args.remove(ARG_SEARCH_KEYWORD);
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
                            Realm realm = Realm.getDefaultInstance();
                            JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);
                            String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                            FrequentItem frequentItem = realm.where(FrequentItem.class)
                                    .equalTo("sectionId", sectionId)
                                    .equalTo("slotNumber", oldSlot)
                                    .equalTo("itemId", itemId).findFirst();

                            realm.beginTransaction();
                            setFrequentItemFromJson(frequentItem, resultItem);
                            frequentItem.setSearchKeyword(searchKeyword);
                            realm.commitTransaction();
                            realm.close();
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
                args.putString(ARG_SEARCH_KEYWORD, searchKeyword);

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
                        JSONObject frequentItems = result.optJSONObject(WhooingKeyValues.RESULT);
                        Iterator<String> keys = frequentItems.keys();
                        int sortOrder = 0;
                        int slotNumber = 1;
                        Realm realm = Realm.getDefaultInstance();
                        ArrayList<FrequentItem> objects = new ArrayList<>();
                        RealmQuery<FrequentItem> query = realm.where(FrequentItem.class).equalTo("sectionId", sectionId);

                        while (keys.hasNext()) {
                            JSONArray itemsInSlot = frequentItems.optJSONArray(keys.next());

                            for (int i = 0; i < itemsInSlot.length(); i++, sortOrder++) {
                                JSONObject frequentItem = itemsInSlot.optJSONObject(i);
                                FrequentItem object = createFrequentItemObjectFromJson(frequentItem, sectionId, slotNumber, i);
                                FrequentItem oldObject = realm.where(FrequentItem.class).equalTo("primaryKey", object.getPrimaryKey()).findFirst();

                                query.notEqualTo("primaryKey", object.getPrimaryKey());
                                if (oldObject != null) {
                                    object.setUseCount(oldObject.getUseCount());
                                    object.setLastUseTime(oldObject.getLastUseTime());
                                    object.setSearchKeyword(oldObject.getSearchKeyword());
                                }
                                objects.add(object);
                            }
                            slotNumber++;
                        }

                        RealmResults<FrequentItem> willDeleteFrequentItems = query.findAll();

                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(objects);
                        willDeleteFrequentItems.deleteAllFromRealm();
                        realm.commitTransaction();
                        realm.close();
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
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
        Realm realm = Realm.getDefaultInstance();

        if (mMethod == Request.Method.PUT) {
            FrequentItem frequentItem = realm.where(FrequentItem.class).equalTo("sectionId", sectionId)
                    .equalTo("slotNumber", slotNumber)
                    .equalTo("itemId", itemId).findFirst();

            if (frequentItem != null) {
                mUseCount = frequentItem.getUseCount();
                mLastUseTime = frequentItem.getLastUseTime();
                mSearchKeyword = frequentItem.getSearchKeyword();
            } else {
                mUseCount = 0;
                mLastUseTime = 0;
                mSearchKeyword = null;
            }
        }
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
                FrequentItem frequentItem = realm.where(FrequentItem.class).equalTo("sectionId", sectionId)
                        .equalTo("slotNumber", slotNumber)
                        .equalTo("itemId", itemId).findFirst();

                realm.beginTransaction();
                frequentItem.deleteFromRealm();
                realm.commitTransaction();
            }
        } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            resultCode = -1;
        }
        realm.close();

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
                    Realm realm = Realm.getDefaultInstance();
                    String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                    RealmResults<FrequentItem> frequentItems = realm.where(FrequentItem.class)
                            .equalTo("sectionId", sectionId)
                            .findAllSorted("sortOrder", Sort.DESCENDING);
                    int sortOrder;

                    if (frequentItems.size() > 0) {
                        sortOrder = frequentItems.get(0).getSortOrder() + 1;
                    } else {
                        sortOrder = 0;
                    }

                    FrequentItem frequentItem = createFrequentItemObjectFromJson(resultItem,
                            sectionId, slotNumber, sortOrder);

                    if (mMethod == Request.Method.PUT) {
                        frequentItem.setUseCount(mUseCount);
                        frequentItem.setLastUseTime(mLastUseTime);
                        frequentItem.setSearchKeyword(mSearchKeyword);
                    }
                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(frequentItem);
                    realm.commitTransaction();
                    realm.close();
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
            e.printStackTrace();

            resultCode = -1;
        }

        return resultCode;
    }

    private FrequentItem createFrequentItemObjectFromJson(JSONObject frequentItem, String sectionId, int slotNumber, int sortOrder) {
        FrequentItem object = new FrequentItem();

        object.setSectionId(sectionId);
        object.setSlotNumber(slotNumber);
        object.setItemId(frequentItem.optString(WhooingKeyValues.ITEM_ID));
        object.setSortOrder(sortOrder);
        setFrequentItemFromJson(object, frequentItem);
        object.composePrimaryKey();

        return object;
    }

    private void setFrequentItemFromJson(FrequentItem object, JSONObject frequentItem) {
        object.setTitle(frequentItem.optString(WhooingKeyValues.ITEM_TITLE));
        object.setMoney(frequentItem.optDouble(WhooingKeyValues.MONEY));
        object.setLeftAccountType(frequentItem.optString(WhooingKeyValues.LEFT_ACCOUNT_TYPE));
        object.setLeftAccountId(frequentItem.optString(WhooingKeyValues.LEFT_ACCOUNT_ID));
        object.setRightAccountType(frequentItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE));
        object.setRightAccountId(frequentItem.optString(WhooingKeyValues.RIGHT_ACCOUNT_ID));
    }
}
