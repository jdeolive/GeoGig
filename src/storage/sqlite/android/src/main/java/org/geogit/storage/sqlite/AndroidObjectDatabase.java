/* Copyright (c) 2014 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static java.lang.String.format;
import static org.geogit.storage.sqlite.AndroidSQLite.array;
import static org.geogit.storage.sqlite.SQLiteStorage.log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.ObjectDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Implementation of {@link ObjectDatabase} based on Android SQLite api.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class AndroidObjectDatabase extends SQLiteObjectDatabase<SQLiteDatabase> {

    static Logger LOG = LoggerFactory.getLogger(AndroidObjectDatabase.class);

    final String dbname;
    final GraphDatabase graphdb;

    @Inject
    public AndroidObjectDatabase(ConfigDatabase configdb, GraphDatabase graphdb, Platform platform) {
        this(configdb, graphdb, platform, OBJECTS);
    }

    public AndroidObjectDatabase(ConfigDatabase configdb, GraphDatabase graphdb, Platform platform,
        String dbname) {
        super(configdb, platform);
        this.graphdb = graphdb;
        this.dbname = dbname;
    }

    @Override
    protected SQLiteDatabase connect(File geogitDir) {
        File file = new File(geogitDir, dbname + ".db");
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    @Override
    protected void close(SQLiteDatabase cx) {
        cx.close();
    }

    @Override
    protected void init(SQLiteDatabase cx) {
        cx.beginTransaction();

        String sql = format("CREATE TABLE IF NOT EXISTS %s (id varchar PRIMARY KEY, object blob)", OBJECTS);
        cx.execSQL(log(sql, LOG));

        sql = format("CREATE INDEX IF NOT EXISTS %s_id_index ON %s (id)", OBJECTS, OBJECTS);
        cx.execSQL(log(sql, LOG));

        cx.setTransactionSuccessful();
        cx.endTransaction();
    }

    @Override
    protected boolean has(String id, SQLiteDatabase cx) {
        log(format("SELECT count(*) FROM %s WHERE id = ?", OBJECTS), LOG, id);

        Cursor c = cx.query(OBJECTS, array("id"), "id = ?", array(id), null, null, null);
        try {
            return c.getCount() > 0;
        } finally {
            c.close();
        }
    }

    @Override
    protected Iterable<String> search(String partialId, SQLiteDatabase cx) {
        log(format("SELECT id FROM %s WHERE id = LIKE '%%%s%%'", OBJECTS, partialId), LOG);

        Cursor c = cx.query(OBJECTS, array("id"), "id LIKE '%" + partialId + "%'", null, null,
                null, null);
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
        } finally {
            c.close();
        }
    }

    @Override
    public boolean put(RevObject object) {
        // JD: Since (for now) we can't support dynamic method interceptors we have to do the job
        // of ObjectDatabasePutInterceptor manually
        if (super.put(object)) {
            if (graphdb != null) {
                if (RevObject.TYPE.COMMIT.equals(object.getType())) {
                    RevCommit commit = (RevCommit) object;
                    graphdb.put(commit.getId(), commit.getParentIds());
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected void put(String id, InputStream obj, SQLiteDatabase cx) {
        log(format("INSERT INTO %s VALUES (?,?)", OBJECTS), LOG, id, obj);

        ContentValues vals = new ContentValues();
        try {
            vals.put("id", id);
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
