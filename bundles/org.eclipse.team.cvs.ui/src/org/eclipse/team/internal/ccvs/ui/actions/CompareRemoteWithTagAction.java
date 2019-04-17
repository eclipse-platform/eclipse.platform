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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation;
import org.eclipse.team.internal.ccvs.ui.tags.TagSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;

/**
 * Compare to versions of a remote resource.
 */
public class CompareRemoteWithTagAction extends CVSAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		
		final ICVSRemoteResource[] resources = getSelectedRemoteResources();
		if (resources.length == 0) return;
		
		// Obtain the tag to compare against
		final ICVSRemoteResource resource = resources[0];
		final CVSTag[] tag = new CVSTag[] { null};
		run((IRunnableWithProgress) monitor -> tag[0] = TagSelectionDialog.getTagToCompareWith(getShell(),
				TagSource.create(resources),
				TagSelectionDialog.INCLUDE_BRANCHES | TagSelectionDialog.INCLUDE_VERSIONS
						| TagSelectionDialog.INCLUDE_DATES | TagSelectionDialog.INCLUDE_HEAD_TAG),
				false /* cancelable */, PROGRESS_BUSYCURSOR);
		if (tag[0] == null) return;
		
		// Run the compare operation in the background
		try {
			RemoteCompareOperation.create(getTargetPart(), resource, tag[0])
				.run();
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
	}

	@Override
	public boolean isEnabled() {
		ICVSRemoteResource[] resources = getSelectedRemoteResources();
		// Only support single select for now.
		// Need to avoid overlap if multi-select is supported
		return resources.length == 1;
	}

}
