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

public class TagInRepositoryAction extends TagAction {

	@Override
	public boolean isEnabled() {
		ICVSResource[] resources = getSelectedCVSResources();
		if (resources.length == 0) return false;
		for (ICVSResource resource : resources) {
			if (resource instanceof ICVSRepositoryLocation) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	@Override
	protected boolean requiresLocalSyncInfo() {
		return false;
	}

	@Override
	protected ITagOperation createTagOperation() {
		return new TagInRepositoryOperation(getTargetPart(), getSelectedRemoteResources());
	}
}
