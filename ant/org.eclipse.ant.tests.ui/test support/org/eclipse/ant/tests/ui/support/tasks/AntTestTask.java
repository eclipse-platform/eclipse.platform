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
package org.eclipse.ant.tests.ui.support.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;

public class AntTestTask extends Task {

	public AntTestTask() {
		super();
	}

	@Override
	public void execute() throws BuildException {
		Echo echo = new Echo();
		echo.setProject(getProject());
		echo.setMessage("Testing Ant in Eclipse with a custom task"); //$NON-NLS-1$
		echo.execute();
	}
}
