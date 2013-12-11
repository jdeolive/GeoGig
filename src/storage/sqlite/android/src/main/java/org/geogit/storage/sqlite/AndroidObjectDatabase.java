package org.geogit.storage.sqlite;

import static java.lang.String.format;
import static org.geogit.storage.sqlite.AndroidSQLite.array;
import static org.geogit.storage.sqlite.SQLite.log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AndroidObjectDatabase extends SQLiteObjectDatabase<SQLiteDatabase> {

    static Logger LOG = LoggerFactory.getLogger(AndroidObjectDatabase.class);

    public AndroidObjectDatabase(ConfigDatabase configdb, Platform platform) {
        super(configdb, platform);
    }

    @Override
    protected SQLiteDatabase connect() {
        File file = new File(new File(platform.pwd(), ".geogit"), OBJECTS + ".db");
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    @Override
    protected void close(SQLiteDatabase cx) {
        cx.close();
    }

    @Override
    protected void init(SQLiteDatabase cx) {
        cx.beginTransaction();

        String sql = 
            format("CREATE TABLE IF NOT EXISTS %s (id varchar, object blob)", OBJECTS);
        cx.execSQL(log(sql,LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_id_index ON %s (id)", OBJECTS, OBJECTS);
        cx.execSQL(log(sql,LOG));

        cx.setTransactionSuccessful();
        cx.endTransaction();
    }

    @Override
    protected boolean has(String id, SQLiteDatabase cx) {
        log(format("SELECT count(*) FROM %s WHERE id = ?", OBJECTS), LOG, id);

        Cursor c = cx.query(OBJECTS, array("id"), "id = ?", array(id), null, null, null);
        try {
            return c.getCount() > 0;
        }
        finally {
            c.close();
        }
    }

    @Override
    protected Iterable<String> search(String partialId, SQLiteDatabase cx) {
        log(format("SELECT id FROM %s WHERE id = LIKE '%?%'", OBJECTS), LOG, partialId);

        Cursor c = 
            cx.query(OBJECTS, array("id"), "id LIKE '%"+partialId+"%'", null, null, null, null);
        return new StringCursorIterable(c);
    }

    @Override
    protected InputStream get(String id, SQLiteDatabase cx) {
        log(format("SELECT id FROM %s WHERE id = ?", OBJECTS), LOG, id);

        Cursor c = cx.query(OBJECTS, array("object"), "id = ?", array(id), null, null, null);
        try {
            if (!c.moveToNext()) {
                return null;
            }

            return new ByteArrayInputStream(c.getBlob(0));
        }
        finally {
            c.close();
        }
    }

    @Override
    protected void put(String id, InputStream obj, SQLiteDatabase cx) {
        log(format("INSERT INTO %s VALUES (?,?)", OBJECTS), LOG, id, obj);

        ContentValues vals = new ContentValues();
        try {
            vals.put("object", ByteStreams.toByteArray(obj));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        cx.insertWithOnConflict(OBJECTS, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    protected boolean delete(String id, SQLiteDatabase cx) {
        log(format("DELETE FROM %s WHERE id = ?", OBJECTS), LOG, id);

        return cx.delete(OBJECTS, "id = ?", array(id)) > 0;
    }

}
