/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.views;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.operations.*;

public class NewExtensionLocationAction extends Action {

	public NewExtensionLocationAction(String text, ImageDescriptor desc) {
		super(text, desc);
	}

	public void run() {
		
		IStatus status = OperationsManager.getValidator().validatePlatformConfigValid();
		if (status != null) {
			ErrorDialog.openError(
					UpdateUI.getActiveWorkbenchShell(),
					null,
					null,
					status);
			return;
		}
		
		DirectoryDialog dialog =
			new DirectoryDialog(UpdateUI.getActiveWorkbenchShell(), SWT.APPLICATION_MODAL);
		dialog.setMessage(UpdateUI.getString("NewExtensionLocationAction.selectExtLocation")); //$NON-NLS-1$

		String dir = dialog.open();
		while (dir != null) {
			File dirFile = getExtensionSite(new File(dir));
			if (dirFile != null) {
				if (addExtensionLocation(dirFile))
					return;
				else {
					// re-open the directory dialog
					dialog.setFilterPath(dir);
					dir = dialog.open();
				}	
			} else {
				MessageDialog.openInformation(
					UpdateUI.getActiveWorkbenchShell(),
					UpdateUI.getString(
						"NewExtensionLocationAction.extInfoTitle"), //$NON-NLS-1$
					UpdateUI.getString(
						"NewExtensionLocationAction.extInfoMessage")); //$NON-NLS-1$
				// re-open the directory dialog
				dialog.setFilterPath(dir);
				dir = dialog.open();
			}
		}
	}

	/**
	 * @param directory
	 * @return the site file (including "eclipse" path) when directory is an eclipse exstension, null otherwise
	 */
	static File getExtensionSite(File directory) {
		// Check the eclipse folder
		if (directory.getName().equals("eclipse")) { //$NON-NLS-1$
			// if we picked up the eclipse directory, check if its parent is a site
			File site = getExtensionSite(directory.getParentFile());
			if (site != null)
				return directory;
			// otherwise, fall through
		}
		
		File eclipse = new File(directory, "eclipse"); //$NON-NLS-1$
		if (!eclipse.exists() || !eclipse.isDirectory())
			return null;

		// check the marker
		File marker = new File(eclipse, ".eclipseextension"); //$NON-NLS-1$
		if (!marker.exists() || marker.isDirectory())
			return null;
		return eclipse;
	}

	private boolean addExtensionLocation(File dir) {
		try {
			IInstallConfiguration config = SiteManager.getLocalSite().getCurrentConfiguration();
			IConfiguredSite csite = config.createLinkedConfiguredSite(dir);
			csite.verifyUpdatableStatus();
			config.addConfiguredSite(csite);
			boolean restartNeeded = SiteManager.getLocalSite().save();
			UpdateUI.requestRestart(restartNeeded);
			return true;
		} catch (CoreException e) {
			String title = UpdateUI.getString("InstallWizard.TargetPage.location.error.title"); //$NON-NLS-1$
			ErrorDialog.openError(UpdateUI.getActiveWorkbenchShell(), title, null, e.getStatus());
			UpdateUI.logException(e,false);
			return false;
		}
	}

}
