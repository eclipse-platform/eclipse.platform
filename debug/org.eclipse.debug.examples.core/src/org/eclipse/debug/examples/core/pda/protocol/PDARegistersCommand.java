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
 * Retrieves registers definition information
 *
 * <pre>
 *    C: registers {group name}
 *    R: {register name} {true|false}|{bit field name} {start bit} {bit count} {mnemonic 1} {mnemonic 2} ...#{register name} ...
 * </pre>
 */

public class PDARegistersCommand extends PDACommand {

	public PDARegistersCommand(String group) {
		super("registers " + group); //$NON-NLS-1$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDARegistersCommandResult(resultText);
	}
}
