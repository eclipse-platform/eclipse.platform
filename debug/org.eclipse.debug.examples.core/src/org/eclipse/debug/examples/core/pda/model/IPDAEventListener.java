/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.examples.core.pda.protocol.PDAEvent;

/**
 * Listeners are notified of events occurring in a PDA program being
 * interpreted.
 *
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMStartedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMTerminatedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMResumedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAStartedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAExitedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDASuspendedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAResumedEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAUnimplementedInstructionEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDARegisterData
 * @see org.eclipse.debug.examples.core.pda.protocol.PDANoSuchLabelEvent
 * @see org.eclipse.debug.examples.core.pda.protocol.PDAEvalResultEvent
 */
public interface IPDAEventListener {

	/**
	 * Notification the given event occurred in the target program
	 * being interpreted.
	 *
	 * @param event the event
	 */
	void handleEvent(PDAEvent event);

}
