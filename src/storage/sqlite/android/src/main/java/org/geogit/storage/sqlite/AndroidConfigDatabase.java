package org.geogit.storage.sqlite;

import java.util.List;
import java.util.Map;

import org.geogit.api.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import static org.geogit.storage.sqlite.SQLite.log;
import static org.geogit.storage.sqlite.AndroidSQLite.array;

public class AndroidConfigDatabase extends SQLiteConfigDatabase {

    static Logger LOG = LoggerFactory.getLogger(AndroidConfigDatabase.class);

    @Inject
    public AndroidConfigDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected String get(Entry entry, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            String s = entry.section;
            String k = entry.key;

            log("SELECT value FROM config WHERE section = ? AND key = ?", LOG, s, k);

            
            Cursor c = db.query("config", array("value"), "section = ? AND key = ?", array(s,k),
                null, null, null);
            try {
                return c.moveToNext() ? c.getString(0) : null;
            }
            finally {
                c.close();
            }
        }
        finally {
            db.close();
        }
    }

    @Override
    protected Map<String, String> all(Config config) {
        SQLiteDatabase db = connect(config);
        try {
            
            log("SELECT section,key,value FROM config", LOG);

            Map<String,String> m = Maps.newLinkedHashMap();
            Cursor c = 
                db.query("config", array("section","key","value"), null, null, null, null, null);
            try {
                while(c.moveToNext()) {
                    m.put(String.format("%s.%s", c.getString(0),c.getString(1)), c.getString(2));
                }
                return m;
            }
            finally {
                c.close();
            }
        }
        finally {
            db.close();
        }
    }
    
    @Override
    protected Map<String, String> all(String section, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            
            log("SELECT section,key,value FROM config WHERE section = ?", LOG, section);

            Map<String,String> m = Maps.newLinkedHashMap();
            Cursor c = 
                db.query("config", array("section","key","value"), "section = ?", array(section), 
                    null, null, null);
            try {
                while(c.moveToNext()) {
                    m.put(String.format("%s.%s", c.getString(0),c.getString(1)), c.getString(2));
                }
                return m;
            }
            finally {
                c.close();
            }
        }
        finally {
            db.close();
        }
    }

    @Override
    protected List<String> list(String section, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            log("SELECT key FROM config WHERE section = ?", LOG, section);

            List<String> l = Lists.newArrayList();
            Cursor c = 
                db.query("config", array("key"), "section = ?", array(section), 
                    null, null, null);
            try {
                while(c.moveToNext()) {
                    l.add(c.getString(0));
                }
                return l;
            }
            finally {
                c.close();
            }
        }
        finally {
            db.close();
        }
    }
    
    @Override
    protected void put(Entry entry, String value, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            db.beginTransaction();
            try {
                doRemove(entry, db);

                String s = entry.section;
                String k = entry.key;

                log("INSERT INTO config (section,key,value) VALUES (?,?,?)", LOG, s, k, value);

                ContentValues vals = new ContentValues();
                vals.put("section", s);
                vals.put("key", k);
                vals.put("value", value);
                db.insert("config", null, vals);

                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            db.close();
        }
    }

    @Override
    protected void remove(Entry entry, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            doRemove(entry, db);
        }
        finally {
            db.close();
        }
    }

    void doRemove(Entry entry, SQLiteDatabase db) {
        String s = entry.section;
        String k = entry.key;

        log("DELETE FROM config WHERE section = ? AND key = ?", LOG, s, k);
        db.delete("config", "section = ? AND key = ?", array(s,k));
    }

    @Override
    protected void removeAll(String section, Config config) {
        SQLiteDatabase db = connect(config);
        try {
            log("DELETE FROM config WHERE section = ?", LOG, section);

            db.delete("config", "section = ?", array(section));
        }
        finally {
            db.close();
        }
    }

    SQLiteDatabase connect(Config config) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(config.file, null);

        db.beginTransaction();
        try {
            String sql = "CREATE TABLE IF NOT EXISTS config (section VARCHAR, key VARCHAR, value VARCHAR)";
            db.execSQL(log(sql,LOG));
    
            sql = "CREATE INDEX IF NOT EXISTS config_section_idx ON config (section)";
            db.execSQL(log(sql,LOG));
    
            sql = "CREATE INDEX IF NOT EXISTS config_section_key_idx ON config (section,key)";
            db.execSQL(log(sql,LOG));

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

        return db;
    }
}
