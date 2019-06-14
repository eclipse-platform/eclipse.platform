/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 161536 Warn user when committing resources with problem markers
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 177519 [Wizards] Adopt new IResource.findMaxProblemSeverity API
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 166333 [Wizards] Show diff in CVS commit dialog
 *     
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.IFileContentManager;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.CommitAction;
import org.eclipse.team.internal.ccvs.ui.mappings.AbstractCommitAction;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * A wizard to commit the resources whose synchronization state is given in form
 * of a set of <code>SyncInfo</code>.
 */
public class CommitWizard extends ResizableWizard {
	
	public static final String COMMIT_WIZARD_DIALOG_SETTINGS = "CommitWizard"; //$NON-NLS-1$
	
	/**
	 * An operation to add and commit resources to a CVS repository.
	 */
	public static class AddAndCommitOperation extends CVSOperation {
		
		private final IResource[] fAllResources;
		private final String fComment;
		
		private Map fModesForExtensionsForOneTime;
		private Map fModesForNamesForOneTime;
		
		private IResource[] fNewResources;
		private IJobChangeListener jobListener;
		
		public AddAndCommitOperation(IWorkbenchPart part, IResource[] allResources, IResource[] newResources, String comment) {
			super(part);
			fAllResources = allResources;
			fNewResources = newResources;
			fModesForExtensionsForOneTime = Collections.EMPTY_MAP;
			fModesForNamesForOneTime= Collections.EMPTY_MAP;
			fComment = comment;
		}
		
		public void setModesForExtensionsForOneTime(Map modes) {
			if (modes != null)
				fModesForExtensionsForOneTime= modes;
		}
		
		public void setModesForNamesForOneTime(Map modes) {
			if (modes != null)
				fModesForNamesForOneTime= modes;
		}
		
		@Override
		protected void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
			try {
				monitor.beginTask(null, (fNewResources.length + fAllResources.length) * 100);
				if (fNewResources.length > 0) {
					final AddOperation op= new AddOperation(getPart(), RepositoryProviderOperation.asResourceMappers(fNewResources));
					op.addModesForExtensions(fModesForExtensionsForOneTime);
					op.addModesForNames(fModesForNamesForOneTime);
					op.run(Policy.subMonitorFor(monitor, fNewResources.length * 100));
				}
				if (fAllResources.length > 0) {
					CommitOperation commitOperation = new CommitOperation(getPart(), RepositoryProviderOperation.asResourceMappers(fAllResources), new Command.LocalOption[0], fComment) {
						@Override
						public boolean consultModelsForMappings() {
							// Do not consult models from the commit wizard
							return false;
						}
					};
					commitOperation.run(Policy.subMonitorFor(monitor, fAllResources.length * 100));
				}
			} catch (InvocationTargetException e) {
				throw CVSException.wrapException(e);
			} finally {
				monitor.done();
			}
		}
		
		@Override
		protected String getJobName() {
			return CVSUIMessages.CommitWizard_0; 
		}
		
		@Override
		protected String getTaskName() {
			return CVSUIMessages.CommitWizard_1; 
		}

		/*
		 * Set the job listener. It will only recieve scheduled and done
		 * events as these are what are used by a sync model operation
		 * to show busy state in the sync view.
		 */
		protected void setJobChangeListener(IJobChangeListener jobListener) {
			this.jobListener = jobListener;
		}
		
		@Override
		public void done(IJobChangeEvent event) {
			super.done(event);
			if (jobListener != null)
				jobListener.done(event);
		}
		
		@Override
		public void scheduled(IJobChangeEvent event) {
			super.scheduled(event);
			if (jobListener != null)
				jobListener.scheduled(event);
		}
	}
	
	private final IResource[] fResources;
	private IResource[] fUnaddedDiffs;
	private final ModelSynchronizeParticipant fParticipant;
	
	private CommitWizardFileTypePage fFileTypePage;
	private CommitWizardCommitPage fCommitPage;
	private IJobChangeListener jobListener;
	private IWorkbenchPart part;
	
	public CommitWizard(SyncInfoSet infos) throws CVSException {
		this(infos.getResources());
	}
	
	public CommitWizard(final IResource [] resources) throws CVSException {
		
		super(COMMIT_WIZARD_DIALOG_SETTINGS, CVSUIPlugin.getPlugin().getDialogSettings());
		
		setNeedsProgressMonitor(true);
		setWindowTitle(CVSUIMessages.CommitWizard_2); 
		setDefaultPageImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION));
		
		fResources= resources;
		ResourceMapping[] mappings = Utils.getResourceMappings(resources);
		fParticipant = createWorkspaceParticipant(mappings, getShell());

		getAllOutOfSync();
		fUnaddedDiffs = getUnaddedResources(getDiffTree().getAffectedResources());
	}

	private ModelSynchronizeParticipant createWorkspaceParticipant(ResourceMapping[] selectedMappings, Shell shell) {
		ISynchronizationScopeManager manager = WorkspaceSubscriberContext.createWorkspaceScopeManager(selectedMappings, true, CommitAction.isIncludeChangeSets(shell, CVSUIMessages.SyncAction_1));
		return new CommitWizardParticipant(WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY), this);
	}

	public CommitWizard(SyncInfoSet infos, IJobChangeListener jobListener) throws CVSException {
		this(infos);
		this.jobListener = jobListener;
	}

	private void getAllOutOfSync() throws CVSException {
		try {
			ISynchronizationContext context = getParticipant().getContext();
			SubscriberDiffTreeEventHandler handler = Adapters.adapt(context, SubscriberDiffTreeEventHandler.class);
			handler.initializeIfNeeded();
			Job.getJobManager().join(context, null);
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
	}

	public boolean hasOutgoingChanges() {
		IResourceDiffTree tree = getDiffTree();
		return tree != null && tree.hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				return AbstractCommitAction.hasLocalChange(diff);
			}
		});
	}
	
	boolean hasConflicts() {
		IResourceDiffTree tree = getDiffTree();
		return tree != null && tree.hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				if (diff instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					return twd.getDirection() == IThreeWayDiff.CONFLICTING;
				}
				return false;
			}
		});
	}
	
	public int getHighestProblemSeverity() {
		IResource[] resources = getDiffTree().getAffectedResources();
		int mostSeriousSeverity = -1;
		
		for (IResource resource : resources) {
			try {
				int severity = resource.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
				if (severity > mostSeriousSeverity) {
					mostSeriousSeverity = severity;
				}
			} catch (CoreException e) {
			}
		}
		
		return mostSeriousSeverity;
	}

	IResourceDiffTree getDiffTree() {
		return fParticipant.getContext().getDiffTree();
	}

	public CommitWizardFileTypePage getFileTypePage() {
		return fFileTypePage;
	}

	public CommitWizardCommitPage getCommitPage() {
		return fCommitPage;
	}

	public ModelSynchronizeParticipant getParticipant() {
		return fParticipant;
	}

	@Override
	public boolean canFinish() {
		final IWizardPage current= getContainer().getCurrentPage();
		if (current == fFileTypePage && fCommitPage != null)
			return false;
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		
		final String comment= fCommitPage.getComment(getShell());
		if (comment == null)
			return false;
		
		IResource[] resources = AbstractCommitAction.getOutgoingChanges(getDiffTree(), fCommitPage.getTraversalsToCommit(), null);
		if (resources.length == 0)
			return true;
		
		final IResource[] unadded;
		try {
			unadded = getUnaddedResources(resources);
		} catch (CVSException e1) {
			return false;
		}
		
		final IResource[] files = getFiles(resources);
		
		final AddAndCommitOperation operation= new AddAndCommitOperation(getPart(), files, unadded, comment);
		if (jobListener != null)
			operation.setJobChangeListener(jobListener);
		
		if (fFileTypePage != null) {
			final Map extensionsToSave= new HashMap();
			final Map extensionsNotToSave= new HashMap();
			
			fFileTypePage.getModesForExtensions(extensionsToSave, extensionsNotToSave);
			CommitWizardFileTypePage.saveExtensionMappings(extensionsToSave);
			operation.setModesForExtensionsForOneTime(extensionsNotToSave);
			
			final Map namesToSave= new HashMap();
			final Map namesNotToSave= new HashMap();
			
			fFileTypePage.getModesForNames(namesToSave, namesNotToSave);
			CommitWizardFileTypePage.saveNameMappings(namesToSave);
			operation.setModesForNamesForOneTime(namesNotToSave);
		}
		
		try {
			operation.run();
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		
		fCommitPage.finish();
		return super.performFinish();
	}
	
	@Override
	public boolean performCancel() {
		fCommitPage.finish();
		return super.performCancel();
	}

	@Override
	public void addPages() {
		
		final Collection<String> names = new HashSet<>();
		final Collection<String> extensions = new HashSet<>();
		getUnknownNamesAndExtension(fUnaddedDiffs, names, extensions);
		
		if (names.size() + extensions.size() > 0) {
			fFileTypePage= new CommitWizardFileTypePage(extensions, names); 
			addPage(fFileTypePage);
		}
		
		fCommitPage= new CommitWizardCommitPage(fResources, this);
		addPage(fCommitPage);
		
		super.addPages();
	}

	@Override
	public void dispose() {
		fParticipant.dispose();
		super.dispose();
	}
	
	public static void run(IWorkbenchPart part, Shell shell, IResource [] resources) throws CVSException {
		try {
			CommitWizard commitWizard = new CommitWizard(resources);
			commitWizard.setPart(part);
			run(shell, commitWizard);
		} catch (OperationCanceledException e) {
			// Ignore
		}
	}
	
	private void setPart(IWorkbenchPart part) {
		this.part = part;
	}

	public static void run(Shell shell, SyncInfoSet infos, IJobChangeListener jobListener) throws CVSException {
		try {
			run(shell, new CommitWizard(infos, jobListener));
		} catch (OperationCanceledException e) {
			// Ignore
		}
	}
	
	public static void run(IWorkbenchPart part, Shell shell, final ResourceTraversal[] traversals) throws CVSException {
		try {
			final IResource [][] resources = new IResource[][] { null };
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					resources[0] = getDeepResourcesToCommit(traversals, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			run(part, shell, resources[0]);
		} catch (OperationCanceledException e) {
			// Ignore
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	private IWorkbenchPart getPart() {
		if (part != null)
			return part;
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
	}
	
	private static void run(Shell shell, CommitWizard wizard) {
		if (!wizard.hasOutgoingChanges()) {
			MessageDialog.openInformation(shell, CVSUIMessages.CommitWizard_6, CVSUIMessages.CommitWizard_7); // 
		} else {
			int highestProblemSeverity = wizard.getHighestProblemSeverity();
			IPreferenceStore preferenceStore = CVSUIPlugin.getPlugin().getPreferenceStore();
			switch (highestProblemSeverity) {
			case IMarker.SEVERITY_WARNING:
				String allowCommitsWithWarnings = preferenceStore.getString(ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS);
				if (MessageDialogWithToggle.PROMPT.equals(allowCommitsWithWarnings) || MessageDialogWithToggle.NEVER.equals(allowCommitsWithWarnings)) {
					MessageDialogWithToggle warningDialog = MessageDialogWithToggle.openYesNoQuestion(shell, CVSUIMessages.CommitWizard_8, CVSUIMessages.CommitWizard_9, CVSUIMessages.CommitWizard_10, false, preferenceStore, ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS);
					if (IDialogConstants.YES_ID != warningDialog.getReturnCode()) {
						return;
					}
				}
				break;
			case IMarker.SEVERITY_ERROR:
				String allowCommitsWithErrors = preferenceStore.getString(ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS);
				if (MessageDialogWithToggle.PROMPT.equals(allowCommitsWithErrors) || MessageDialogWithToggle.NEVER.equals(allowCommitsWithErrors)) {
					MessageDialogWithToggle errorDialog = MessageDialogWithToggle.openYesNoQuestion(shell, CVSUIMessages.CommitWizard_11, CVSUIMessages.CommitWizard_12, CVSUIMessages.CommitWizard_13, false, preferenceStore, ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS);
					if (IDialogConstants.YES_ID != errorDialog.getReturnCode()) {
						return;
					}
				}
				break;
			}
			open(shell, wizard);
		}
	}

	protected static int open(Shell shell, ResizableWizard wizard) {
		final WizardDialog dialog= new WizardDialog(shell, wizard);
		dialog.setPageSize(wizard.loadSize());
		return dialog.open();
	}
	
	private void getUnknownNamesAndExtension(IResource[] resources, Collection<String> names,
			Collection<String> extensions) {

		final IFileContentManager manager= Team.getFileContentManager();

		for (IResource local : resources) {
			if (local instanceof IFile && manager.getType((IFile)local) == Team.UNKNOWN) {
				final String extension= local.getFileExtension();
				if (extension != null && !manager.isKnownExtension(extension)) {
					extensions.add(extension);
				}

				final String name= local.getName();
				if (extension == null && name != null && !manager.isKnownFilename(name))
					names.add(name);
			}
		}
	}
	
	private IResource[] getUnaddedResources(IResource[] resources) throws CVSException {
		List/* <IResource> */<IResource> unadded = new ArrayList<>();
		for (IResource resource : resources) {
			if (!isAdded(resource)) {
				unadded.add(resource);
			}
		}
		return unadded.toArray(new IResource[0]);
	}

	private IResource[] getFiles(IResource[] resources) {
		final List<IResource> files = new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				files.add(resource);
			}
		}
		return files.toArray(new IResource[0]);
	}
	
	private static boolean isAdded(IResource resource) throws CVSException {
		final ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		if (cvsResource.isFolder()) {
			return ((ICVSFolder)cvsResource).isCVSFolder();
		}
		return cvsResource.isManaged();
	}

	private static IResource[] getDeepResourcesToCommit(ResourceTraversal[] traversals, IProgressMonitor monitor) throws CoreException {
		List<IResource> roots = new ArrayList<>();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
				roots.addAll(Arrays.asList(resources));
			} else if (traversal.getDepth() == IResource.DEPTH_ZERO) {
				collectShallowFiles(resources, roots);
			} else if (traversal.getDepth() == IResource.DEPTH_ONE) {
				collectShallowFiles(resources, roots);
				for (IResource resource : resources) {
					if (resource.getType() != IResource.FILE) {
						collectShallowFiles(members(resource), roots);
					}
				}
			}
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	private static IResource[] members(IResource resource) throws CoreException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().members(resource);
	}

	private static void collectShallowFiles(IResource[] resources, List<IResource> roots) {
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE)
				roots.add(resource);
		}
	}
}    

