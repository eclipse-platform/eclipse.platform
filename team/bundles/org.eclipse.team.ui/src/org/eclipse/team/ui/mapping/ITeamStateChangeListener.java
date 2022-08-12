/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.EventListener;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.team.ui.synchronize.TeamStateProvider;

/**
 * A listener registered with an {@link TeamStateProvider} in order to
 * receive change events when the team state of any
 * resources change. It is the responsibility of clients to determine if
 * a label update is required based on the changed resources.
 * <p>
 * Change events may not be issued if a local change has resulted in a
 * synchronization state change. It is up to clients to check whether
 * a label update is required for a model element when local resources change
 * by using the resource delta mechanism.
 * <p>
 * Clients may implement this interface
 *
 * @see IWorkspace#addResourceChangeListener(org.eclipse.core.resources.IResourceChangeListener)
 * @since 3.2
 */
public interface ITeamStateChangeListener extends EventListener {

	/**
	 * Notification that the team state of resources
	 * has changed.
	 * @param event the event that describes which resources have changed
	 */
	public void teamStateChanged(ITeamStateChangeEvent event);

}
