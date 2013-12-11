package org.geogit.storage.sqlite;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

import android.database.Cursor;

public class StringCursorIterable implements Iterable<String> {

    Cursor c;

    StringCursorIterable(Cursor c) {
        this.c = c;
    }

    @Override
    public Iterator<String> iterator() {
        return new AbstractIterator<String>() {
            @Override
            protected String computeNext() {
                if (!c.moveToNext()) {
                    c.close();
                    return endOfData();
                }

                return c.getString(1);
            }
        };
    }

}
