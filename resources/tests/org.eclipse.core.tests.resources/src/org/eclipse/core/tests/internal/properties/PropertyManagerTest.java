/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.properties;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.internal.properties.IPropertyManager;
import org.eclipse.core.internal.properties.PropertyManager2;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PropertyManagerTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IProject project;

	@Before
	public void createTestProject() throws CoreException {
		project = getWorkspace().getRoot().getProject("test");
		createInWorkspace(project);
	}

	public static class StoredProperty {
		protected QualifiedName name = null;
		protected String value = null;

		public StoredProperty(QualifiedName name, String value) {
			super();
			this.name = name;
			this.value = value;
		}

		public QualifiedName getName() {
			return name;
		}

		public String getStringValue() {
			return value;
		}
	}

	private void createProperties(IFile target, QualifiedName[] names, String[] values) throws CoreException {
		for (int i = 0; i < names.length; i++) {
			names[i] = new QualifiedName("org.eclipse.core.tests", "prop" + i);
			values[i] = "property value" + i;
		}
		// create properties
		for (int i = 0; i < names.length; i++) {
			target.setPersistentProperty(names[i], values[i]);
		}
	}

	private Thread[] createThreads(final IFile target, final QualifiedName[] names, final String[] values, final CoreException[] errorPointer) {
		final int THREAD_COUNT = 3;
		Thread[] threads = new Thread[THREAD_COUNT];
		for (int j = 0; j < THREAD_COUNT; j++) {
			final String id = "GetSetProperty" + j;
			threads[j] = new Thread((Runnable) () -> {
				try {
					doGetSetProperties(target, id, names, values);
				} catch (CoreException e) {
					//ignore failure if the project has been deleted
					if (target.exists()) {
						e.printStackTrace();
						errorPointer[0] = e;
						return;
					}
				}
			}, id);
			threads[j].start();
		}
		return threads;
	}

	protected void doGetSetProperties(IFile target, String threadID, QualifiedName[] names, String[] values) throws CoreException {
		final int N = names.length;
		for (int j = 0; j < 10; j++) {
			for (int i = 0; i < N; i++) {
				target.getPersistentProperty(names[i]);
			}
			// change properties
			for (int i = 0; i < N; i++) {
				//			values[i] = values[i] + " - changed (" + threadID + ")";
				target.setPersistentProperty(names[i], values[i]);
			}
			// verify
			for (int i = 0; i < N; i++) {
				target.getPersistentProperty(names[i]);
			}
		}
	}

	/**
	 * Tests concurrent acces to the property store.
	 */
	@Test
	public void testConcurrentAccess() throws Exception {

		// create common objects
		final IFile target = project.getFile("target");
		target.create(createRandomContentsStream(), true, createTestMonitor());

		// prepare keys and values
		final int N = 50;
		final QualifiedName[] names = new QualifiedName[N];
		final String[] values = new String[N];
		createProperties(target, names, values);

		final CoreException[] errorPointer = new CoreException[1];
		Thread[] threads = createThreads(target, names, values, errorPointer);
		for (Thread thread : threads) {
			thread.join();
		}
		if (errorPointer[0] != null) {
			throw errorPointer[0];
		}
	}

	/**
	 * Tests concurrent access to the property store while the project is being
	 * deleted.
	 */
	@Test
	public void testConcurrentDelete() throws Exception {
		Thread[] threads;
		final IFile target = project.getFile("target");
		final int REPEAT = 8;
		for (int i = 0; i < REPEAT; i++) {
			// create common objects
			createInWorkspace(project);
			createInWorkspace(target);

			// prepare keys and values
			final int N = 50;
			final QualifiedName[] names = new QualifiedName[N];
			final String[] values = new String[N];
			createProperties(target, names, values);

			final CoreException[] errorPointer = new CoreException[1];
			threads = createThreads(target, names, values, errorPointer);
			// give the threads a chance to start
			Thread.sleep(10);
			// delete the project while the threads are still running
			target.getProject().delete(IResource.NONE, createTestMonitor());
			for (Thread thread : threads) {
				thread.join();
			}
			if (errorPointer[0] != null) {
				throw errorPointer[0];
			}
		}
	}

	@Test
	public void testCache() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = project;
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		QualifiedName propName = new QualifiedName("test", "prop");
		String propValue = "this is the property value";

		createInWorkspace(new IResource[] { source, sourceFolder, sourceFile });

		System.gc();
		System.runFinalization();

		manager.setProperty(source, propName, propValue);
		manager.setProperty(sourceFolder, propName, propValue);
		manager.setProperty(sourceFile, propName, propValue);
		assertNotNull("1.1", manager.getProperty(source, propName));

		String hint = "Property cache returned another instance. Same instance is not required but expected. Eiter the Garbage Collector deleted the cache or the cache is not working.";
		assertSame(hint + "1.2", propValue, manager.getProperty(source, propName));
		assertNotNull("1.3", manager.getProperty(sourceFolder, propName));
		assertSame(hint + "1.4", propValue, manager.getProperty(sourceFolder, propName));
		assertNotNull("1.5", manager.getProperty(sourceFile, propName));
		assertSame(hint + "1.6", propValue, manager.getProperty(sourceFile, propName));
	}

	@Test
	public void testOOME() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = project;
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		QualifiedName propName = new QualifiedName("test", "prop");
		int MAX_VALUE_SIZE = 2 * 1024; // PropertyManager2.MAX_VALUE_SIZE
		String propValue = new String(new byte[MAX_VALUE_SIZE], StandardCharsets.ISO_8859_1);

		createInWorkspace(new IResource[] { source, sourceFolder, sourceFile });

		manager.setProperty(source, propName, propValue);
		manager.setProperty(sourceFolder, propName, propValue);
		manager.setProperty(sourceFile, propName, propValue);

		String hint = "Property cache returned another instance. Same instance is not required but expected. Eiter the Garbage Collector deleted the cache or the cache is not working.";
		assertSame(hint + "1.2", propValue, manager.getProperty(source, propName));
		assertNotNull("1.3", manager.getProperty(sourceFolder, propName));
		assertSame(hint + "1.4", propValue, manager.getProperty(sourceFolder, propName));
		assertNotNull("1.5", manager.getProperty(sourceFile, propName));
		assertSame(hint + "1.6", propValue, manager.getProperty(sourceFile, propName));

		List<byte[]> wastedMemory = new LinkedList<>();
		try {
			// 200MB, should be smaller then -Xmx, but big to get OOME quick
			long maxMemory = Runtime.getRuntime().maxMemory();
			int quickAllocationSize = Math.max(200_000_000,
					(int) Math.min((long) Integer.MAX_VALUE - 8, maxMemory / 2));
			System.out.println("Waste memory to force OOME: " + quickAllocationSize + "/" + maxMemory);
			while (wastedMemory.add(new byte[quickAllocationSize])) {
				System.out.println("Waste memory to force OOME: " + quickAllocationSize);
				// force OOME
			}
		} catch (OutOfMemoryError e1) {
			wastedMemory.clear();
			wastedMemory= null;
			System.gc(); //try to hint jvm to release memory on parallel tests
			// it's not allowed to allocate an array at once that is larger then the Heap:
			assertNotEquals("Requested array size exceeds VM limit", e1.getMessage());
			// in case the -Xmx is set too low the quickAllocationSize has to be lowered.
		}

		// the cache is guaranteed to be emptied before OutOfMemoryError:
		assertNotNull("2.1", manager.getProperty(source, propName));
		assertEquals("2.2", propValue, manager.getProperty(source, propName));
		assertNotSame("2.3", propValue, manager.getProperty(source, propName));

		assertNotNull("3.1", manager.getProperty(sourceFolder, propName));
		assertEquals("3.2", propValue, manager.getProperty(sourceFolder, propName));
		assertNotSame("3.3", propValue, manager.getProperty(sourceFolder, propName));

		assertNotNull("4.1", manager.getProperty(sourceFile, propName));
		assertEquals("4.2", propValue, manager.getProperty(sourceFile, propName));

		// We can not squeeze the active working set.
		// The cache was emptied but the active Bucket.entries map did survive:
		assertSame("4.3", propValue, manager.getProperty(sourceFile, propName));
	}

	@Test
	public void testCopy() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = project;
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		IProject destination = getWorkspace().getRoot().getProject("destination");
		createInWorkspace(destination);
		IFolder destFolder = destination.getFolder(sourceFolder.getName());
		IResource destFile = destFolder.getFile(sourceFile.getName());
		QualifiedName propName = new QualifiedName("test", "prop");
		String propValue = "this is the property value";

		createInWorkspace(new IResource[] {source, sourceFolder, sourceFile});

		/*
		 * persistent properties
		 */
		manager.setProperty(source, propName, propValue);
		manager.setProperty(sourceFolder, propName, propValue);
		manager.setProperty(sourceFile, propName, propValue);

		assertNotNull("1.1", manager.getProperty(source, propName));
		assertTrue("1.2", manager.getProperty(source, propName).equals(propValue));
		assertNotNull("1.3", manager.getProperty(sourceFolder, propName));
		assertTrue("1.4", manager.getProperty(sourceFolder, propName).equals(propValue));
		assertNotNull("1.5", manager.getProperty(sourceFile, propName));
		assertTrue("1.6", manager.getProperty(sourceFile, propName).equals(propValue));

		// do the copy at the project level
		manager.copy(source, destination, IResource.DEPTH_INFINITE);

		assertNotNull("1.7", manager.getProperty(destination, propName));
		assertTrue("1.8", manager.getProperty(destination, propName).equals(propValue));

		assertNotNull("1.9", manager.getProperty(destFolder, propName));
		assertTrue("1.10", manager.getProperty(destFolder, propName).equals(propValue));
		assertNotNull("1.11", manager.getProperty(destFile, propName));
		assertTrue("1.12", manager.getProperty(destFile, propName).equals(propValue));

		// do the same thing but copy at the folder level
		manager.deleteProperties(source, IResource.DEPTH_INFINITE);
		manager.deleteProperties(destination, IResource.DEPTH_INFINITE);
		assertNull("2.0", manager.getProperty(source, propName));
		assertNull("2.1", manager.getProperty(sourceFolder, propName));
		assertNull("2.2", manager.getProperty(sourceFile, propName));
		assertNull("2.3", manager.getProperty(destination, propName));
		assertNull("2.4", manager.getProperty(destFolder, propName));
		assertNull("2.5", manager.getProperty(destFile, propName));
		manager.setProperty(sourceFolder, propName, propValue);
		manager.setProperty(sourceFile, propName, propValue);
		assertNotNull("2.6", manager.getProperty(sourceFolder, propName));
		assertTrue("2.7", manager.getProperty(sourceFolder, propName).equals(propValue));
		assertNotNull("2.8", manager.getProperty(sourceFile, propName));
		assertTrue("2.9", manager.getProperty(sourceFile, propName).equals(propValue));

		manager.copy(sourceFolder, destFolder, IResource.DEPTH_INFINITE);

		assertNotNull("2.10", manager.getProperty(destFolder, propName));
		assertTrue("2.11", manager.getProperty(destFolder, propName).equals(propValue));
		assertNotNull("2.12", manager.getProperty(destFile, propName));
		assertTrue("2.13", manager.getProperty(destFile, propName).equals(propValue));

		/* test overwrite */
		String newPropValue = "change property value";
		manager.setProperty(source, propName, newPropValue);
		assertTrue("2.0", manager.getProperty(source, propName).equals(newPropValue));
		manager.copy(source, destination, IResource.DEPTH_INFINITE);
		assertTrue("2.1", manager.getProperty(destination, propName).equals(newPropValue));
	}

	@Test
	public void testDeleteProperties() throws Throwable {
		/* create common objects */
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IFile target = project.getFile("target");
		createInWorkspace(target);

		/* server properties */
		QualifiedName propName = new QualifiedName("eclipse", "prop");
		String propValue = "this is the property value";
		manager.setProperty(target, propName, propValue);
		assertTrue("1.1", manager.getProperty(target, propName).equals(propValue));
		/* delete */
		manager.deleteProperties(target, IResource.DEPTH_INFINITE);
		assertTrue("1.3", manager.getProperty(target, propName) == null);

		//test deep deletion of project properties
		IProject source = project;
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		propName = new QualifiedName("test", "prop");
		propValue = "this is the property value";
		createInWorkspace(new IResource[] {source, sourceFolder, sourceFile});

		/*
		 * persistent properties
		 */
		manager.setProperty(source, propName, propValue);
		manager.setProperty(sourceFolder, propName, propValue);
		manager.setProperty(sourceFile, propName, propValue);

		assertNotNull("2.1", manager.getProperty(source, propName));
		assertTrue("2.2", manager.getProperty(source, propName).equals(propValue));
		assertNotNull("2.3", manager.getProperty(sourceFolder, propName));
		assertTrue("2.4", manager.getProperty(sourceFolder, propName).equals(propValue));
		assertNotNull("2.5", manager.getProperty(sourceFile, propName));
		assertTrue("2.6", manager.getProperty(sourceFile, propName).equals(propValue));

		//delete properties
		manager.deleteProperties(source, IResource.DEPTH_INFINITE);
		assertNull("3.1", manager.getProperty(source, propName));
		assertNull("3.2", manager.getProperty(sourceFolder, propName));
		assertNull("3.3", manager.getProperty(sourceFile, propName));
	}

	/**
	 * See bug 93849.
	 */
	@Test
	public void testFileRename() throws CoreException {
		IFolder folder = project.getFolder("folder");
		IFile file1a = folder.getFile("file1");
		createInWorkspace(file1a);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		file1a.setPersistentProperty(key, "value");
		file1a.move(IPath.fromOSString("file2"), true, createTestMonitor());
		IFile file1b = folder.getFile("file1");
		createInWorkspace(file1b);
		String value = null;
		value = file1b.getPersistentProperty(key);
		assertNull("1.0", value);
		file1a = folder.getFile("file2");
		value = file1a.getPersistentProperty(key);
		assertEquals("2.0", "value", value);
	}

	/**
	 * See bug 93849.
	 */
	@Test
	public void testFolderRename() throws CoreException {
		IFolder folder1a = project.getFolder("folder1");
		createInWorkspace(folder1a);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		folder1a.setPersistentProperty(key, "value");
		folder1a.move(IPath.fromOSString("folder2"), true, createTestMonitor());
		IFolder folder1b = project.getFolder("folder1");
		createInWorkspace(folder1b);
		String value = null;
		value = folder1b.getPersistentProperty(key);
		assertNull("1.0", value);
		folder1a = project.getFolder("folder2");
		value = folder1a.getPersistentProperty(key);
		assertEquals("2.0", "value", value);
	}

	/**
	 * Do a stress test by adding a very large property to the store.
	 */
	@Test
	public void testLargeProperty() throws CoreException {
		// create common objects
		IFile target = project.getFile("target");
		target.create(createRandomContentsStream(), true, createTestMonitor());

		QualifiedName name = new QualifiedName("stressTest", "prop");
		final int SIZE = 10000;
		StringBuilder valueBuf = new StringBuilder(SIZE);
		for (int i = 0; i < SIZE; i++) {
			valueBuf.append("a");
		}
		String value = valueBuf.toString();
		assertThrows(CoreException.class, () -> target.setPersistentProperty(name, value));
	}

	/**
	 * See bug 93849.
	 */
	@Test
	public void testProjectRename() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project1a = root.getProject("proj1");
		createInWorkspace(project1a);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		project1a.setPersistentProperty(key, "value");
		project1a.move(IPath.fromOSString("proj2"), true, createTestMonitor());
		IProject project1b = root.getProject("proj1");
		createInWorkspace(project1b);
		String value = project1b.getPersistentProperty(key);
		assertNull("1.0", value);

		project1a = root.getProject("proj2");
		value = project1a.getPersistentProperty(key);
		assertEquals("2.0", "value", value);
	}

	@Test
	public void testProperties() throws Throwable {
		// create common objects
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IFile target = project.getFile("target");
		target.create(null, false, createTestMonitor());

		// these are the properties that we are going to use
		QualifiedName propName1 = new QualifiedName("org.eclipse.core.tests", "prop1");
		QualifiedName propName2 = new QualifiedName("org.eclipse.core.tests", "prop2");
		QualifiedName propName3 = new QualifiedName("org.eclipse.core.tests", "prop3");
		String propValue1 = "this is the property value1";
		String propValue2 = "this is the property value2";
		String propValue3 = "this is the property value3";
		ArrayList<StoredProperty> props = new ArrayList<>(3);
		props.add(new StoredProperty(propName1, propValue1));
		props.add(new StoredProperty(propName2, propValue2));
		props.add(new StoredProperty(propName3, propValue3));

		// set the properties individually and retrieve them
		for (StoredProperty prop : props) {
			manager.setProperty(target, prop.getName(), prop.getStringValue());
			assertEquals("1.0." + prop.getName(), prop.getStringValue(), manager.getProperty(target, prop.getName()));
		}
		// check properties are be appropriately deleted (when set to null)
		for (StoredProperty prop : props) {
			manager.setProperty(target, prop.getName(), null);
			assertEquals("2.0." + prop.getName(), null, manager.getProperty(target, prop.getName()));
		}
		assertEquals("3.0", 0, manager.getProperties(target).size());
		manager.deleteProperties(target, IResource.DEPTH_INFINITE);
	}

	@Test
	public void testSimpleUpdate() throws CoreException {
		// create common objects
		IFile target = project.getFile("target");
		target.create(createRandomContentsStream(), true, createTestMonitor());

		// prepare keys and values
		int N = 3;
		QualifiedName[] names = new QualifiedName[3];
		String[] values = new String[N];
		for (int i = 0; i < N; i++) {
			names[i] = new QualifiedName("org.eclipse.core.tests", "prop" + i);
			values[i] = "property value" + i;
		}

		// create properties
		for (int i = 0; i < N; i++) {
			target.setPersistentProperty(names[i], values[i]);
		}

		// verify
		for (int i = 0; i < N; i++) {
			assertTrue("2.0", target.getPersistentProperty(names[i]).equals(values[i]));
		}

		for (int j = 0; j < 20; j++) {
			// change properties
			for (int i = 0; i < N; i++) {
				values[i] = values[i] + " - changed";
				target.setPersistentProperty(names[i], values[i]);
			}

			// verify
			for (int i = 0; i < N; i++) {
				assertTrue("5.0", target.getPersistentProperty(names[i]).equals(values[i]));
			}

		}
	}

	/**
	 * Whenever a delete operation is called on an IFile it's properties also
	 * deleted from .index file. This Test case validates for given IFile resource
	 * Zero depth is calculated to traverse through folders for loading right index
	 * file and delete its properties, because the required index file is present
	 * under corresponding bucket of the folder same as the IFile and no need to
	 * traverse to Infinite depth.
	 */
	@Test
	public void testFileDeleteTraversalDepth() throws CoreException {
		Workspace ws;
		PropertyManager2 manager;

		ArgumentCaptor<IResource> resourceArgCaptor = ArgumentCaptor.forClass(IResource.class);
		ArgumentCaptor<Integer> depthArgCapture = ArgumentCaptor.forClass(Integer.class);

		IFolder tempFolder = project.getFolder("temp");
		tempFolder.create(true, true, new NullProgressMonitor());

		IFile fileToBeDeleted = tempFolder.getFile("testfile" + 0);
		fileToBeDeleted.create(createRandomContentsStream(), true, createTestMonitor());
		fileToBeDeleted.setPersistentProperty(new QualifiedName(this.getClass().getName(), fileToBeDeleted.getName()),
				fileToBeDeleted.getName());

		MockitoAnnotations.openMocks(this);
		ws = Mockito.spy(new Workspace());
		manager = Mockito.spy(new PropertyManager2(ws));

		manager.deleteResource(fileToBeDeleted);

		Mockito.verify(manager).deleteProperties(resourceArgCaptor.capture(), depthArgCapture.capture());
		Integer expectedDepth = 0;
		assertEquals(expectedDepth, depthArgCapture.getValue());

	}
}
