/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.net;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ProxyEntryDialog extends StatusDialog {

	private ProxyData data;
	private String[] addedTypes;

	private Label typeLabel;
	private Text typeText;
	private Label hostLabel;
	private Text hostText;
	private Label portLabel;
	private Text portText;

	private Button requiresAuthentificationButton;
	private Label userIdLabel;
	private Text userIdText;
	private Label passwordLabel;
	private Text passwordText;

	private static final String PLUGIN_ID = "org.eclipse.ui.net"; //$NON-NLS-1$

	public ProxyEntryDialog(Shell parent, ProxyData data, String[] addedArray,
			String title) {
		super(parent);
		if (data == null) {
			this.data = new ProxyData("", "", 0, false, ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			this.data = data;
		}
		this.addedTypes = addedArray;
		this.setTitle(title);
	}

	public ProxyData getValue() {
		return data;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.FILL_VERTICAL));

		typeLabel = new Label(composite, SWT.NONE);
		typeLabel.setText(NetUIMessages.ProxyEntryDialog_2);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
				1, 1));
		typeText = new Text(composite, SWT.BORDER);
		typeText.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 3,
				1));
		typeText.setEditable(false);

		hostLabel = new Label(composite, SWT.NONE);
		hostLabel.setText(NetUIMessages.ProxyEntryDialog_3);
		hostLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
				1, 1));
		hostText = new Text(composite, SWT.BORDER);
		GridData gdata = new GridData(SWT.FILL, SWT.TOP, true, false);
		gdata.widthHint = 250;
		hostText.setLayoutData(gdata);

		portLabel = new Label(composite, SWT.NONE);
		portLabel.setText(NetUIMessages.ProxyEntryDialog_4);
		portText = new Text(composite, SWT.BORDER);
		gdata = new GridData();
		gdata.widthHint = 25;
		portText.setLayoutData(gdata);

		requiresAuthentificationButton = new Button(composite, SWT.CHECK);
		requiresAuthentificationButton
				.setText(NetUIMessages.ProxyEntryDialog_6);
		requiresAuthentificationButton.setLayoutData(new GridData(SWT.LEFT,
				SWT.CENTER, false, false, 4, 1));

		userIdLabel = new Label(composite, SWT.NONE);
		userIdLabel.setText(NetUIMessages.ProxyEntryDialog_7);
		userIdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
				1, 1));
		userIdText = new Text(composite, SWT.BORDER);
		userIdText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false,
				3, 1));

		passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
				true, 1, 1));
		passwordLabel.setText(NetUIMessages.ProxyEntryDialog_8);
		passwordText = new Text(composite, SWT.BORDER);
		passwordText.setEchoChar('*');
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true,
				3, 1));

		ModifyListener validationListener = e -> updateStatus();
		typeText.addModifyListener(validationListener);
		hostText.addModifyListener(validationListener);
		portText.addModifyListener(validationListener);
		userIdText.addModifyListener(validationListener);
		passwordText.addModifyListener(validationListener);
		requiresAuthentificationButton
				.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						enableButtons();
						updateStatus();
					}

					@Override
					public void widgetSelected(SelectionEvent e) {
						enableButtons();
						updateStatus();
					}
				});

		Dialog.applyDialogFont(composite);
		applyData();
		enableButtons();
		hostText.setFocus();
		return composite;
	}

	@Override
	public void create() {
		super.create();
		validateHostName();
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	private String toString(String str) {
		return str == null ? "" : str; //$NON-NLS-1$
	}

	private void applyData() {
		typeText.setText(toString(data.getType()));
		hostText.setText(toString(data.getHost()));
		if (data.getPort() != -1) {
			portText.setText(toString(Integer.toString(data.getPort())));
		} else {
			portText.setText(""); //$NON-NLS-1$
		}
		boolean auth = data.isRequiresAuthentication();
		requiresAuthentificationButton.setSelection(auth);
		userIdText.setText(toString(data.getUserId()));
		passwordText.setText(toString(data.getPassword()));
	}

	private boolean validateHostName() {
		String scheme = null;
		try {
			URI uri = new URI(hostText.getText());
			scheme = uri.getScheme();
		} catch (URISyntaxException e) {
			updateStatus(new Status(IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK, NetUIMessages.ProxyEntryDialog_10, null));
			return false;
		}
		if (scheme != null) {
			updateStatus(new Status(IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK, NetUIMessages.ProxyEntryDialog_13, null));
			return false;
		}
		return true;
	}

	@Override
	protected void okPressed() {
		data.setType(typeText.getText());
		data.setHost(hostText.getText());
		data.setPort(Integer.parseInt(portText.getText()));
		if (requiresAuthentificationButton.getSelection()) {
			data.setUserid(userIdText.getText());
			data.setPassword(passwordText.getText());
		} else {
			data.setUserid(null);
			data.setPassword(null);
		}
		super.okPressed();
	}

	protected void enableButtons() {
		boolean auth = requiresAuthentificationButton.getSelection();
		userIdText.setEnabled(auth);
		passwordText.setEnabled(auth);
	}

	protected void updateStatus() {
		String type = typeText.getText();
		for (String addedType : addedTypes) {
			if (addedType.equalsIgnoreCase(type)) {
				updateStatus(new Status(IStatus.ERROR,
						PLUGIN_ID,
						IStatus.OK, NetUIMessages.ProxyEntryDialog_9, null));
				return;
			}
		}
		if (!validateHostName()) {
			return;
		}
		if (hostText.getText().length() == 0) {
			updateStatus(new Status(IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK, NetUIMessages.ProxyEntryDialog_10, null));
			return;
		}
		try {
			int port = Integer.parseInt(portText.getText());
			if (port < 0) {
				updateStatus(new Status(IStatus.ERROR,
						PLUGIN_ID,
						IStatus.OK, NetUIMessages.ProxyEntryDialog_11, null));
				return;
			}
		} catch (NumberFormatException e) {
			updateStatus(new Status(IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK, NetUIMessages.ProxyEntryDialog_11, null));
			return;
		}
		if (requiresAuthentificationButton.getSelection()) {
			if (userIdText.getText().length() == 0) {
				updateStatus(new Status(IStatus.ERROR, PLUGIN_ID,
						IStatus.OK, NetUIMessages.ProxyEntryDialog_12, null));
				return;
			}
		}
		updateStatus(Status.OK_STATUS);
	}
}
