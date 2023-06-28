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
 * Set a watchpoint on variable <code>variable_name</code> in function
 * <code>function</code> to magic value <code>watch_operation</code>.  The magic
 * value is a bit flag corresponding to read access (1), write access (2), or
 * both (3); the magic value 0 clears the watchpoint.
 *
 * <pre>
 *    C: watch {function}::{variable_name} {watch_operation}
 *    R: ok
 *    C: vmresume
 *    R: vmresumed client
 *    E: vmsuspended {thread_id} watch {watch_operation} {function}::{variable_name}
 * </pre>
 */
public class PDAWatchCommand extends PDACommand {

	public static final int READ = 1;
	public static final int WRITE = 2;
	public static final int BOTH = READ | WRITE;
	public static final int NONE = 0;

	public PDAWatchCommand(String function, String variable, int operation) {
		super("watch " + function+ "::" + variable + " " + operation); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
