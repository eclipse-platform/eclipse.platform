/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui.benchmark;

import java.io.*;

import junit.framework.Test;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
import org.eclipse.team.tests.ccvs.ui.ModelParticipantSyncInfoSource;
import org.eclipse.team.tests.ccvs.ui.SubscriberParticipantSyncInfoSource;

/**
 * The test performed by this class is used to compare the performance of the old-style synchronization
 * and the model-based synchronization. It also performs an update to use as a baseline. 
 * <p>
 * The data used for the test is from bug 152581. The data used is the client/server
 * communication trace. This data is used to create and share a similar file structure.
 */
public class Bug152581Test extends BenchmarkTest {

	private static final String OLD_SYNCHRONIZE_GROUP_SUFFIX = "OldSynchronize";
	private static final String NEW_SYNCHRONIZE_GROUP_SUFFIX = "NewSynchronize";
	private static final String UPDATE_GROUP_SUFFIX = "Update";
	private static final String OLD_SYNCHRONIZE_GROUP_SUFFIX2 = "OldSynchronize2";
	private static final String NEW_SYNCHRONIZE_GROUP_SUFFIX2 = "NewSynchronize2";
	private static final String UPDATE_GROUP_SUFFIX2 = "Update2";
	private static final String[] PERFORMANCE_GROUPS = new String[] {OLD_SYNCHRONIZE_GROUP_SUFFIX, NEW_SYNCHRONIZE_GROUP_SUFFIX, UPDATE_GROUP_SUFFIX, OLD_SYNCHRONIZE_GROUP_SUFFIX2, NEW_SYNCHRONIZE_GROUP_SUFFIX2, UPDATE_GROUP_SUFFIX2};
	
	public Bug152581Test() {
		super();
	}

	public Bug152581Test(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(Bug152581Test.class);
	}
	
	private void populateProject(BufferedReader reader, IProject project) throws IOException {
		String line;
		IContainer currentDir = null;
		while((line = reader.readLine()) != null) {
			if (line.startsWith("Directory")) {
				String path = line.substring(9).trim();
				currentDir = ensureFolderExists(project, path);
			} else if (line.startsWith("Unchanged")) {
				String filename = line.substring(9).trim();
				ensureFileExists(currentDir, filename);
			}
		}
	}
	
	private void ensureFileExists(IContainer currentDir, String filename) {
		if (filename.equals(".project") && currentDir.getType() == IResource.PROJECT)
			return;
		ensureExistsInWorkspace(currentDir.getFile(new Path(null,filename)), true);
	}

	@Override
	public void ensureExistsInWorkspace(final IResource resource, final boolean local) {
		IWorkspaceRunnable body = monitor -> create(resource, local);
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			if (!resource.exists())
				fail("#ensureExistsInWorkspace(IResource): " + resource.getFullPath(), e);
		}
	}
	
	private IContainer ensureFolderExists(IProject project, String path) {
		if (path.equals("."))
			return project;
		IFolder folder = project.getFolder(path);
		ensureExistsInWorkspace(folder,true);
		return folder;
	}

	private IProject createProject(String filename) throws IOException, CoreException {
		File file = BenchmarkTestSetup.getTestFile(filename + ".txt");
		try (InputStream content = getContents(file, "Could not read seed file " + filename + ".txt")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
			IProject project = getUniqueTestProject(filename);
			populateProject(reader, project);
			shareProject(project);
			// Perform an update to prune any empty directories
			updateProject(project, null, false);
			return project;
		}
	}
	
	public void testCase1() throws IOException, CoreException {
		openEmptyPerspective();
		IProject project = createProject("bug152581case1");
		IProject project2 = createProject("bug152581case2");
		setupGroups(PERFORMANCE_GROUPS, "Sync Tests", false);
		System.out.println("Here we go");
		for (int i = 0; i < 100; i++) {
			SyncInfoSource source = new SubscriberParticipantSyncInfoSource();
			startGroup(OLD_SYNCHRONIZE_GROUP_SUFFIX);
			syncResources(source, source.createWorkspaceSubscriber(), new IResource[] { project });
			endGroup();
			startGroup(OLD_SYNCHRONIZE_GROUP_SUFFIX2);
			syncResources(source, source.createWorkspaceSubscriber(), new IResource[] { project2 });
			endGroup();
			source = new ModelParticipantSyncInfoSource();
			startGroup(NEW_SYNCHRONIZE_GROUP_SUFFIX);
			syncResources(source, source.createWorkspaceSubscriber(), new IResource[] { project });
			endGroup();
			startGroup(NEW_SYNCHRONIZE_GROUP_SUFFIX2);
			syncResources(source, source.createWorkspaceSubscriber(), new IResource[] { project2 });
			endGroup();
			startGroup(UPDATE_GROUP_SUFFIX);
			updateResources(new IResource[] { project }, false);
			endGroup();
			startGroup(UPDATE_GROUP_SUFFIX2);
			updateResources(new IResource[] { project2 }, false);
			endGroup();
			System.out.println(i + 1);
		}
		commitGroups(false);
	}
}
