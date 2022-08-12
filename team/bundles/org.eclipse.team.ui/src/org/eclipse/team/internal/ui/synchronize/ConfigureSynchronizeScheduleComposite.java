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
 *     Sebastian Davids <sdavids@gmx.de> - bug 54630
 *     Trevor S. Kaufman <endante@gmail.com> - bug 156152
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * A composite that allows editing a subscriber refresh schedule. A validator can be used to allow
 * containers to show page completion.
 *
 * @since 3.0
 */
public class ConfigureSynchronizeScheduleComposite extends Composite {

	private SubscriberRefreshSchedule schedule;
	private Button enableBackgroundRefresh;
	private Text timeInterval;
	private Combo hoursOrMinutes;
	private IPageValidator validator;
	private DateTime startTime;
	private Button repeatEvery;
	private Label synchronizeAt;

	public ConfigureSynchronizeScheduleComposite(Composite parent, SubscriberRefreshSchedule schedule, IPageValidator validator) {
		super(parent, SWT.NONE);
		this.schedule = schedule;
		this.validator = validator;
		createMainDialogArea(parent);
	}

	private void initializeValues() {
		boolean enableBackground = schedule.isEnabled();
		boolean hours = false;

		enableBackgroundRefresh.setSelection(enableBackground);

		long seconds = schedule.getRefreshInterval();
		if(seconds <= 60) {
			seconds = 60;
		}

		long minutes = seconds / 60;

		if(minutes >= 60) {
			minutes = minutes / 60;
			hours = true;
		}
		hoursOrMinutes.select(hours ? 0 : 1);
		timeInterval.setText(Long.toString(minutes));
		repeatEvery.setSelection(!schedule.getRunOnce());

		Date start = schedule.getRefreshStartTime();
		Calendar cal = Calendar.getInstance();
		if (start != null) {
			cal.setTime(start);
			startTime.setTime(cal.get(Calendar.HOUR_OF_DAY), cal
					.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		} else {
			startTime.setTime(0, 0, 0); // default to 00:00:00
		}
	}

	protected void createMainDialogArea(Composite parent) {
		GC gc = new GC(parent);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics	fontMetrics = gc.getFontMetrics();
		gc.dispose();
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
		gridLayout.verticalSpacing = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
		setLayout(gridLayout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite area = this;

		createWrappingLabel(area, NLS.bind(TeamUIMessages.ConfigureRefreshScheduleDialog_1, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, schedule.getParticipant().getName()) }), 0, 3);

		enableBackgroundRefresh = new Button(area, SWT.CHECK);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 3;
		enableBackgroundRefresh.setLayoutData(gridData);
		enableBackgroundRefresh.setText(TeamUIMessages.ConfigureRefreshScheduleDialog_3);
		enableBackgroundRefresh.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnablements();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		synchronizeAt = createIndentedLabel(area, TeamUIMessages.ConfigureRefreshScheduleDialog_3a, 20);

		startTime = new DateTime(area, SWT.TIME | SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		startTime.setLayoutData(gridData);

		repeatEvery = createIndentedButton(area, TeamUIMessages.ConfigureRefreshScheduleDialog_4, 20);
		repeatEvery.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnablements();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		timeInterval = new Text(area, SWT.BORDER | SWT.RIGHT);
		gridData = new GridData();
		gridData.widthHint = 35;
		timeInterval.setLayoutData(gridData);
		timeInterval.addModifyListener(e -> updateEnablements());
		timeInterval.addVerifyListener(e -> {
			String string = e.text;
			char[] chars = new char[string.length()];
			string.getChars(0, chars.length, chars, 0);
			for (char element : chars) {
				if (!('0' <= element && element <= '9')) {
					e.doit = false;
					return;
				}
			}
		});

		hoursOrMinutes = new Combo(area, SWT.READ_ONLY);
		hoursOrMinutes.setItems(TeamUIMessages.ConfigureRefreshScheduleDialog_5, TeamUIMessages.ConfigureRefreshScheduleDialog_6); //
		hoursOrMinutes.setLayoutData(new GridData());

		final Label label = new Label(area, SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);
		label.setText(NLS.bind(TeamUIMessages.ConfigureRefreshScheduleDialog_2, new String[] { SubscriberRefreshSchedule.refreshEventAsString(schedule.getLastRefreshEvent()) }));

		initializeValues();
		updateEnablements();
	}

	public void saveValues() {
		if (enableBackgroundRefresh.getSelection()) {

			// start time
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, startTime.getHours());
			cal.set(Calendar.MINUTE, startTime.getMinutes());
			cal.set(Calendar.SECOND, startTime.getSeconds());
			schedule.setRefreshStartTime(cal.getTime());

			// repeat interval
			if (repeatEvery.getSelection()) {
				int hours = hoursOrMinutes.getSelectionIndex();
				try {
					long seconds = Long.parseLong(timeInterval.getText());
					if (hours == 0) {
						seconds = seconds * 3600;
					} else {
						seconds = seconds * 60;
					}
					schedule.setRefreshInterval(seconds);
				} catch (NumberFormatException e) {
					// keep old value
				}
			} else {
				schedule.setRunOnce(true);
			}
		}

		if(schedule.isEnabled() != enableBackgroundRefresh.getSelection()) {
			schedule.setEnabled(enableBackgroundRefresh.getSelection(), true /* allow to start */);
		}

		// update schedule
		ISynchronizeParticipant participant = schedule.getParticipant();
		if (!participant.isPinned() && schedule.isEnabled()) {
			participant.setPinned(MessageDialog.openQuestion(getShell(),
					NLS.bind(TeamUIMessages.ConfigureSynchronizeScheduleComposite_0, new String[] { Utils.getTypeName(participant) }),
					NLS.bind(TeamUIMessages.ConfigureSynchronizeScheduleComposite_1, new String[] { Utils.getTypeName(participant) })));
		}
		schedule.getRefreshable().setRefreshSchedule(schedule);
	}

	public void updateEnablements() {
		if (!enableBackgroundRefresh.getSelection()) {
			validator.setComplete(null);
		} else {
			if (repeatEvery.getSelection()) {
				try {
					long number = Long.parseLong(timeInterval.getText());
					if(number <= 0) {
						validator.setComplete(TeamUIMessages.ConfigureRefreshScheduleDialog_7);
					} else {
						validator.setComplete(null);
					}
				} catch (NumberFormatException e) {
					validator.setComplete(TeamUIMessages.ConfigureRefreshScheduleDialog_7);
				}
			}
		}
		synchronizeAt.setEnabled(enableBackgroundRefresh.getSelection());
		startTime.setEnabled(enableBackgroundRefresh.getSelection());
		repeatEvery.setEnabled(enableBackgroundRefresh.getSelection());
		timeInterval.setEnabled(enableBackgroundRefresh.getSelection() && repeatEvery.getSelection());
		hoursOrMinutes.setEnabled(enableBackgroundRefresh.getSelection() && repeatEvery.getSelection());
	}

	private Label createWrappingLabel(Composite parent, String text, int indent, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = 400;
		label.setLayoutData(data);
		return label;
	}

	private static Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}

	private static Button createIndentedButton(Composite parent, String text, int indent) {
		Button label = new Button(parent, SWT.CHECK);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}
}
