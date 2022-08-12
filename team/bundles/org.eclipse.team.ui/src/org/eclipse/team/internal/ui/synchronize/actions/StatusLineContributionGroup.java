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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

public abstract class StatusLineContributionGroup extends ActionGroup {

	private static final String INCOMING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.incoming"; //$NON-NLS-1$
	private static final String OUTGOING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.outgoing"; //$NON-NLS-1$
	private static final String CONFLICTING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.conflicting"; //$NON-NLS-1$
	private static final String TOTALS_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.totals"; //$NON-NLS-1$
	private final static int TEXT_FIELD_MAX_SIZE = 25;

	private StatusLineCLabelContribution incoming;
	private StatusLineCLabelContribution outgoing;
	private StatusLineCLabelContribution conflicting;
	private StatusLineCLabelContribution totalChanges;

	private Image incomingImage = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DLG_SYNC_INCOMING).createImage();
	private Image outgoingImage = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DLG_SYNC_OUTGOING).createImage();
	private Image conflictingImage = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DLG_SYNC_CONFLICTING).createImage();

	private ISynchronizePageConfiguration configuration;

	public StatusLineContributionGroup(final Shell shell, ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		if (isThreeWay()) {
			this.incoming = createStatusLineContribution(INCOMING_ID, ISynchronizePageConfiguration.INCOMING_MODE, "0", incomingImage); //$NON-NLS-1$
			this.outgoing = createStatusLineContribution(OUTGOING_ID, ISynchronizePageConfiguration.OUTGOING_MODE, "0", outgoingImage); //$NON-NLS-1$
			this.conflicting = createStatusLineContribution(CONFLICTING_ID, ISynchronizePageConfiguration.CONFLICTING_MODE, "0", conflictingImage); //$NON-NLS-1$
		} else {
			this.totalChanges = new StatusLineCLabelContribution(TOTALS_ID, TEXT_FIELD_MAX_SIZE);
		}
		updateCounts();
	}

	private boolean isThreeWay() {
		return configuration.getComparisonType() == ISynchronizePageConfiguration.THREE_WAY;
	}

	private StatusLineCLabelContribution createStatusLineContribution(String id, final int mode, String label, Image image) {
		StatusLineCLabelContribution item = new StatusLineCLabelContribution(id, 15);
		item.addListener(SWT.MouseDown, event -> configuration.setMode(mode));
		item.setText(label);
		item.setImage(image);
		return item;
	}

	@Override
	public void dispose() {
		if (isThreeWay()) {
			incomingImage.dispose();
			outgoingImage.dispose();
			conflictingImage.dispose();
		}
	}

	protected void updateCounts() {
		final int total = getChangeCount();
		int supportedModes = configuration.getSupportedModes();
		// count changes only if the given mode is supported
		final int workspaceConflicting = ((supportedModes & ISynchronizePageConfiguration.CONFLICTING_MODE) != 0) ? countFor(SyncInfo.CONFLICTING) : 0;
		final int workspaceOutgoing = ((supportedModes & ISynchronizePageConfiguration.OUTGOING_MODE) != 0) ? countFor(SyncInfo.OUTGOING) : 0;
		final int workspaceIncoming = ((supportedModes & ISynchronizePageConfiguration.INCOMING_MODE) != 0) ? countFor(SyncInfo.INCOMING) : 0;

		TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
			if (isThreeWay()) {
				conflicting.setText(Integer.toString(workspaceConflicting));
				incoming.setText(Integer.toString(workspaceIncoming));
				outgoing.setText(Integer.toString(workspaceOutgoing));

				conflicting.setTooltip(NLS.bind(TeamUIMessages.StatisticsPanel_numbersTooltip, new String[] { TeamUIMessages.StatisticsPanel_conflicting }));
				outgoing.setTooltip(NLS.bind(TeamUIMessages.StatisticsPanel_numbersTooltip, new String[] { TeamUIMessages.StatisticsPanel_outgoing }));
				incoming.setTooltip(NLS.bind(TeamUIMessages.StatisticsPanel_numbersTooltip, new String[] { TeamUIMessages.StatisticsPanel_incoming }));
			} else {
				if (total == 1) {
					totalChanges.setText(NLS.bind(TeamUIMessages.StatisticsPanel_numberTotalSingular, new String[] { Integer.toString(total) }));
				} else {
					totalChanges.setText(NLS.bind(TeamUIMessages.StatisticsPanel_numberTotalPlural, new String[] { Integer.toString(total) }));
				}
			}
		});
	}

	protected abstract int getChangeCount();

	protected abstract int countFor(int state);

	@Override
	public void fillActionBars(IActionBars actionBars) {
		IStatusLineManager mgr = actionBars.getStatusLineManager();
		if (isThreeWay()) {
			mgr.add(incoming);
			mgr.add(outgoing);
			mgr.add(conflicting);
		} else {
			mgr.add(totalChanges);
		}
	}

	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}
}
