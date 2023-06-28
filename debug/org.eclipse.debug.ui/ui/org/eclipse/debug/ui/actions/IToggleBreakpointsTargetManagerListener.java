/*******************************************************************************
 * Copyright (c) 2008, 2012 Wind River Systems and others.
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
package org.eclipse.debug.ui.actions;


/**
 * Listener for changes in the preferred toggle targets in the toggle
 * breakpoints target manager.  This interface allows toggle breakpoint
 * actions to update their enablement when the user changes the preferred
 * toggle target settings.
 *
 * @see IToggleBreakpointsTargetManager
 * @see IToggleBreakpointsTargetFactory
 * @since 3.8
 */
public interface IToggleBreakpointsTargetManagerListener {

	/**
	 * Called when the preferred toggle targets have changed.
	 */
	void preferredTargetsChanged();
}
