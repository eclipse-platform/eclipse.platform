/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * Workbench advisor to run an ant script after starting the workbench and then exit the workbench. Used with {@link WorkbenchAntRunner}.
 * 
 * @since 3.4
 */
public class AntRunnerWorkbenchAdvisor extends WorkbenchAdvisor {

	private final Object fContext;

	protected AntRunnerWorkbenchAdvisor(Object context) {
		fContext = context;
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return null;
	}

	@Override
	public void preStartup() {
		try {
			new AntRunner().run(fContext);
		}
		catch (Exception e) {
			AntUIPlugin.log(e);
		}
		try {
			ResourcesPlugin.getWorkspace().save(true, null);
		}
		catch (CoreException e) {
			AntUIPlugin.log(e.getStatus());
		}
	}

	@Override
	public boolean openWindows() {
		return false;
	}

}
