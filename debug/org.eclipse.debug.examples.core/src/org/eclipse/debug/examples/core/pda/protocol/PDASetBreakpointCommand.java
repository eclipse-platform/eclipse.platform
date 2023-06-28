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
 * Sets a breakpoint at given line
 *
 * <pre>
 * Suspend a single thread:
 *    C: set {line_number} 0
 *    R: ok
 *    C: resume {thread_id}
 *    E: resumed {thread_id} client
 *    E: suspended {thread_id} breakpoint line_number
 *
 * Suspend the VM:
 *    C: set {line_number} 1
 *    R: ok
 *    C: vmresume
 *    E: vmresumed client
 *    E: vmsuspended {thread_id} breakpoint line_number
 * </pre>
 */

public class PDASetBreakpointCommand extends PDACommand {

	public PDASetBreakpointCommand(int line, boolean stopVM) {
		super("set " +  //$NON-NLS-1$
			  line + " " +  //$NON-NLS-1$
			  (stopVM ? "1" : "0")); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
