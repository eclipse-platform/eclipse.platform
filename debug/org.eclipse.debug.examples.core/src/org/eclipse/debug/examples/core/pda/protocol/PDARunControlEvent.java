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
 * Base class for run-control events.
 */
public class PDARunControlEvent extends PDAEvent {

	public final int fThreadId;
	public final String fReason;

	public PDARunControlEvent(String message) {
		super(message);
		fThreadId = getThreadId(message);
		fReason = getStateChangeReason(message);
	}

	protected int getThreadId(String message) {
		int nameEnd = getName(message).length();
		if ( Character.isDigit(message.charAt(nameEnd + 1)) ) {
			int threadIdEnd = message.indexOf(' ', nameEnd + 1);
			threadIdEnd = threadIdEnd == -1 ? message.length() : threadIdEnd;
			try {
				return Integer.parseInt(message.substring(nameEnd + 1, threadIdEnd));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid event: " + message);                 //$NON-NLS-1$
			}
		} else {
			return -1;
		}
	}

	protected String getStateChangeReason(String message) {
		int idx = getName(message).length();
		if ( Character.isDigit(message.charAt(idx + 1)) ) {
			idx = message.indexOf(' ', idx + 1);
			idx = idx == -1 ? message.length() : idx + 1;
		} else {
			idx++;
		}
		if (idx >=  message.length()) {
			return ""; //$NON-NLS-1$
		}

		int endIdx = message.indexOf(' ', idx);
		endIdx = endIdx == -1 ? message.length() : endIdx;
		return message.substring(idx, endIdx);
	}

	@Override
	protected String getName(String message) {
		int nameEnd = message.indexOf(' ');
		nameEnd = nameEnd == -1 ? message.length() : nameEnd;
		return message.substring(0, nameEnd);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("started"); //$NON-NLS-1$
	}
}
