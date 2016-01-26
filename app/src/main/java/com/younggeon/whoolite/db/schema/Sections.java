package com.younggeon.whoolite.db.schema;

import android.provider.BaseColumns;

/**
 * Created by sadless on 2015. 10. 18..
 */
public class Sections implements BaseColumns {
    public static final String TABLE_NAME = "sections";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MEMO = "memo";
    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_DATE_FORMAT = "date_format";
    public static final String COLUMN_SORT_ORDER = "sort_order";

    public static final int COLUMN_INDEX_SECTION_ID = 1;
    public static final int COLUMN_INDEX_TITLE = 2;
    public static final int COLUMN_INDEX_MEMO = 3;
    public static final int COLUMN_INDEX_CURRENCY = 4;
    public static final int COLUMN_INDEX_DATE_FORMAT = 5;
    public static final int COLUMN_INDEX_SORT_ORDER = 6;

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_SECTION_ID + " TEXT," +
            COLUMN_TITLE + " TEXT," +
            COLUMN_MEMO + " TEXT," +
            COLUMN_CURRENCY + " TEXT," +
            COLUMN_DATE_FORMAT + " TEXT," +
            COLUMN_SORT_ORDER + " INTEGER)";
}
