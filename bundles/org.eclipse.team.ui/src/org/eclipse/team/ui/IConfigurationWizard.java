/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.ui;


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbench;

/**
 * IConfigurationWizard defines the interface that users of the extension
 * point <code>org.eclipse.team.ui.configurationWizards</code> must implement.
 *
 * @since 2.0
 */
public interface IConfigurationWizard extends IWizard {
	/**
	 * Initializes this creation wizard using the passed workbench and
	 * the selected project.
	 * <p>
	 * This method is called after the no argument constructor and
	 * before other methods are called.
	 * </p>
	 *
	 * @param workbench the current workbench
	 * @param project the selected project
	 */
	void init(IWorkbench workbench, IProject project);
}

