/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.patch.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.wizards.PatchInaccessibleProjectsPage;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbench;

public class ApplyPatchSynchronizationWizard extends PatchWizard implements
		IConfigurationWizard {

	private PatchInaccessibleProjectsPage fPatchInaccessibleProjectsPage;

	public ApplyPatchSynchronizationWizard() {
		this(null, null, new CompareConfiguration());
	}

	public ApplyPatchSynchronizationWizard(IStorage patch, IResource target,
			CompareConfiguration configuration) {
		super(patch, target, configuration);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		if (fPatchInaccessibleProjectsPage != null) {
			IProject[] projects = fPatchInaccessibleProjectsPage
					.getSelectedProjects();
			if (projects != null && projects.length != 0)
				openSelectedProjects(projects);
		}

		ApplyPatchSubscriber subscriber = new ApplyPatchSubscriber(getPatcher());

		// Get ResourceMappings for root resources from the patch.
		ResourceMapping[] inputMappings = Utils.getResourceMappings(subscriber
				.roots());

		// Take the given mappings, consult logical models and construct the
		// complete set of resources to be operated on.
		// Use SubscriberResourceMappingContext which uses subscriber to access
		// to the remote state of local resources.
		SubscriberScopeManager scopeManager = new SubscriberScopeManager(
				subscriber.getName(), inputMappings, subscriber, true);

		// Initialize the diff tree.
		// TODO: are we going to perform head-less auto-merges? do we need to
		// subclass MergeContext?
		SubscriberMergeContext mergeContext = ApplyPatchSubscriberMergeContext
				.createContext(subscriber, scopeManager);

		// Create the participant and show it.
		ModelSynchronizeParticipant participant = new ApplyPatchModelSynchronizeParticipant(
				mergeContext);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(
				new ISynchronizeParticipant[] { participant });
		// We don't know in which site to show progress because a participant
		// could actually be shown in multiple sites.
		participant.run(null /* no site */);
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IProject project) {
		// nothing to do here
	}

	@Override
	public void addPages() {
		if (getPatch() == null)
			addPage(fPatchWizardPage = new InputPatchPage(this));
		if (getPatch() == null || !getPatcher().isWorkspacePatch())
			addPage(fPatchTargetPage = new PatchTargetPage(getPatcher()) {
				@Override
				public IWizardPage getNextPage() {
					IWizardPage nextPage = super.getNextPage();
					if (!isTargetingInaccessibleProjects() && nextPage != this)
						return nextPage.getNextPage();
					return nextPage;
				}
			});
		if (getPatch() == null || isTargetingInaccessibleProjects())
			addPage(fPatchInaccessibleProjectsPage = new PatchInaccessibleProjectsPage(
					getPatcher()));
		addPage(new PatchParsedPage());
	}

	public boolean isComplete() {
		if (getPatch() == null || !getPatcher().isWorkspacePatch()
				|| isTargetingInaccessibleProjects())
			return false;
		return true;
	}

	private boolean isTargetingInaccessibleProjects() {
		DiffProject[] diffProjects = getPatcher().getDiffProjects();
		if (diffProjects != null) {
			for (int i = 0; i < diffProjects.length; i++) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(diffProjects[i].getName());
				if (!project.isAccessible())
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage.getName().equals(
				PatchInaccessibleProjectsPage.PATCH_INACCESSIBLE_PROJECTS_NAME)) {
			return currentPage.isPageComplete();
		}
		if (currentPage.getName()
				.equals(PatchParsedPage.PATCH_PARSED_PAGE_NAME)) {
			return currentPage.isPageComplete();
		}
		return super.canFinish();
	}

	@Override
	public WorkspacePatcher getPatcher() {
		// make the patcher available to other classes in the package
		return super.getPatcher();
	}

	private void openSelectedProjects(final IProject projects[]) {
		Job openProjectsJob = new Job(
				TeamUIMessages.PatchInaccessibleProjectsPage_openingProjects) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(
						TeamUIMessages.PatchInaccessibleProjectsPage_openingProjects,
						projects.length);
				MultiStatus errorStatus = new MultiStatus(
						TeamUIPlugin.ID,
						IStatus.ERROR,
						TeamUIMessages.PatchInaccessibleProjectsPage_openingProjects,
						null);
				for (int i = 0; i < projects.length; i++) {
					IProject project = projects[i];
					try {
						project.open(new SubProgressMonitor(monitor, 1));
					} catch (CoreException e) {
						errorStatus.add(e.getStatus());
					}
				}
				monitor.done();
				return errorStatus;
			}
		};
		openProjectsJob.setUser(true);
		openProjectsJob.schedule();
	}

}
