/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.mappings.CompareSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelCompareParticipant;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.internal.ccvs.ui.tags.TagSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

public class CompareWithTagAction extends WorkspaceTraversalAction {

	private static boolean isOpenEditorForSingleFile() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_OPEN_COMPARE_EDITOR_FOR_SINGLE_FILE);
	}
	
	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		
		// First, determine the tag to compare with
		IResource[] resources = getSelectedResources();
		CVSTag tag = promptForTag(resources);
		if (tag == null)
			return;
		
		if (isOpenEditorForSingleFile()) {
			IFile file = getSelectedFile();
			if (file != null && SyncAction.isOKToShowSingleFile(file)) {
				CVSCompareSubscriber compareSubscriber = new CVSCompareSubscriber(resources, tag);
				SyncAction.showSingleFileComparison(getShell(), compareSubscriber, file, getTargetPage());
				compareSubscriber.dispose();
				return;
			}
		}
		
		// Create a subscriber that can cover all projects involved
		if (isShowModelSync()) {
			final CVSCompareSubscriber compareSubscriber = new CVSCompareSubscriber(getProjects(resources), tag);
			ResourceMapping[] mappings = getCVSResourceMappings();
			try {
				// TODO: Only prime the area covered by the mappings
				compareSubscriber.primeRemoteTree();
			} catch(CVSException e) {
				// ignore, the compare will fail if there is a real problem.
			}
			SubscriberScopeManager manager = new SubscriberScopeManager(compareSubscriber.getName(), mappings, compareSubscriber, true){
				@Override
				public void dispose() {
					compareSubscriber.dispose();
					super.dispose();
				}
			};
			SynchronizationContext context = CompareSubscriberContext.createContext(manager, compareSubscriber);
			ModelCompareParticipant participant = new ModelCompareParticipant(context);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
			participant.run(getTargetPart());
		} else {
			CVSCompareSubscriber compareSubscriber = new CVSCompareSubscriber(getProjects(resources), tag);
			ResourceMapping[] resourceMappings = getCVSResourceMappings();
			if (isLogicalModel(resourceMappings)) {
				compareSubscriber = new CVSCompareSubscriber(getProjects(resources), tag);
				resources = getResourcesToCompare(compareSubscriber);
				compareSubscriber.dispose();
			}
			// create a subscriber specifically for the resources for display to the user
			compareSubscriber = new CVSCompareSubscriber(resources, tag);
			try {
				compareSubscriber.primeRemoteTree();
			} catch(CVSException e) {
				// ignore, the compare will fail if there is a real problem.
			}
			//	First check if there is an existing matching participant, if so then re-use it
			CompareParticipant participant = CompareParticipant.getMatchingParticipant(resources, tag);
			if (participant == null) {
				CVSCompareSubscriber s = compareSubscriber;
				participant = new CompareParticipant(s);
				TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
			}
			participant.refresh(resources, null, null, null); 
		}
	}

	private boolean isShowModelSync() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_ENABLE_MODEL_SYNC);
	}
	
	protected CVSTag promptForTag(IResource[] resources) {
		CVSTag tag = TagSelectionDialog.getTagToCompareWith(getShell(), TagSource.create(resources), TagSelectionDialog.INCLUDE_ALL_TAGS);
		return tag;
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
}
