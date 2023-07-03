/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 431683
 *******************************************************************************/

package org.eclipse.help.ui.internal.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.base.util.TestConnectionUtility;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;


public class ICDialog extends StatusDialog implements Listener, SelectionListener {

	private IC ic = null;

	private Text nameText;
	private Text hrefText;

	private Button testButton;

	private boolean testConnect = false;

	public ICDialog(Shell parentShell) {
		this(parentShell,null,false);
	}
	public ICDialog(Shell parentShell, IC initialIC) {
		this(parentShell,initialIC,false);
	}
	public ICDialog(Shell parentShell, IC initialIC,boolean testConnect) {
		super(parentShell);
		this.ic = initialIC;
		this.testConnect  = testConnect;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				"org.eclipse.help.ui.prefPageHelpContent"); //$NON-NLS-1$

		Composite top = (Composite) super.createDialogArea(parent);
		top.setSize(top.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Composite topGroup = new Composite(top, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		layout.makeColumnsEqualWidth = false;
		topGroup.setLayout(layout);
		topGroup.setFont(top.getFont());
		topGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		Label label = new Label(topGroup, SWT.NONE);
		label.setText(Messages.AddICDialog_4);

		nameText = new Text(topGroup, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (nameText.getOrientation() == SWT.RIGHT_TO_LEFT)
			nameText.setOrientation(SWT.LEFT_TO_RIGHT);
		nameText.addListener(SWT.Modify, this);

		label = new Label(topGroup, SWT.NONE);
		label.setText(Messages.InfoCenterPage_url);

		hrefText = new Text(topGroup, SWT.BORDER);
		hrefText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (hrefText.getOrientation() == SWT.RIGHT_TO_LEFT)
			hrefText.setOrientation(SWT.LEFT_TO_RIGHT);
		hrefText.addListener(SWT.Modify, this);


		if (ic!=null)
		{
			nameText.setText(ic.getName());
			hrefText.setText(ic.getHref());
			this.getShell().setText(NLS.bind(Messages.EditICDialog_7, ic.getName()));
		}
		else
			this.getShell().setText(Messages.AddICDialog_2);


		Dialog.applyDialogFont(top);
		return top;

	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		testButton = this.createButton(
				parent,
				IDialogConstants.CLIENT_ID,
				Messages.HelpContentBlock_testConnectionTitle,
				true);
		testButton.addSelectionListener(this);
		super.createButtonsForButtonBar(parent);


		//Initialize validity
		updateValidity();

		if (testConnect)
			testConnection();
	}


	@Override
	public void initializeBounds() {
		Point size = getInitialSize();
		Point location = getInitialLocation(size);
		this.getShell().setBounds(location.x, location.y,
				size.x + 180, size.y);
	}

	@Override
	public void okPressed() {
		try {
			String href = formalizeHref(hrefText.getText());

			ic = new IC(nameText.getText(),href,true);
		} catch (MalformedURLException e) {}  // This should never happen since we test in updateValidity
		this.setReturnCode(OK);
		this.close();
	}

	public String formalizeHref(String href)
	{
		if (href.endsWith("/index.jsp")) //$NON-NLS-1$
			href = href.substring(0,href.lastIndexOf("/index.jsp")); //$NON-NLS-1$
		else if (href.endsWith("/site/site.xml")) //$NON-NLS-1$
			href = href.substring(0,href.lastIndexOf("/site/site.xml")); //$NON-NLS-1$

		return href;
	}

	/*
	 * Checks for errors in the user input and shows/clears the error message
	 * as appropriate.
	 */
	private void updateValidity() {

		IStatus status = Status.OK_STATUS;

		if (nameText!=null && nameText.getText().equals(""))  //$NON-NLS-1$
			status = new Status(IStatus.ERROR,"org.eclipse.help.ui",Messages.InfoCenterPage_invalidURL); //$NON-NLS-1$
		else if (hrefText!=null)
		{
			try {
				String href = hrefText.getText();
				new URL(href);

				if (!href.matches(".*\\://.+/.+")) //$NON-NLS-1$
					status = new Status(IStatus.ERROR,"org.eclipse.help.ui",Messages.InfoCenterPage_invalidURL); //$NON-NLS-1$

			} catch (MalformedURLException e) {
				status = new Status(IStatus.ERROR,"org.eclipse.help.ui",Messages.InfoCenterPage_invalidURL); //$NON-NLS-1$
			}
		}

//		if (testButton!=null)
//			testButton.setEnabled(status.isOK());

		this.updateStatus(status);
	}

	private boolean areFieldsValid()
	{
		if (nameText!=null && nameText.getText().equals(""))  //$NON-NLS-1$
			return false;
		else if (hrefText!=null)
		{
			try {
				String href = hrefText.getText();
				new URL(href);

				if (!href.matches(".*\\://.+/.+")) //$NON-NLS-1$
					return false;

			} catch (MalformedURLException e) {
				return false;
			}
		}
		return true;
	}

	private void testConnection()
	{
		IC testIC;
		IStatus status;
		try {
			testIC = new IC(nameText.getText(),formalizeHref(hrefText.getText()),true);
			boolean connected = TestConnectionUtility.testConnection(testIC.getHost(),
						testIC.getPort()+"", testIC.getPath(),testIC.getProtocol()); //$NON-NLS-1$


			if(connected)
				status = new Status(IStatus.INFO,"org.eclipse.help.ui",Messages.TestConnectionDialog_12); //$NON-NLS-1$
			else
				status = new Status(IStatus.WARNING,"org.eclipse.help.ui",Messages.TestConnectionDialog_13); //$NON-NLS-1$

		} catch (MalformedURLException e) {

			status = new Status(IStatus.WARNING,"org.eclipse.help.ui",Messages.TestConnectionDialog_13); //$NON-NLS-1$
		}


		updateStatus(status);
		this.getButton(IDialogConstants.OK_ID).setEnabled(areFieldsValid());
		this.getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
	}

	public IC getIC()
	{
		return ic;
	}

	@Override
	public void handleEvent(Event event) {
		updateValidity();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() instanceof Button)
			testConnection();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}

}
