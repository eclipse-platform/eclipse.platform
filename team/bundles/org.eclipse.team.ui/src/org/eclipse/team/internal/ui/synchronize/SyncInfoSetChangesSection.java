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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * Section shown in a participant page to show the changes for this participant. This
 * includes a diff viewer for browsing the changes.
 *
 * @since 3.0
 */
public class SyncInfoSetChangesSection extends ForwardingChangesSection {

	/**
	 * Boolean that indicates whether the error page is being shown.
	 * This is used to avoid redrawing the error page when new events come in
	 */
	private boolean showingError;

	/**
	 * Register an action contribution in order to receive model
	 * change notification so that we can update message to user and totals.
	 */
	private SynchronizePageActionGroup changedListener = new SynchronizePageActionGroup() {
		@Override
		public void modelChanged(ISynchronizeModelElement root) {
			calculateDescription();
		}
	};

	/**
	 * Listener registered with the subscriber sync info set which contains
	 * all out-of-sync resources for the subscriber.
	 */
	private ISyncInfoSetChangeListener subscriberListener = new ISyncInfoSetChangeListener() {
		@Override
		public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
			// Handled by output set listener
		}
		@Override
		public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
			calculateDescription();
		}
		@Override
		public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
			// Handled by output set listener
		}
	};

	/**
	 * Listener registered with the output sync info set which contains
	 * only the visible sync info.
	 */
	private ISyncInfoSetChangeListener outputSetListener = new ISyncInfoSetChangeListener() {
		@Override
		public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
			calculateDescription();
		}
		@Override
		public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
			// Input changed listener will call calculateDescription()
			// The input will then react to output set changes
		}
		@Override
		public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
			calculateDescription();
		}
	};

	/**
	 * Create a changes section on the following page.
	 *
	 * @param parent the parent control
	 * @param page the page showing this section
	 * @param configuration the configuration for the synchronize page
	 */
	public SyncInfoSetChangesSection(Composite parent, AbstractSynchronizePage page, ISynchronizePageConfiguration configuration) {
		super(parent, page, configuration);
	}

	@Override
	protected void initializeChangesViewer() {
		super.initializeChangesViewer();
		getConfiguration().addActionContribution(changedListener);
		getParticipantSyncInfoSet().addSyncSetChangedListener(subscriberListener);
		getVisibleSyncInfoSet().addSyncSetChangedListener(outputSetListener);
	}

	@Override
	protected void calculateDescription() {
		SyncInfoTree syncInfoTree = getVisibleSyncInfoSet();
		if (syncInfoTree.getErrors().length > 0) {
			if (!showingError) {
				TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
					updatePage(getErrorComposite(getContainer()));
					showingError = true;
				});
			}
			return;
		}

		showingError = false;
		super.calculateDescription();
	}

	@Override
	protected long getChangesInMode(int candidateMode) {
		SyncInfoSet participantSet = getParticipantSyncInfoSet();
		long numChanges;
		switch (candidateMode) {
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			numChanges = participantSet.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
			break;
		case ISynchronizePageConfiguration.INCOMING_MODE:
			numChanges = participantSet.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
			break;
		case ISynchronizePageConfiguration.BOTH_MODE:
			numChanges = participantSet.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK)
				+ participantSet.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
			break;
		default:
			numChanges = 0;
			break;
		}
		return numChanges;
	}

	/*
	 * Return the candidate mode based on the presence of unfiltered changes
	 * and the modes supported by the page.
	 */
	@Override
	protected int getCandidateMode() {
		SyncInfoSet participantSet = getParticipantSyncInfoSet();
		SynchronizePageConfiguration configuration = (SynchronizePageConfiguration)getConfiguration();
		long outgoingChanges = participantSet.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		if (outgoingChanges > 0) {
			if (configuration.isModeSupported(ISynchronizePageConfiguration.OUTGOING_MODE)) {
				return ISynchronizePageConfiguration.OUTGOING_MODE;
			}
			if (configuration.isModeSupported(ISynchronizePageConfiguration.BOTH_MODE)) {
				return ISynchronizePageConfiguration.BOTH_MODE;
			}
		}
		long incomingChanges = participantSet.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		if (incomingChanges > 0) {
			if (configuration.isModeSupported(ISynchronizePageConfiguration.INCOMING_MODE)) {
				return ISynchronizePageConfiguration.INCOMING_MODE;
			}
			if (configuration.isModeSupported(ISynchronizePageConfiguration.BOTH_MODE)) {
				return ISynchronizePageConfiguration.BOTH_MODE;
			}
		}
		return configuration.getMode();
	}

	@Override
	public void dispose() {
		super.dispose();
		getConfiguration().removeActionContribution(changedListener);
		getParticipantSyncInfoSet().removeSyncSetChangedListener(subscriberListener);
		getVisibleSyncInfoSet().removeSyncSetChangedListener(outputSetListener);
	}

	private Composite getErrorComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getListBackgroundColor());
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		Hyperlink link = new Hyperlink(composite, SWT.WRAP);
		link.setText(TeamUIMessages.ChangesSection_8);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showErrors();
			}
		});
		link.setBackground(getListBackgroundColor());
		link.setUnderlined(true);

		link = new Hyperlink(composite, SWT.WRAP);
		link.setText(TeamUIMessages.ChangesSection_9);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				getPage().reset();
			}
		});
		link.setBackground(getListBackgroundColor());
		link.setUnderlined(true);

		createDescriptionLabel(composite, NLS.bind(TeamUIMessages.ChangesSection_10, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, getConfiguration().getParticipant().getName()) }));

		return composite;
	}

	/* private */ void showErrors() {
		ITeamStatus[] status = getVisibleSyncInfoSet().getErrors();
		String title = TeamUIMessages.ChangesSection_11;
		if (status.length == 1) {
			ErrorDialog.openError(getShell(), title, status[0].getMessage(), status[0]);
		} else {
			MultiStatus multi = new MultiStatus(TeamUIPlugin.ID, 0, status, TeamUIMessages.ChangesSection_12, null);
			ErrorDialog.openError(getShell(), title, null, multi);
		}
	}

	@Override
	protected int getChangesCount() {
		return getParticipantSyncInfoSet().size();
	}

	@Override
	protected long getVisibleChangesCount() {
		return getVisibleSyncInfoSet().size();
	}

	/*
	 * Return the sync info set that contains the visible resources
	 */
	private SyncInfoTree getVisibleSyncInfoSet() {
		return (SyncInfoTree)getConfiguration().getProperty(ISynchronizePageConfiguration.P_SYNC_INFO_SET);
	}

	/*
	 * Return the sync info set for the participant that contains all the resources
	 * including those that may not be visible due to filters (e.g. mode)
	 */
	private SyncInfoSet getParticipantSyncInfoSet() {
		return (SyncInfoSet)getConfiguration().getProperty(SynchronizePageConfiguration.P_WORKING_SET_SYNC_INFO_SET);
	}
}
