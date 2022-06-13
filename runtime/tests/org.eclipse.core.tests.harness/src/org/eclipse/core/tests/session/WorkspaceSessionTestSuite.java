/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.session;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.session.SetupManager.SetupException;

public class WorkspaceSessionTestSuite extends SessionTestSuite {

	private IPath instanceLocation = FileSystemHelper.getRandomLocation(FileSystemHelper.getTempDir());
	// should the test cases be run in alphabetical order?
	private boolean shouldSort;

	public WorkspaceSessionTestSuite(String pluginId) {
		super(pluginId);
	}

	public WorkspaceSessionTestSuite(String pluginId, Class<?> theClass) {
		super(pluginId, theClass);
		this.shouldSort = true;
	}

	public WorkspaceSessionTestSuite(String pluginId, Class<? extends TestCase> theClass, String name) {
		super(pluginId, theClass, name);
		this.shouldSort = true;
	}

	public WorkspaceSessionTestSuite(String pluginId, String name) {
		super(pluginId, name);
	}

	/**
	 * Ensures setup uses this suite's instance location.
	 * @throws SetupException
	 */
	@Override
	protected Setup newSetup() throws SetupException {
		Setup base = super.newSetup();
		base.setEclipseArgument(Setup.DATA, instanceLocation.toOSString());
		return base;
	}

	/**
	 * Ensures workspace location is empty before running the first test, and after
	 * running the last test. Also sorts the test cases to be run if this suite was
	 * created by reifying a test case class.
	 */
	@Override
	public void run(TestResult result) {
		try {
			if (!shouldSort) {
				super.run(result);
				return;
			}
			// we have to sort the tests cases
			Test[] allTests = getTests(true);
			// now run the tests in order
			for (int i = 0; i < allTests.length && !result.shouldStop(); i++) {
				runTest(allTests[i], result);
			}
		} finally {
			FileSystemHelper.clear(instanceLocation.toFile());
		}

	}

	/**
	 * @return workspace location
	 */
	public IPath getInstanceLocation() {
		return instanceLocation;
	}
}
