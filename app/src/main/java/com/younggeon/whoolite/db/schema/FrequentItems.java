package com.younggeon.whoolite.db.schema;

import android.provider.BaseColumns;

/**
 * Created by sadless on 2015. 10. 30..
 */
public class FrequentItems implements BaseColumns {
    public static final String TABLE_NAME = "frequent_item";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_SLOT_NUMBER = "slot_number";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MONEY = "money";
    public static final String COLUMN_LEFT_ACCOUNT_TYPE = "left_account_type";
    public static final String COLUMN_LEFT_ACCOUNT_ID = "left_account_id";
    public static final String COLUMN_RIGHT_ACCOUNT_TYPE = "right_account_type";
    public static final String COLUMN_RIGHT_ACCOUNT_ID = "right_account_id";

    public static final int COLUMN_INDEX_SLOT_NUMBER = 2;
    public static final int COLUMN_INDEX_ITEM_ID = 3;
    public static final int COLUMN_INDEX_TITLE = 4;
    public static final int COLUMN_INDEX_MONEY = 5;
    public static final int COLUMN_INDEX_LEFT_ACCOUNT_TYPE = 6;
    public static final int COLUMN_INDEX_LEFT_ACCOUNT_ID = 7;
    public static final int COLUMN_INDEX_RIGHT_ACCOUNT_TYPE = 8;
    public static final int COLUMN_INDEX_RIGHT_ACCOUNT_ID = 9;

    public static final String PREFIX_LEFT = "left";
    public static final String PREFIX_RIGHT = "right";

    public static final String[] COLUMNS_WITH_TABLE_NAME = new String[] {
            TABLE_NAME + "." + _ID,
            TABLE_NAME + "." + COLUMN_SECTION_ID,
            TABLE_NAME + "." + COLUMN_SLOT_NUMBER,
            TABLE_NAME + "." + COLUMN_ITEM_ID,
            TABLE_NAME + "." + COLUMN_TITLE,
            TABLE_NAME + "." + COLUMN_MONEY,
            TABLE_NAME + "." + COLUMN_LEFT_ACCOUNT_TYPE,
            TABLE_NAME + "." + COLUMN_LEFT_ACCOUNT_ID,
            TABLE_NAME + "." + COLUMN_RIGHT_ACCOUNT_TYPE,
            TABLE_NAME + "." + COLUMN_RIGHT_ACCOUNT_ID
    };

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_SECTION_ID + " TEXT," +
            COLUMN_SLOT_NUMBER + " INTEGER," +
            COLUMN_ITEM_ID + " TEXT," +
            COLUMN_TITLE + " TEXT," +
            COLUMN_MONEY + " REAL," +
            COLUMN_LEFT_ACCOUNT_TYPE + " TEXT," +
            COLUMN_LEFT_ACCOUNT_ID + " TEXT," +
            COLUMN_RIGHT_ACCOUNT_TYPE + " TEXT," +
            COLUMN_RIGHT_ACCOUNT_ID + " TEXT)";
}
