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
 * Eval result event generated when an evaluation has completed.
 *
 * <pre>
 *    E: evalresult {result}
 * </pre>
 */
public class PDAEvalResultEvent extends PDAEvent {

	public final String fResult;

	public PDAEvalResultEvent(String message) {
		super(message);
		fResult = message.substring(getName(message).length() + 1);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("evalresult"); //$NON-NLS-1$
	}
}
