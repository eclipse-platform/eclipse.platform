/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.*;
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createMainDialogArea(Composite parent) {
		IPageValidator validator = new IPageValidator() {
			@Override
			public void setComplete(String errorMessage) {
				setPageComplete(errorMessage == null);
				setErrorMessage(errorMessage);
			}
		};
		scheduleComposite = new ConfigureSynchronizeScheduleComposite(parent, schedule, validator);
		Dialog.applyDialogFont(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		scheduleComposite.saveValues();
		super.okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#includeDetailsButton()
	 */
	@Override
	protected boolean includeDetailsButton() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#updateEnablements()
	 */
	@Override
	protected void updateEnablements() {
	}

    @Override
	protected String getHelpContextId() {
        return IHelpContextIds.CONFIGURE_REFRESH_SCHEDULE_DIALOG;
    }
}
