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
 * Instructs the debugger to terminate.
 *
 * <pre>
 *    C: terminate
 *    R: ok
 *    E: vmterminated
 * </pre>
 */

public class PDATerminateCommand extends PDACommand {

	public PDATerminateCommand() {
		super("terminate"); //$NON-NLS-1$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
