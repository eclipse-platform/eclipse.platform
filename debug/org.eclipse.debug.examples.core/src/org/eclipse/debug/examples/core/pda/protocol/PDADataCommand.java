/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems and others.
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
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;


/**
 * Return the contents of the data stack; reply is the data from oldest to newest
 * as a single string
 *
 * <pre>
 *    C: data {thread_id}
 *    R: {value 1}|{value 2}|{value 3}|...|
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */
public class PDADataCommand extends PDACommand {

	public PDADataCommand(int threadId) {
		super("data " + threadId); //$NON-NLS-1$
	}

	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDAListResult(resultText);
	}
}
