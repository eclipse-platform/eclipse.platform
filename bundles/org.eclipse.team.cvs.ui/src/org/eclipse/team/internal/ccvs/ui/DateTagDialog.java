/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.util.Date;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.PlatformUI;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

/**
 * Dialog for obtaining a date from the user
 */
public class DateTagDialog extends TrayDialog {

	DateArea dateArea;
	TimeArea timeArea;
	IDialogSettings settings;
	private Date dateEntered;

	public class DateArea extends DialogArea {
		private DateTime date;

		/**
		 * This is the minimum year that is accepted by {@link DateTime#setYear}.
		 */
		private static final int DateTime_MIN_YEAR = 1752;

		public void createArea(Composite parent) {
			Composite composite = createComposite(parent, 2, false);
			initializeDialogUnits(composite);
			createLabel(composite, CVSUIMessages.DateTagDialog_0, 1); 
			date = new DateTime(composite, SWT.DATE | SWT.BORDER);
		}
		
		public void initializeValues(Calendar calendar ) {
			date.setDay(calendar.get(Calendar.DATE) - 1);
			date.setMonth(calendar.get(Calendar.MONTH));
			date.setYear(calendar.get(Calendar.YEAR));
			timeArea.initializeValues(calendar);
		}

		public void updateWidgetEnablements() {
			// Do nothing
		}
		
		public void adjustCalendar(Calendar calendar) {
			int dateYear = date.getYear();
			int todaysYear = calendar.get(Calendar.YEAR);
			if (todaysYear < DateTime_MIN_YEAR) {
				// year would be ignored by DateTime if it is less than MIN_YEAR
				// The code below is to specify the correct year as per the calendar chosen
				int extended_year = calendar.get(Calendar.EXTENDED_YEAR);
				dateYear = todaysYear + (dateYear - extended_year);
			}
			calendar.set(
					dateYear,
					date.getMonth(),
					date.getDay(),
					0,0,0);
		}
	}
	public class TimeArea extends DialogArea {

		private Button includeTime, localTime, utcTime;
		private DateTime time;

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ui.dialogs.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
		 */
		public void createArea(Composite parent) {
			Composite composite = createComposite(parent, 2, false);
			initializeDialogUnits(composite);
			includeTime = createCheckbox(composite, CVSUIMessages.DateTagDialog_1, 2);  
			createLabel(composite, CVSUIMessages.DateTagDialog_2, 1); 
			time = new DateTime(composite, SWT.TIME | SWT.BORDER);
			localTime = createRadioButton(composite, CVSUIMessages.DateTagDialog_3, 2);  
			utcTime = createRadioButton(composite, CVSUIMessages.DateTagDialog_4, 2);  
			
			includeTime.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateWidgetEnablements();
				}
			});
		}
		
		public void initializeValues(Calendar calendar) {
			time.setHours(calendar.get(Calendar.HOUR_OF_DAY));//24 hour clock
			time.setMinutes(calendar.get(Calendar.MINUTE));
			time.setSeconds(calendar.get(Calendar.SECOND));
			
			includeTime.setSelection(settings.getBoolean("includeTime")); //$NON-NLS-1$
			localTime.setSelection(!settings.getBoolean("utcTime")); //$NON-NLS-1$
			utcTime.setSelection(settings.getBoolean("utcTime")); //$NON-NLS-1$
		}
		public void updateWidgetEnablements() {
			time.setEnabled(includeTime.getSelection());
			localTime.setEnabled(includeTime.getSelection());
			utcTime.setEnabled(includeTime.getSelection());
		}
		public void adjustCalendar(Calendar calendar) {
			if (includeTime.getSelection()) {
				calendar.set(Calendar.HOUR_OF_DAY, time.getHours());//24 hour clock
				calendar.set(Calendar.MINUTE, time.getMinutes());
				calendar.set(Calendar.SECOND, time.getSeconds());
				if (utcTime.getSelection()) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
				}
			}
		}
	}
	
	public DateTagDialog(Shell parentShell) {
		super(parentShell);
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("DateTagDialog");//$NON-NLS-1$
		if (this.settings == null) {
			this.settings = workbenchSettings.addNewSection("DateTagDialog");//$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on Window.
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CVSUIMessages.DateTagDialog_5); 
	} 

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		initializeDialogUnits(topLevel);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		topLevel.setLayout(layout);
		
		createDateArea(topLevel);
		createTimeArea(topLevel);
		initializeValues();
		updateWidgetEnablements();
		
		// set F1 help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(topLevel, IHelpContextIds.DATE_TAG_DIALOG);
		Dialog.applyDialogFont(parent);
		return topLevel;
	}

	private void createDateArea(Composite topLevel) {
		dateArea = new DateArea();
		dateArea.createArea(topLevel);
	}

	private void createTimeArea(Composite topLevel) {
		timeArea = new TimeArea();
		timeArea.createArea(topLevel);
	}

	private void initializeValues() {
		Calendar calendar = Calendar.getInstance();
		dateArea.initializeValues(calendar);
		timeArea.initializeValues(calendar);
	}
	
	private void updateWidgetEnablements() {
		timeArea.updateWidgetEnablements();
		dateArea.updateWidgetEnablements();
	}

	/**
	 * Return the date specified by the user in UTC.
	 * @return the date specified by the user
	 */
	public Date getDate() {
		return dateEntered;
	}
	
	private Date privateGetDate() {
		Calendar calendar = Calendar.getInstance();
		dateArea.adjustCalendar(calendar);
		timeArea.adjustCalendar(calendar);
		return calendar.getTime();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			dateEntered = privateGetDate();
			if (dateEntered.after(Calendar.getInstance().getTime())) {
				MessageDialog dialog = new MessageDialog(getShell(),
						CVSUIMessages.DateTagDialog_6, null,
						CVSUIMessages.DateTagDialog_7, MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL,
								IDialogConstants.NO_LABEL, }, 1);
				if (dialog.open() == 1)
					return;
			}
		}
		super.buttonPressed(buttonId);
	}

}
