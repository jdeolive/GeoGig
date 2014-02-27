/* Copyright (c) 2014 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import java.util.Iterator;

import android.database.Cursor;

import com.google.common.collect.AbstractIterator;

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

                return c.getString(0);
            }
        };
    }

}
