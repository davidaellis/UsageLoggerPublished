package geyerk.sensorlab.uractivity;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

class StoreInSQL extends SQLiteOpenHelper {

    private final String SQL_CREATE_ENTRIES, SQL_DELETE_ENTRIES;

    StoreInSQL(Context context, String name,  int version, String tableName, String SQL_CREATE_ENTRIES) {
        super(context, name, null, version);
        this.SQL_CREATE_ENTRIES =  "CREATE TABLE " +tableName + SQL_CREATE_ENTRIES;
        this.SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + tableName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
