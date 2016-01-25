package com.younggeon.whoolite.db.schema;

import android.provider.BaseColumns;

/**
 * Created by sadless on 2015. 12. 6..
 */
public class Entries implements BaseColumns {
    public static final String TABLE_NAME = "entries";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_ENTRY_ID = "entry_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MONEY = "money";
    public static final String COLUMN_LEFT_ACCOUNT_TYPE = "left_account_type";
    public static final String COLUMN_LEFT_ACCOUNT_ID = "left_account_id";
    public static final String COLUMN_RIGHT_ACCOUNT_TYPE = "right_account_type";
    public static final String COLUMN_RIGHT_ACCOUNT_ID = "right_account_id";
    public static final String COLUMN_MEMO = "memo";
    public static final String COLUMN_ENTRY_DATE = "entry_date";

    public static final String PREFIX_LEFT = "left";
    public static final String PREFIX_RIGHT = "right";

    public static final String[] COLUMNS_WITH_TABLE_NAME = new String[] {
            TABLE_NAME + "." + _ID,
            TABLE_NAME + "." + COLUMN_SECTION_ID,
            TABLE_NAME + "." + COLUMN_ENTRY_ID,
            TABLE_NAME + "." + COLUMN_TITLE,
            TABLE_NAME + "." + COLUMN_MONEY,
            TABLE_NAME + "." + COLUMN_LEFT_ACCOUNT_TYPE,
            TABLE_NAME + "." + COLUMN_LEFT_ACCOUNT_ID,
            TABLE_NAME + "." + COLUMN_RIGHT_ACCOUNT_TYPE,
            TABLE_NAME + "." + COLUMN_RIGHT_ACCOUNT_ID,
            TABLE_NAME + "." + COLUMN_MEMO,
            TABLE_NAME + "." + COLUMN_ENTRY_DATE
    };

    public static final int COLUMN_INDEX_ENTRY_ID = 2;
    public static final int COLUMN_INDEX_TITLE = 3;
    public static final int COLUMN_INDEX_MONEY = 4;
    public static final int COLUMN_INDEX_LEFT_ACCOUNT_TYPE = 5;
    public static final int COLUMN_INDEX_LEFT_ACCOUNT_ID = 6;
    public static final int COLUMN_INDEX_RIGHT_ACCOUNT_TYPE = 7;
    public static final int COLUMN_INDEX_RIGHT_ACCOUNT_ID = 8;
    public static final int COLUMN_INDEX_MEMO = 9;
    public static final int COLUMN_INDEX_ENTRY_DATE = 10;

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_SECTION_ID + " TEXT," +
            COLUMN_ENTRY_ID + " INTEGER," +
            COLUMN_TITLE + " TEXT," +
            COLUMN_MONEY + " REAL," +
            COLUMN_LEFT_ACCOUNT_TYPE + " TEXT," +
            COLUMN_LEFT_ACCOUNT_ID + " TEXT," +
            COLUMN_RIGHT_ACCOUNT_TYPE + " TEXT," +
            COLUMN_RIGHT_ACCOUNT_ID + " TEXT," +
            COLUMN_MEMO + " TEXT," +
            COLUMN_ENTRY_DATE + " REAL)";
}
