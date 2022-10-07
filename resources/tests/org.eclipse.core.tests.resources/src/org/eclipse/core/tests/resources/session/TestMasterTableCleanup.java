/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import java.util.Properties;
import junit.framework.Test;
import org.eclipse.core.internal.resources.TestingSupport;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * This is an internal test that makes sure the workspace master table does
 * not contain any stable entries after restart
 */
public class TestMasterTableCleanup extends WorkspaceSerializationTest {
	private static final String CLOSE_OPEN = "CloseOpen";
	private static final String CLOSE_DELETE = "CloseDelete";

	/**
	 * Setup.  Two scenarios with stale entries were:
	 *  1) Project that was closed and then opened
	 *  2) Project that was closed and then deleted
	 */
	public void test1() {
		IProject closeOpen = getWorkspace().getRoot().getProject(CLOSE_OPEN);
		try {
			closeOpen.create(null);
			closeOpen.open(null);
			closeOpen.close(null);
			closeOpen.open(null);

			IProject closeDelete = getWorkspace().getRoot().getProject(CLOSE_DELETE);
			closeDelete.create(null);
			closeDelete.open(null);
			closeDelete.close(null);
			closeDelete.delete(IResource.NONE, null);

			getWorkspace().save(true, null);
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Verify. Ensure safe table does not contain stale entries.
	 */
	public void test2() {
		Properties masterTable = TestingSupport.getMasterTable();
		//ensure master table does not contain entries for stale projects
		IProject closeOpen = getWorkspace().getRoot().getProject(CLOSE_OPEN);
		IProject closeDelete = getWorkspace().getRoot().getProject(CLOSE_DELETE);
		assertTrue("2.0", !masterTable.containsKey(closeOpen.getFullPath().append(".tree").toString()));
		assertTrue("2.1", !masterTable.containsKey(closeDelete.getFullPath().append(".tree").toString()));
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestMasterTableCleanup.class);
	}
}
