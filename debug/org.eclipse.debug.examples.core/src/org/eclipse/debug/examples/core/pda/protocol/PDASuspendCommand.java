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
 * Suspends execution of a single thread.  Can be issued only if the virtual
 * machine is running.
 *
 * <pre>
 *    C: suspend {thread_id}
 *    R: ok
 *    E: suspended {thread_id} client
 *
 * Errors:
 *    error: invalid thread
      error: vm already suspended
 *    error: thread already suspended
 * </pre>
 */

public class PDASuspendCommand extends PDACommand {

	public PDASuspendCommand(int threadId) {
		super("suspend " + threadId); //$NON-NLS-1$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
