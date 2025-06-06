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

package org.eclipse.ant.internal.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;

public class URLFieldEditor extends StringButtonFieldEditor {

	public URLFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
		setEmptyStringAllowed(false);
		setChangeButtonText(JFaceResources.getString("openBrowse"));//$NON-NLS-1$
		setErrorMessage(AntPreferencesMessages.URLFieldEditor_0);
	}

	@Override
	protected boolean doCheckState() {
		String text = getTextControl().getText();
		if (text != null && text.length() > 0) {
			try {
				new URL(text);
			}
			catch (MalformedURLException e) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Opens the directory chooser dialog and returns the <code>URL</code> of the selected directory.
	 */
	@Override
	protected String changePressed() {
		URL url = null;
		try {
			url = new URL(getTextControl().getText());
		}
		catch (MalformedURLException e1) {
			// do nothing
		}
		File f = null;
		if (url != null) {
			f = new File(url.getFile());
			if (!f.exists()) {
				f = null;
			}
		}

		File d = getDirectory(f);
		if (d == null) {
			return null;
		}

		try {
			return d.toURI().toURL().toExternalForm();
		}
		catch (MalformedURLException e) {
			AntUIPlugin.log("Internal error setting documentation location", e); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Helper that opens the directory chooser dialog.
	 *
	 * @param startingDirectory
	 *            The directory the dialog will open in.
	 * @return File File or <code>null</code>.
	 */
	private File getDirectory(File startingDirectory) {

		DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN | SWT.SHEET);
		if (startingDirectory != null) {
			fileDialog.setFilterPath(startingDirectory.getPath());
		}
		String dir = fileDialog.open();
		if (dir != null) {
			dir = dir.trim();
			if (dir.length() > 0) {
				return new File(dir);
			}
		}

		return null;
	}
}
