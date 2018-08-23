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
package org.eclipse.team.internal.ccvs.ui.actions;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagInRepositoryOperation;
import org.eclipse.team.internal.ui.actions.TeamAction;

public class TagInRepositoryAction extends TagAction {

	/**
	 * @see TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		ICVSResource[] resources = getSelectedCVSResources();
		if (resources.length == 0) return false;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof ICVSRepositoryLocation) return false;
		}
		return true;
	}
	
	/**
	 * @see CVSAction#needsToSaveDirtyEditors()
	 */
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#requiresLocalSyncInfo()
	 */
	protected boolean requiresLocalSyncInfo() {
		return false;
	}

	protected ITagOperation createTagOperation() {
		return new TagInRepositoryOperation(getTargetPart(), getSelectedRemoteResources());
	}
}
