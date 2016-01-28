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

    public WhooingOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Sections.CREATE_TABLE_QUERY);
        db.execSQL(FrequentItems.CREATE_TABLE_QUERY);
        db.execSQL(Accounts.CREATE_TABLE_QUERY);
        db.execSQL(Entries.CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
