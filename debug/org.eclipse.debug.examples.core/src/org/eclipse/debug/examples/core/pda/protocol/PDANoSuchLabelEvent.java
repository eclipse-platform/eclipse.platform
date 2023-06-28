/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
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
 * No Such Label event generated when the PDA program encounters an call to a
 * non-existant label in a PDA program.
 *
 * <pre>
 *    E: no such label {label}
 * </pre>
 */
public class PDANoSuchLabelEvent extends PDAEvent {

	public final String fLabel;

	public PDANoSuchLabelEvent(String message) {
		super(message);
		fLabel = message.substring(getName(message).length() + 1);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("no such label"); //$NON-NLS-1$
	}

	@Override
	protected String getName(String message) {
		if (isEventMessage(message)) {
			return "no such label"; //$NON-NLS-1$
		}
		throw new IllegalArgumentException("Invalid event: " + message); //$NON-NLS-1$
	}
}
