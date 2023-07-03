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
 * Single step forward until the next <code>return</code> op code. Stop before
 * executing the <code>return</code> .
 *
 * <pre>
 * If VM running:
 *    C: stepreturn {thread_id}
 *    R: ok
 *    E: resumed {thread_id} client
 *    E: suspended {thread_id} step
 *
 * If VM suspended:
 *    C: stepreturn {thread_id}
 *    R: ok
 *    E: vmresumed client
 *    E: vmsuspended {thread_id} step
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */

public class PDAStepReturnCommand extends PDACommand {

	public PDAStepReturnCommand(int threadId) {
		super("stepreturn " + threadId); //$NON-NLS-1$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
