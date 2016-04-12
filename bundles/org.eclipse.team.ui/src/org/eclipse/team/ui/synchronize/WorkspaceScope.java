/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ui.TeamUIMessages;

/**
 * A synchronize scope whose roots are the workspace.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class WorkspaceScope extends AbstractSynchronizeScope  {

	/**
	 * Create the resource scope that indicates that the subscriber roots should be used
	 */
	public WorkspaceScope() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#getName()
	 */
	@Override
	public String getName() {
		return TeamUIMessages.WorkspaceScope_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#getRoots()
	 */
	@Override
	public IResource[] getRoots() {
		// Return null which indicates to use the subscriber roots
		return null;
	}
}
