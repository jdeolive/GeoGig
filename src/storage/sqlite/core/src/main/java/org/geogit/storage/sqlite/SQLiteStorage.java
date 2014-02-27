/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.slf4j.Logger;

import com.google.common.base.Optional;

/**
 * Utility class for SQLite storage.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class SQLiteStorage {
    /**
     * Format name used for configuration.
     */
    public static final String FORMAT_NAME = "sqlite";

    /**
     * Implementation version.
     */
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
                sb.setLength(sb.length() - 2);
            }
            log.debug(sb.toString());
        }
        return sql;
    }

    /**
     * Returns the .geogit directory for the platform object.
     */
    public static File geogitDir(Platform platform) {
        Optional<URL> url = new ResolveGeogitDir(platform).call();
        if (!url.isPresent()) {
            throw new RuntimeException("Unable to resolve .geogit directory");
        }
        try {
            return new File(url.get().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error resolving .geogit directory", e);
        }
    }
}
