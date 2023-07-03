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
 * Sets a data value in the data stack at the given location (the data stack is
 * indexed from 0, 0 being the oldest).
 *
 * <pre>
 *    C: setdata {thread_id} {index} {value}
 *    R: ok
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */
public class PDASetDataCommand extends PDACommand {

	public PDASetDataCommand(int threadId, int index, String value) {
		super("setdata " + threadId + " " + index + " " + value); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
