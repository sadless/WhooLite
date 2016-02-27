package com.younggeon.whoolite.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.younggeon.whoolite.db.schema.Accounts;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.db.schema.Sections;

/**
 * Created by sadless on 2015. 10. 18..
 */
public class WhooingOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "whooing";
    private static final int VERSION = 1;

    private static final String CREATE_SECTION_DELETE_TRIGGER_QUERY = "CREATE TRIGGER section_delete_trigger " +
            "AFTER DELETE ON " + Sections.TABLE_NAME + " BEGIN " +
            "DELETE FROM " + FrequentItems.TABLE_NAME + " WHERE " +
            FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_SECTION_ID + " = " +
            "OLD." + Sections.COLUMN_SECTION_ID + ";" +
            "DELETE FROM " + Accounts.TABLE_NAME + " WHERE " +
            Accounts.TABLE_NAME + "." + Accounts.COLUMN_SECTION_ID + " = " +
            "OLD." + Sections.COLUMN_SECTION_ID + ";" +
            "DELETE FROM " + Entries.TABLE_NAME + " WHERE " +
            Entries.TABLE_NAME + "." + Entries.COLUMN_SECTION_ID + " = " +
            "OLD." + Sections.COLUMN_SECTION_ID + ";" +
            "END";

    public WhooingOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Sections.CREATE_TABLE_QUERY);
        db.execSQL(Accounts.CREATE_TABLE_QUERY);
        db.execSQL(FrequentItems.CREATE_TABLE_QUERY);
        db.execSQL(Entries.CREATE_TABLE_QUERY);
        db.execSQL(CREATE_SECTION_DELETE_TRIGGER_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}