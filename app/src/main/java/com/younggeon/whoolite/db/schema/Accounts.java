package com.younggeon.whoolite.db.schema;

import android.provider.BaseColumns;

/**
 * Created by sadless on 2015. 11. 5..
 */
public class Accounts implements BaseColumns {
    public static final String TABLE_NAME = "accounts";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_ACCOUNT_TYPE = "account_type";
    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MEMO = "memo";
    public static final String COLUMN_IS_GROUP = "is_group";
    public static final String COLUMN_SORT_ORDER = "sort_order";

    public static final int COLUMN_INDEX_ACCOUNT_TYPE = 2;
    public static final int COLUMN_INDEX_ACCOUNT_ID = 3;
    public static final int COLUMN_INDEX_TITLE = 4;
    public static final int COLUMN_INDEX_MEMO = 5;
    public static final int COLUMN_INDEX_IS_GROUP = 6;
    public static final int COLUMN_INDEX_SORT_ORDER = 7;

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_SECTION_ID + " TEXT," +
            COLUMN_ACCOUNT_TYPE + " TEXT," +
            COLUMN_ACCOUNT_ID + " TEXT," +
            COLUMN_TITLE + " TEXT," +
            COLUMN_MEMO + " TEXT," +
            COLUMN_IS_GROUP + " INTEGER," +
            COLUMN_SORT_ORDER + " INTEGER," +

            "FOREIGN KEY(" + COLUMN_SECTION_ID + ") REFERENCES " +
            Sections.TABLE_NAME + "(" + Sections.COLUMN_SECTION_ID + ") ON DELETE CASCADE)";
}
