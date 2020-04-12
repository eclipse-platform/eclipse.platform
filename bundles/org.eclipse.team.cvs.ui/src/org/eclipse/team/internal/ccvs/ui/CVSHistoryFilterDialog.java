/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class CVSHistoryFilterDialog extends TrayDialog {

	private CVSHistoryFilter historyFilter;

	//widgets
	private Button orRadio;
	private Button andRadio;
	private Text branchName;
	private Text author;
	private Text comment;
	private DateTime fromDate;
	private DateTime toDate;

	public CVSHistoryFilterDialog(Shell shell) {
		super(shell);
		setHelpAvailable(false); // Disable help controls - F1 will still work
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CVSUIMessages.HistoryFilterDialog_title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.HISTORY_FILTER_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		topLevel.setLayout(layout);

		//"and" and "or" search radio buttons
		Label label = new Label(topLevel, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(CVSUIMessages.HistoryFilterDialog_showMatching);

		andRadio = new Button(topLevel, SWT.RADIO);
		andRadio.setText(CVSUIMessages.HistoryFilterDialog_matchingAll);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		andRadio.setLayoutData(data);
		andRadio.setSelection(true);

		orRadio = new Button(topLevel, SWT.RADIO);
		orRadio.setText(CVSUIMessages.HistoryFilterDialog_matchingAny);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		orRadio.setLayoutData(data);

		//branch name
		label = new Label(topLevel, SWT.NONE);
		label.setText(CVSUIMessages.HistoryFilterDialog_branchName);
		branchName = new Text(topLevel, SWT.BORDER);
		branchName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//author
		label = new Label(topLevel, SWT.NONE);
		label.setText(CVSUIMessages.HistoryFilterDialog_author);
		author = new Text(topLevel, SWT.BORDER);
		author.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//comment
		label = new Label(topLevel, SWT.NONE);
		label.setText(CVSUIMessages.HistoryFilterDialog_comment);
		comment = new Text(topLevel, SWT.BORDER);
		comment.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//"from" date
		label = new Label(topLevel, SWT.NONE);
		label.setText(CVSUIMessages.HistoryFilterDialog_fromDate);
		fromDate = new DateTime(topLevel, SWT.DATE | SWT.BORDER);

		//"to" date	
		label = new Label(topLevel, SWT.NONE);
		label.setText(CVSUIMessages.HistoryFilterDialog_toDate);
		toDate = new DateTime(topLevel, SWT.DATE | SWT.BORDER);

		initializeValues();

		Dialog.applyDialogFont(parent);
		return topLevel;
	}

	void initializeValues() {
		if (historyFilter == null)
			return;
		if (historyFilter.branchName != null) {
			branchName.setText(historyFilter.branchName);
		}
		if (historyFilter.author != null) {
			author.setText(historyFilter.author);
		}
		if (historyFilter.comment != null) {
			comment.setText(historyFilter.comment);
		}
		orRadio.setSelection(historyFilter.isOr);
		andRadio.setSelection(!historyFilter.isOr);
		Calendar calendar = Calendar.getInstance();
		if (historyFilter.fromDate != null) {
			calendar.setTime(historyFilter.fromDate);
			fromDate.setDay(calendar.get(Calendar.DATE));
			fromDate.setMonth(calendar.get(Calendar.MONTH));
			fromDate.setYear(calendar.get(Calendar.YEAR));
		}
		if (historyFilter.toDate != null) {
			calendar.setTime(historyFilter.toDate);
			toDate.setDay(calendar.get(Calendar.DATE));
			toDate.setMonth(calendar.get(Calendar.MONTH));
			toDate.setYear(calendar.get(Calendar.YEAR));
		}
	}

	/**
	 * A button has been pressed.  Process the dialog contents.
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CANCEL_ID == buttonId) {
			super.buttonPressed(buttonId);
			return;
		}
		Date fromDate = getFromDate();
		Date toDate = getToDate();

		//create the filter
		historyFilter = new CVSHistoryFilter(branchName.getText(), author.getText(), comment.getText(), fromDate, toDate, orRadio.getSelection());

		super.buttonPressed(buttonId);
	}

	/**
	 * Get the date from the given widget or <code>null</code>
	 * if the date is today's date.
	 * @param calendar a calendar to compute the date
	 * @param dateWidget the date widget holding the date
	 * @return the date from the given widget or <code>null</code>
	 */
	private Calendar getCalendar(DateTime dateWidget) {
		Calendar calendar = Calendar.getInstance();
		if (isFutureDate(dateWidget, calendar)) {
			return null;
		}
		calendar.set(Calendar.YEAR, dateWidget.getYear());
		calendar.set(Calendar.MONTH, dateWidget.getMonth());
		calendar.set(Calendar.DATE, dateWidget.getDay());

		//set the hours, minutes and seconds to 00
		//so as to cover the whole day
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar;
	}

	private boolean isFutureDate(DateTime dateWidget, Calendar calendar) {
		if (calendar.get(Calendar.YEAR) < dateWidget.getYear()) 
			return true;
		if (calendar.get(Calendar.YEAR) == dateWidget.getYear()) {
			if (calendar.get(Calendar.MONTH) < dateWidget.getMonth())
				return true;
			if (calendar.get(Calendar.MONTH) == dateWidget.getMonth()
					&& calendar.get(Calendar.DAY_OF_MONTH) <= dateWidget.getDay())
				return true;
		}
		return false;
	}
	
	//either user input or the smallest date available
	private Date getFromDate() {
		Calendar calendar = getCalendar(fromDate);
		if (calendar == null)
			return null;

		//set the hours, minutes and seconds to 00
		//so as to cover the whole day
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	//either user input or today
	private Date getToDate() {
		Calendar calendar = getCalendar(toDate);
		if (calendar == null)
			return null;

		//set the hours, minutes and seconds to 23, 59, 59
		//so as to cover the whole day
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		return calendar.getTime();
	}

	/**
	 * Returns the filter that was created from the provided
	 * user input.
	 */
	public CVSHistoryFilter getFilter() {
		return historyFilter;
	}

	/**
	 * Set the intial value of the dialog to the given filter.
	 */
	public void setFilter(CVSHistoryFilter filter) {
		this.historyFilter = filter;
	}

}
