/*******************************************************************************
 * Copyright (c) 2011 - 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Christoph Läubrich - extract to interface
 *******************************************************************************/
package org.eclipse.terminal.view.ui.launcher;

import org.eclipse.jface.viewers.ISelection;

public interface ILaunchDelegateManager {

	/**
	 * Returns the list of all contributed terminal launcher delegates.
	 *
	 * @param unique If <code>true</code>, the method returns new instances for each
	 *               contributed terminal launcher delegate.
	 *
	 * @return The list of contributed terminal launcher delegates, or an empty array.
	 */
	ILauncherDelegate[] getLauncherDelegates(boolean unique);

	/**
	 * Returns the terminal launcher delegate identified by its unique id. If no terminal
	 * launcher delegate with the specified id is registered, <code>null</code> is returned.
	 *
	 * @param id The unique id of the terminal launcher delegate or <code>null</code>
	 * @param unique If <code>true</code>, the method returns new instances of the terminal launcher delegate contribution.
	 *
	 * @return The terminal launcher delegate instance or <code>null</code>.
	 */
	ILauncherDelegate getLauncherDelegate(String id, boolean unique);

	/**
	 * Returns the applicable terminal launcher delegates for the given selection.
	 *
	 * @param selection The selection or <code>null</code>.
	 * @return The list of applicable terminal launcher delegates or an empty array.
	 */
	ILauncherDelegate[] getApplicableLauncherDelegates(ISelection selection);

}