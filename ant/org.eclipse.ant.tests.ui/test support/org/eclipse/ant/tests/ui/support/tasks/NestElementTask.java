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

public class NestElementTask extends Task {

	String message = "bar"; //$NON-NLS-1$
	NestedElement e;

	public static class NestedElement {
		Boolean works = Boolean.FALSE;

		public NestedElement() {
		}

		public void setWorks(Boolean booleanValue) {
			works = booleanValue;
		}

		public boolean works() {
			return works.booleanValue();
		}
	}

	public void addNestedElement(NestedElement nestedElement) {
		e = nestedElement;
	}

	@Override
	public void execute() throws BuildException {
		Echo echo = new Echo();
		echo.setProject(getProject());
		if (e.works()) {
			echo.setMessage(message);
		} else {
			echo.setMessage("ack"); //$NON-NLS-1$
		}
		echo.execute();
	}
}
