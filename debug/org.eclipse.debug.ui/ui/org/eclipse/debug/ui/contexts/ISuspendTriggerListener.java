/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.debug.ui.contexts;

import org.eclipse.debug.core.ILaunch;

/**
 * Listeners are notified when a launch has suspended at a context
 * where debugging should begin. For example, in a stack frame where
 * a breakpoint has been encountered.
 * <p>
 * Generally, clients implement <code>ISuspendTrigger</code> and the debug platform registers
 * as a suspend trigger listener.
 * </p>
 * @see ISuspendTrigger
 * @since 3.3
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISuspendTriggerListener {

	/**
	 * Notification the given launch has suspended at the
	 * specified context.
	 *
	 * @param launch the launch that has suspended
	 * @param context the context on which the launch suspended
	 */
	void suspended(ILaunch launch, Object context);

}
