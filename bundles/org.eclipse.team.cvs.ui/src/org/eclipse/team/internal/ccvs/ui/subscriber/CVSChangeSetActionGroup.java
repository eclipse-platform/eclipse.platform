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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.synchronize.*;

/**
 * Action group that is used by CVS Change Set Capabilities
 */
public class CVSChangeSetActionGroup extends SynchronizePageActionGroup {

	private OpenChangeSetAction openCommitSet;
	
	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		openCommitSet = new OpenChangeSetAction(configuration);
	}
	
	@Override
	public void fillContextMenu(IMenuManager menu) {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {  
			if (((IChangeSetProvider)participant).getChangeSetCapability().enableCheckedInChangeSetsFor(getConfiguration())) {
				appendToGroup(
						menu, 
						ISynchronizePageConfiguration.FILE_GROUP, 
						openCommitSet);
			}
		}
	}
}
