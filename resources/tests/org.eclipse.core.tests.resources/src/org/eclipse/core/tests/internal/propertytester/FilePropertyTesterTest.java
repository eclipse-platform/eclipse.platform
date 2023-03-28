/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.propertytester;

import java.io.ByteArrayInputStream;
import org.eclipse.core.internal.propertytester.FilePropertyTester;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.content.IContentTypeManagerTest;

public class FilePropertyTesterTest extends ResourceTest {

	private static final String CONTENT_TYPE_ID = "contentTypeId";
	private static final String IS_KIND_OF = "kindOf";
	private static final String USE_FILENAME_ONLY = "useFilenameOnly";

	private FilePropertyTester tester = null;
	private IProject project = null;
	private IProgressMonitor monitor = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		monitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject("project1");
		project.create(monitor);
		project.open(monitor);

		tester = new FilePropertyTester();
	}

	public void testNonExistingTextFile() throws Throwable {
		String expected = "org.eclipse.core.runtime.text";
		IFile target = project.getFile("tmp.txt");

		boolean ret;
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {}, expected);
		assertFalse("1.0", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF}, expected);
		assertFalse("1.1", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {USE_FILENAME_ONLY}, expected);
		assertTrue("1.2", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF, USE_FILENAME_ONLY}, expected);
		assertTrue("1.3", ret);

	}

	public void testExistingTextFile() throws Throwable {
		String expected = "org.eclipse.core.runtime.text";
		IFile target = project.getFile("tmp.txt");
		target.create(getRandomContents(), true, getMonitor());

		boolean ret;
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {}, expected);
		assertTrue("1.0", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF}, expected);
		assertTrue("1.1", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {USE_FILENAME_ONLY}, expected);
		assertTrue("1.2", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF, USE_FILENAME_ONLY}, expected);
		assertTrue("1.3", ret);
	}

	public void testNonExistingNsRootElementFile() throws Throwable {
		String expectedBase = "org.eclipse.core.runtime.xml";
		String expectedExact = "org.eclipse.core.tests.resources.ns-root-element";
		IFile target = project.getFile("tmp.xml");

		boolean ret;
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {}, expectedExact);
		assertFalse("1.0", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF}, expectedBase);
		assertFalse("1.1", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {USE_FILENAME_ONLY}, expectedBase);
		assertTrue("1.2", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF, USE_FILENAME_ONLY}, expectedBase);
		assertTrue("1.3", ret);
	}

	public void testExistingNsRootElementFile() throws Throwable {
		String expectedBase = "org.eclipse.core.runtime.xml";
		String expectedExact = "org.eclipse.core.tests.resources.ns-root-element";
		IFile target = project.getFile("tmp.xml");
		byte[] bytes = IContentTypeManagerTest.XML_ROOT_ELEMENT_NS_MATCH1.getBytes("UTF-8");
		target.create(new ByteArrayInputStream(bytes), true, getMonitor());

		boolean ret;
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {}, expectedExact);
		assertTrue("1.0", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF}, expectedBase);
		assertTrue("1.1", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {USE_FILENAME_ONLY}, expectedBase);
		assertTrue("1.2", ret);
		ret = tester.test(target, CONTENT_TYPE_ID, new String[] {IS_KIND_OF, USE_FILENAME_ONLY}, expectedBase);
		assertTrue("1.3", ret);
	}

}
