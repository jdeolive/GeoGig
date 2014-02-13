/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import org.slf4j.Logger;

/**
 * Utility constants for SQLite storage.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class SQLite {

    public static final String FORMAT_NAME = "sqlite";
    public static final String VERSION = "0.1";

    /**
     * Logs a (prepared) sql statement.
     * 
     * @param sql Base sql to log.
     * @param log The logger object.
     * @param args Optional arguments to the statement.
     * 
     * @return The original statement.
     */
    public static String log(String sql, Logger log, Object... args) {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder(sql);
            if (args.length > 0) {
                sb.append(";");
                for (int i = 0; i < args.length; i++) {
                    sb.append(i).append("=").append(args[i]).append(", ");
                }
                sb.setLength(sb.length()-2);
            }
            log.debug(sb.toString());
        }
        return sql;
    }

}
