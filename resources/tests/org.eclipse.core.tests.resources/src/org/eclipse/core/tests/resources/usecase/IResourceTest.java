/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.usecase;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

public abstract class IResourceTest extends ResourceTest {
	public static QualifiedName Q_NAME_SESSION = new QualifiedName("prop", "session");
	public static String STRING_VALUE = "value";
	public static String PROJECT = "Project";
	public static String FOLDER = "Folder";
	public static String FILE = "File";

	public IResourceTest() {
		super();
	}

	public IResourceTest(String name) {
		super(name);
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent or unopened solution.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void commonFailureTestsForResource(IResource resource, boolean created) {
		/* Prefix to assertion messages. */
		String method = "commonFailureTestsForResource(IResource," + (created ? "CREATED" : "NONEXISTENT") + "): ";
		if (!created) {
			assertTrue(method + "1", getWorkspace().getRoot().findMember(resource.getFullPath()) == null);
		}

		/* Session properties */
		assertThrows(CoreException.class, () -> resource.getSessionProperty(Q_NAME_SESSION));
		assertThrows(CoreException.class, () -> resource.setSessionProperty(Q_NAME_SESSION, STRING_VALUE));
	}

	/**
	 * Wrapper for deprecated method {@link IResource#isLocal(int)} to reduce
	 * warnings.
	 */
	protected boolean isLocal(IResource resource, int depth) {
		return resource.isLocal(depth);
	}
}
