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
 * VM started event generated when the the interpreter has started (guaranteed
 * to be the first event sent)
 *
 * <pre>
 *    E: vmstarted
 * </pre>
 */
public class PDAVMStartedEvent extends PDAEvent {

	public PDAVMStartedEvent(String message) {
		super(message);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("vmstarted"); //$NON-NLS-1$
	}
}
