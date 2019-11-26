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
 *     Sebastian Davids <sdavids@gmx.de> - bug 69926
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.*;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

public class WorkspaceSynchronizeParticipant extends ScopableSubscriberParticipant implements IChangeSetProvider, IPreferenceChangeListener {

	public static final String ID = "org.eclipse.team.cvs.ui.cvsworkspace-participant"; //$NON-NLS-1$

	/**
	 * The id of a workspace action group to which additions actions can 
	 * be added.
	 */
	public static final String TOOLBAR_CONTRIBUTION_GROUP = "toolbar_group_1"; //$NON-NLS-1$
	
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_1 = "context_group_1"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_2 = "context_group_2"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_3 = "context_group_3"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_4 = "context_group_4"; //$NON-NLS-1$

	private WorkspaceChangeSetCapability capability;

	/**
	 * CVS workspace action contribution
	 */
	public class WorkspaceActionContribution extends SynchronizePageActionGroup {
		private WorkspaceCommitAction commitToolbar;
		private WorkspaceUpdateAction updateToolbar;
		
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			
			updateToolbar = new WorkspaceUpdateAction(
					configuration, 
					getVisibleRootsSelectionProvider(), 
					"WorkspaceToolbarUpdateAction."); //$NON-NLS-1$
			updateToolbar.setPromptBeforeUpdate(true);
			appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU,
					TOOLBAR_CONTRIBUTION_GROUP,
					updateToolbar);
			
			commitToolbar = new WorkspaceCommitAction(
					configuration, 
					getVisibleRootsSelectionProvider(), 
					"WorkspaceToolbarCommitAction."); //$NON-NLS-1$
			appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU,
					TOOLBAR_CONTRIBUTION_GROUP,
					commitToolbar);
			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new WorkspaceUpdateAction(configuration));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new WorkspaceCommitAction(configuration));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_2,
					new OverrideAndUpdateAction(configuration));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_2,
					new OverrideAndCommitAction(configuration));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_2,
					new ConfirmMergedAction(configuration));		
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_3,
					new CVSActionDelegateWrapper(new IgnoreAction(), configuration));
			if (!configuration.getSite().isModal()) {
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CreatePatchAction(configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new ApplyPatchAction(configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CVSActionDelegateWrapper(new BranchAction(), configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CVSActionDelegateWrapper(new ShowAnnotationAction(), configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CVSActionDelegateWrapper(new ShowResourceInHistoryAction(), configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CVSActionDelegateWrapper(new SetKeywordSubstitutionAction(), configuration));	
			}
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_4,
					new RefreshDirtyStateAction(configuration));
		}
	}
	
	public class WorkspaceChangeSetCapability extends CVSChangeSetCapability {
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
	}
	
	/**
	 * No-arg constructor used for
	 * creation of persisted participant after startup
	 */
	public WorkspaceSynchronizeParticipant() {
	}
	
	public WorkspaceSynchronizeParticipant(ISynchronizeScope scope) {
		super(scope);
		setSubscriber(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
		SyncInfoFilter filter = createSyncInfoFilter();
		if (filter != null) {
			setSyncInfoFilter(filter);
		}
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).addPreferenceChangeListener(this); //$NON-NLS-1$
	}

	private boolean isConsiderContents() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS);
	}

	private SyncInfoFilter contentComparison = new SyncInfoFilter() {
		private SyncInfoFilter contentCompare = new SyncInfoFilter.ContentComparisonSyncInfoFilter();
		@Override
		public boolean select(SyncInfo info, IProgressMonitor monitor) {
			IResource local = info.getLocal();
			// don't select folders
			if (local.getType() != IResource.FILE) return false;
			// Want to select infos whose contents do not match
			return !contentCompare.select(info, monitor);
		}
	};

	private SyncInfoFilter createSyncInfoFilter() {
		final SyncInfoFilter regexFilter = createRegexFilter();
		if (isConsiderContents() && regexFilter != null) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return contentComparison.select(info, monitor)
							&& !regexFilter.select(info, monitor);
				}
			};
		} else if (isConsiderContents()) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return contentComparison.select(info, monitor);
				}
			};
		} else if (regexFilter != null) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					// want to select infos which contain at least one unmatched difference
					return !regexFilter.select(info, monitor);
				}
			};
		}
		return null;
	}

	private SyncInfoFilter createRegexFilter() {
		if (isConsiderContents()) {
			String pattern = CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN);
			if (pattern != null && !pattern.isEmpty()) {
				return new RegexSyncInfoFilter(pattern);
			}
		}
		return null;
	}

	@Override
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		setSubscriber(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
	}
	
	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		ILabelDecorator labelDecorator = getLabelDecorator(configuration);
		configuration.addLabelDecorator(labelDecorator);
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, TOOLBAR_CONTRIBUTION_GROUP);
		configuration.addActionContribution(new WorkspaceActionContribution());
		configuration.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
		configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);
		
		// Add context menu groups here to give the client displaying the
		// page a chance to remove the context menu
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_1);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_2);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_3);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_4);
	}
	
	protected  ILabelDecorator getLabelDecorator(ISynchronizePageConfiguration configuration) {
		return new CVSParticipantLabelDecorator(configuration);
	}
	
	@Override
	protected ISynchronizeParticipantDescriptor getDescriptor() {
		return TeamUI.getSynchronizeManager().getParticipantDescriptor(ID);
	}
	
	@Override
	public void prepareCompareInput(ISynchronizeModelElement element, CompareConfiguration config, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100);
		CVSParticipant.deriveBaseContentsFromLocal(element, Policy.subMonitorFor(monitor, 10));
		super.prepareCompareInput(element, config, Policy.subMonitorFor(monitor, 80));
		CVSParticipant.updateLabelsForCVS(element, config, Policy.subMonitorFor(monitor, 10));
		monitor.done();
	}
	
	@Override
	public PreferencePage[] getPreferencePages() {
		return CVSParticipant.addCVSPreferencePages(super.getPreferencePages());
	}
	
	@Override
	public ChangeSetCapability getChangeSetCapability() {
		if (capability == null) {
			capability = new WorkspaceChangeSetCapability();
		}
		return capability;
	}
	
	@Override
	protected boolean isViewerContributionsSupported() {
		return true;
	}

	public void refresh(IResource[] resources, IWorkbenchPartSite site) {
		refresh(resources, getShortTaskName(), getLongTaskName(resources), site);
	}

	@Override
	public void dispose() {
		super.dispose();
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).removePreferenceChangeListener(this); //$NON-NLS-1$
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(ICVSUIConstants.PREF_CONSIDER_CONTENTS) || event.getKey().equals(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN)) {
			SyncInfoFilter filter = createSyncInfoFilter();
			if (filter != null) {
				setSyncInfoFilter(filter);
			} else {
				setSyncInfoFilter(new FastSyncInfoFilter());
			}
		}
	}
}
