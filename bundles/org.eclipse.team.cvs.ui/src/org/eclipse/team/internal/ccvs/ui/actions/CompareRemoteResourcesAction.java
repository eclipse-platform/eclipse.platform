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

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.ui.CVSCompareEditorInput;
import org.eclipse.team.internal.ccvs.ui.ResourceEditionNode;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation;

/**
 * This action is used for comparing two arbitrary remote resources. This is
 * enabled in the repository explorer.
 */
public class CompareRemoteResourcesAction extends CVSAction {

	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRemoteResource[] editions = getSelectedRemoteResources();
		if (editions == null || editions.length != 2) {
			return;
		}
		try {
			if (isSameFolder(editions)) {
				RemoteCompareOperation.create(null, editions[0], RemoteCompareOperation.getTag(editions[1])).run();
			} else {
				ResourceEditionNode left = new ResourceEditionNode(editions[0]);
				ResourceEditionNode right = new ResourceEditionNode(editions[1]);
				CompareUI.openCompareEditorOnPage(new CVSCompareEditorInput(left, right), getTargetPage());
			}
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
	}

	protected boolean isSameFolder(ICVSRemoteResource[] editions) {
		return editions[0].isContainer() && editions[0].getRepository().equals(editions[1].getRepository())
				&& editions[0].getRepositoryRelativePath().equals(editions[1].getRepositoryRelativePath());
	}
	
	@Override
	public boolean isEnabled() {
		ICVSRemoteResource[] resources = getSelectedRemoteResources();
		if (resources.length != 2) return false;
		if (resources[0].isContainer() != resources[1].isContainer()) return false;
		// Don't allow comparisons of two unrelated remote projects
		return !resources[0].isContainer() || isSameFolder(resources);
	}

}
