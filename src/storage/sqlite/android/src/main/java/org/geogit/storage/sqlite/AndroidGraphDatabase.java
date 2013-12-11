package org.geogit.storage.sqlite;

import static java.lang.String.format;
import static org.geogit.storage.sqlite.AndroidSQLite.array;
import static org.geogit.storage.sqlite.SQLite.log;

import java.io.File;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AndroidGraphDatabase extends SQLiteGraphDatabase<SQLiteDatabase> {

    static Logger LOG = LoggerFactory.getLogger(AndroidGraphDatabase.class);

    public AndroidGraphDatabase(ConfigDatabase configdb, Platform platform) {
        super(configdb, platform);
    }

    @Override
    protected SQLiteDatabase connect() {
        File file = new File(new File(platform.pwd(), ".geogit"), "graph.db");
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    @Override
    protected void close(SQLiteDatabase cx) {
        cx.close();
    }

    @Override
    protected void init(SQLiteDatabase cx) {
        cx.beginTransaction();

        String sql = format("CREATE TABLE IF NOT EXISTS %s (id VARCHAR)", NODES);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_id_index ON %s(id)", NODES, NODES);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE TABLE IF NOT EXISTS %s (src VARCHAR, dst VARCHAR)", EDGES);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_src_index ON %s(src)", EDGES, EDGES);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_dst_index ON %s(dst)", EDGES, EDGES);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE TABLE IF NOT EXISTS %s (nid VARCHAR, key VARCHAR, val VARCHAR)", 
            PROPS);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_nid_key_index ON %s(nid, key)", 
            PROPS, PROPS);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE TABLE IF NOT EXISTS %s (alias VARCHAR, nid VARCHAR)", MAPPINGS);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_alias_index ON %s(alias)", 
            MAPPINGS, MAPPINGS);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_nid_index ON %s(nid)", 
            MAPPINGS, MAPPINGS);
        cx.execSQL(log(sql,LOG));

        cx.setTransactionSuccessful();
        cx.endTransaction();
    }

    @Override
    protected boolean put(String node, SQLiteDatabase cx) {
        ContentValues vals = new ContentValues();
        vals.put("id", node);

        return cx.insert(NODES, null, vals) != -1;
    }

    @Override
    protected boolean has(String node, SQLiteDatabase cx) {
        Cursor c = cx.query(NODES, array("id"), "id = ?", array(node), null, null, null);
        try {
            return c.getCount() > 0;
        }
        finally {
            c.close();
        }
    }

    @Override
    protected void relate(String src, String dst, SQLiteDatabase cx) {
        log(format("INSERT INTO %s (src, dst) VALUES (?, ?)", EDGES), LOG, src, dst);

        ContentValues vals = new ContentValues();
        vals.put("src", src);
        vals.put("dst", dst);

        cx.insert(EDGES, null, vals);
    }

    @Override
    protected void map(String from, String to, SQLiteDatabase cx) {
        ContentValues vals = new ContentValues();
        vals.put("alias", from);
        vals.put("nid", to);

        cx.insertWithOnConflict(MAPPINGS, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    protected String mapping(String node, SQLiteDatabase cx) {
        log(format("SELECT nid FROM %s WHERE alias = ?", MAPPINGS), LOG);

        Cursor c = 
            cx.query(MAPPINGS, array("nid"), "alias = ?", array(node), null, null, null);
        try {
            if (c.moveToNext()) {
                return c.getString(1);
            }
            return null;
        }
        finally {
            c.close();
        }
    }

    @Override
    protected void property(String node, String key, String value, SQLiteDatabase cx) {
        log(format("INSERT INTO %s VALUES (?,?,?)", PROPS), LOG, node, key, value);

        ContentValues vals = new ContentValues();
        vals.put("nid", node);
        vals.put("key", key);
        vals.put("val", value);

        cx.insertWithOnConflict(PROPS, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    protected String property(String node, String key, SQLiteDatabase cx) {
        log(format("SELECT val FROM %s WHERE nid = ? AND key = ?", PROPS), LOG, node, key);

        Cursor c = 
            cx.query(PROPS, array("val"), "nid = ? AND key = ?", array(node, key), null, null, null);
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        }
        finally {
            c.close();
        }
    }

    @Override
    protected Iterable<String> outgoing(String node, SQLiteDatabase cx) {
        log(format("SELECT dst FROM %s WHERE src = ?", EDGES), LOG, node);
        Cursor c = 
            cx.query(EDGES, array("dst"), "src = ?", array(node), null, null, null);
        return new StringCursorIterable(c);
    }

    @Override
    protected Iterable<String> incoming(String node, SQLiteDatabase cx) {
        log(format("SELECT src FROM %s WHERE dst = ?", EDGES), LOG, node);
        Cursor c = 
            cx.query(EDGES, array("src"), "dst = ?", array(node), null, null, null);
        return new StringCursorIterable(c);
    }

    @Override
    protected void clear(SQLiteDatabase cx) {
        cx.delete(PROPS, null, null);
        cx.delete(EDGES, null, null);
        cx.delete(NODES, null, null);
    }
}
