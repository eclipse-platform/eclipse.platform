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
 * Pushes the given value on top of the data stack.
 *
 * <pre>
 *    C: pushdata {thread_id} {value}
 *    R: ok
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */

public class PDAPushDataCommand extends PDACommand {

	public PDAPushDataCommand(int threadId, String value) {
		super("pushdata " + threadId + " " + value); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
