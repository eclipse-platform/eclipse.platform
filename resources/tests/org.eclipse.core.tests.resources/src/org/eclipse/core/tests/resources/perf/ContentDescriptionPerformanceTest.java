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

import java.util.Set;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContentDescriptionPerformanceTest extends ResourceTest {

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
		bigProject.create(getMonitor());
		bigProject.open(getMonitor());
		for (int i = 0; i < SUBDIRS; i++) {
			IFolder folder = bigProject.getFolder("folder_" + i);
			folder.create(false, true, getMonitor());
			for (int j = 0; j < TOTAL_FILES / SUBDIRS; j++) {
				IFile file = folder.getFile("file_" + j + getFileName(j));
				file.create(getContents(getContents(j)), false, getMonitor());
			}
		}
	}

	public void doTestContentDescription() {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("bigproject");
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					project.accept(resource -> {
						if (resource.getType() == IResource.FILE && !IGNORED_FILES.contains(resource.getName())) {
							assertHasExpectedDescription(resource.getName(), ((IFile) resource).getContentDescription());
						}
						return true;
					});
				} catch (CoreException e) {
					CoreTest.fail("Failed visiting resources", e);
				}
			}
		}.run(this, 1, 1);
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

	@Override
	protected void tearDown() throws Exception {
		// do not call super.tearDown() because we want to keep the test data accross test cases
	}

	public void test1SetUp() throws CoreException {
		createFiles();
	}

	public void test2ColdContentDescription() {
		doTestContentDescription();
	}

	public void test3WarmedUpContentDescription() {
		doTestContentDescription();
	}

	public void test4TearDown() throws Exception {
		super.tearDown();
	}
}
