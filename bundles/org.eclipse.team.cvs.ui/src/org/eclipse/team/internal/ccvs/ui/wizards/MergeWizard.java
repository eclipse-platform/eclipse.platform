/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceTraversalAction;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelMergeOperation;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelMergeParticipant;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOperation;
import org.eclipse.team.internal.ccvs.ui.subscriber.MergeSynchronizeParticipant;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;

public class MergeWizard extends Wizard {
	MergeWizardPage page;
	IResource[] resources;
	private final IWorkbenchPart part;
	private final ResourceMapping[] mappings;
	
	public MergeWizard(IWorkbenchPart part, IResource[] resources, ResourceMapping[] mappings) {
		this.part = part;
		this.resources = resources;
		this.mappings = mappings;
	}

	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		TagSource tagSource = TagSource.create(resources);
		setWindowTitle(CVSUIMessages.MergeWizard_title); 
		ImageDescriptor mergeImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_MERGE);
		page = new MergeWizardPage("mergePage", CVSUIMessages.MergeWizard_0, mergeImage, CVSUIMessages.MergeWizard_1, tagSource); //$NON-NLS-1$  
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		
		CVSTag startTag = page.getStartTag();
		CVSTag endTag = page.getEndTag();			
		
		if (startTag == null || !page.isPreview()) {
			// Perform the update (merge) in the background
			UpdateOperation op = new UpdateOperation(getPart(), mappings, getLocalOptions(startTag, endTag), null);
			try {
				op.run();
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
			} catch (InterruptedException e) {
				// Ignore
			}
		} else {
			if (isShowModelSync()) {
				ModelMergeParticipant participant = ModelMergeParticipant.getMatchingParticipant(mappings, startTag, endTag);
				if(participant == null) {
					CVSMergeSubscriber s = new CVSMergeSubscriber(getProjects(resources), startTag, endTag, true);
					try {
						new ModelMergeOperation(getPart(), mappings, s, page.isOnlyPreviewConflicts()).run();
					} catch (InvocationTargetException e) {
						CVSUIPlugin.log(IStatus.ERROR, "Internal error", e.getTargetException()); //$NON-NLS-1$
					} catch (InterruptedException e) {
						// Ignore
					}
				} else {
					participant.refresh(null, mappings);
				}
			} else {
				// First check if there is an existing matching participant, if so then re-use it
				try {
					resources = getAllResources(startTag, endTag);
				} catch (InvocationTargetException e) {
					// Log and continue with the original resources
					CVSUIPlugin.log(IStatus.ERROR, "An error occurred while determining if extra resources should be included in the merge", e.getTargetException()); //$NON-NLS-1$
				}
				MergeSynchronizeParticipant participant = MergeSynchronizeParticipant.getMatchingParticipant(resources, startTag, endTag);
				if(participant == null) {
					CVSMergeSubscriber s = new CVSMergeSubscriber(resources, startTag, endTag, false);
					participant = new MergeSynchronizeParticipant(s);
					TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
				}
				participant.refresh(resources, null, null, null);

			}
		}
		return true;
	}

	private IResource[] getAllResources(CVSTag startTag, CVSTag endTag) throws InvocationTargetException {
		// Only do the extra work if the model is a logical model (i.e. not IResource)
		if (!WorkspaceTraversalAction.isLogicalModel(mappings))
			return resources;
		CVSMergeSubscriber s = new CVSMergeSubscriber(WorkspaceTraversalAction.getProjects(resources), startTag, endTag, false);
		IResource[] allResources = WorkspaceTraversalAction.getResourcesToCompare(mappings, s);
		s.cancel();
		return allResources;
	}
	
	public static boolean isShowModelSync() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_ENABLE_MODEL_SYNC);
	}
	
	private IResource[] getProjects(IResource[] resources) {
		Set<IProject> projects = new HashSet<>();
		for (IResource resource : resources) {
			projects.add(resource.getProject());
		}
		return projects.toArray(new IResource[projects.size()]);
	}

	private Command.LocalOption[] getLocalOptions(CVSTag startTag, CVSTag endTag) {
		List<LocalOption> options = new ArrayList<>();
		if (startTag != null) {
			options.add(Command.makeArgumentOption(Update.JOIN, startTag.getName()));
		}
		options.add(Command.makeArgumentOption(Update.JOIN, endTag.getName()));
		return options.toArray(new Command.LocalOption[options.size()]);
	}

	private IWorkbenchPart getPart() {
		return part;
	}
}
