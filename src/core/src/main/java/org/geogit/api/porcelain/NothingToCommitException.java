/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

/**
 * Indicates there are no staged changes to commit as the result of the execution of a
 * {@link CommitOp}
 * 
 * @author groldan
 * 
 */
public class NothingToCommitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code NothingToCommitException} with the given message.
     * 
     * @param msg the message for the exception
     */
    public NothingToCommitException(String msg) {
        super(msg);
    }
}
