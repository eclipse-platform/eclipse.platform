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
 * Listeners are notified of events occurring in a PDA program
 * being interpreted.
 *
 * @see org.eclipse.debug.examples.core.protocol.PDAVMStarted
 * @see org.eclipse.debug.examples.core.protocol.PDAVMTerminated
 * @see org.eclipse.debug.examples.core.protocol.PDAVMSuspneded
 * @see org.eclipse.debug.examples.core.protocol.PDAVMResumed
 * @see org.eclipse.debug.examples.core.protocol.PDAStarted
 * @see org.eclipse.debug.examples.core.protocol.PDAExited
 * @see org.eclipse.debug.examples.core.protocol.PDASuspended
 * @see org.eclipse.debug.examples.core.protocol.PDAResumed
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
