/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.*;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext.ChangeSetSubscriberScopeManager;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSActionDelegateWrapper;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.MergeAllActionHandler;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

public class WorkspaceModelParticipant extends
	CVSModelSynchronizeParticipant implements IChangeSetProvider {

	public static final String VIEWER_ID = "org.eclipse.team.cvs.ui.workspaceSynchronization"; //$NON-NLS-1$
	
	public static final String CONTEXT_MENU_UPDATE_GROUP_1 = "update"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_COMMIT_GROUP_1 = "commit"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_2 = "overrideActions"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_3 = "otherActions1"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_4 = "otherActions2"; //$NON-NLS-1$

	public static final String ID = "org.eclipse.team.cvs.ui.workspace-participant"; //$NON-NLS-1$
	
	private static final String CTX_CONSULT_CHANGE_SETS = "consultChangeSets"; //$NON-NLS-1$
	
	/**
	 * CVS workspace action contribution
	 */
	public class WorkspaceMergeActionGroup extends ModelSynchronizeParticipantActionGroup {
		private WorkspaceCommitAction commitToolbar;
		
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			configuration.setProperty(MERGE_ALL_ACTION_ID, new MergeAllActionHandler(configuration) {
				@Override
				protected String getJobName() {
					String name = getConfiguration().getParticipant().getName();
					return NLS.bind(CVSUIMessages.WorkspaceModelParticipant_0, Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, name));
				}
				
				@Override
				protected boolean promptToUpdate() {
					final IResourceDiffTree tree = getMergeContext().getDiffTree();
					if (tree.isEmpty()) {
						return false;
					}
					final long count = tree.countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) + tree.countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK);
					if (count == 0)
						return false;
					final boolean[] result = new boolean[] {true};
					TeamUIPlugin.getStandardDisplay().syncExec(() -> {
						String sizeString = Long.toString(count);
						String message = tree.size() > 1
								? NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateSeveral,
										new String[] { sizeString })
								: NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateOne, new String[] { sizeString }); //
						result[0] = MessageDialog.openQuestion(getConfiguration().getSite().getShell(),
								NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateTitle, new String[] { sizeString }),
								message);

					});
					return result[0];
				}
				private IMergeContext getMergeContext() {
					return ((IMergeContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT));
				}
				@Override
				protected boolean needsToSaveDirtyEditors() {
					int option = CVSUIPlugin.getPlugin().getPreferenceStore().getInt(ICVSUIConstants.PREF_SAVE_DIRTY_EDITORS);
					return option != ICVSUIConstants.OPTION_NEVER;
				}
				@Override
				protected boolean confirmSaveOfDirtyEditor() {
					int option = CVSUIPlugin.getPlugin().getPreferenceStore().getInt(ICVSUIConstants.PREF_SAVE_DIRTY_EDITORS);
					return option == ICVSUIConstants.OPTION_PROMPT;
				}
			});
			super.initialize(configuration);
			
			int modes = configuration.getSupportedModes();
			if ((modes & (ISynchronizePageConfiguration.OUTGOING_MODE | ISynchronizePageConfiguration.BOTH_MODE)) != 0) {	
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_COMMIT_GROUP_1,
						new CommitAction(configuration));
				
				commitToolbar = new WorkspaceCommitAction(configuration);
				appendToGroup(
						ISynchronizePageConfiguration.P_TOOLBAR_MENU,
						MERGE_ACTION_GROUP,
						commitToolbar);
				// TODO: let's leave off override and commit for now
//				appendToGroup(
//					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
//					CONTEXT_MENU_CONTRIBUTION_GROUP_2,
//					new OverrideAndCommitAction(configuration));
				
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						CONTEXT_MENU_CONTRIBUTION_GROUP_3,
						new CVSActionDelegateWrapper(new IgnoreAction(), configuration));
			}
			
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
		}
		
		@Override
		protected void configureMergeAction(String mergeActionId, Action action) {
			if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
				Utils.initAction(action, "WorkspaceUpdateAction.", Policy.getActionBundle()); //$NON-NLS-1$
				action.setId(ICVSUIConstants.CMD_UPDATE);
				action.setActionDefinitionId(ICVSUIConstants.CMD_UPDATE);
			} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
				Utils.initAction(action, "OverrideAndUpdateAction.", Policy.getActionBundle()); //$NON-NLS-1$
			} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
				Utils.initAction(action, "ConfirmMergedAction.", Policy.getActionBundle()); //$NON-NLS-1$
			} else if (mergeActionId == MERGE_ALL_ACTION_ID) {
				Utils.initAction(action, "WorkspaceToolbarUpdateAction.", Policy.getActionBundle()); //$NON-NLS-1$
				action.setId(ICVSUIConstants.CMD_UPDATE_ALL);
				action.setActionDefinitionId(ICVSUIConstants.CMD_UPDATE_ALL);
			} else {
				super.configureMergeAction(mergeActionId, action);
			}
		}
		
		@Override
		protected void addToContextMenu(String mergeActionId, Action action, IMenuManager manager) {
			IContributionItem group = null;;
			if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
				group = manager.find(CONTEXT_MENU_UPDATE_GROUP_1);
			} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
				group = manager.find(CONTEXT_MENU_CONTRIBUTION_GROUP_2);
			} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
				group = manager.find(CONTEXT_MENU_CONTRIBUTION_GROUP_2);
			} else {
				super.addToContextMenu(mergeActionId, action, manager);
				return;
			}
			if (group != null) {
				manager.appendToGroup(group.getId(), action);
			} else {
				manager.add(action);
			}
		}
	}

	private WorkspaceChangeSetCapability capability;

	private boolean isConsultChangeSets;
	
	public WorkspaceModelParticipant() {
	}
	
	public WorkspaceModelParticipant(SynchronizationContext context) {
		super(context);
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor("org.eclipse.team.cvs.ui.workspace-participant")); //$NON-NLS-1$
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
		isConsultChangeSets = isConsultChangeSets(context.getScopeManager());
	}
	
	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID, VIEWER_ID);
		super.initializeConfiguration(configuration);
	}
	
	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new WorkspaceMergeActionGroup();
	}
	
	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) {
		return WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY);
	}
	
	@Override
	protected ISynchronizationScopeManager createScopeManager(ResourceMapping[] mappings) {
		return WorkspaceSubscriberContext.createWorkspaceScopeManager(mappings, true, isConsultChangeSets);
	}
	
	@Override
	public ChangeSetCapability getChangeSetCapability() {
		if (capability == null) {
			capability = new WorkspaceChangeSetCapability();
		}
		return capability;
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString(CTX_CONSULT_CHANGE_SETS, Boolean.toString(isConsultChangeSets));
	}
	
	@Override
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		try {
			String consult = memento.getString(CTX_CONSULT_CHANGE_SETS);
			if (consult != null)
				isConsultChangeSets = Boolean.valueOf(consult).booleanValue();
		} finally {
			super.init(secondaryId, memento);
		}
	}

	private boolean isConsultChangeSets(ISynchronizationScopeManager manager) {
		if (manager instanceof ChangeSetSubscriberScopeManager) {
			ChangeSetSubscriberScopeManager man = (ChangeSetSubscriberScopeManager) manager;
			return man.isConsultSets();
		}
		return false;
	}
	
}
