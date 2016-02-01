package com.younggeon.whoolite.db.schema;

/**
 * Created by sadless on 2015. 10. 18..
 */
public class Sections {
    public static final String TABLE_NAME = "sections";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MEMO = "memo";
    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_DATE_FORMAT = "date_format";
    public static final String COLUMN_SORT_ORDER = "sort_order";

    public static final int COLUMN_INDEX_SECTION_ID = 0;
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_MEMO = 2;
    public static final int COLUMN_INDEX_CURRENCY = 3;
    public static final int COLUMN_INDEX_DATE_FORMAT = 4;

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            COLUMN_SECTION_ID + " TEXT PRIMARY KEY," +
            COLUMN_TITLE + " TEXT," +
            COLUMN_MEMO + " TEXT," +
            COLUMN_CURRENCY + " TEXT," +
            COLUMN_DATE_FORMAT + " TEXT," +
            COLUMN_SORT_ORDER + " INTEGER)";

    public static final String[] PROJECTION_FOR_CURSOR_ADAPTER = new String[] {
            COLUMN_SECTION_ID,
            COLUMN_TITLE,
            COLUMN_MEMO,
            COLUMN_CURRENCY,
            COLUMN_DATE_FORMAT,
            COLUMN_SORT_ORDER,
            "ROWID as _id"
    };
}
