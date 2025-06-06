/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.ant.internal.launching.launchConfigurations.AntHomeClasspathEntry;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.internal.debug.ui.actions.AddExternalJarAction;
import org.eclipse.jdt.internal.debug.ui.actions.AddFolderAction;
import org.eclipse.jdt.internal.debug.ui.actions.AddJarAction;
import org.eclipse.jdt.internal.debug.ui.actions.MoveDownAction;
import org.eclipse.jdt.internal.debug.ui.actions.MoveUpAction;
import org.eclipse.jdt.internal.debug.ui.actions.RemoveAction;
import org.eclipse.jdt.internal.debug.ui.actions.RestoreDefaultEntriesAction;
import org.eclipse.jdt.internal.debug.ui.actions.RuntimeClasspathAction;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathEntry;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathModel;
import org.eclipse.jdt.internal.debug.ui.classpath.IClasspathEntry;
import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * The Ant classpath tab
 */
public class AntClasspathTab extends JavaClasspathTab {

	@Override
	public boolean isShowBootpath() {
		return false;
	}

	/**
	 * Creates actions to manipulate the classpath.
	 *
	 * @param pathButtonComp
	 *            composite buttons are contained in
	 * @since 3.0
	 */
	@Override
	protected void createPathButtons(Composite pathButtonComp) {
		createButton(pathButtonComp, new MoveUpAction(fClasspathViewer));
		createButton(pathButtonComp, new MoveDownAction(fClasspathViewer));
		createButton(pathButtonComp, new RemoveAction(fClasspathViewer));
		createButton(pathButtonComp, new AddJarAction(fClasspathViewer));
		createButton(pathButtonComp, new AddExternalJarAction(fClasspathViewer, DIALOG_SETTINGS_PREFIX));
		Button button = createButton(pathButtonComp, new AddFolderAction(fClasspathViewer));
		button.setText(AntLaunchConfigurationMessages.AntClasspathTab_0);
		createButton(pathButtonComp, new AddVariableStringAction(fClasspathViewer));
		RuntimeClasspathAction action = new RestoreDefaultEntriesAction(fClasspathViewer, this);
		createButton(pathButtonComp, action);
		action.setEnabled(true);

		action = new EditAntHomeEntryAction(fClasspathViewer, this);
		createButton(pathButtonComp, action);
		action.setEnabled(true);
	}

	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			AntUtil.migrateToNewClasspathFormat(configuration);
		}
		catch (CoreException e) {
			// do nothing
		}
		super.initializeFrom(configuration);
	}

	@Override
	public void entriesChanged(IClasspathViewer viewer) {
		super.entriesChanged(viewer);
		for (ILaunchConfigurationTab tab : getLaunchConfigurationDialog().getTabs()) {
			if (tab instanceof AntTargetsTab) {
				((AntTargetsTab) tab).setDirty(true);
			}
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {

		boolean valid = super.isValid(launchConfig);
		if (!valid) {
			return false;
		}

		return validateAntHome();
	}

	private boolean validateAntHome() {
		ClasspathModel model = getModel();
		IClasspathEntry userEntry = model.getUserEntry();
		for (IClasspathEntry entry : userEntry.getEntries()) {
			IRuntimeClasspathEntry runtimeEntry = ((ClasspathEntry) entry).getDelegate();
			if (runtimeEntry instanceof AntHomeClasspathEntry) {
				try {
					((AntHomeClasspathEntry) runtimeEntry).resolveAntHome();
				}
				catch (CoreException ce) {
					setErrorMessage(ce.getStatus().getMessage());
					return false;
				}
				break;
			}
		}
		return true;
	}
}
