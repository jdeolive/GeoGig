/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import java.sql.ResultSet;
import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

/**
 * Wraps a ResultSet consisting of a single string column in an iterable.
 * 
 * @author Justin Deoliveira, Boundless
 *
 */
public class StringResultSetIterable implements Iterable<String> {

    ResultSet rs;

    StringResultSetIterable(ResultSet rs) {
        this.rs = rs;
    }

    @Override
    public Iterator<String> iterator() {
        return new AbstractIterator<String>() {
            @Override
            protected String computeNext() {
                try {
                    if (!rs.next()) {
                        rs.close();
                        return endOfData();
                    }

                    return rs.getString(1);
                }
                catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
