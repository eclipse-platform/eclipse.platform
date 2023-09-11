/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.internal.launching.launchConfigurations.AntHomeClasspathEntry;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.preferences.AntPreferencesMessages;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.RuntimeClasspathAction;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathEntry;
import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Edits the Ant Home classpath entry.
 * 
 * @since 3.0
 */
public class EditAntHomeEntryAction extends RuntimeClasspathAction {

	private final AntClasspathTab fTab;

	/**
	 * Constructs an action to edit the Ant Home setting for a launch config.
	 * 
	 * @param viewer
	 *            classpath viewer
	 */
	public EditAntHomeEntryAction(IClasspathViewer viewer, AntClasspathTab tab) {
		super(AntLaunchConfigurationMessages.EditAntHomeEntryAction_1, viewer);
		fTab = tab;
	}

	@Override
	public void run() {
		IDialogSettings dialogSettings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(EditAntHomeEntryAction.class)).getDialogSettings();
		String lastUsedPath = dialogSettings.get(IAntUIConstants.DIALOGSTORE_LASTANTHOME);
		if (lastUsedPath == null) {
			lastUsedPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		}
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
		dialog.setMessage(AntPreferencesMessages.AntClasspathBlock_3);
		dialog.setFilterPath(lastUsedPath);
		String path = dialog.open();
		if (path == null) {
			return;
		}
		dialogSettings.put(IAntUIConstants.DIALOGSTORE_LASTANTHOME, path);
		AntCorePreferences preferences = AntCorePlugin.getPlugin().getPreferences();
		String defaultHome = preferences.getAntHome();
		if (path.equalsIgnoreCase(defaultHome)) {
			path = null;
		}
		fTab.setDirty(true);
		// update existing entry or add a new one
		for (IRuntimeClasspathEntry entry : getViewer().getEntries()) {
			if (entry.getType() == IRuntimeClasspathEntry.OTHER) {
				IRuntimeClasspathEntry2 entry2 = (IRuntimeClasspathEntry2) ((ClasspathEntry) entry).getDelegate();
				if (entry2.getTypeId().equals(AntHomeClasspathEntry.TYPE_ID)) {
					((AntHomeClasspathEntry) entry2).setAntHome(path);
					getViewer().refresh(entry);
					getViewer().notifyChanged();
					return;
				}
			}
		}
		// no entry found - add a new one
		getViewer().addEntries(new IRuntimeClasspathEntry[] { new AntHomeClasspathEntry(path) });
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return true;
	}
}
