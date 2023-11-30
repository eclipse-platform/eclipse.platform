/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.function.ThrowingRunnable;

public class Bug_265810 extends ResourceTest {

	protected final static String VARIABLE_NAME = "ROOT";
	List<IResourceDelta> resourceDeltas = new ArrayList<>();

	public IPath createFolderAtRandomLocation() throws IOException {
		IPath path = getRandomLocation();
		path.toFile().createNewFile();
		deleteOnTearDown(path);
		return path;
	}

	public void testBug() throws Throwable {
		// create a project
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());

		// create a linked resource
		final IFile file = project.getFile(getUniqueString());
		// the file should not exist yet
		assertDoesNotExistInWorkspace("2.0", file);
		file.createLink(createFolderAtRandomLocation(), IResource.NONE, new NullProgressMonitor());
		file.setContents(getContents("contents for a file"), IResource.NONE, new NullProgressMonitor());

		// save the .project [1] content
		byte[] dotProject1 = storeDotProject(project);

		// create a new linked file
		final IFile newFile = project.getFile("newFile");
		// the file should not exist yet
		assertDoesNotExistInWorkspace("5.0", newFile);
		newFile.createLink(createFolderAtRandomLocation(), IResource.NONE, new NullProgressMonitor());

		// save the .project [2] content
		byte[] dotProject2 = storeDotProject(project);

		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
		});

		IResourceChangeListener listener = event -> {
			try {
				event.getDelta().accept(delta -> {
					IResource resource = delta.getResource();
					if (resource instanceof IFile && !resource.getName().equals(".project")) {
						addToResourceDelta(delta);
					}
					if (delta.getAffectedChildren().length > 0) {
						return true;
					}
					return false;
				});
			} catch (CoreException e) {
				listenerInMainThreadCallback.set(() -> {
					throw e;
				});
			}
		};

		try {
			resourceDeltas = new ArrayList<>();
			getWorkspace().addResourceChangeListener(listener);

			// restore .project [1]
			restoreDotProject(project, dotProject1);

			assertEquals("9.0", 1, resourceDeltas.size());
			assertEquals("9.1", newFile, resourceDeltas.get(0).getResource());
			assertEquals("9.2", IResourceDelta.REMOVED, resourceDeltas.get(0).getKind());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		listenerInMainThreadCallback.get().run();

		// create newFile as a non-linked resource
		newFile.create(getContents("content"), IResource.NONE, new NullProgressMonitor());

		try {
			resourceDeltas = new ArrayList<>();
			getWorkspace().addResourceChangeListener(listener);

			// restore .project [2]
			restoreDotProject(project, dotProject2);

			assertEquals("11.0", 1, resourceDeltas.size());
			assertEquals("11.1", newFile, resourceDeltas.get(0).getResource());
			assertEquals("11.2", IResourceDelta.REPLACED, resourceDeltas.get(0).getFlags() & IResourceDelta.REPLACED);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		listenerInMainThreadCallback.get().run();
	}

	private byte[] storeDotProject(IProject project) throws Exception {
		byte[] buffer = new byte[2048];
		int bytesRead = 0;
		byte[] doProject = new byte[0];

		try (InputStream iS = project.getFile(".project").getContents()) {
			bytesRead = iS.read(buffer);
		}

		doProject = new byte[bytesRead];
		System.arraycopy(buffer, 0, doProject, 0, bytesRead);

		return doProject;
	}

	private void restoreDotProject(IProject project, byte[] dotProject) throws CoreException {
		project.getFile(".project").setContents(new ByteArrayInputStream(dotProject), IResource.NONE,
				new NullProgressMonitor());
	}

	boolean addToResourceDelta(IResourceDelta delta) {
		return resourceDeltas.add(delta);
	}
}
