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
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Reference;

public class AntTestTask2 extends Task {

	String fMessage = "Default"; //$NON-NLS-1$
	Reference fRef = null;

	public AntTestTask2() {
		super();
	}

	@Override
	public void execute() throws BuildException {
		Echo echo = new Echo();
		echo.setProject(getProject());
		echo.setMessage("Testing Ant in Eclipse with a custom task2: " + fMessage); //$NON-NLS-1$
		echo.execute();
	}

	public void setMessage(Cool attr) {
		fMessage = attr.getValue();
	}

	public void setReference(Reference ref) {
		fRef = ref;
	}

	/**
	 * Enumerated attribute with the values "cool", "chillin" and "awesome".
	 */
	public static class Cool extends EnumeratedAttribute {
		@Override
		public String[] getValues() {
			return new String[] { "cool", "chillin", "awesome" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
