/*******************************************************************************
 *  Copyright (c) 2011, 2015 Broadcom Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     James Blackburn (Broadcom Corp.) - initial API and implementation
 *     Sergey Prigogin (Google) - [462440] IFile#getContents methods should specify the status codes for its exceptions
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.touchInFilesystem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that, when the workspace discovery a resource is out-of-sync
 * it brings the resource back into sync in a timely manner.
 */
public class Bug_303517 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private final String[] resourcePaths = new String[] { "/", "/Bug303517/", "/Bug303517/Folder/",
			"/Bug303517/Folder/Resource", };
	private boolean originalRefreshSetting;

	@Before
	public void setUp() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		prefs.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, true);
		IResource[] resources = buildResources(getWorkspace().getRoot(), resourcePaths);
		createInWorkspace(resources);
	}

	@After
	public void tearDown() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
		prefs.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false);
	}

	/**
	 * Tests that file deleted is updated after #getContents
	 */
	@Test
	public void testExists() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		f.getLocation().toFile().delete();
		// Core.resources still thinks the file exists
		assertTrue("1.2", f.exists());
		assertThrows(CoreException.class, () -> {
			try(InputStream in = f.getContents()) {}
		});

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// Core.resources should be aware that the file no longer exists...
		assertFalse("1.4", f.exists());
	}

	/**
	 * Tests that file discovered out-of-sync during #getContents is updated
	 */
	@Test
	public void testGetContents() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream in = f.getContents(false)) {
			}
		});
		// File is out-of-sync, so this is good.
		assertEquals("2.1", IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is now in sync.
		try (InputStream in = f.getContents(false)) {
		}
	}

	/**
	 * Tests that file discovered out-of-sync during #getContents is updated
	 */
	@Test
	public void testGetContentsTrue() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		try (InputStream in = f.getContents(true)) {
		}

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is now in sync.
		try (InputStream in = f.getContents()) {
		}

		// Test that getContent(true) on an out-if-sync deleted file throws a CoreException
		// with IResourceStatus.RESOURCE_NOT_FOUND error code.
		f.getLocation().toFile().delete();
		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream in = f.getContents(true)) {
			}
		});
		assertEquals("2.1", IResourceStatus.RESOURCE_NOT_FOUND, exception.getStatus().getCode());
	}

	/**
	 * Tests that resource discovered out-of-sync during #isSynchronized is updated
	 */
	@Test
	public void testIsSynchronized() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		assertFalse("1.2", f.isSynchronized(IResource.DEPTH_ONE));

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is now in sync.
		assertTrue("1.3", f.isSynchronized(IResource.DEPTH_ONE));
	}

	/**
	 * Tests that when changing resource gender is correctly picked up.
	 */
	@Test
	public void testChangeResourceGender() throws Exception {
		IResource f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Replace the file with a folder
		File osResource = f.getLocation().toFile();
		osResource.delete();
		osResource.mkdir();
		assertTrue(osResource.exists());
		File osChild = new File(osResource, "child");
		osChild.createNewFile();
		assertTrue(osChild.exists());

		assertFalse("1.2", f.isSynchronized(IResource.DEPTH_ONE));

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is no longer a file - i.e. still out-of-sync
		assertFalse("1.3", f.exists());
		assertFalse("1.4", f.isSynchronized(IResource.DEPTH_ONE));
		// Folder + child are now in-sync
		f = getWorkspace().getRoot().getFolder(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertTrue("1.5", f.exists());
		assertTrue("1.6", f.isSynchronized(IResource.DEPTH_INFINITE));
	}

}
