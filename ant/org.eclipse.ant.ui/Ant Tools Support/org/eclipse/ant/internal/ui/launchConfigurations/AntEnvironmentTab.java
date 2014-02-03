/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

/**
 * An environment tab for Ant launch configurations.
 * Environment variables are only supported when Ant is
 * run in a separate VM, so this tab adds a warning message
 * at the top and disables the widgets if the config isn't
 * set to run in a separate VM.
 */
public class AntEnvironmentTab extends EnvironmentTab {
	
	protected Composite wrappingComposite;
	protected Label warningLabel;
	
	public void createControl(Composite parent) {
		wrappingComposite= new Composite(parent, SWT.NONE);
		wrappingComposite.setLayout(new GridLayout());
		wrappingComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		wrappingComposite.setFont(parent.getFont());
		
		warningLabel= new Label(wrappingComposite, SWT.NONE);
		warningLabel.setText(AntLaunchConfigurationMessages.AntEnvironmentTab_0);
		
		super.createControl(wrappingComposite);
		setControl(wrappingComposite); // Overwrite setting in super method
		Dialog.applyDialogFont(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		updateWidgetsEnabled(workingCopy);
	}
	
	protected void updateWidgetsEnabled(ILaunchConfigurationWorkingCopy workingCopy) {
		if (wrappingComposite == null) {
			return;
		}
		boolean isSeparateJREBuild= AntUtil.isSeparateJREAntBuild(workingCopy);
		
		Color tableColor= isSeparateJREBuild ? null : Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		Color labelColor= isSeparateJREBuild ? null : Display.getDefault().getSystemColor(SWT.COLOR_RED);
		Table table = environmentTable.getTable();
		table.setEnabled(isSeparateJREBuild);
		table.setBackground(tableColor);
		warningLabel.setForeground(labelColor);
		envAddButton.setEnabled(isSeparateJREBuild);
		envSelectButton.setEnabled(isSeparateJREBuild);
		updateAppendReplace();
		//update the enabled state of the edit and remove buttons
		environmentTable.setSelection(environmentTable.getSelection());
	}
}
