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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isAttributeSupported;
import static org.junit.Assert.assertThrows;

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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.function.ThrowingRunnable;

public class IResourceTest extends ResourceTest {

	private final boolean DISABLED = true;

	/**
	 * 1G9RBH5: ITPCORE:WIN98 - IFile.appendContents might lose data
	 */
	public void testAppendContents_1G9RBH5() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		IFile target = project.getFile("file1");
		target.create(getContents("abc"), false, null);
		target.appendContents(getContents("def"), false, true, null);

		InputStream content = target.getContents(false);
		assertTrue("3.0", compareContent(content, getContents("abcdef")));
	}

	/**
	 * Bug states that JDT cannot copy the .project file from the project root to
	 * the build output folder.
	 */
	public void testBug25686() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder outputFolder = project.getFolder("bin");
		IFile description = project.getFile(".project");
		IFile destination = outputFolder.getFile(".project");
		createInWorkspace(new IResource[] {project, outputFolder});

		assertTrue("0.0", description.exists());
		description.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue("0.1", destination.exists());
	}

	public void testBug28790() throws CoreException {
		// only activate this test on platforms that support it
		if (!isAttributeSupported(EFS.ATTRIBUTE_ARCHIVE)) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("a.txt");
		createInWorkspace(file, getRandomString());
		// ensure archive bit is not set
		ResourceAttributes attributes = file.getResourceAttributes();
		attributes.setArchive(false);
		file.setResourceAttributes(attributes);
		assertTrue("1.0", !file.getResourceAttributes().isArchive());
		// modify the file
		file.setContents(getRandomContents(), IResource.KEEP_HISTORY, createTestMonitor());

		//now the archive bit should be set
		assertTrue("2.0", file.getResourceAttributes().isArchive());
	}

	/**
	 * Bug 31750 states that an OperationCanceledException is
	 * not handled correctly if it occurs within a proxy visitor.
	 */
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

		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
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
			assertTrue("1.0", !seen[0]);
			assertTrue("1.0", phantomSeen[0]);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	/**
	 * Calling isSynchronized on a non-local resource caused an internal error.
	 */
	public void testBug83777() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("testBug83777");
		IFolder folder = project.getFolder("f");
		createInWorkspace(project);
		createInWorkspace(folder);
		folder.setLocal(false, IResource.DEPTH_ZERO, createTestMonitor());
		// non-local resource is never synchronized because it doesn't exist on disk
		assertTrue("1.0", !project.isSynchronized(IResource.DEPTH_INFINITE));
	}

	public void testBug111821() throws CoreException {
		//this test only makes sense on Windows
		if (!OS.isWindows()) {
			return;
		}
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
	public void testCopy_1GA6QJP() throws CoreException, InterruptedException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile source = project.getFile("file1");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		source.create(getContents("abc"), true, createTestMonitor());

		Thread.sleep(2000);

		IPath destinationPath = IPath.fromOSString("copy of file");
		source.copy(destinationPath, true, createTestMonitor());

		IFile destination = project.getFile(destinationPath);
		long expected = source.getLocation().toFile().lastModified();
		long actual = destination.getLocation().toFile().lastModified();
		// java.io.File.lastModified() has only second accuracy on some OSes
		long difference = Math.abs(expected - actual);
		assertTrue("time difference>1000ms: " + difference, difference <= 1000);
	}

	/**
	 * 1FW87XF: ITPUI:WIN2000 - Can create 2 files with same name
	 */
	public void testCreate_1FW87XF() throws Throwable {
		// FIXME: remove when fix this PR
		String os = Platform.getOS();
		if (!os.equals(Platform.OS_LINUX)) {
			return;
		}

		// test if the file system is case sensitive
		boolean caseSensitive = new java.io.File("abc").compareTo(new java.io.File("ABC")) != 0;

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("file");
		project.create(null);
		project.open(null);
		file.create(getRandomContents(), true, null);

		// force = true
		assertTrue("2.0", file.exists());
		IFile anotherFile = project.getFile("File");

		ThrowingRunnable forcedFileCreation = () -> anotherFile.create(getRandomContents(), true, null);
		if (caseSensitive) {
			forcedFileCreation.run();
		} else {
			assertThrows(CoreException.class, forcedFileCreation);
		}

		// clean-up
		anotherFile.delete(true, false, null);

		// force = false
		ThrowingRunnable fileCreation = () -> anotherFile.create(getRandomContents(), false, null);
		if (caseSensitive) {
			fileCreation.run();
		} else {
			assertThrows(CoreException.class, fileCreation);
		}

		// test refreshLocal
		ThrowingRunnable refresh = () -> anotherFile.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		if (caseSensitive) {
			refresh.run();
		} else {
			assertThrows(CoreException.class, refresh);
		}
	}

	/**
	 * 1FWYTKT: ITPCORE:WINNT - Error creating folder with long name
	 */
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
		assertTrue("2.2", !folder.exists());

		IFile file = project.getFile(sb.toString());
		assertThrows(CoreException.class, () -> file.create(getRandomContents(), true, null));
		assertTrue("3.1", !file.exists());

		// clean up
		project.delete(true, true, null);

		IProject finalProject = project = getWorkspace().getRoot().getProject(sb.toString());
		assertThrows(CoreException.class, () -> finalProject.create(null));
		assertTrue("4.1", !finalProject.exists());
	}

	/**
	 * 1GD7CSU: ITPCORE:ALL - IFile.create bug?
	 *
	 * Ensure that creating a file with force==true doesn't throw
	 * a CoreException if the resource already exists on disk.
	 */
	public void testCreate_1GD7CSU() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		IFile file = project.getFile("MyFile");
		createInFileSystem(file);

		file.create(getRandomContents(), true, createTestMonitor());
	}

	/*
	 * Test PR: 1GD3ZUZ. Ensure that a CoreException is being thrown
	 * when we try to delete a read-only resource. It will depend on the
	 * OS and file system.
	 */
	public void testDelete_1GD3ZUZ() throws CoreException {
		// This test cannot be done automatically because we don't know in that
		// file system we are running. Will leave test here in case it needs
		// to be run it in a special environment.
		if (DISABLED) {
			return;
		}

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("MyFile");

		// setup
		createInWorkspace(new IResource[] {project, file});
		ResourceAttributes attributes = file.getResourceAttributes();
		attributes.setReadOnly(true);
		file.setResourceAttributes(attributes);
		assertTrue("2.0", file.isReadOnly());

		// doit
		assertThrows(CoreException.class, () -> file.delete(false, createTestMonitor()));

		// cleanup
		attributes = file.getResourceAttributes();
		attributes.setReadOnly(false);
		file.setResourceAttributes(attributes);
		assertTrue("4.0", !file.isReadOnly());
		removeFromWorkspace(new IResource[] {project, file});
	}

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
			assertEquals("1.1", 1, children.length);
			status = children[0];
		}
		assertEquals("1.2", IResourceStatus.OUT_OF_SYNC_LOCAL, status.getCode());
		//cleanup
		removeFromWorkspace(new IResource[] {project, file});
	}

	public void testEquals_1FUOU25() {
		IResource fileResource = getWorkspace().getRoot().getFile(IPath.fromOSString("a/b/c/d"));
		IResource folderResource = getWorkspace().getRoot().getFolder(IPath.fromOSString("a/b/c/d"));
		assertTrue("1FUOU25: ITPCORE:ALL - Bug in Resource.equals()", !fileResource.equals(folderResource));
	}

	public void testExists_1FUP8U6() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		project.create(null);
		project.open(null);
		folder.create(true, true, null);
		IFile file = project.getFile("folder");
		assertTrue("2.0", !file.exists());
	}

	/**
	 * 1GA6QYV: ITPCORE:ALL - IContainer.findMember( Path, boolean ) breaking API
	 */
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
		assertTrue("3.0", folder3.equals(target));

		targetPath = IPath.fromOSString("/Folder2/Folder3");
		target = (IFolder) folder1.findMember(targetPath);
		assertTrue("4.0", folder3.equals(target));
	}

	/**
	 * 1GBZD4S: ITPCORE:API - IFile.getContents(true) fails if performed during delta notification
	 */
	public void testGetContents_1GBZD4S() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		final IFile target = project.getFile("file1");
		String contents = "some random contents";
		target.create(getContents(contents), false, null);

		try (InputStream is = target.getContents(false)) {
			assertTrue("2.0", compareContent(getContents(contents), is));
		}

		final String newContents = "some other contents";
		Thread.sleep(5000);
		try (FileOutputStream output = new FileOutputStream(target.getLocation().toFile())) {
			getContents(newContents).transferTo(output);
		}

		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(() -> {
		});
		IResourceChangeListener listener = event -> {
			listenerInMainThreadCallback.set(() -> {
				try (InputStream is = target.getContents(true)) {
					assertTrue("4.0", compareContent(getContents(newContents), is));
				}
			});
		};
		try {
			getWorkspace().addResourceChangeListener(listener);
			// trigger delta notification
			project.touch(null);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();

		CoreException exception = assertThrows(CoreException.class, () -> {
			try (InputStream is = target.getContents(false)) {
			}
		});
		assertEquals("5.1", IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		try (InputStream is = target.getContents(true)) {
			assertTrue("6.0", compareContent(getContents(newContents), is));
		}
	}

	/**
	 * 1G60AFG: ITPCORE:WIN - problem calling RefreshLocal with DEPTH_ZERO on folder
	 */
	public void testRefreshLocal_1G60AFG() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		project.create(null);
		project.open(null);
		folder.create(true, true, null);
		file.create(getRandomContents(), true, null);
		assertTrue("2.0", file.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue("2.2", file.exists());
	}

	/**
	 * 553269: Eclipse sends unexpected ENCODING change after closing/opening
	 * project with explicit encoding settings changed in the same session
	 */
	public void testBug553269() throws Exception {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder settingsFolder = project.getFolder(".settings");
		IFile settingsFile = settingsFolder.getFile("org.eclipse.core.resources.prefs");
		project.create(null);
		project.open(null);
		project.setDefaultCharset(StandardCharsets.UTF_8.name(), null);

		assertTrue("Preferences saved", settingsFile.exists());

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
			assertTrue(verifier.getMessage(), verifier.isDeltaValid());
		} finally {
			workspace.removeResourceChangeListener(verifier);
		}
	}
}
