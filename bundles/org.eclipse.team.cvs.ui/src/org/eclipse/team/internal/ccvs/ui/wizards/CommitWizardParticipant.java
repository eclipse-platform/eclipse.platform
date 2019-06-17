/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 166333 [Wizards] Show diff in CVS commit dialog
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ccvs.ui.CVSDecoration;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceChangeSetCapability;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceModelParticipant;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantLabelDecorator;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.*;

/**
 * A participant that uses our decorator instead of the standard one.
 */
public class CommitWizardParticipant extends WorkspaceModelParticipant {
	
	/**
	 * The actions to be displayed in the context menu.
	 */
	private class ActionContribution extends SynchronizePageActionGroup {
		@Override
		public void modelChanged(final ISynchronizeModelElement root) {
			super.modelChanged(root);
			Display.getDefault().asyncExec(() -> {
				final CommitWizardCommitPage page = fWizard.getCommitPage();
				if (page != null)
					page.updateForModelChange();
			});
		}
	}
	
	private WorkspaceChangeSetCapability capability;
	
	@Override
	public ChangeSetCapability getChangeSetCapability() {
		if (capability == null) {
			capability = new WorkspaceChangeSetCapability() {
				@Override
				public boolean supportsCheckedInChangeSets() {
					return false;
				}
				@Override
				public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
					return false;
				}
			};
		}
		return capability;
	}

	/**
	 * An extension of the standard label decorator which configures the keyword substitution 
	 * mode according to the settings on the file type wizard page.
	 */
	private static class Decorator extends CVSParticipantLabelDecorator {
		
		private final CommitWizard fWizard;

		public Decorator(ISynchronizePageConfiguration configuration, CommitWizard wizard) {
			super(configuration);
			fWizard= wizard;
		}
		
		@Override
		protected CVSDecoration getDecoration(IResource resource) throws CoreException {
			final CVSDecoration decoration= super.getDecoration(resource);
			final CommitWizardFileTypePage page= fWizard.getFileTypePage();
			
			if (page != null && resource instanceof IFile) 
				decoration.setKeywordSubstitution(page.getOption((IFile)resource).getShortDisplayText());
			return decoration;
		}
	}

	protected static final String ACTION_GROUP = "org.eclipse.tam.cvs.ui.CommitActions"; //$NON-NLS-1$
	
	final CommitWizard fWizard;
	protected Action showComparePaneAction;
	
	public CommitWizardParticipant(SynchronizationContext context, CommitWizard wizard) {
		super(context);
		fWizard= wizard;
	}
	
	@Override
	protected ILabelDecorator getLabelDecorator(ISynchronizePageConfiguration configuration) {
		return new Decorator(configuration, fWizard);
	}

	@Override
	protected void initializeConfiguration(final ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.setProperty(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] {ACTION_GROUP, ISynchronizePageConfiguration.NAVIGATE_GROUP});
		configuration.setProperty(ISynchronizePageConfiguration.P_CONTEXT_MENU, ISynchronizePageConfiguration.DEFAULT_CONTEXT_MENU);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_3);
		configuration.addActionContribution(new ActionContribution());

		// Wrap the container so that we can update the enablements after the runnable
		// (i.e. the container resets the state to what it was at the beginning of the
		// run even if the state of the page changed. Remove from View changes the state)
		configuration.setRunnableContext((fork, cancelable, runnable) -> {
			fWizard.getContainer().run(fork, cancelable, runnable);
			final CommitWizardCommitPage page = fWizard.getCommitPage();
			if (page != null)
				page.updateEnablements();
		});
		configuration.setSupportedModes(ISynchronizePageConfiguration.OUTGOING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.OUTGOING_MODE);
		configuration.addActionContribution(new SynchronizePageActionGroup() {
			@Override
			public void initialize(ISynchronizePageConfiguration configuration) {
				super.initialize(configuration);
				showComparePaneAction = new Action(null, Action.AS_CHECK_BOX) {
					@Override
					public void run() {
						fWizard.getCommitPage().showComparePane(this.isChecked());
					}
				};
				Utils.initAction(showComparePaneAction, "ComnitWizardComparePaneToggle.", Policy.getActionBundle()); //$NON-NLS-1$
				showComparePaneAction.setChecked(isComparePaneVisible());
				appendToGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, ACTION_GROUP, showComparePaneAction);
			}
		});
		configuration.setProperty(SynchronizePageConfiguration.P_OPEN_ACTION, new Action() {
			@Override
			public void run() {
				ISelection selection = configuration.getSite().getSelectionProvider().getSelection();
				if(selection instanceof IStructuredSelection) {
					final Object obj = ((IStructuredSelection) selection).getFirstElement();
					if (fWizard.getParticipant().hasCompareInputFor(obj)) {
						fWizard.getCommitPage().showComparePane(true);
						showComparePaneAction.setChecked(true);
					}
				}
			}
		});
	}
	
	@Override
	public boolean doesSupportSynchronize() {
		return false;
	}
	
	private boolean isComparePaneVisible() {
		IDialogSettings section = fWizard.getDialogSettings().getSection(CommitWizard.COMMIT_WIZARD_DIALOG_SETTINGS);
		return section == null ? false : section.getBoolean(CommitWizardCommitPage.SHOW_COMPARE);
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new WorkspaceMergeActionGroup() {
			@Override
			protected void addToContextMenu(String mergeActionId, Action action, IMenuManager manager) {
				if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID
						|| mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID
						|| mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
					// skip merge actions
					return;
				}
				super.addToContextMenu(mergeActionId, action, manager);
			}

			@Override
			protected void appendToGroup(String menuId, String groupId,	IAction action) {
				if (menuId == ISynchronizePageConfiguration.P_CONTEXT_MENU
						&& groupId == WorkspaceModelParticipant.CONTEXT_MENU_COMMIT_GROUP_1) {
					// skip commit action
					return;
				}
				super.appendToGroup(menuId, groupId, action);
			}
		};
	}
}
