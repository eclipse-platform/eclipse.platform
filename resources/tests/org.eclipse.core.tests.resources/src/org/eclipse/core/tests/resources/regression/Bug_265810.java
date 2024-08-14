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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(WorkspaceResetExtension.class)
public class Bug_265810 {

	private @TempDir Path tempDirectory;

	List<IResourceDelta> resourceDeltas = new ArrayList<>();

	public IPath createFolderAtRandomLocation() throws IOException {
		IPath path = IPath.fromPath(tempDirectory).append(createUniqueString());
		path.toFile().createNewFile();
		return path;
	}

	@Test
	public void testBug() throws Throwable {
		// create a project
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());

		// create a linked resource
		final IFile file = project.getFile(createUniqueString());
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);
		file.createLink(createFolderAtRandomLocation(), IResource.NONE, new NullProgressMonitor());
		file.setContents(createInputStream("contents for a file"), IResource.NONE, new NullProgressMonitor());

		// save the .project [1] content
		byte[] dotProject1 = storeDotProject(project);

		// create a new linked file
		final IFile newFile = project.getFile("newFile");
		// the file should not exist yet
		assertDoesNotExistInWorkspace(newFile);
		newFile.createLink(createFolderAtRandomLocation(), IResource.NONE, new NullProgressMonitor());

		// save the .project [2] content
		byte[] dotProject2 = storeDotProject(project);

		final AtomicReference<Executable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
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

			assertThat(resourceDeltas).hasSize(1);
			assertEquals(newFile, resourceDeltas.get(0).getResource());
			assertEquals(IResourceDelta.REMOVED, resourceDeltas.get(0).getKind());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		listenerInMainThreadCallback.get().execute();

		// create newFile as a non-linked resource
		newFile.create(createInputStream("content"), IResource.NONE, new NullProgressMonitor());

		try {
			resourceDeltas = new ArrayList<>();
			getWorkspace().addResourceChangeListener(listener);

			// restore .project [2]
			restoreDotProject(project, dotProject2);

			assertThat(resourceDeltas).hasSize(1);
			assertEquals(newFile, resourceDeltas.get(0).getResource());
			assertEquals(IResourceDelta.REPLACED, resourceDeltas.get(0).getFlags() & IResourceDelta.REPLACED);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		listenerInMainThreadCallback.get().execute();
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
