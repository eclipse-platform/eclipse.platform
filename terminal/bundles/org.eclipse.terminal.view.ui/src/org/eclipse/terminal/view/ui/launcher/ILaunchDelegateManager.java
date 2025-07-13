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
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.launcher;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.viewers.ISelection;

public interface ILaunchDelegateManager {

	/**
	 * Returns the list of all contributed terminal launcher delegates.
	 *
	 * @param unique If <code>true</code>, the method returns new instances for each
	 *               contributed terminal launcher delegate.
	 *
	 * @return The list of contributed terminal launcher delegates, or an empty list.
	 */
	List<ILauncherDelegate> getLauncherDelegates(boolean unique);

	/**
	 * Lookup a terminal launcher delegate identified by its unique id.
	 *
	 * @param id The unique id of the terminal launcher delegate or <code>null</code>
	 * @param unique If <code>true</code>, the method returns new instances of the terminal launcher delegate contribution.
	 *
	 * @return The terminal launcher delegate instance or an empty optional if not found.
	 */
	Optional<ILauncherDelegate> findLauncherDelegate(String id, boolean unique);

	/**
	 * Returns the applicable terminal launcher delegates for the given selection.
	 *
	 * @param selection The selection or <code>null</code>.
	 * @return The list of applicable terminal launcher delegates or an empty list.
	 */
	List<ILauncherDelegate> getApplicableLauncherDelegates(ISelection selection);

}