package com.younggeon.whoolite.db.schema;

import android.provider.BaseColumns;

/**
 * Created by sadless on 2015. 12. 31..
 */
public class FrequentItemUseCount implements BaseColumns {
    public static final String TABLE_NAME = "frequent_item_use_count";

    public static final String COLUMN_SECTION_ID = "section_id";
    public static final String COLUMN_SLOT_NUMBER = "slot_number";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_USE_COUNT = "use_count";

    public static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_SECTION_ID + " TEXT," +
            COLUMN_SLOT_NUMBER + " INTEGER," +
            COLUMN_ITEM_ID + " TEXT," +
            COLUMN_USE_COUNT + " INTEGER)";
}
