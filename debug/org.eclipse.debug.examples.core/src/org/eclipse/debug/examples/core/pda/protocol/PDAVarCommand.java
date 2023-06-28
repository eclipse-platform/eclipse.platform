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
 * Return the contents of variable <code>variable_name</code> in the control
 * stack frame <code>frame_number</code> (stack frames are indexed from 0, 0
 * being the oldest).
 *
 * <pre>
 *    C: var  {thread_id} {frame_number} {variable_name}
 *    R: {variable_value}
 *
 * Errors:
 *    error: invalid thread
 *    error: variable undefined
 * </pre>
 */

public class PDAVarCommand extends PDACommand {

	public PDAVarCommand(int threadId, int frameId, String name) {
		super("var " + threadId + " " + frameId + " " + name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
