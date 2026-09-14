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

import static org.eclipse.core.tests.harness.PerformanceTestUtil.exercise;
import static org.eclipse.core.tests.harness.PerformanceTestUtil.reportTimings;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.resources.ContentDescriptionManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Times content type look-ups and content descriptions over a project with
 * thousands of files.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class ContentDescriptionPerformanceTest {

	private final static String DEFAULT_DESCRIPTION_FILE_NAME = "default.xml";
	private final static String NO_DESCRIPTION_FILE_NAME = "none.some-uncommon-file-extension";
	private final static String NON_DEFAULT_DESCRIPTION_FILE_NAME = "specific.xml";
	private final static String XML_CONTENT_TYPE = "org.eclipse.core.runtime.xml";
	private final static int SUBDIRS = 200;
	private final static int TOTAL_FILES = 5000;
	private final static Set<String> IGNORED_FILES = Set.of(".project", ".settings");
	private final static String VALID_XML_CONTENTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><some-uncommon-root-element/>";
	private final static String VALID_XML_CONTENTS_WITH_NON_DEFAULT_ENCODING = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?><some-uncommon-root-element/>";

	/** Extra xml content types, so a look-up visits as many candidates as in a full IDE. */
	private static final int EXTRA_CONTENT_TYPES = 300;
	private static final String EXTRA_CONTENT_TYPE_ID = ContentDescriptionPerformanceTest.class.getName() + ".type";

	private static final int WARMUP_ROUNDS = 2;
	private static final int MIN_ROUNDS = 5;
	private static final int MAX_ROUNDS = 20;
	private static final int MAX_MEASURE_TIME_MS = 10000;
	/** A single name look-up is too fast to time, so batches are timed. */
	private static final int LOOKUP_BATCH = 1000;
	private static final int LOOKUP_MAX_ROUNDS = 100;

	private IProject project;

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

	void assertHasExpectedDescription(String fileName, IContentDescription description) throws CoreException {
		if (fileName.endsWith(DEFAULT_DESCRIPTION_FILE_NAME)) {
			assertEquals(XML_CONTENT_TYPE, description.getContentType().getId(), "content type for " + fileName);
			assertEquals("UTF-8", description.getCharset(), "charset for " + fileName);
		} else if (fileName.endsWith(NON_DEFAULT_DESCRIPTION_FILE_NAME)) {
			assertEquals(XML_CONTENT_TYPE, description.getContentType().getId(), "content type for " + fileName);
			assertEquals("US-ASCII", description.getCharset(), "charset for " + fileName);
		} else {
			assertNull(description, "description for " + fileName);
		}
	}

	@BeforeAll
	static void addContentTypes() throws CoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		for (int i = 0; i < EXTRA_CONTENT_TYPES; i++) {
			IContentType type = manager.addContentType(EXTRA_CONTENT_TYPE_ID + i, "perf type " + i, null);
			type.addFileSpec("xml", IContentType.FILE_EXTENSION_SPEC);
		}
	}

	@AfterAll
	static void removeContentTypes() throws CoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		for (int i = 0; i < EXTRA_CONTENT_TYPES; i++) {
			manager.removeContentType(EXTRA_CONTENT_TYPE_ID + i);
		}
	}

	@BeforeEach
	void createFiles() throws CoreException {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("bigproject");
		assertFalse(project.exists());
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		for (int i = 0; i < SUBDIRS; i++) {
			IFolder folder = project.getFolder("folder_" + i);
			folder.create(false, true, createTestMonitor());
			for (int j = 0; j < TOTAL_FILES / SUBDIRS; j++) {
				IFile file = folder.getFile("file_" + j + getFileName(j));
				file.create(createInputStream(getContents(j)), false, createTestMonitor());
			}
		}
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

	private int describeAllFiles() throws CoreException {
		int[] described = { 0 };
		project.accept(resource -> {
			if (IGNORED_FILES.contains(resource.getName())) {
				return false;
			}
			if (resource.getType() == IResource.FILE) {
				assertHasExpectedDescription(resource.getName(), ((IFile) resource).getContentDescription());
				described[0]++;
			}
			return true;
		});
		return described[0];
	}

	private static void flushDescriptionCache() throws InterruptedException {
		((Workspace) ResourcesPlugin.getWorkspace()).getContentDescriptionManager().invalidateCache(true, null);
		Job.getJobManager().join(ContentDescriptionManager.FAMILY_DESCRIPTION_CACHE_FLUSH, null);
	}

	private void measureCold(String label) throws Exception {
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			flushDescriptionCache();
			describeAllFiles();
		}
		List<Long> times = new ArrayList<>();
		exercise(() -> {
			flushDescriptionCache();
			long before = System.nanoTime();
			int described = describeAllFiles();
			times.add(System.nanoTime() - before);
			assertEquals(TOTAL_FILES, described);
		}, MIN_ROUNDS, MAX_ROUNDS, MAX_MEASURE_TIME_MS);
		reportTimings(label + " [" + TOTAL_FILES + " files]", times);
	}

	@Test
	public void testColdContentDescription() throws Exception {
		measureCold("ContentDescription cold");
	}

	@Test
	public void testWarmContentDescription() throws Exception {
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			describeAllFiles();
		}
		List<Long> times = new ArrayList<>();
		exercise(() -> {
			long before = System.nanoTime();
			int described = describeAllFiles();
			times.add(System.nanoTime() - before);
			assertEquals(TOTAL_FILES, described);
		}, MIN_ROUNDS, MAX_ROUNDS, MAX_MEASURE_TIME_MS);
		reportTimings("ContentDescription warm [" + TOTAL_FILES + " files]", times);
	}

	private void measureNameLookups(String label) throws Exception {
		IContentTypeMatcher matcher = project.getContentTypeMatcher();
		String[] names = { DEFAULT_DESCRIPTION_FILE_NAME, NO_DESCRIPTION_FILE_NAME, "foo.txt", "plugin.xml" };
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			runLookupBatch(matcher, names);
		}
		List<Long> times = new ArrayList<>();
		exercise(() -> times.add(runLookupBatch(matcher, names)), MIN_ROUNDS, LOOKUP_MAX_ROUNDS,
				MAX_MEASURE_TIME_MS);
		reportTimings(label + " [per " + LOOKUP_BATCH + " look-ups]", times);
	}

	private static long runLookupBatch(IContentTypeMatcher matcher, String[] names) {
		long before = System.nanoTime();
		int found = 0;
		for (int i = 0; i < LOOKUP_BATCH; i++) {
			found += matcher.findContentTypesFor(names[i % names.length]).length;
		}
		long elapsed = System.nanoTime() - before;
		assertTrue(found > 0, "no content types found");
		return elapsed;
	}

	@Test
	public void testNameLookup() throws Exception {
		measureNameLookups("Project matcher name look-up");
	}

	@Test
	public void testNameLookupWithProjectSettings() throws Exception {
		enableProjectSettings();
		measureNameLookups("Project matcher name look-up, project settings");
	}

	private void enableProjectSettings() throws BackingStoreException {
		IEclipsePreferences contentTypePrefs = new ProjectScope(project)
				.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();
	}

	@Test
	public void testColdContentDescriptionWithProjectSettings() throws Exception {
		enableProjectSettings();
		measureCold("ContentDescription cold, project settings");
	}
}
