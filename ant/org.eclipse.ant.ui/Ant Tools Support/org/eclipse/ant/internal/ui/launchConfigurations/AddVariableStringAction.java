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

import org.eclipse.jdt.internal.debug.ui.actions.RuntimeClasspathAction;
import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class AddVariableStringAction extends RuntimeClasspathAction {
	
	public AddVariableStringAction(IClasspathViewer viewer) {
		super(AntLaunchConfigurationMessages.AddVariableStringAction_1, viewer);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.actions.RuntimeClasspathAction#getActionType()
	 */
	protected int getActionType() {
		return ADD;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		VariableInputDialog inputDialog = new VariableInputDialog(getShell());
		inputDialog.open();
		String variableString= inputDialog.getVariableString();
		if (variableString != null && variableString.trim().length() > 0) {
			IRuntimeClasspathEntry newEntry = JavaRuntime.newStringVariableClasspathEntry(variableString);
			getViewer().addEntries(new IRuntimeClasspathEntry[] {newEntry});
		}
	}
}
