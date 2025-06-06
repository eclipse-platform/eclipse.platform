/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Application to run an Ant script after starting the workbench. Shuts down the workbench after the script runs.
 *
 * @since 3.4
 */
public class WorkbenchAntRunner implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			Shell shell = new Shell(display, SWT.ON_TOP);
			shell.dispose();
			// create the workbench with this advisor and run it until it exits
			// N.B. createWorkbench remembers the advisor, and also registers
			// the workbench globally so that all UI plug-ins can find it using
			// PlatformUI.getWorkbench() or AbstractUIPlugin.getWorkbench()
			PlatformUI.createAndRunWorkbench(display, new AntRunnerWorkbenchAdvisor(context.getArguments().get(IApplicationContext.APPLICATION_ARGS)));
			return EXIT_OK;
		}
		finally {
			if (display != null) {
				display.dispose();
			}
		}
	}

	@Override
	public void stop() {
		// do nothing
	}

}
