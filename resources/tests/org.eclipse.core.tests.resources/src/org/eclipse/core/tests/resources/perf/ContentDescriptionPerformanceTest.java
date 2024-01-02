/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class ContentDescriptionPerformanceTest {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private final static String DEFAULT_DESCRIPTION_FILE_NAME = "default.xml";
	private final static String NO_DESCRIPTION_FILE_NAME = "none.some-uncommon-file-extension";
	private final static String NON_DEFAULT_DESCRIPTION_FILE_NAME = "specific.xml";
	private final static int SUBDIRS = 200;
	private final static int TOTAL_FILES = 5000;
	private final static Set<String> IGNORED_FILES = Set.of(".project", "org.eclipse.core.resources.prefs");
	private final static String VALID_XML_CONTENTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><some-uncommon-root-element/>";
	private final static String VALID_XML_CONTENTS_WITH_NON_DEFAULT_ENCODING = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?><some-uncommon-root-element/>";

	private static String getFileName(int number) {
		number = number % 3;
		switch (number) {
			case 0 :
				return DEFAULT_DESCRIPTION_FILE_NAME;
			case 1 :
				return NON_DEFAULT_DESCRIPTION_FILE_NAME;
			default :
				return NO_DESCRIPTION_FILE_NAME;
		}
	}

	void assertHasExpectedDescription(String fileName, IContentDescription description) {
		if (fileName.endsWith(DEFAULT_DESCRIPTION_FILE_NAME)) {
			assertTrue("description for " + fileName, description == description.getContentType().getDefaultDescription());
		} else if (fileName.endsWith(NON_DEFAULT_DESCRIPTION_FILE_NAME)) {
			assertTrue("description for " + fileName, description != description.getContentType().getDefaultDescription());
		} else {
			assertNull("description for " + fileName, description);
		}
	}

	void createFiles() throws CoreException {
		// create a project with thousands of files
		IProject bigProject = ResourcesPlugin.getWorkspace().getRoot().getProject("bigproject");
		assertTrue("1.0", !bigProject.exists());
		bigProject.create(createTestMonitor());
		bigProject.open(createTestMonitor());
		for (int i = 0; i < SUBDIRS; i++) {
			IFolder folder = bigProject.getFolder("folder_" + i);
			folder.create(false, true, createTestMonitor());
			for (int j = 0; j < TOTAL_FILES / SUBDIRS; j++) {
				IFile file = folder.getFile("file_" + j + getFileName(j));
				file.create(createInputStream(getContents(j)), false, createTestMonitor());
			}
		}
	}

	public void doTestContentDescription(String testDescription) throws Exception {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("bigproject");
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				project.accept(resource -> {
					if (resource.getType() == IResource.FILE && !IGNORED_FILES.contains(resource.getName())) {
						assertHasExpectedDescription(resource.getName(), ((IFile) resource).getContentDescription());
					}
					return true;
				});
			}
		}.run(getClass(), testDescription, 1, 1);
	}

	private String getContents(int number) {
		number = number % 3;
		switch (number) {
			case 0 :
				return VALID_XML_CONTENTS;
			case 1 :
				return VALID_XML_CONTENTS_WITH_NON_DEFAULT_ENCODING;
			default :
				return "whatever";
		}
	}

	@Test
	public void test() throws Exception {
		test1SetUp();
		test2ColdContentDescription();
		test3WarmedUpContentDescription();
	}

	private void test1SetUp() throws CoreException {
		createFiles();
	}

	private void test2ColdContentDescription() throws Exception {
		doTestContentDescription("ColdContentDescription");
	}

	private void test3WarmedUpContentDescription() throws Exception {
		doTestContentDescription("WarmContentDescription");
	}

}
