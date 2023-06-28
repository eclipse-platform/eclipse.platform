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
 * Executes next instruction
 *
 * <pre>
 * If VM running:
 *    C: step {thread_id}
 *    R: ok
 *    E: resumed {thread_id} step
 *    E: suspended {thread_id} step
 *
 * If VM suspended:
 *    C: step {thread_id}
 *    R: ok
 *    E: vmresumed step
 *    E: vmsuspended {thread_id} step
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */

public class PDAStepCommand extends PDACommand {

	public PDAStepCommand(int threadId) {
		super("step " + threadId); //$NON-NLS-1$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
