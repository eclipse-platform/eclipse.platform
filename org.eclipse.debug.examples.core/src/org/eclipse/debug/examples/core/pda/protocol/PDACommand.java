/*******************************************************************************
 * Copyright (c) 2007, 2009 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;


/**
 * Base class for PDA commands.  Sub-classes should format the request string
 * and implement the method to create the proper result object.
 */
abstract public class PDACommand {

	final private String fRequest;

	public PDACommand(String request) {
		fRequest = request;
	}

	/**
	 * Returns the request to be sent to PDA.
	 */
	public String getRequest() {
		return fRequest;
	}

	/**
	 * Returns the command result based on the given PDA response.  This command
	 * uses the class type parameter as the return type to allow the compiler to
	 * enforce the correct command result.  This class must be implemented by
	 * each command to create the concrete result type.
	 */
	abstract public PDACommandResult createResult(String resultText);
}
