/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.subscriber.CommitSetDialog;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class WorkspaceChangeSetCapability extends ModelParticipantChangeSetCapability {

	@Override
	public boolean supportsActiveChangeSets() {
		return true;
	}
	
	@Override
	public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
		return supportsActiveChangeSets() && 
		configuration.getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
	}
	
	@Override
	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration, IDiff[] infos) {
		ActiveChangeSet set = getActiveChangeSetManager().createSet(CVSUIMessages.WorkspaceChangeSetCapability_1, new IDiff[0]); 
		CommitSetDialog dialog = new CommitSetDialog(configuration.getSite().getShell(), set, getResources(infos), CommitSetDialog.NEW);
		dialog.open();
		if (dialog.getReturnCode() != Window.OK) return null;
		set.add(infos);
		return set;
	}

	private IResource[] getResources(IDiff[] diffs) {
		Set<IResource> result = new HashSet<>();
		for (IDiff diff : diffs) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null)
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}
	
	@Override
	public void editChangeSet(ISynchronizePageConfiguration configuration, ActiveChangeSet set) {
		CommitSetDialog dialog = new CommitSetDialog(configuration.getSite().getShell(), set, set.getResources(), CommitSetDialog.EDIT);
		dialog.open();
		if (dialog.getReturnCode() != Window.OK) return;
		// Nothing to do here as the set was updated by the dialog 
	}

	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return CVSUIPlugin.getPlugin().getChangeSetManager();
	}
	
	@Override
	public CheckedInChangeSetCollector createCheckedInChangeSetCollector(ISynchronizePageConfiguration configuration) {
		return new CheckedInChangeSetCollector(configuration, CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
	}
}
