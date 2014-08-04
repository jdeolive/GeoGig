/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.locationtech.geogig.api.hooks;

public class CannotRunGeogigOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CannotRunGeogigOperationException() {
        // default constructor, needed by jdk6
    }

    /**
     * Constructs a new {@code CannotRunGeogigOperationException} with the given message.
     * 
     * @param msg the message for the exception
     */
    public CannotRunGeogigOperationException(String msg) {
        super(msg);
    }

    public CannotRunGeogigOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}