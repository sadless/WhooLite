package com.younggeon.whoolite.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.database.DatabaseUtilsCompat;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.db.WhooingOpenHelper;
import com.younggeon.whoolite.db.schema.Accounts;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.db.schema.Sections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WhooingProvider extends ContentProvider {
    private static final String PATH_SECTIONS = "sections";
    private static final String PATH_ACCOUNTS = "accounts";
    private static final String PATH_TYPE_COUNTS = "type_counts";
    private static final String PATH_FREQUENT_ITEMS = "frequent_items";
    private static final String PATH_SLOT_COUNTS = "slot_counts";
    private static final String PATH_ENTRIES = "entries";
    private static final String PATH_DATE_COUNTS = "date_counts";

    private static final int CODE_SECTIONS = 1;
    private static final int CODE_SECTION_ITEM = 2;
    private static final int CODE_ACCOUNTS = 3;
    private static final int CODE_ACCOUNTS_TYPE_COUNTS = 4;
    private static final int CODE_FREQUENT_ITEMS = 5;
    private static final int CODE_FREQUENT_ITEM_SLOT_COUNTS = 6;
    private static final int CODE_ENTRIES = 7;
    private static final int CODE_ENTRY_DATE_COUNTS = 8;
    private static final int CODE_FREQUENT_ITEM = 9;
    private static final int CODE_ENTRY_ITEM = 10;

    private static String sAuthority;

    private UriMatcher mUriMatcher;
    private WhooingOpenHelper mWhooingOpenHelper;

    public WhooingProvider() {
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int count;

        switch (mUriMatcher.match(uri)) {
            case CODE_SECTIONS: {
                count = mWhooingOpenHelper.getWritableDatabase().delete(Sections.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            }
            case CODE_ACCOUNTS: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().delete(Accounts.TABLE_NAME,
                        DatabaseUtilsCompat.concatenateWhere(selection, Accounts.COLUMN_SECTION_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{
                                pathSegments.get(pathSegments.size() - 2)}));
                break;
            }
            case CODE_FREQUENT_ITEMS: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().delete(FrequentItems.TABLE_NAME,
                        DatabaseUtilsCompat.concatenateWhere(selection, FrequentItems.COLUMN_SECTION_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{
                                pathSegments.get(pathSegments.size() - 2)}));
                break;
            }
            case CODE_ENTRIES: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().delete(Entries.TABLE_NAME,
                        DatabaseUtilsCompat.concatenateWhere(selection, Entries.COLUMN_SECTION_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{
                                pathSegments.get(pathSegments.size() - 2)}));
                break;
            }
            case CODE_FREQUENT_ITEM: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().delete(FrequentItems.TABLE_NAME,
                        DatabaseUtilsCompat.concatenateWhere(selection, FrequentItems.COLUMN_SECTION_ID + " = ? AND " +
                                FrequentItems.COLUMN_SLOT_NUMBER + " = ? AND " +
                                FrequentItems.COLUMN_ITEM_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{
                                pathSegments.get(pathSegments.size() - 4),
                                pathSegments.get(pathSegments.size() - 2),
                                pathSegments.get(pathSegments.size() - 1)}));
                break;
            }
            case CODE_ENTRY_ITEM: {
                count = mWhooingOpenHelper.getWritableDatabase().delete(Entries.TABLE_NAME,
                        DatabaseUtilsCompat.concatenateWhere(selection, Entries.COLUMN_ENTRY_DATE + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{
                                uri.getLastPathSegment()}));
                break;
            }
            default: {
                throw new UnsupportedOperationException("Not yet implemented : " + uri);
            }
        }
        if (getContext() != null && count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case CODE_SECTIONS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_SECTIONS;
            }
            case CODE_SECTION_ITEM: {
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + sAuthority + "/" + PATH_SECTIONS;
            }
            case CODE_ACCOUNTS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_ACCOUNTS;
            }
            case CODE_FREQUENT_ITEMS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_FREQUENT_ITEMS;
            }
            case CODE_FREQUENT_ITEM_SLOT_COUNTS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" +
                        PATH_FREQUENT_ITEMS + "/" + PATH_SLOT_COUNTS;
            }
            case CODE_ACCOUNTS_TYPE_COUNTS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_ACCOUNTS +
                        "/" + PATH_TYPE_COUNTS;
            }
            case CODE_ENTRIES: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_ENTRIES;
            }
            case CODE_ENTRY_DATE_COUNTS: {
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + sAuthority + "/" + PATH_ENTRIES +
                        "/" + PATH_DATE_COUNTS;
            }
            case CODE_FREQUENT_ITEM: {
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + sAuthority + "/" + PATH_FREQUENT_ITEMS;
            }
            case CODE_ENTRY_ITEM: {
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + sAuthority + "/" + PATH_ENTRIES;
            }
            default: {
                throw new UnsupportedOperationException("Not yet implemented : " + uri);
            }
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long id;

        switch (mUriMatcher.match(uri)) {
            case CODE_FREQUENT_ITEMS: {
                List<String> pathSegments = uri.getPathSegments();
                String sectionId = pathSegments.get(pathSegments.size() - 2);
                int sortOrder = 0;
                SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
                Cursor c = db.query(FrequentItems.TABLE_NAME,
                        new String[]{FrequentItems.COLUMN_SORT_ORDER},
                        FrequentItems.COLUMN_SECTION_ID + " = ?",
                        new String[]{sectionId},
                        null,
                        null,
                        FrequentItems.COLUMN_SORT_ORDER + " DESC");

                if (c.moveToFirst()) {
                    sortOrder = c.getInt(0) + 1;
                }
                c.close();
                values.put(FrequentItems.COLUMN_SECTION_ID, sectionId);
                values.put(FrequentItems.COLUMN_SORT_ORDER, sortOrder);
                id = db.insert(FrequentItems.TABLE_NAME,
                        null,
                        values);
                break;
            }
            case CODE_ENTRIES: {
                List<String> pathSegments = uri.getPathSegments();

                values.put(Entries.COLUMN_SECTION_ID, pathSegments.get(pathSegments.size() - 2));
                id = mWhooingOpenHelper.getWritableDatabase().insert(Entries.TABLE_NAME,
                        null,
                        values);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Not yet implemented : " + uri);
            }
        }
        if (getContext() != null && id >= 0) {
            getContext().getContentResolver().notifyChange(uri, null);

            return ContentUris.withAppendedId(uri, id);
        } else {
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            sAuthority = getContext().getString(R.string.whooing_authority);
        }
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS, CODE_SECTIONS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*", CODE_SECTION_ITEM);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_ACCOUNTS, CODE_ACCOUNTS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_ACCOUNTS + "/" +
                PATH_TYPE_COUNTS, CODE_ACCOUNTS_TYPE_COUNTS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_FREQUENT_ITEMS, CODE_FREQUENT_ITEMS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_FREQUENT_ITEMS + "/" +
                PATH_SLOT_COUNTS, CODE_FREQUENT_ITEM_SLOT_COUNTS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_ENTRIES, CODE_ENTRIES);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_ENTRIES + "/" +
                PATH_DATE_COUNTS, CODE_ENTRY_DATE_COUNTS);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_FREQUENT_ITEMS + "/#/*",
                CODE_FREQUENT_ITEM);
        mUriMatcher.addURI(sAuthority, PATH_SECTIONS + "/*/" + PATH_ENTRIES + "/#", CODE_ENTRY_ITEM);
        mWhooingOpenHelper = new WhooingOpenHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor returnCursor;

        switch (mUriMatcher.match(uri)) {
            case CODE_SECTIONS: {
                returnCursor = mWhooingOpenHelper.getReadableDatabase().query(Sections.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null);
                break;
            }
            case CODE_SECTION_ITEM: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

                builder.setTables(Sections.TABLE_NAME);
                builder.appendWhere(Sections.COLUMN_SECTION_ID + " = '" + uri.getLastPathSegment() + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_ACCOUNTS: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();

                builder.setTables(Accounts.TABLE_NAME);
                builder.appendWhere(Accounts.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 2) + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_FREQUENT_ITEMS: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();
                String[] newProjection;

                builder.setTables(FrequentItems.TABLE_NAME +
                        " LEFT JOIN " + Accounts.TABLE_NAME + " AS " + FrequentItems.PREFIX_LEFT + " ON " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_SECTION_ID + " = " +
                        FrequentItems.PREFIX_LEFT + "." + Accounts.COLUMN_SECTION_ID + " AND " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE + " = " +
                        FrequentItems.PREFIX_LEFT + "." + Accounts.COLUMN_ACCOUNT_TYPE + " AND " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_LEFT_ACCOUNT_ID + " = " +
                        FrequentItems.PREFIX_LEFT + "." + Accounts.COLUMN_ACCOUNT_ID +
                        " LEFT JOIN " + Accounts.TABLE_NAME + " AS " + FrequentItems.PREFIX_RIGHT + " ON " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_SECTION_ID + " = " +
                        FrequentItems.PREFIX_RIGHT + "." + Accounts.COLUMN_SECTION_ID + " AND " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE + " = " +
                        FrequentItems.PREFIX_RIGHT + "." + Accounts.COLUMN_ACCOUNT_TYPE + " AND " +
                        FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_RIGHT_ACCOUNT_ID + " = " +
                        FrequentItems.PREFIX_RIGHT + "." + Accounts.COLUMN_ACCOUNT_ID);
                builder.appendWhere(FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 2) + "'");
                if (projection == null) {
                    newProjection = new String[FrequentItems.COLUMNS_WITH_TABLE_NAME.length + 3];
                    System.arraycopy(FrequentItems.COLUMNS_WITH_TABLE_NAME,
                            0,
                            newProjection,
                            0,
                            FrequentItems.COLUMNS_WITH_TABLE_NAME.length);
                } else {
                    newProjection = new String[projection.length + 3];
                    for (int i = 0; i < projection.length; i++) {
                        String column = projection[i];

                        if (!column.startsWith(FrequentItems.TABLE_NAME + ".")) {
                            column = FrequentItems.TABLE_NAME + "." + column;
                        }
                        newProjection[i] = column;
                    }
                }
                newProjection[newProjection.length - 2] = FrequentItems.PREFIX_LEFT + "." + Accounts.COLUMN_TITLE + " AS " +
                        FrequentItems.PREFIX_LEFT + "_" + Accounts.COLUMN_TITLE;
                newProjection[newProjection.length - 1] = FrequentItems.PREFIX_RIGHT + "." + Accounts.COLUMN_TITLE + " AS " +
                        FrequentItems.PREFIX_RIGHT + "_" + Accounts.COLUMN_TITLE;
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        newProjection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_FREQUENT_ITEM_SLOT_COUNTS: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();

                builder.setTables(FrequentItems.TABLE_NAME);
                builder.appendWhere(FrequentItems.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 3) + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        new String[]{
                                FrequentItems.COLUMN_SLOT_NUMBER,
                                "COUNT(" + FrequentItems.COLUMN_SLOT_NUMBER + ")"
                        },
                        selection,
                        selectionArgs,
                        FrequentItems.COLUMN_SLOT_NUMBER,
                        null,
                        FrequentItems.COLUMN_SLOT_NUMBER + " ASC");
                break;
            }
            case CODE_ACCOUNTS_TYPE_COUNTS: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();

                builder.setTables(Accounts.TABLE_NAME);
                builder.appendWhere(Accounts.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 3) + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        new String[]{
                                Accounts.COLUMN_ACCOUNT_TYPE,
                                "COUNT(" + Accounts.COLUMN_ACCOUNT_TYPE + ")"
                        },
                        selection,
                        selectionArgs,
                        Accounts.COLUMN_ACCOUNT_TYPE,
                        null,
                        Accounts.COLUMN_SORT_ORDER + " ASC");
                break;
            }
            case CODE_ENTRIES: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();
                String[] newProjection;

                builder.setTables(Entries.TABLE_NAME +
                        " LEFT JOIN " + Accounts.TABLE_NAME + " AS " + Entries.PREFIX_LEFT + " ON " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_SECTION_ID + " = " +
                        Entries.PREFIX_LEFT + "." + Accounts.COLUMN_SECTION_ID + " AND " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_LEFT_ACCOUNT_TYPE + " = " +
                        Entries.PREFIX_LEFT + "." + Accounts.COLUMN_ACCOUNT_TYPE + " AND " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_LEFT_ACCOUNT_ID + " = " +
                        Entries.PREFIX_LEFT + "." + Accounts.COLUMN_ACCOUNT_ID +
                        " LEFT JOIN " + Accounts.TABLE_NAME + " AS " + Entries.PREFIX_RIGHT + " ON " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_SECTION_ID + " = " +
                        Entries.PREFIX_RIGHT + "." + Accounts.COLUMN_SECTION_ID + " AND " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_RIGHT_ACCOUNT_TYPE + " = " +
                        Entries.PREFIX_RIGHT + "." + Accounts.COLUMN_ACCOUNT_TYPE + " AND " +
                        Entries.TABLE_NAME + "." + Entries.COLUMN_RIGHT_ACCOUNT_ID + " = " +
                        Entries.PREFIX_RIGHT + "." + Accounts.COLUMN_ACCOUNT_ID);
                builder.appendWhere(Entries.TABLE_NAME + "." + Entries.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 2) + "'");
                if (projection == null) {
                    newProjection = new String[Entries.COLUMNS_WITH_TABLE_NAME.length + 2];
                    System.arraycopy(Entries.COLUMNS_WITH_TABLE_NAME,
                            0,
                            newProjection,
                            0,
                            Entries.COLUMNS_WITH_TABLE_NAME.length);
                } else {
                    newProjection = new String[projection.length + 2];
                    for (int i = 0; i < projection.length; i++) {
                        String column = projection[i];

                        if (!column.startsWith(Entries.TABLE_NAME + ".")) {
                            column = Entries.TABLE_NAME + "." + column;
                        }
                        newProjection[i] = column;
                    }
                }
                newProjection[newProjection.length - 2] = Entries.PREFIX_LEFT + "." + Accounts.COLUMN_TITLE + " AS " +
                        Entries.PREFIX_LEFT + "_" + Accounts.COLUMN_TITLE;
                newProjection[newProjection.length - 1] = Entries.PREFIX_RIGHT + "." + Accounts.COLUMN_TITLE + " AS " +
                        Entries.PREFIX_RIGHT + "_" + Accounts.COLUMN_TITLE;
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        newProjection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_ENTRY_DATE_COUNTS: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();

                builder.setTables(Entries.TABLE_NAME);
                builder.appendWhere(Entries.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 3) + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        new String[]{
                                Entries.COLUMN_ENTRY_DATE,
                                "COUNT(" + Entries.COLUMN_ENTRY_DATE + ")"
                        },
                        selection,
                        selectionArgs,
                        "CAST(" + Entries.COLUMN_ENTRY_DATE + " AS INTEGER)",
                        null,
                        Entries.COLUMN_ENTRY_DATE + " DESC");
                break;
            }
            case CODE_FREQUENT_ITEM: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                List<String> pathSegments = uri.getPathSegments();

                builder.setTables(FrequentItems.TABLE_NAME);
                builder.appendWhere(FrequentItems.COLUMN_SECTION_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 4) + "' AND " +
                        FrequentItems.COLUMN_SLOT_NUMBER + " = " +
                        pathSegments.get(pathSegments.size() - 2) + " AND " +
                        FrequentItems.COLUMN_ITEM_ID + " = '" +
                        pathSegments.get(pathSegments.size() - 1) + "'");
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_ENTRY_ITEM: {
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

                builder.setTables(Entries.TABLE_NAME);
                builder.appendWhere(Entries.COLUMN_ENTRY_ID + " = " + uri.getLastPathSegment());
                returnCursor = builder.query(mWhooingOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Not yet implemented : " + uri);
            }
        }
        if (getContext() != null) {
            returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return returnCursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count;

        switch (mUriMatcher.match(uri)) {
            case CODE_FREQUENT_ITEM: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().update(FrequentItems.TABLE_NAME,
                        values,
                        DatabaseUtilsCompat.concatenateWhere(selection,
                                FrequentItems.COLUMN_SECTION_ID + " = ? AND " +
                                        FrequentItems.COLUMN_SLOT_NUMBER + " = ? AND " +
                                        FrequentItems.COLUMN_ITEM_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                                new String[]{pathSegments.get(pathSegments.size() - 4),
                                        pathSegments.get(pathSegments.size() - 2),
                                        pathSegments.get(pathSegments.size() - 1)}));
                break;
            }
            case CODE_ENTRY_ITEM: {
                List<String> pathSegments = uri.getPathSegments();

                count = mWhooingOpenHelper.getWritableDatabase().update(Entries.TABLE_NAME,
                        values,
                        DatabaseUtilsCompat.concatenateWhere(selection,
                                Entries.COLUMN_SECTION_ID + " = ? AND " +
                                        Entries.COLUMN_ENTRY_ID + " = ?"),
                        DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                                new String[]{pathSegments.get(pathSegments.size() - 3),
                                        pathSegments.get(pathSegments.size() - 1)}));
                break;
            }
            default: {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        }
        if (getContext() != null && count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
        ContentProviderResult[] returnResult;

        db.beginTransaction();
        returnResult = super.applyBatch(operations);
        db.setTransactionSuccessful();
        db.endTransaction();

        return returnResult;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int count = 0;

        switch (mUriMatcher.match(uri)) {
            case CODE_SECTIONS: {
                SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
                HashSet<String> idSet = new HashSet<>();

                db.beginTransaction();
                for (ContentValues cv : values) {
                    if (db.insertWithOnConflict(Sections.TABLE_NAME,
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE) > 0) {
                        count++;
                    }
                    idSet.add(cv.getAsString(Sections.COLUMN_SECTION_ID));
                }
                if (idSet.size() > 0) {
                    String idArray = "(";
                    boolean isFirst = true;

                    for (String id : idSet) {
                        if (isFirst) {
                            idArray += "'" + id + "'";
                            isFirst = false;
                        } else {
                            idArray += ", '" + id + "'";
                        }
                    }
                    idArray += ")";
                    count += db.delete(Sections.TABLE_NAME,
                            Sections.COLUMN_SECTION_ID + " NOT IN " + idArray,
                            null);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            }
            case CODE_ACCOUNTS: {
                List<String> pathSegments = uri.getPathSegments();
                String sectionId = pathSegments.get(pathSegments.size() - 2);
                SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
                HashSet<String> accountTypeSet = new HashSet<>();
                HashSet<String> accountIdSet = new HashSet<>();

                db.beginTransaction();
                for (ContentValues cv : values) {
                    cv.put(Accounts.COLUMN_SECTION_ID, sectionId);
                    if (db.insertWithOnConflict(Accounts.TABLE_NAME,
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE) > 0) {
                        count++;
                    }
                    accountTypeSet.add(cv.getAsString(Accounts.COLUMN_ACCOUNT_TYPE));
                    accountIdSet.add(cv.getAsString(Accounts.COLUMN_ACCOUNT_ID));
                }
                if (accountTypeSet.size() > 0) {
                    String accountTypeArray = "(";
                    String accountIdArray = "(";
                    boolean isFirst = true;

                    for (String accountType : accountTypeSet) {
                        if (isFirst) {
                            accountTypeArray += "'" + accountType + "'";
                            isFirst = false;
                        } else {
                            accountTypeArray += ", '" + accountType + "'";
                        }
                    }
                    accountTypeArray += ")";
                    isFirst = true;
                    for (String accountId : accountIdSet) {
                        if (isFirst) {
                            accountIdArray += "'" + accountId + "'";
                            isFirst = false;
                        } else {
                            accountIdArray += ", '" + accountId + "'";
                        }
                    }
                    accountIdArray += ")";
                    count += db.delete(Accounts.TABLE_NAME,
                            Accounts.COLUMN_SECTION_ID + " = ? AND (" +
                                    Accounts.COLUMN_ACCOUNT_TYPE + " NOT IN " + accountTypeArray + " OR " +
                                    Accounts.COLUMN_ACCOUNT_ID + " NOT IN " + accountIdArray + ")",
                            new String[]{sectionId});
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            }
            case CODE_FREQUENT_ITEMS: {
                List<String> pathSegments = uri.getPathSegments();
                String sectionId = pathSegments.get(pathSegments.size() - 2);
                SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
                HashSet<Integer> slotNumberSet = new HashSet<>();
                HashSet<String> itemIdSet = new HashSet<>();

                db.beginTransaction();
                for (ContentValues cv : values) {
                    String slotNumber = cv.getAsString(FrequentItems.COLUMN_SLOT_NUMBER);
                    String itemId = cv.getAsString(FrequentItems.COLUMN_ITEM_ID);

                    db.execSQL(FrequentItems.UPDATE_WITH_USE_INFO_QUERY,
                            new String[]{
                                    sectionId,
                                    slotNumber,
                                    itemId,
                                    sectionId,
                                    slotNumber,
                                    itemId,
                                    cv.getAsString(FrequentItems.COLUMN_TITLE),
                                    cv.getAsString(FrequentItems.COLUMN_MONEY),
                                    cv.getAsString(FrequentItems.COLUMN_LEFT_ACCOUNT_TYPE),
                                    cv.getAsString(FrequentItems.COLUMN_LEFT_ACCOUNT_ID),
                                    cv.getAsString(FrequentItems.COLUMN_RIGHT_ACCOUNT_TYPE),
                                    cv.getAsString(FrequentItems.COLUMN_RIGHT_ACCOUNT_ID),
                                    cv.getAsString(FrequentItems.COLUMN_SORT_ORDER)
                            });
                    count++;
                    slotNumberSet.add(cv.getAsInteger(FrequentItems.COLUMN_SLOT_NUMBER));
                    itemIdSet.add(cv.getAsString(FrequentItems.COLUMN_ITEM_ID));
                }
                if (slotNumberSet.size() > 0) {
                    String slotNumberArray = "(";
                    String itemIdArray = "(";
                    boolean isFirst = true;

                    for (Integer slotNumber : slotNumberSet) {
                        if (isFirst) {
                            slotNumberArray += slotNumber;
                            isFirst = false;
                        } else {
                            slotNumberArray += ", " + slotNumber;
                        }
                    }
                    slotNumberArray += ")";
                    isFirst = true;
                    for (String itemId : itemIdSet) {
                        if (isFirst) {
                            itemIdArray += "'" + itemId + "'";
                            isFirst = false;
                        } else {
                            itemIdArray += ", '" + itemId + "'";
                        }
                    }
                    itemIdArray += ")";
                    count += db.delete(FrequentItems.TABLE_NAME,
                            FrequentItems.COLUMN_SECTION_ID + " = ? AND (" +
                                    FrequentItems.COLUMN_SLOT_NUMBER + " NOT IN " + slotNumberArray + " OR " +
                                    FrequentItems.COLUMN_ITEM_ID + " NOT IN " + itemIdArray + ")",
                            new String[]{sectionId});
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            }
            case CODE_ENTRIES: {
                List<String> pathSegments = uri.getPathSegments();
                String sectionId = pathSegments.get(pathSegments.size() - 2);
                SQLiteDatabase db = mWhooingOpenHelper.getWritableDatabase();
                HashSet<String> idSet = new HashSet<>();

                db.beginTransaction();
                for (ContentValues cv : values) {
                    cv.put(Entries.COLUMN_SECTION_ID, sectionId);
                    if (db.insertWithOnConflict(Entries.TABLE_NAME,
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE) > 0) {
                        count++;
                    }
                    idSet.add(cv.getAsString(Entries.COLUMN_ENTRY_ID));
                }
                if (idSet.size() > 0) {
                    String idArray = "(";
                    boolean isFirst = true;

                    for (String id : idSet) {
                        if (isFirst) {
                            idArray += id;
                            isFirst = false;
                        } else {
                            idArray += ", " + id;
                        }
                    }
                    idArray += ")";
                    count += db.delete(Entries.TABLE_NAME,
                            Entries.COLUMN_SECTION_ID + " = ? AND " +
                            Entries.COLUMN_ENTRY_ID + " NOT IN " + idArray,
                            new String[] {sectionId});
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            }
            default: {
                return super.bulkInsert(uri, values);
            }
        }
        if (getContext() != null && count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    public static Uri getSectionsUri() {
        return Uri.withAppendedPath(Uri.parse("content://" + sAuthority), PATH_SECTIONS);
    }

    public static Uri getAccountsUri(String sectionId) {
        return getSectionsUri().buildUpon()
                .appendPath(sectionId)
                .appendPath(PATH_ACCOUNTS).build();
    }

    public static Uri getFrequentItemsUri(String sectionId) {
        return getSectionsUri().buildUpon()
                .appendPath(sectionId)
                .appendPath(PATH_FREQUENT_ITEMS).build();
    }

    public static Uri getFrequentItemSlotCountsUri(String sectionId) {
        return Uri.withAppendedPath(getFrequentItemsUri(sectionId), PATH_SLOT_COUNTS);
    }

    public static Uri getSectionUri(String sectionId) {
        return Uri.withAppendedPath(getSectionsUri(), sectionId);
    }

    public static Uri getAccountsTypeCountsUri(String sectionId) {
        return Uri.withAppendedPath(getAccountsUri(sectionId), PATH_TYPE_COUNTS);
    }

    public static Uri getEntriesUri(String sectionId) {
        return getSectionsUri().buildUpon()
                .appendPath(sectionId)
                .appendPath(PATH_ENTRIES).build();
    }

    public static Uri getEntriesDateCountsUri(String sectionId) {
        return Uri.withAppendedPath(getEntriesUri(sectionId), PATH_DATE_COUNTS);
    }

    public static Uri getFrequentItemUri(String sectionId, int slotNumber, String itemId) {
        return getFrequentItemsUri(sectionId).buildUpon()
                .appendPath("" + slotNumber)
                .appendPath(itemId).build();
    }

    public static Uri getEntryItemUri(String sectionId, long entryId) {
        return ContentUris.withAppendedId(getEntriesUri(sectionId), entryId);
    }
}
