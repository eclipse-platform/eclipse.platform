/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import junit.framework.Test;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

public class TestBug94498 extends TestCase {

	private static final String FILE_NAME = "foo.bar.zoo";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug94498.class);
	}

	public void test1() throws CoreException {
		IContentType text = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.0", text);
		text.addFileSpec(FILE_NAME, IContentType.FILE_NAME_SPEC);
		String[] fileSpecs = text.getFileSpecs(IContentType.FILE_NAME_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertEquals("2.0", 1, fileSpecs.length);
		assertEquals("2.1", FILE_NAME, fileSpecs[0]);
	}

	public void test2() {
		IContentType text = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.0", text);
		String[] fileSpecs = text.getFileSpecs(IContentType.FILE_NAME_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertEquals("2.0", 1, fileSpecs.length);
		assertEquals("2.1", FILE_NAME, fileSpecs[0]);
	}
}
