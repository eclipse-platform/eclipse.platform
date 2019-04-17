/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.operations.FetchAllMembersOperation;

public class FetchAllMembersAction extends CVSAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		ICVSRepositoryLocation repoLocation = getRepositoryManager().getRepositoryLocationFor(folders[0]);
		new FetchAllMembersOperation (getTargetPart(), folders, repoLocation).run();
	}

	@Override
	public boolean isEnabled() {
		//Only enable for one selection for now
		return getSelectedRemoteFolders().length == 1;		
	}

}
