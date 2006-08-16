/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.scheduler.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.internal.scheduler.*;
import org.eclipse.update.internal.scheduler.UpdateSchedulerPlugin;

public class AutomaticUpdatesPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private Button enabledCheck;
	private Button onStartupRadio;
	private Button onScheduleRadio;
	private Combo dayCombo;
	private Label atLabel;
	private Combo hourCombo;
	private Button searchOnlyRadio;
	private Button searchAndDownloadRadio;
	private Group updateScheduleGroup;
	private Group downloadGroup;

	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		enabledCheck = new Button(container, SWT.CHECK);
		enabledCheck.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_findUpdates); 

		createSpacer(container, 1);

		updateScheduleGroup = new Group(container, SWT.NONE);
		updateScheduleGroup.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_UpdateSchedule); 
		layout = new GridLayout();
		layout.numColumns = 3;
		updateScheduleGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		updateScheduleGroup.setLayoutData(gd);

		onStartupRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onStartupRadio.setText(
			UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_findOnStart); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		onStartupRadio.setLayoutData(gd);
		onStartupRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		onScheduleRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onScheduleRadio.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_findOnSchedule); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		onScheduleRadio.setLayoutData(gd);
		onScheduleRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		dayCombo = new Combo(updateScheduleGroup, SWT.READ_ONLY);
		dayCombo.setItems(SchedulerStartup.DAYS);
		gd = new GridData();
		gd.widthHint = 200;
		gd.horizontalIndent = 30;
		dayCombo.setLayoutData(gd);
		
		atLabel = new Label(updateScheduleGroup, SWT.NULL);
		atLabel.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_at); 
		
		hourCombo = new Combo(updateScheduleGroup, SWT.READ_ONLY);
		hourCombo.setItems(SchedulerStartup.HOURS);
		gd = new GridData();
		gd.widthHint = 100;
		hourCombo.setLayoutData(gd);
	
		createSpacer(container, 1);
		
		downloadGroup = new Group(container, SWT.NONE);
		downloadGroup.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_downloadOptions); 
		layout = new GridLayout();
		layout.numColumns = 3;
		downloadGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		downloadGroup.setLayoutData(gd);

		searchOnlyRadio = new Button(downloadGroup, SWT.RADIO);
		searchOnlyRadio.setText(
			UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_searchAndNotify); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchOnlyRadio.setLayoutData(gd);
		searchOnlyRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		searchAndDownloadRadio = new Button(downloadGroup, SWT.RADIO);
		searchAndDownloadRadio.setText(UpdateSchedulerMessages.AutomaticUpdatesPreferencePage_downloadAndNotify); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchAndDownloadRadio.setLayoutData(gd);
		searchAndDownloadRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
			
		initialize();

		enabledCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		Dialog.applyDialogFont(container);
		return container;
	}

	protected void createSpacer(Composite composite, int columnSpan) {
		Label label = new Label(composite, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = columnSpan;
		label.setLayoutData(gd);
	}

	private void initialize() {
		Preferences pref = UpdateSchedulerPlugin.getDefault().getPluginPreferences();
		enabledCheck.setSelection(pref.getBoolean(UpdateSchedulerPlugin.P_ENABLED));
		setSchedule(pref.getString(UpdateSchedulerPlugin.P_SCHEDULE));

		dayCombo.setText(SchedulerStartup.DAYS[getDay(pref, false)]);
		hourCombo.setText(SchedulerStartup.HOURS[getHour(pref, false)]);
		
		searchOnlyRadio.setSelection(!pref.getBoolean(UpdateSchedulerPlugin.P_DOWNLOAD));
		searchAndDownloadRadio.setSelection(pref.getBoolean(UpdateSchedulerPlugin.P_DOWNLOAD));

		pageChanged();
	}

	private void setSchedule(String value) {
		if (value.equals(UpdateSchedulerPlugin.VALUE_ON_STARTUP))
			onStartupRadio.setSelection(true);
		else
			onScheduleRadio.setSelection(true);
	}

	private void pageChanged() {
		boolean master = enabledCheck.getSelection();
		updateScheduleGroup.setEnabled(master);
		onStartupRadio.setEnabled(master);
		onScheduleRadio.setEnabled(master);
		dayCombo.setEnabled(master && onScheduleRadio.getSelection());
		atLabel.setEnabled(master && onScheduleRadio.getSelection());
		hourCombo.setEnabled(master && onScheduleRadio.getSelection());
		downloadGroup.setEnabled(master);
		searchOnlyRadio.setEnabled(master);
		searchAndDownloadRadio.setEnabled(master);
	}

	protected void performDefaults() {
		super.performDefaults();
		Preferences pref = UpdateSchedulerPlugin.getDefault().getPluginPreferences();
		enabledCheck.setSelection(pref.getDefaultBoolean(UpdateSchedulerPlugin.P_ENABLED));		
		
		setSchedule(pref.getDefaultString(UpdateSchedulerPlugin.P_SCHEDULE));
		onScheduleRadio.setSelection(pref.getDefaultBoolean(UpdateSchedulerPlugin.P_SCHEDULE));
		
		dayCombo.setText(SchedulerStartup.DAYS[getDay(pref, true)]);
		hourCombo.setText(SchedulerStartup.HOURS[getHour(pref, true)]);
		
		searchOnlyRadio.setSelection(!pref.getDefaultBoolean(UpdateSchedulerPlugin.P_DOWNLOAD));
		searchAndDownloadRadio.setSelection(pref.getDefaultBoolean(UpdateSchedulerPlugin.P_DOWNLOAD));
		pageChanged();
	}

	/** 
	 * Method declared on IPreferencePage.
	 * Subclasses should override
	 */
	public boolean performOk() {
		Preferences pref = UpdateSchedulerPlugin.getDefault().getPluginPreferences();
		pref.setValue(UpdateSchedulerPlugin.P_ENABLED, enabledCheck.getSelection());
		if (onStartupRadio.getSelection())
			pref.setValue(UpdateSchedulerPlugin.P_SCHEDULE, UpdateSchedulerPlugin.VALUE_ON_STARTUP);
		else 
			pref.setValue(UpdateSchedulerPlugin.P_SCHEDULE, UpdateSchedulerPlugin.VALUE_ON_SCHEDULE);
			
		pref.setValue(SchedulerStartup.P_DAY, dayCombo.getText());
		pref.setValue(SchedulerStartup.P_HOUR, hourCombo.getText());
		
		pref.setValue(UpdateSchedulerPlugin.P_DOWNLOAD, searchAndDownloadRadio.getSelection());
		
		UpdateSchedulerPlugin.getDefault().savePluginPreferences();
		
		UpdateSchedulerPlugin.getScheduler().scheduleUpdateJob();
		return true;
	}
	
	private int getDay(Preferences pref, boolean useDefault) {
		String day = useDefault? pref.getDefaultString(SchedulerStartup.P_DAY): pref.getString(SchedulerStartup.P_DAY);
		for (int i=0; i<SchedulerStartup.DAYS.length; i++)
			if (SchedulerStartup.DAYS[i].equals(day))
				return i;
		return 0;
	}
	
	private int getHour(Preferences pref, boolean useDefault) {
		String hour = useDefault? pref.getDefaultString(SchedulerStartup.P_HOUR): pref.getString(SchedulerStartup.P_HOUR);
		for (int i=0; i<SchedulerStartup.HOURS.length; i++)
			if (SchedulerStartup.HOURS[i].equals(hour))
				return i;
		return 0;
	}
}
