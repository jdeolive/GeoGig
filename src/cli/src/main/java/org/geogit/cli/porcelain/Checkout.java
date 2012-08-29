/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.beust.jcommander.Parameters;

/**
 *
 */
@Service
@Scope(value = "prototype")
@Parameters(commandNames = { "checkout", "co" }, commandDescription = "Checkout a branch or paths to the working tree")
public class Checkout extends AbstractCommand implements CLICommand {

    @Override
    public void runInternal(GeogitCLI cli) {
        // TODO Auto-generated method stub

    }

}