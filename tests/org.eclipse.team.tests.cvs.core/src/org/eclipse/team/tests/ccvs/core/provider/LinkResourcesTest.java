/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.provider;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * Test linked resources
 */
public class LinkResourcesTest extends EclipseTest {

	/**
	 * Constructor for CVSProviderTest
	 */
	public LinkResourcesTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public LinkResourcesTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(LinkResourcesTest.class);
		return new CVSTestSetup(suite);
		//return new CVSTestSetup(new WatchEditTest("testReadOnly"));
	}

	
	public void testMapSuccess() throws CoreException, TeamException {
		IProject project = getUniqueTestProject("testLinkSuccess");
		buildResources(project, new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" }, true);
		IFolder folder = project.getFolder("link");
		folder.createLink(Platform.getLocation().append("temp"), IResource.ALLOW_MISSING_LOCAL, null);
		
		// Add CVS info to the project so the map doesn't log an error
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
		cvsFolder.setFolderSyncInfo(new FolderSyncInfo("repo/root", ":pserver:name@host:/root", null, false));
		RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
	}
	
	public void testLinkSuccess() throws CoreException, TeamException {
		IProject project = createProject("testLinkFailure", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IFolder folder = project.getFolder("link");
		folder.createLink(Platform.getLocation().append("temp"), IResource.ALLOW_MISSING_LOCAL, null);
		assertIsIgnored(folder, true);
		folder = project.getFolder("folder1/folder2/");
		folder.createLink(Platform.getLocation().append("temp"), IResource.ALLOW_MISSING_LOCAL, null);
		assertIsIgnored(folder, true);
	}
	
	public void testLinkCVSFolder() throws CoreException, TeamException, IOException {
		IProject source = createProject("testLinkSource", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IProject sourceCopy = checkoutCopy(source, "copy");
		EclipseSynchronizer.getInstance().flush(source, true, DEFAULT_MONITOR);
		IProject target = createProject("testLinkTarget", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IFolder folder = target.getFolder("link");
		folder.createLink(source.getLocation(), 0, null);
		assertEquals(sourceCopy, source);
	}
}
