package org.geogit.storage.sqlite;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.ObjectDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import static org.geogit.storage.sqlite.SQLiteStorage.log;
import static java.lang.String.format;
import static org.geogit.storage.sqlite.AndroidSQLite.array;

public class AndroidStagingDatabase extends SQLiteStagingDatabase<SQLiteDatabase> {

    final static Logger LOG = LoggerFactory.getLogger(AndroidStagingDatabase.class);
    
    final static String CONFLICTS = "conflicts";
    
    @Inject
    public AndroidStagingDatabase(ObjectDatabase repoDb, ConfigDatabase configdb, Platform platform) {
        super(repoDb, new AndroidObjectDatabase(configdb, null, platform, "stage"), configdb,
            platform);
    }

    @Override
    protected void init(SQLiteDatabase cx) {
        String sql = format("CREATE TABLE IF NOT EXISTS %s (namespace VARCHAR, "
                + "path VARCHAR, conflict VARCHAR, PRIMARY KEY(namespace,path))", CONFLICTS);
        cx.execSQL(log(sql, LOG));
    }

    @Override
    protected Iterable<String> get(String namespace, String pathFilter, SQLiteDatabase cx) {
        String sql = format(
            "SELECT conflict FROM %s WHERE namespace = ? AND path LIKE '%%%s%%'", 
            CONFLICTS, pathFilter);
        log(sql, LOG);

        String where = null;
        String[] args = null;

        if (namespace != null && pathFilter != null) {
            if (namespace != null) {
                where = "namespace = ?";
                args = array(namespace);
            }
            if (pathFilter != null) {
                where = where != null ? where + " AND " : "";
                where += "path LIKE '%"+pathFilter+"%'";
            }
        }

        Cursor c = cx.query(CONFLICTS, array("conflict"), where, args, null, null, null);
        return new StringCursorIterable(c);
    }

    @Override
    protected void put(String namespace, String path, String conflict, SQLiteDatabase cx) {
        String sql = format("INSERT INTO %s VALUES (?,?,?)", CONFLICTS);
        log(sql, LOG, namespace, path, conflict);

        ContentValues vals = new ContentValues();
        vals.put("namespace", namespace);
        vals.put("path", path);
        vals.put("conflict", conflict);

        cx.insertWithOnConflict(CONFLICTS, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    @Override
    protected void remove(String namespace, String path, SQLiteDatabase cx) {
        String sql = format("DELETE FROM %s WHERE namespace = ? AND path = ?", CONFLICTS);
        log(sql, LOG, namespace, path);

        cx.delete(CONFLICTS, "namespace = ? AND path = ?", array(namespace, path));
    }
}
