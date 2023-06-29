/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * The browser for adding an external archive.
 * @since 3.0
 */
public class ExternalArchiveSourceContainerBrowser extends AbstractSourceContainerBrowser {

	private static final String ROOT_DIR = ExternalArchiveSourceContainerBrowser.class.getName() + ".rootDir";   //$NON-NLS-1$

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI | SWT.SHEET);
		IDialogSettings dialogSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ExternalArchiveSourceContainerBrowser.class))
				.getDialogSettings();
		String rootDir = dialogSettings.get(ROOT_DIR);
		dialog.setText(SourceLookupUIMessages.ExternalArchiveSourceContainerBrowser_2);
		dialog.setFilterExtensions(new String[]{"*.jar;*.zip"});  //$NON-NLS-1$
		if (rootDir != null) {
			dialog.setFilterPath(rootDir);
		}
		dialog.open();
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
		if (nChosen > 0) {
			rootDir = dialog.getFilterPath();
			IPath filterPath = IPath.fromOSString(rootDir);
			ISourceContainer[] containers= new ISourceContainer[nChosen];
			for (int i= 0; i < nChosen; i++) {
				IPath path= filterPath.append(fileNames[i]).makeAbsolute();
				// TODO: configure auto-detect
				containers[i]= new ExternalArchiveSourceContainer(path.toOSString(), true);
			}
			dialogSettings.put(ROOT_DIR, rootDir);
			return containers;
		}
		return new ISourceContainer[0];
	}

}
