/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jdt.internal.debug.ui.actions.RuntimeClasspathAction;
import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class AddVariableStringAction extends RuntimeClasspathAction {

	public AddVariableStringAction(IClasspathViewer viewer) {
		super(AntLaunchConfigurationMessages.AddVariableStringAction_1, viewer);
	}

	@Override
	protected int getActionType() {
		return ADD;
	}

	@Override
	public void run() {
		VariableInputDialog inputDialog = new VariableInputDialog(getShell());
		inputDialog.open();
		String variableString = inputDialog.getVariableString();
		if (variableString != null && variableString.trim().length() > 0) {
			IRuntimeClasspathEntry newEntry = JavaRuntime.newStringVariableClasspathEntry(variableString);
			getViewer().addEntries(new IRuntimeClasspathEntry[] { newEntry });
		}
	}
}
