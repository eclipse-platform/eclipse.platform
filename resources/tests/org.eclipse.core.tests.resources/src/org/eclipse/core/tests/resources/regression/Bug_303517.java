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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.touchInFilesystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.InputStream;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that, when the workspace discovery a resource is out-of-sync
 * it brings the resource back into sync in a timely manner.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_303517 {

	private static final Predicate<IResource> isSynchronizedDepthInfinite = resource -> resource
			.isSynchronized(IResource.DEPTH_INFINITE);

	private static final Predicate<IResource> isSynchronizedDepthOne = resource -> resource
			.isSynchronized(IResource.DEPTH_ONE);

	private final String[] resourcePaths = new String[] { "/", "/Bug303517/", "/Bug303517/Folder/",
			"/Bug303517/Folder/Resource", };
	private boolean originalRefreshSetting;

	@BeforeEach
	public void setUp() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		prefs.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, true);
		IResource[] resources = buildResources(getWorkspace().getRoot(), resourcePaths);
		createInWorkspace(resources);
	}

	@AfterEach
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
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");

		// Touch on file-system
		f.getLocation().toFile().delete();
		// Core.resources still thinks the file exists
		assertThat(f).matches(IResource::exists, "exists");
		assertThrows(CoreException.class, () -> {
			try(InputStream in = f.getContents()) {}
		});

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// Core.resources should be aware that the file no longer exists...
		assertThat(f).matches(not(IResource::exists), "not exists");
	}

	/**
	 * Tests that file discovered out-of-sync during #getContents is updated
	 */
	@Test
	public void testGetContents() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");

		// Touch on file-system
		touchInFilesystem(f);
		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream in = f.getContents(false)) {
			}
		});
		// File is out-of-sync, so this is good.
		assertEquals(IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

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
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");

		// Touch on file-system
		touchInFilesystem(f);
		f.readAllBytes();

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is now in sync.
		f.readAllBytes();

		// Test that getContent(true) on an out-if-sync deleted file throws a CoreException
		// with IResourceStatus.RESOURCE_NOT_FOUND error code.
		f.getLocation().toFile().delete();
		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream in = f.getContents(true)) {
			}
		});
		assertEquals(IResourceStatus.RESOURCE_NOT_FOUND, exception.getStatus().getCode());
	}

	/**
	 * Tests that resource discovered out-of-sync during #isSynchronized is updated
	 */
	@Test
	public void testIsSynchronized() throws Exception {
		IFile f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");

		// Touch on file-system
		touchInFilesystem(f);
		assertThat(f).matches(not(isSynchronizedDepthOne), "is not synchronized");

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is now in sync.
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");
	}

	/**
	 * Tests that when changing resource gender is correctly picked up.
	 */
	@Test
	public void testChangeResourceGender() throws Exception {
		IResource f = getWorkspace().getRoot().getFile(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthOne, "is synchronized");

		// Replace the file with a folder
		File osResource = f.getLocation().toFile();
		osResource.delete();
		osResource.mkdir();
		assertThat(osResource).matches(File::exists, "exists");
		File osChild = new File(osResource, "child");
		osChild.createNewFile();
		assertThat(osChild).matches(File::exists, "exists");

		assertThat(f).matches(not(isSynchronizedDepthOne), "is not synchronized");

		// Wait for auto-refresh to happen
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, createTestMonitor());

		// File is no longer a file - i.e. still out-of-sync
		assertThat(f).matches(not(IResource::exists), "not exists");
		assertThat(f).matches(not(isSynchronizedDepthOne), "is not synchronized");
		// Folder + child are now in-sync
		f = getWorkspace().getRoot().getFolder(IPath.fromOSString(resourcePaths[resourcePaths.length - 1]));
		assertThat(f).matches(IResource::exists, "exists");
		assertThat(f).matches(isSynchronizedDepthInfinite, "is synchronized");
	}

}
