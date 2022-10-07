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

import java.io.File;
import java.io.InputStream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests that, when the workspace discovery a resource is out-of-sync
 * it brings the resource back into sync in a timely manner.
 */
public class Bug_303517 extends ResourceTest {

	String[] resources = new String[] {"/", "/Bug303517/", "/Bug303517/Folder/", "/Bug303517/Folder/Resource",};
	private boolean originalRefreshSetting;

	@Override
	public String[] defineHierarchy() {
		return resources;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		prefs.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, true);
	}

	@Override
	protected void tearDown() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
		prefs.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false);
		super.tearDown();
	}

	/**
	 * Tests that file deleted is updated after #getContents
	 */
	public void testExists() throws Exception {
		createHierarchy();
		IFile f = getWorkspace().getRoot().getFile(new Path(resources[resources.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		f.getLocation().toFile().delete();
		// Core.resources still thinks the file exists
		assertTrue("1.2", f.exists());
		try {
			InputStream in = f.getContents();
			in.close();
			assertTrue("1.3", false);
		} catch (CoreException e) {
			// File doesn't exist - expected
		}

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());

		// Core.resources should be aware that the file no longer exists...
		assertFalse("1.4", f.exists());
	}

	/**
	 * Tests that file discovered out-of-sync during #getContents is updated
	 */
	public void testGetContents() throws Exception {
		createHierarchy();
		IFile f = getWorkspace().getRoot().getFile(new Path(resources[resources.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		try {
			InputStream in = f.getContents(false);
			in.close();
			assertTrue("2.0", false);
		} catch (CoreException e) {
			// File is out-of-sync, so this is good.
			assertEquals("2.1", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());

		// File is now in sync.
		try {
			InputStream in = f.getContents(false);
			in.close();
		} catch (CoreException e) {
			// Bad, file shouldn't be out-of-sync
			fail("3.0", e);
		}
	}

	/**
	 * Tests that file discovered out-of-sync during #getContents is updated
	 */
	public void testGetContentsTrue() throws Exception {
		createHierarchy();
		IFile f = getWorkspace().getRoot().getFile(new Path(resources[resources.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		try (InputStream in = f.getContents(true)) {
		} catch (CoreException e) {
			// Bad, getContents(true) should succeed.
			fail("1.2", e);
		}

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());

		// File is now in sync.
		try (InputStream in = f.getContents()) {
		} catch (CoreException e) {
			// Bad, file shouldn't be out-of-sync.
			fail("1.3", e);
		}

		// Test that getContent(true) on an out-if-sync deleted file throws a CoreException
		// with IResourceStatus.RESOURCE_NOT_FOUND error code.
		f.getLocation().toFile().delete();
		try (InputStream in = f.getContents(true)) {
			fail("2.0");
		} catch (CoreException e) {
			// Expected.
			assertEquals("2.1", IResourceStatus.RESOURCE_NOT_FOUND, e.getStatus().getCode());
		}
	}

	/**
	 * Tests that resource discovered out-of-sync during #isSynchronized is updated
	 */
	public void testIsSynchronized() throws Exception {
		createHierarchy();
		IFile f = getWorkspace().getRoot().getFile(new Path(resources[resources.length - 1]));
		assertTrue("1.0", f.exists());
		assertTrue("1.1", f.isSynchronized(IResource.DEPTH_ONE));

		// Touch on file-system
		touchInFilesystem(f);
		assertFalse("1.2", f.isSynchronized(IResource.DEPTH_ONE));

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());

		// File is now in sync.
		assertTrue("1.3", f.isSynchronized(IResource.DEPTH_ONE));
	}

	/**
	 * Tests that when changing resource gender is correctly picked up.
	 */
	public void testChangeResourceGender() throws Exception {
		createHierarchy();
		IResource f = getWorkspace().getRoot().getFile(new Path(resources[resources.length - 1]));
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
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());

		// File is no longer a file - i.e. still out-of-sync
		assertFalse("1.3", f.exists());
		assertFalse("1.4", f.isSynchronized(IResource.DEPTH_ONE));
		// Folder + child are now in-sync
		f = getWorkspace().getRoot().getFolder(new Path(resources[resources.length - 1]));
		assertTrue("1.5", f.exists());
		assertTrue("1.6", f.isSynchronized(IResource.DEPTH_INFINITE));
	}
}
