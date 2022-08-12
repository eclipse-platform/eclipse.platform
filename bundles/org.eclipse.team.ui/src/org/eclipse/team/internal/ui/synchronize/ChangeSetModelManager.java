/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * Manager for hierarchical models
 */
public class ChangeSetModelManager extends HierarchicalModelManager implements IPropertyChangeListener {

	private static final String P_COMMIT_SET_ENABLED = TeamUIPlugin.ID + ".P_COMMIT_SET_ENABLED"; //$NON-NLS-1$

	public static final String CHANGE_SET_GROUP = "ChangeSet"; //$NON-NLS-1$

	boolean enabled = false;

	private class ToggleCommitSetAction extends Action {
		public ToggleCommitSetAction() {
			super(TeamUIMessages.ChangeLogModelManager_0, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_CHANGE_SET));
			setToolTipText(TeamUIMessages.ChangeLogModelManager_0);
			update();
		}
		@Override
		public void run() {
			setCommitSetsEnabled(!enabled);
			update();
		}
		private void update() {
			setChecked(enabled);
		}
	}

	private ToggleCommitSetAction toggleCommitSetAction;

	private class CommitSetActionContribution extends SynchronizePageActionGroup {

		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);

			toggleCommitSetAction = new ToggleCommitSetAction();
			appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU,
					CHANGE_SET_GROUP,
					toggleCommitSetAction);
			updateEnablement();
		}
	}

	public ChangeSetModelManager(ISynchronizePageConfiguration configuration) {
		super(configuration);
		configuration.addPropertyChangeListener(this);
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, CHANGE_SET_GROUP);
		configuration.addActionContribution(new CommitSetActionContribution());
		ChangeSetCapability changeSetCapability = getChangeSetCapability(configuration);
		if (changeSetCapability != null && changeSetCapability.supportsActiveChangeSets()) {
			configuration.addLabelDecorator(new ChangeSetLabelDecorator(configuration));
		}
		configuration.addPropertyChangeListener(event -> {
			if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODE)) {
				updateEnablement();
			}
		});
	}

	private ChangeSetCapability getChangeSetCapability(ISynchronizePageConfiguration configuration) {
		ISynchronizeParticipant participant = configuration.getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			return provider.getChangeSetCapability();
		}
		return null;
	}

	private void updateEnablement() {
		if (toggleCommitSetAction != null) {
			ISynchronizePageConfiguration configuration = getConfiguration();
			ChangeSetCapability changeSetCapability = getChangeSetCapability(configuration);
			boolean enabled = changeSetCapability != null && (changeSetCapability.enableActiveChangeSetsFor(configuration)
				|| changeSetCapability.enableCheckedInChangeSetsFor(configuration));
			toggleCommitSetAction.setEnabled(enabled);
		}

	}

	@Override
	public void dispose() {
		getConfiguration().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	protected ISynchronizeModelProvider createModelProvider(String id) {
		if (enabled) {
			return new ChangeSetModelProvider(getConfiguration(), getSyncInfoSet(), id);
		} else {
			return super.createModelProvider(id);
		}
	}

	@Override
	protected String getSelectedProviderId() {
		String id = super.getSelectedProviderId();
		if (id.equals(ChangeSetModelProvider.ChangeSetModelProviderDescriptor.ID)) {
			return ((ChangeSetModelProvider)getActiveModelProvider()).getSubproviderId();
		} else {
			return id;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
	}

	@Override
	protected void saveProviderSettings(String id) {
		super.saveProviderSettings(id);
		IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
		if(pageSettings != null) {
			pageSettings.put(P_COMMIT_SET_ENABLED, enabled);
		}
	}

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		// Load our setting before invoking super since the inherited
		// initialize will create the provider
		IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
		ChangeSetCapability changeSetCapability = getChangeSetCapability(getConfiguration());
		enabled = changeSetCapability != null && changeSetCapability.enableChangeSetsByDefault();
		if(pageSettings != null && pageSettings.get(P_COMMIT_SET_ENABLED) != null) {
			enabled = pageSettings.getBoolean(P_COMMIT_SET_ENABLED);
		}
		super.initialize(configuration);
	}

	/*
	 * This method is public so it can be invoked from test cases
	 */
	public void setCommitSetsEnabled(boolean enable) {
		if (this.enabled != enable) {
			this.enabled = enable;
			setInput(getSelectedProviderId(), null);
		}
	}

	@Override
	public ISynchronizeModelProvider getActiveModelProvider() {
		return super.getActiveModelProvider();
	}
}
