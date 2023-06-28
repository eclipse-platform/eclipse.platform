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
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;

/**
 * Terminated event generated when the PDA program has ended execution.
 *
 * <pre>
 *    E: termianted
 * </pre>
 */
public class PDAVMTerminatedEvent extends PDAEvent {

	public PDAVMTerminatedEvent(String message) {
		super(message);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("vmterminated"); //$NON-NLS-1$
	}
}
