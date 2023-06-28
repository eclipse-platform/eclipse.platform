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
 * Unimplemented Instruction event generated when the PDA program encounters
 * an instruction that it does not recognize.  This event is usually followed
 * by a VM Suspended event.
 *
 * <pre>
 *    E: unimplemented instruction {label}
 * </pre>
 */
public class PDAUnimplementedInstructionEvent extends PDAEvent {

	public final String fOperation;

	public PDAUnimplementedInstructionEvent(String message) {
		super(message);
		fOperation = message.substring(getName(message).length() + 1);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("unimplemented instruction"); //$NON-NLS-1$
	}

	@Override
	protected String getName(String message) {
		if (isEventMessage(message)) {
			return "unimplemented instruction"; //$NON-NLS-1$
		}
		throw new IllegalArgumentException("Invalid event: " + message); //$NON-NLS-1$
	}
}
