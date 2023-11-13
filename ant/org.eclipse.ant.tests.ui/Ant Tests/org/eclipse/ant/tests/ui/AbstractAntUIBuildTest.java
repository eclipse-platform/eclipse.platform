/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IHyperlink;
import org.junit.Rule;

public abstract class AbstractAntUIBuildTest extends AbstractAntUITest {

	@Rule
	public RunInSeparateThreadRule runInSeparateThread = new RunInSeparateThreadRule();

	/**
	 * Launches the launch configuration Waits for all of the lines to be appended
	 * to the console.
	 *
	 * @param config the config to execute
	 */
	protected void launch(ILaunchConfiguration config) throws CoreException {
		launchAndTerminate(config, 20000);
	}

	protected void activateLink(final IHyperlink link) {
		Display.getDefault().asyncExec(() -> link.linkActivated());
	}

}
