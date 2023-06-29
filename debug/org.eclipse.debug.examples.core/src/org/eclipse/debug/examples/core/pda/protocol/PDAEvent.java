/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
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
 * Base class for PDA events.
 */
public class PDAEvent {
	public final String fMessage;
	public final String fName;

	public PDAEvent(String message) {
		fMessage = message;
		fName = getName(message);
	}

	protected String getName(String message) {
		int nameEnd = message.indexOf(' ');
		nameEnd = nameEnd == -1 ? message.length() : nameEnd;
		return message.substring(0, nameEnd);
	}

	public static PDAEvent parseEvent(String message) {
		if (PDAEvalResultEvent.isEventMessage(message)) {
			return new PDAEvalResultEvent(message);
		}
		else if (PDAExitedEvent.isEventMessage(message)) {
			return new PDAExitedEvent(message);
		}
		else if (PDANoSuchLabelEvent.isEventMessage(message)) {
			return new PDANoSuchLabelEvent(message);
		}
		else if (PDARegistersEvent.isEventMessage(message)) {
			return new PDARegistersEvent(message);
		}
		else if (PDAResumedEvent.isEventMessage(message)) {
			return new PDAResumedEvent(message);
		}
		else if (PDAStartedEvent.isEventMessage(message)) {
			return new PDAStartedEvent(message);
		}
		else if (PDASuspendedEvent.isEventMessage(message)) {
			return new PDASuspendedEvent(message);
		}
		else if (PDATerminatedEvent.isEventMessage(message)) {
			return new PDATerminatedEvent(message);
		}
		else if (PDAUnimplementedInstructionEvent.isEventMessage(message)) {
			return new PDAUnimplementedInstructionEvent(message);
		}
		else if (PDAVMResumedEvent.isEventMessage(message)) {
			return new PDAVMResumedEvent(message);
		}
		else if (PDAVMStartedEvent.isEventMessage(message)) {
			return new PDAVMStartedEvent(message);
		}
		else if (PDAVMSuspendedEvent.isEventMessage(message)) {
			return new PDAVMSuspendedEvent(message);
		}
		else {
			return new PDAEvent(message);
		}
	}
}
