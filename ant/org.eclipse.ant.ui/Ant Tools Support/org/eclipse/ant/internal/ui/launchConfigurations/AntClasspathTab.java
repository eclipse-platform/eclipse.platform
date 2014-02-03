/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.launching.launchConfigurations.AntHomeClasspathEntry;
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
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab#isShowBootpath()
	 */
	public boolean isShowBootpath() {
		return false;
	}
	
	/**
	 * Creates actions to manipulate the classpath.
	 * 
	 * @param pathButtonComp composite buttons are contained in
	 * @since 3.0
	 */
	protected void createPathButtons(Composite pathButtonComp) {		
		createButton(pathButtonComp, new MoveUpAction(fClasspathViewer));
		createButton(pathButtonComp, new MoveDownAction(fClasspathViewer));
		createButton(pathButtonComp, new RemoveAction(fClasspathViewer));
		createButton(pathButtonComp, new AddJarAction(fClasspathViewer));
		createButton(pathButtonComp, new AddExternalJarAction(fClasspathViewer, DIALOG_SETTINGS_PREFIX));
		Button button = createButton(pathButtonComp, new AddFolderAction(fClasspathViewer));
		button.setText(AntLaunchConfigurationMessages.AntClasspathTab_0);
		createButton(pathButtonComp, new AddVariableStringAction(fClasspathViewer));
		RuntimeClasspathAction action= new RestoreDefaultEntriesAction(fClasspathViewer, this);
		createButton(pathButtonComp, action);
		action.setEnabled(true);
		
		action= new EditAntHomeEntryAction(fClasspathViewer, this);
		createButton(pathButtonComp, action);
		action.setEnabled(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			AntUtil.migrateToNewClasspathFormat(configuration);
		} catch (CoreException e) {
		}
		super.initializeFrom(configuration);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.launcher.IEntriesChangedListener#entriesChanged(org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer)
	 */
	public void entriesChanged(IClasspathViewer viewer) {
		super.entriesChanged(viewer);
		ILaunchConfigurationTab[] tabs = getLaunchConfigurationDialog().getTabs();
		for (int i = 0; i < tabs.length; i++) {
			ILaunchConfigurationTab tab = tabs[i];
			if (tab instanceof AntTargetsTab) {
				((AntTargetsTab)tab).setDirty(true);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		
		boolean valid= super.isValid(launchConfig);
		if (!valid) {
			return false;
		}
		
		return validateAntHome();
	}
	
	private boolean validateAntHome() {
		ClasspathModel model= getModel();
		IClasspathEntry userEntry= model.getUserEntry();
		IClasspathEntry[] entries= userEntry.getEntries();
		for (int i = 0; i < entries.length; i++) {
			ClasspathEntry entry = (ClasspathEntry) entries[i];
			IRuntimeClasspathEntry runtimeEntry= entry.getDelegate();
			if (runtimeEntry instanceof AntHomeClasspathEntry) {
				try {
					((AntHomeClasspathEntry)runtimeEntry).resolveAntHome();
				} catch (CoreException ce) {
					setErrorMessage(ce.getStatus().getMessage());
					return false;
				}
				break;
			}
		}
		return true;
	}
}
