/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.SyncInfoReader;
import org.eclipse.core.internal.resources.Synchronizer;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ISynchronizerTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	public static int NUMBER_OF_PARTNERS = 100;
	public IResource[] resources;

	/*
	 * Internal method used for flushing all sync information for a particular resource
	 * and its children.
	 */
	protected void flushAllSyncInfo(final IResource root) throws CoreException {
		assertNotNull(root);

		final ISynchronizer synchronizer = getWorkspace().getSynchronizer();
		final QualifiedName[] partners = synchronizer.getPartners();
		IWorkspaceRunnable body = monitor -> {
			IResourceVisitor visitor = resource -> {
				for (QualifiedName partner : partners) {
					synchronizer.setSyncInfo(partner, resource, null);
				}
				return true;
			};
			root.accept(visitor, IResource.DEPTH_INFINITE, true);
		};
		getWorkspace().run(body, null);
	}

	@Before
	public void setUp() throws Exception {
		resources = buildResources(getWorkspace().getRoot(),
				new String[] { "/", "1/", "1/1", "1/2/", "1/2/1", "1/2/2/", "2/", "2/1", "2/2/", "2/2/1", "2/2/2/" });
		createInWorkspace(resources);
	}

	@After
	public void tearDown() throws Exception {
		// remove all registered sync partners so we don't create
		// phantoms when we delete
		QualifiedName[] names = getWorkspace().getSynchronizer().getPartners();
		for (QualifiedName name : names) {
			getWorkspace().getSynchronizer().remove(name);
		}
	}

	private void assertExpectedSyncInfo(IResource resource, byte[] actualSyncInfo, byte[] expectedSyncInfo) {
		if (resource.getType() == IResource.ROOT) {
			assertThat("Sync info for root resource should be null: " + resource.getFullPath(), actualSyncInfo,
					nullValue());
			return;
		}
		assertThat("Sync info for non-root resource should not be null: " + resource.getFullPath(), actualSyncInfo,
				notNullValue());
		assertThat("Unexpected sync info for resource: " + resource.getFullPath(), expectedSyncInfo,
				is(actualSyncInfo));
	}

	@Test
	public void testDeleteResources() throws CoreException {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = createRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			synchronizer.setSyncInfo(qname, resource, b);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// get the info and ensure its the same
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// delete all resources under the projects.
		final IProject[] projects = getWorkspace().getRoot().getProjects();
		IWorkspaceRunnable body = monitor -> {
			for (IProject project : projects) {
				IResource[] children = project.members();
				for (IResource element : children) {
					element.delete(false, createTestMonitor());
				}
			}
		};
		getWorkspace().run(body, createTestMonitor());

		// sync info should remain for the resources since they are now phantoms
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// delete the projects
		for (IProject project : projects) {
			project.delete(false, createTestMonitor());
		}

		// sync info should be gone since projects can't become phantoms
		visitor = resource -> {
			assertNull(resource.getFullPath().toString(), synchronizer.getSyncInfo(qname, resource));
			return true;
		};
		getWorkspace().getRoot().accept(visitor);
	}

	@Test
	public void testDeleteResources2() throws CoreException {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = createRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			synchronizer.setSyncInfo(qname, resource, b);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// get the info and ensure its the same
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// delete all resources under the projects.
		final IProject[] projects = getWorkspace().getRoot().getProjects();
		IWorkspaceRunnable body = monitor -> {
			for (IProject project : projects) {
				for (IResource element : project.members()) {
					if (!element.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
						element.delete(false, createTestMonitor());
					}
				}
			}
		};
		getWorkspace().run(body, createTestMonitor());

		// sync info should remain for the resources since they are now phantoms
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// remove the sync info for the immediate children of the projects.
		body = monitor -> {
			for (IProject project : projects) {
				for (IResource element : project.members(true)) {
					synchronizer.setSyncInfo(qname, element, null);
				}
			}
		};
		getWorkspace().run(body, createTestMonitor());

		// there should be no sync info for any resources except the project
		visitor = resource -> {
			int type = resource.getType();
			if (type == IResource.ROOT || type == IResource.PROJECT) {
				return true;
			}
			if (type == IResource.FILE && resource.getParent().getType() == IResource.PROJECT && resource.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
				return true;
			}
			assertNull("5.0." + resource.getFullPath(), synchronizer.getSyncInfo(qname, resource));
			return true;
		};

		getWorkspace().getRoot().accept(visitor, IResource.DEPTH_INFINITE, true);
	}

	@Test
	public void testMoveResource() throws CoreException {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// cleanup auto-created resources
		getWorkspace().getRoot().delete(true, createTestMonitor());

		// setup
		IResource[] testResources = buildResources(getWorkspace().getRoot(), new String[] { "/Foo", "/Foo/file.txt" });
		IProject project = (IProject) testResources[0];
		IFile source = (IFile) testResources[1];
		// create in workspace
		createInWorkspace(testResources);

		// register partner and add sync info
		synchronizer.add(qname);
		byte[] b = new byte[] {1, 2, 3, 4};
		synchronizer.setSyncInfo(qname, source, b);

		// move the file
		IFile destination = project.getFile("newFile.txt");
		source.move(destination.getFullPath(), true, createTestMonitor());

		// check sync info
		byte[] syncInfo = synchronizer.getSyncInfo(qname, source);
		assertThat("sync info at source should not be null: " + source.getFullPath(), syncInfo, notNullValue());
		assertThat("unexpected sync info for resource: " + source.getFullPath(), syncInfo, is(b));
		assertThat("sync info at destination should be null: " + destination.getFullPath(),
				synchronizer.getSyncInfo(qname, destination), nullValue());
	}

	@Test
	public void testMoveResource2() throws CoreException {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// cleanup auto-created resources
		getWorkspace().getRoot().delete(true, createTestMonitor());

		// setup
		IResource[] toTest = buildResources(getWorkspace().getRoot(), new String[] {"/Foo", "/Foo/file.txt"});
		IProject sourceProject = (IProject) toTest[0];
		IFile sourceFile = (IFile) toTest[1];
		// create in workspace
		createInWorkspace(toTest);

		// register partner and add sync info
		synchronizer.add(qname);
		byte[] b = new byte[] {1, 2, 3, 4};
		synchronizer.setSyncInfo(qname, sourceProject, b);
		synchronizer.setSyncInfo(qname, sourceFile, b);

		// move the file
		IFile destFile = sourceProject.getFile("newFile.txt");
		sourceFile.move(destFile.getFullPath(), true, createTestMonitor());

		// check sync info
		byte[] syncInfo = synchronizer.getSyncInfo(qname, sourceFile);
		assertThat("sync info at source should not be null: " + sourceFile.getFullPath(), syncInfo, notNullValue());
		assertThat("unexpected sync info for resource: " + sourceFile.getFullPath(), syncInfo, is(b));
		assertThat("sync info at destination should be null: " + destFile.getFullPath(),
				synchronizer.getSyncInfo(qname, destFile), nullValue());

		// move the file back
		destFile.move(sourceFile.getFullPath(), true, createTestMonitor());

		// check the sync info
		syncInfo = synchronizer.getSyncInfo(qname, sourceFile);
		assertThat("sync info at source should not be null: " + sourceFile.getFullPath(), syncInfo, notNullValue());
		assertThat("unexpected sync info for resource: " + sourceFile.getFullPath(), syncInfo, is(b));
		assertThat("sync info at destination should be null: " + destFile.getFullPath(),
				synchronizer.getSyncInfo(qname, destFile), nullValue());

		// rename the file and ensure that the sync info is moved with it
		IProject destProject = getWorkspace().getRoot().getProject("newProject");
		sourceProject.move(destProject.getFullPath(), true, createTestMonitor());
		assertNull("7.1", synchronizer.getSyncInfo(qname, sourceProject));
		assertNull("7.2", synchronizer.getSyncInfo(qname, sourceFile));
		syncInfo = synchronizer.getSyncInfo(qname, destProject.getFile(sourceFile.getName()));
		assertThat("sync info should not be null: " + sourceFile.getFullPath(), syncInfo, notNullValue());
		assertThat("unexpected sync info for resource: " + sourceFile.getFullPath(), syncInfo, is(b));
		syncInfo = synchronizer.getSyncInfo(qname, destProject);
		assertThat("sync info should not be null: " + sourceFile.getFullPath(), syncInfo, notNullValue());
		assertThat("unexpected sync info for resource: " + sourceFile.getFullPath(), syncInfo, is(b));
	}

	@Test
	public void testRegistration() {
		// setup
		QualifiedName[] partners = new QualifiedName[NUMBER_OF_PARTNERS];
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			QualifiedName name = new QualifiedName("org.eclipse.core.deployment", "myTarget" + i);
			partners[i] = name;
		}
		ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the targets...twice to ensure dups aren't registered
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			synchronizer.add(partners[i]);
			synchronizer.add(partners[i]);
		}

		// get the array of targets
		QualifiedName[] list = synchronizer.getPartners();
		assertThat(list, arrayWithSize(NUMBER_OF_PARTNERS));

		// unregister all targets
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			synchronizer.remove(partners[i]);
		}
		assertThat(synchronizer.getPartners(), arrayWithSize(0));
	}

	@Test
	public void testSave() throws Exception {
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final Synchronizer synchronizer = (Synchronizer) ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the sync partner and set the sync info on the resources
		synchronizer.add(qname);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = createRandomString().getBytes();
			synchronizer.setSyncInfo(qname, resource, b);
			table.put(resource.getFullPath(), b);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// write out the data
		IPath syncInfoPath = Platform.getLocation().append(".testsyncinfo");
		File file = syncInfoPath.toFile();
		workspaceRule.deleteOnTearDown(syncInfoPath);
		try (OutputStream fileOutput = new FileOutputStream(file)) {
			try (DataOutputStream output = new DataOutputStream(fileOutput)) {
				final List<QualifiedName> list = new ArrayList<>(5);
				visitor = resource -> {
					ResourceInfo info = ((Resource) resource).getResourceInfo(false, false);
					if (info == null) {
						return true;
					}
					IPathRequestor requestor = new IPathRequestor() {
						@Override
						public IPath requestPath() {
							return resource.getFullPath();
						}

						@Override
						public String requestName() {
							return resource.getName();
						}
					};
					try {
						synchronizer.saveSyncInfo(info, requestor, output, list);
					} catch (IOException e) {
						CoreException wrappedException = new CoreException(
								new Status(IStatus.ERROR, PI_RESOURCES_TESTS, "Could not save sync info"));
						wrappedException.addSuppressed(e);
						throw wrappedException;
					}
					return true;
				};
				getWorkspace().getRoot().accept(visitor);
			}
		}

		// flush the sync info in memory
		flushAllSyncInfo(getWorkspace().getRoot());

		// read in the data
		try (InputStream fileInput = new FileInputStream(file)) {
			try (DataInputStream input = new DataInputStream(fileInput)) {
				IWorkspaceRunnable body = monitor -> {
					SyncInfoReader reader = new SyncInfoReader((Workspace) getWorkspace(), synchronizer);
					try {
						reader.readSyncInfo(input);
					} catch (IOException e) {
						CoreException wrappedException = new CoreException(
								new Status(IStatus.ERROR, PI_RESOURCES_TESTS, "Could not read sync info"));
						wrappedException.addSuppressed(e);
						throw wrappedException;
					}
				};
				getWorkspace().run(body, createTestMonitor());
			}
		}

		// confirm the sync bytes are the same
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);
	}

	@Test
	public void testSnap() {
		/*
		 final Hashtable table = new Hashtable(10);
		 final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		 final Synchronizer synchronizer = (Synchronizer) ResourcesPlugin.getWorkspace().getSynchronizer();

		 // register the sync partner and set the sync info on the resources
		 synchronizer.add(qname);
		 IResourceVisitor visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 if (resource.getType() == IResource.ROOT)
		 return true;
		 try {
		 byte[] b = getRandomString().getBytes();
		 synchronizer.setSyncInfo(qname, resource, b);
		 table.put(resource.getFullPath(), b);
		 } catch (CoreException e) {
		 fail("0.0." + resource.getFullPath(), e);
		 }
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 } catch (CoreException e) {
		 fail("0.1", e);
		 }

		 // write out the data
		 File file = Platform.getLocation().append(".testsyncinfo.snap").toFile();
		 SafeChunkyOutputStream safeOutput = null;
		 DataOutputStream o1 = null;
		 try {
		 safeOutput = new SafeChunkyOutputStream(file);
		 o1 = new DataOutputStream(safeOutput);
		 } catch (IOException e) {
		 if (safeOutput != null)
		 try {
		 safeOutput.close();
		 } catch (IOException e2) {
		 }
		 fail("1.0", e);
		 }
		 final DataOutputStream output = o1;
		 visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 try {
		 synchronizer.snapSyncInfo(resource, output);
		 } catch (IOException e) {
		 fail("1.1", e);
		 }
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 safeOutput.succeed();
		 } catch (CoreException e) {
		 fail("1.2", e);
		 } catch (IOException e) {
		 fail("1.3", e);
		 } finally {
		 try {
		 output.close();
		 } catch (IOException e) {
		 fail("1.4", e);
		 }
		 }

		 // flush the sync info in memory
		 try {
		 flushAllSyncInfo(getWorkspace().getRoot());
		 } catch (CoreException e) {
		 fail("2.0", e);
		 }

		 // read in the data
		 try {
		 InputStream safeInput = new SafeChunkyInputStream(file);
		 final DataInputStream input = new DataInputStream(safeInput);
		 IWorkspaceRunnable body = new IWorkspaceRunnable() {
		 public void run(IProgressMonitor monitor) throws CoreException {
		 SyncInfoSnapReader reader = new SyncInfoSnapReader((Workspace) getWorkspace(), synchronizer);
		 try {
		 reader.readSyncInfo(input);
		 } catch (IOException e) {
		 fail("3.0", e);
		 }
		 }
		 };
		 try {
		 getWorkspace().run(body, getMonitor());
		 } finally {
		 try {
		 input.close();
		 } catch (IOException e) {
		 fail("3.1", e);
		 }
		 }
		 } catch (FileNotFoundException e) {
		 fail("3.2", e);
		 } catch (IOException e) {
		 fail("3.3", e);
		 } catch (CoreException e) {
		 fail("3.4", e);
		 }

		 // confirm the sync bytes are the same
		 visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 byte[] actual = synchronizer.getSyncInfo(qname, resource);
		 if (resource.getType() == IResource.ROOT) {
		 assertNull("4.0", actual);
		 return true;
		 } else
		 assertNotNull("4.1." + resource.getFullPath(), actual);
		 byte[] expected = (byte[]) table.get(resource.getFullPath());
		 assertEquals("4.2." + resource.getFullPath(), expected, actual);
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 } catch (CoreException e) {
		 fail("4.3", e);
		 }
		 */
	}

	@Test
	public void testSyncInfo() throws CoreException {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = createRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// should not be able to set sync info before the target has been registered.
		visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			assertThrows(CoreException.class, () -> synchronizer.setSyncInfo(qname, resource, table.get(resource.getFullPath())));
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// should not be able to get sync info before the target has been registered
		visitor = resource -> {
			assertThrows(CoreException.class, () -> synchronizer.getSyncInfo(qname, resource));
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// there shouldn't be any info yet
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			assertNull("3.0." + resource.getFullPath(), actual);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// set the sync info
		visitor = resource -> {
			synchronizer.setSyncInfo(qname, resource, table.get(resource.getFullPath()));
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// get the info and ensure its the same
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// change the info and then set it
		visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = createRandomString().getBytes();
			synchronizer.setSyncInfo(qname, resource, b);
			table.put(resource.getFullPath(), b);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// get the new info
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			byte[] expected = table.get(resource.getFullPath());
			assertExpectedSyncInfo(resource, actual, expected);
			return true;
		};
		getWorkspace().getRoot().accept(visitor);

		// cleanup
		synchronizer.remove(qname);

		// should not be able to get sync info because the target has been unregistered
		visitor = resource -> {
			assertThrows(CoreException.class, () -> synchronizer.getSyncInfo(qname, resource));
			return true;
		};
		getWorkspace().getRoot().accept(visitor);
	}

	/**
	 * Removes resources, sets sync info to <code>null</code> and ensures the
	 * phantoms do not exist any more (see bug 3024)
	 */
	@Test
	public void testPhantomRemoval() throws CoreException {
		final QualifiedName partner = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final IWorkspace workspace = getWorkspace();
		final ISynchronizer synchronizer = workspace.getSynchronizer();

		// set up
		synchronizer.add(partner);
		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("foo");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		createInWorkspace(new IResource[] {file1, file2});

		// sets sync info for the folder and its children
		synchronizer.setSyncInfo(partner, folder, createRandomString().getBytes());
		synchronizer.setSyncInfo(partner, file1, createRandomString().getBytes());
		synchronizer.setSyncInfo(partner, file2, createRandomString().getBytes());

		// 1) tests with one child first
		assertTrue("1.1", file1.exists());
		assertTrue("1.2", !file1.isPhantom());
		// deletes file
		file1.delete(true, createTestMonitor());
		// file is now a phantom resource
		assertTrue("2.1", !file1.exists());
		assertTrue("2.2", file1.isPhantom());
		// removes sync info
		synchronizer.setSyncInfo(partner, file1, null);
		// phantom should not exist any more
		assertTrue("3.1", !file1.exists());
		assertTrue("3.2", !file1.isPhantom());

		// 2) tests with the folder and remaining child
		assertTrue("4.1", folder.exists());
		assertTrue("4.2", !folder.isPhantom());
		assertTrue("4.3", file2.exists());
		assertTrue("4.4", !file2.isPhantom());
		// deletes the folder and its only child
		folder.delete(true, createTestMonitor());
		// both resources are now phantom resources
		assertTrue("5.1", !folder.exists());
		assertTrue("5.2", folder.isPhantom());
		assertTrue("5.3", !file2.exists());
		assertTrue("5.4", file2.isPhantom());
		// removes only folder sync info
		synchronizer.setSyncInfo(partner, folder, null);
		// phantoms should not exist any more
		assertTrue("6.1", !folder.exists());
		assertTrue("6.2", !folder.isPhantom());
		assertTrue("6.3", !file2.exists());
		assertTrue("6.4", !file2.isPhantom());

		// clean-up
		synchronizer.remove(partner);
		removeFromWorkspace(project);
	}

}
