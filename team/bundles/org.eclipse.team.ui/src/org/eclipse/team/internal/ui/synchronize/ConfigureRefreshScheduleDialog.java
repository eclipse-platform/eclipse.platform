/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;

/**
 * Dialog that allows configuring a subscriber refresh schedule.
 *
 * @since 3.0
 */
public class ConfigureRefreshScheduleDialog extends DetailsDialog {

	private ConfigureSynchronizeScheduleComposite scheduleComposite;
	private SubscriberRefreshSchedule schedule;

	public ConfigureRefreshScheduleDialog(Shell parentShell, SubscriberRefreshSchedule schedule) {
		super(parentShell, NLS.bind(TeamUIMessages.ConfigureRefreshScheduleDialog_0, new String[] { Utils.getTypeName(schedule.getParticipant()) }));
		this.schedule = schedule;
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		IPageValidator validator = errorMessage -> {
			setPageComplete(errorMessage == null);
			setErrorMessage(errorMessage);
		};
		scheduleComposite = new ConfigureSynchronizeScheduleComposite(parent, schedule, validator);
		Dialog.applyDialogFont(parent);
	}

	@Override
	protected void okPressed() {
		scheduleComposite.saveValues();
		super.okPressed();
	}

	@Override
	protected boolean includeDetailsButton() {
		return false;
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		return null;
	}

	@Override
	protected void updateEnablements() {
	}

	@Override
	protected String getHelpContextId() {
		return IHelpContextIds.CONFIGURE_REFRESH_SCHEDULE_DIALOG;
	}
}
