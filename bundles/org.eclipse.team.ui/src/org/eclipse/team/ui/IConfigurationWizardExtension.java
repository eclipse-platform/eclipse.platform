/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.ui.IWorkbench;

/**
 * Extends {@link IConfigurationWizard} to support the sharing of multiple projects.
 * The Share Project wizard uses the "adaptable" mechanism (see {@link IAdapterManager} to obtain an
 * <code>IConfigurationWizardExtension</code> for an <code>IConfigurationWizard</code>
 * class so clients may choose to have their <code>IConfigurationWizard</code> class implement this
 * interface as well or may choose to use the adaptable mechanism to provide the extension.
 * <p>
 * Clients may implement this interface.
 *
 * @see IConfigurationWizard
 * @since 3.4
 */
public interface IConfigurationWizardExtension {

	/**
	 * Initializes this creation wizard using the passed workbench and
	 * selected projects.
	 * <p>
	 * This method is called after the no argument constructor and
	 * before other methods are called.
	 * </p>
	 *
	 * @param workbench the current workbench
	 * @param projects the selected projects
	 */
	void init(IWorkbench workbench, IProject[] projects);
}
