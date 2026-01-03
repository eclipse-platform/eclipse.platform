/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - [462440] IFile#getContents methods should specify the status codes for its exceptions
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isAttributeSupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

@ExtendWith(WorkspaceResetExtension.class)
public class IResourceTest {

	/**
	 * 1G9RBH5: ITPCORE:WIN98 - IFile.appendContents might lose data
	 */
	@Test
	public void testAppendContents_1G9RBH5() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		IFile target = project.getFile("file1");
		target.create(createInputStream("abc"), false, null);
		target.appendContents(createInputStream("def"), false, true, null);

		try (InputStream content = target.getContents(false)) {
			assertThat(content).hasContent("abcdef");
		}
	}

	/**
	 * Bug states that JDT cannot copy the .project file from the project root to
	 * the build output folder.
	 */
	@Test
	public void testBug25686() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder outputFolder = project.getFolder("bin");
		IFile description = project.getFile(".project");
		IFile destination = outputFolder.getFile(".project");
		createInWorkspace(new IResource[] {project, outputFolder});

		assertThat(description).matches(IFile::exists, "exists");
		description.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertThat(destination).matches(IFile::exists, "exists");
	}

	@Test
	public void testBug28790() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_ARCHIVE),
				"only relevant for platforms supporting archive attribute");

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("a.txt");
		createInWorkspace(file, createRandomString());
		// ensure archive bit is not set
		ResourceAttributes attributes = file.getResourceAttributes();
		attributes.setArchive(false);
		file.setResourceAttributes(attributes);
		assertThat(file).matches(it -> !it.getResourceAttributes().isArchive(), "is archive");
		// modify the file
		file.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());

		//now the archive bit should be set
		assertThat(file).matches(it -> it.getResourceAttributes().isArchive(), "is archive");
	}

	/**
	 * Bug 31750 states that an OperationCanceledException is
	 * not handled correctly if it occurs within a proxy visitor.
	 */
	@Test
	public void testBug31750() {
		IResourceProxyVisitor visitor = proxy -> {
			throw new OperationCanceledException();
		};
		assertThrows(OperationCanceledException.class, () -> getWorkspace().getRoot().accept(visitor, IResource.NONE));
	}

	/**
	 * A resource that is deleted, recreated, and converted to a phantom
	 * all in one operation should not appear in the resource delta for
	 * clients that are not interested in phantoms.
	 */
	@Test
	public void testBug35991() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		final IFile file = project.getFile("file1");
		createInWorkspace(project);
		//create phantom file by adding sync info
		final QualifiedName name = new QualifiedName("test", "testBug35991");
		getWorkspace().getSynchronizer().add(name);
		getWorkspace().getSynchronizer().setSyncInfo(name, file, new byte[] { 1 });
		final boolean[] seen = new boolean[] {false};
		final boolean[] phantomSeen = new boolean[] {false};
		class DeltaVisitor implements IResourceDeltaVisitor {
			private final boolean[] mySeen;

			DeltaVisitor(boolean[] mySeen) {
				this.mySeen = mySeen;
			}

			@Override
			public boolean visit(IResourceDelta aDelta) {
				if (aDelta.getResource().equals(file)) {
					mySeen[0] = true;
				}
				return true;
			}
		}

		final AtomicReference<Executable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
		});
		IResourceChangeListener listener = event -> {
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			try {
				delta.accept(new DeltaVisitor(seen));
				delta.accept(new DeltaVisitor(phantomSeen), true);
			} catch (CoreException e) {
				listenerInMainThreadCallback.set(() -> {
					throw e;
				});
			}
		};
		try {
			getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);

			// removing and adding sync info causes phantom to be deleted and recreated
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				ISynchronizer synchronizer = getWorkspace().getSynchronizer();
				synchronizer.flushSyncInfo(name, file, IResource.DEPTH_INFINITE);
				synchronizer.setSyncInfo(name, file, new byte[] { 1 });
			}, null, IWorkspace.AVOID_UPDATE, createTestMonitor());
			// ensure file was only seen by phantom listener
			assertFalse(seen[0]);
			assertTrue(phantomSeen[0]);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().execute();
	}

	/**
	 * Calling isSynchronized on a non-local resource caused an internal error.
	 */
	@Test
	@Deprecated // Explicitly tests deprecated API
	public void testBug83777() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("testBug83777");
		IFolder folder = project.getFolder("f");
		createInWorkspace(project);
		createInWorkspace(folder);
		folder.setLocal(false, IResource.DEPTH_ZERO, createTestMonitor());
		// non-local resource is never synchronized because it doesn't exist on disk
		assertThat(project).matches(it -> !it.isSynchronized(IResource.DEPTH_INFINITE), "is synchronized");
	}

	@Test
	public void testBug111821() throws CoreException {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IProject project = getWorkspace().getRoot().getProject("testBug111821");
		IFolder folder = project.getFolder(new Path(null, "c:"));
		createInWorkspace(project);
		QualifiedName partner = new QualifiedName("HowdyThere", "Partner");
		ISynchronizer sync = getWorkspace().getSynchronizer();
		sync.add(partner);
		assertThrows(CoreException.class, () -> sync.setSyncInfo(partner, folder, new byte[] { 1 }));
	}

	/**
	 * 1GA6QJP: ITPCORE:ALL - Copying a resource does not copy its lastmodified time
	 */
	@Test
	public void testCopy_1GA6QJP() throws CoreException, InterruptedException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile source = project.getFile("file1");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		source.create(createInputStream("abc"), true, createTestMonitor());

		Thread.sleep(2000);

		IPath destinationPath = IPath.fromOSString("copy of file");
		source.copy(destinationPath, true, createTestMonitor());

		IFile destination = project.getFile(destinationPath);
		long expected = source.getLocation().toFile().lastModified();
		long actual = destination.getLocation().toFile().lastModified();
		// java.io.File.lastModified() has only second accuracy on some OSes
		long difference = Math.abs(expected - actual);
		assertTrue(difference <= 1000, "time difference>1000ms: " + difference);
	}

	/**
	 * 1FW87XF: ITPUI:WIN2000 - Can create 2 files with same name
	 */
	@Test
	public void testCreate_1FW87XF() throws Throwable {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		// test if the file system is case sensitive
		boolean caseSensitive = new java.io.File("abc").compareTo(new java.io.File("ABC")) != 0;

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("file");
		project.create(null);
		project.open(null);
		file.create(createRandomContentsStream(), true, null);

		// force = true
		assertThat(file).matches(IFile::exists, "exists");
		IFile anotherFile = project.getFile("File");

		Executable forcedFileCreation = () -> anotherFile.create(createRandomContentsStream(), true, null);
		if (caseSensitive) {
			forcedFileCreation.execute();
		} else {
			assertThrows(CoreException.class, forcedFileCreation);
		}

		// clean-up
		anotherFile.delete(true, false, null);

		// force = false
		Executable fileCreation = () -> anotherFile.create(createRandomContentsStream(), false, null);
		if (caseSensitive) {
			fileCreation.execute();
		} else {
			assertThrows(CoreException.class, fileCreation);
		}

		// test refreshLocal
		Executable refresh = () -> anotherFile.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		if (caseSensitive) {
			refresh.execute();
		} else {
			assertThrows(CoreException.class, refresh);
		}
	}

	/**
	 * 1FWYTKT: ITPCORE:WINNT - Error creating folder with long name
	 */
	@Test
	public void testCreate_1FWYTKT() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 260; i++) {
			sb.append('a');
		}
		sb.append('b');
		IFolder folder = project.getFolder(sb.toString());
		assertThrows(CoreException.class, () -> folder.create(true, true, null));
		assertThat(folder).matches(not(IFolder::exists), "not exists");

		IFile file = project.getFile(sb.toString());
		assertThrows(CoreException.class, () -> file.create(createRandomContentsStream(), true, null));
		assertThat(file).matches(not(IFile::exists), "not exists");

		// clean up
		project.delete(true, true, null);

		IProject finalProject = project = getWorkspace().getRoot().getProject(sb.toString());
		assertThrows(CoreException.class, () -> finalProject.create(null));
		assertThat(finalProject).matches(not(IProject::exists), "not exists");
	}

	/**
	 * 1GD7CSU: ITPCORE:ALL - IFile.create bug?
	 *
	 * Ensure that creating a file with force==true doesn't throw
	 * a CoreException if the resource already exists on disk.
	 */
	@Test
	public void testCreate_1GD7CSU() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		IFile file = project.getFile("MyFile");
		createInFileSystem(file);

		file.create(createRandomContentsStream(), true, createTestMonitor());
	}

	/*
	 * Test PR: 1GD3ZUZ. Ensure that a CoreException is being thrown
	 * when we try to delete a read-only resource. It will depend on the
	 * OS and file system.
	 */
	@Test
	@Disabled("This test cannot be done automatically because we don't know in that file system we are running. Will leave test here in case it needs to be run it in a special environment.")
	public void testDelete_1GD3ZUZ() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("MyFile");

		// setup
		createInWorkspace(new IResource[] {project, file});
		ResourceAttributes attributes = file.getResourceAttributes();
		attributes.setReadOnly(true);
		file.setResourceAttributes(attributes);
		assertThat(file).matches(IFile::isReadOnly, "is read-only");

		// doit
		assertThrows(CoreException.class, () -> file.delete(false, createTestMonitor()));

		// cleanup
		attributes = file.getResourceAttributes();
		attributes.setReadOnly(false);
		file.setResourceAttributes(attributes);
		assertThat(file).matches(not(IFile::isReadOnly), "is not read-only");
		removeFromWorkspace(new IResource[] {project, file});
	}

	@Test
	public void testDelete_Bug8754() throws Exception {
		//In this test, we delete with force false on a file that does not exist in the file system,
		//and ensure that the returned exception is of type OUT_OF_SYNC_LOCAL

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("MyFile");

		// setup
		createInWorkspace(new IResource[] {project, file});
		ensureOutOfSync(file);

		// doit
		CoreException exception = assertThrows(CoreException.class, () -> file.delete(false, createTestMonitor()));
		IStatus status = exception.getStatus();
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			assertThat(children).hasSize(1);
			status = children[0];
		}
		assertEquals(IResourceStatus.OUT_OF_SYNC_LOCAL, status.getCode());
		//cleanup
		removeFromWorkspace(new IResource[] {project, file});
	}

	@Test
	public void testEquals_1FUOU25() {
		IResource fileResource = getWorkspace().getRoot().getFile(IPath.fromOSString("a/b/c/d"));
		IResource folderResource = getWorkspace().getRoot().getFolder(IPath.fromOSString("a/b/c/d"));
		assertNotEquals(fileResource, folderResource);
	}

	@Test
	public void testExists_1FUP8U6() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		project.create(null);
		project.open(null);
		folder.create(true, true, null);
		IFile file = project.getFile("folder");
		assertThat(file).matches(not(IFile::exists), "not exists");
	}

	/**
	 * 1GA6QYV: ITPCORE:ALL - IContainer.findMember( Path, boolean ) breaking API
	 */
	@Test
	public void testFindMember_1GA6QYV() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFolder folder1 = project.getFolder("Folder1");
		IFolder folder2 = folder1.getFolder("Folder2");
		IFolder folder3 = folder2.getFolder("Folder3");
		folder1.create(true, true, createTestMonitor());
		folder2.create(true, true, createTestMonitor());
		folder3.create(true, true, createTestMonitor());

		IPath targetPath = IPath.fromOSString("Folder2/Folder3");
		IFolder target = (IFolder) folder1.findMember(targetPath);
		assertEquals(target, folder3);

		targetPath = IPath.fromOSString("/Folder2/Folder3");
		target = (IFolder) folder1.findMember(targetPath);
		assertEquals(target, folder3);
	}

	/**
	 * 1GBZD4S: ITPCORE:API - IFile.getContents(true) fails if performed during delta notification
	 */
	@Test
	public void testGetContents_1GBZD4S() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		final IFile target = project.getFile("file1");
		String contents = "some random contents";
		target.create(createInputStream(contents), false, null);

		try (InputStream is = target.getContents(false)) {
			assertThat(is).hasContent(contents);
		}

		final String newContents = "some other contents";
		Thread.sleep(5000);
		try (FileOutputStream output = new FileOutputStream(target.getLocation().toFile())) {
			createInputStream(newContents).transferTo(output);
		}

		final AtomicReference<Executable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
		});
		IResourceChangeListener listener = event -> {
			listenerInMainThreadCallback.set(() -> {
				assertEquals(newContents, target.readString());
			});
		};
		try {
			getWorkspace().addResourceChangeListener(listener);
			// trigger delta notification
			project.touch(null);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().execute();

		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream is = target.getContents(false)) {
			}
		});
		assertEquals(IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		try (InputStream is = target.getContents(true)) {
			assertThat(is).hasContent(newContents);
		}
	}

	/**
	 * 1G60AFG: ITPCORE:WIN - problem calling RefreshLocal with DEPTH_ZERO on folder
	 */
	@Test
	public void testRefreshLocal_1G60AFG() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		project.create(null);
		project.open(null);
		folder.create(true, true, null);
		file.create(createRandomContentsStream(), true, null);

		assertThat(file).matches(IFile::exists, "exists");
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertThat(file).matches(IFile::exists, "exists");
	}

	/**
	 * 553269: Eclipse sends unexpected ENCODING change after closing/opening
	 * project with explicit encoding settings changed in the same session
	 */
	@Test
	public void testBug553269() throws Exception {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder settingsFolder = project.getFolder(".settings");
		IFile settingsFile = settingsFolder.getFile("org.eclipse.core.resources.prefs");
		project.create(null);
		project.open(null);
		project.setDefaultCharset(StandardCharsets.UTF_8.name(), null);

		assertThat(settingsFile.exists()).withFailMessage("Preferences saved").isTrue();

		project.close(null);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		try {
			workspace.addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);
			// We expect only OPEN change, the original code generated
			// IResourceDelta.OPEN | IResourceDelta.ENCODING
			verifier.addExpectedChange(project, IResourceDelta.CHANGED, IResourceDelta.OPEN);

			// This is irrelevant for the test but verifier verifies entire delta...
			verifier.addExpectedChange(settingsFolder, IResourceDelta.ADDED, 0);
			verifier.addExpectedChange(settingsFile, IResourceDelta.ADDED, 0);
			verifier.addExpectedChange(project.getFile(".project"), IResourceDelta.ADDED, 0);

			project.open(null);
			assertThat(verifier.isDeltaValid()).withFailMessage(verifier.getMessage()).isTrue();
		} finally {
			workspace.removeResourceChangeListener(verifier);
		}
	}

}
