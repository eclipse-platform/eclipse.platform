/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;

/**
 * Action that refreshs the tags in the CVS repositories view.
 */
public class RefreshTagsAction extends CVSRepoViewAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		RefreshRemoteProjectWizard.execute(getShell(), locations[0]);
	}

	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (locations.length != 1) return false;
		return true;
	}
}
