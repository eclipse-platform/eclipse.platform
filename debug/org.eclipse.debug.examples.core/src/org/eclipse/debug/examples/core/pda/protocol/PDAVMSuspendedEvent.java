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
 * VM Suspended event generated when the virtual machine is suspended.  If the VM
 * is suspended as a result of a thread event (e.g. thread hitting a breakpoint),
 * then the thread_id is included in the event.
 *
 * <pre>
 *    E: vmsuspended [thread_id] {reason}
 * </pre>
 *
 * <code>{reason}</code> is the cause of the suspension:
 * <ul>
 *   <li><code>breakpoint N</code> - a breakpoint at line <code>N</code> was hit</li>
 *   <li><code>client</code> - a client request to suspend has completed</li>
 *   <li><code>drop</code> - a client request to drop a frame has completed</li>
 *   <li><code>event E</code> - an error was encountered, where <code>E</code> is one
 *       of <code>unimpinstr</code> or <code>nosuchlabel</code></li>
 *   <li><code>step</code> - a step request has completed</li>
 *   <li><code>watch A F::V</code> - a watchpoint was hit for reason <code>A</code>
 *       (<code>read</code> or <code>write</code>), on variable <code>V</code> in
 *       function <code>F</code></li>
 * </ul>
 */
public class PDAVMSuspendedEvent extends PDARunControlEvent {

	public PDAVMSuspendedEvent(String message) {
		super(message);
	}

	public static boolean isEventMessage(String message) {
		return message.startsWith("vmsuspended"); //$NON-NLS-1$
	}
}
