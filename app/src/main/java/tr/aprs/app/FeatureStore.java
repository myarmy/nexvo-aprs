package tr.aprs.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class FeatureStore extends SQLiteOpenHelper {
    FeatureStore(Context context) { super(context, "tr_aprs_012.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,peer TEXT,direction TEXT,body TEXT,status TEXT,time INTEGER)");
        db.execSQL("CREATE TABLE favorites(callsign TEXT PRIMARY KEY,label TEXT,radius_km REAL DEFAULT 10,last_lat REAL,last_lng REAL,last_seen INTEGER)");
        db.execSQL("CREATE TABLE track(id INTEGER PRIMARY KEY AUTOINCREMENT,lat REAL,lng REAL,speed REAL,bearing REAL,time INTEGER)");
        db.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,level TEXT,event TEXT,detail TEXT,time INTEGER)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    void message(String peer, String direction, String body, String status) {
        ContentValues v = new ContentValues(); v.put("peer", peer); v.put("direction", direction); v.put("body", body);
        v.put("status", status); v.put("time", System.currentTimeMillis()); getWritableDatabase().insert("messages", null, v);
    }
    void track(double lat, double lng, double speed, double bearing) {
        ContentValues v = new ContentValues(); v.put("lat", lat); v.put("lng", lng); v.put("speed", speed); v.put("bearing", bearing);
        v.put("time", System.currentTimeMillis()); getWritableDatabase().insert("track", null, v);
        getWritableDatabase().execSQL("DELETE FROM track WHERE id NOT IN (SELECT id FROM track ORDER BY id DESC LIMIT 5000)");
    }
    void log(String level, String event, String detail) {
        ContentValues v = new ContentValues(); v.put("level", level); v.put("event", event); v.put("detail", detail);
        v.put("time", System.currentTimeMillis()); getWritableDatabase().insert("logs", null, v);
        getWritableDatabase().execSQL("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY id DESC LIMIT 2000)");
    }
    void toggleFavorite(String callsign) {
        String call = callsign.trim().toUpperCase();
        int deleted = getWritableDatabase().delete("favorites", "callsign=?", new String[]{call});
        if (deleted == 0) { ContentValues v = new ContentValues(); v.put("callsign", call); v.put("label", call); getWritableDatabase().insert("favorites", null, v); }
    }
    boolean isFavorite(String callsign) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM favorites WHERE callsign=?", new String[]{callsign.toUpperCase()})) { return c.moveToFirst(); }
    }
    List<String> recentMessages(int limit) { return rows("SELECT direction||' '||peer||' • '||body||' ['||status||']' FROM messages ORDER BY id DESC LIMIT " + limit); }
    List<String> favorites() { return rows("SELECT callsign||' • yakınlık '||CAST(radius_km AS INTEGER)||' km' FROM favorites ORDER BY callsign"); }
    List<String> recentLogs(int limit) { return rows("SELECT level||' • '||event||' • '||detail FROM logs ORDER BY id DESC LIMIT " + limit); }
    private List<String> rows(String sql) {
        ArrayList<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) { while (c.moveToNext()) out.add(c.getString(0)); }
        return out;
    }
}
