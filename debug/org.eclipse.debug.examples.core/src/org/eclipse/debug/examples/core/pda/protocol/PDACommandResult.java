/*******************************************************************************
 * Copyright (c) 2008, 2009 Wind River Systems and others.
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
 * Basic command result object.  This command result simply allows access to the
 * PDA response.  Sub-classes may override to optionally parse the response text
 * and return higher-level objects.
 */
public class PDACommandResult {

	final public String fResponseText;

	public PDACommandResult(String response) {
		fResponseText = response;
	}
}
