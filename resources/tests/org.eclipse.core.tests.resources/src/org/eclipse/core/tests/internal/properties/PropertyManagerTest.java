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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.properties;

import static org.junit.Assert.assertNotEquals;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.internal.localstore.LocalStoreTest;

public class PropertyManagerTest extends LocalStoreTest {

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

	private void createProperties(IFile target, QualifiedName[] names, String[] values) {
		for (int i = 0; i < names.length; i++) {
			names[i] = new QualifiedName("org.eclipse.core.tests", "prop" + i);
			values[i] = "property value" + i;
		}
		// create properties
		for (int i = 0; i < names.length; i++) {
			try {
				target.setPersistentProperty(names[i], values[i]);
			} catch (CoreException e) {
				fail("1." + i, e);
			}
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

	private void join(Thread[] threads) {
		//wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				fail("#join", e);
			}
		}
	}

	/**
	 * Tests concurrent acces to the property store.
	 */
	public void testConcurrentAccess() {

		// create common objects
		final IFile target = projects[0].getFile("target");
		try {
			target.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// prepare keys and values
		final int N = 50;
		final QualifiedName[] names = new QualifiedName[N];
		final String[] values = new String[N];
		createProperties(target, names, values);

		final CoreException[] errorPointer = new CoreException[1];
		Thread[] threads = createThreads(target, names, values, errorPointer);
		join(threads);
		if (errorPointer[0] != null) {
			fail("2.0", errorPointer[0]);
		}

		// remove trash
		try {
			target.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

	/**
	 * Tests concurrent access to the property store while the project is being
	 * deleted.
	 */
	public void testConcurrentDelete() {
		Thread[] threads;
		final IFile target = projects[0].getFile("target");
		final int REPEAT = 8;
		for (int i = 0; i < REPEAT; i++) {
			// create common objects
			ensureExistsInWorkspace(projects[0], true);
			ensureExistsInWorkspace(target, true);

			// prepare keys and values
			final int N = 50;
			final QualifiedName[] names = new QualifiedName[N];
			final String[] values = new String[N];
			createProperties(target, names, values);

			final CoreException[] errorPointer = new CoreException[1];
			threads = createThreads(target, names, values, errorPointer);
			try {
				//give the threads a chance to start
				Thread.sleep(10);
			} catch (InterruptedException e) {
				fail("1.98", e);
			}
			try {
				//delete the project while the threads are still running
				target.getProject().delete(IResource.NONE, getMonitor());
			} catch (CoreException e) {
				fail("1.99." + i, e);
			}
			join(threads);
			if (errorPointer[0] != null) {
				fail("2.0." + i, errorPointer[0]);
			}
		}
		// remove trash
		try {
			target.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

	public void testCache() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = projects[0];
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		QualifiedName propName = new QualifiedName("test", "prop");
		String propValue = "this is the property value";

		ensureExistsInWorkspace(new IResource[] { source, sourceFolder, sourceFile }, true);

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

	public void testOOME() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = projects[0];
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		QualifiedName propName = new QualifiedName("test", "prop");
		int MAX_VALUE_SIZE = 2 * 1024; // PropertyManager2.MAX_VALUE_SIZE
		String propValue = new String(new byte[MAX_VALUE_SIZE], StandardCharsets.ISO_8859_1);

		ensureExistsInWorkspace(new IResource[] { source, sourceFolder, sourceFile }, true);

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
			int quickAllocationSize = 200_000_000; // 200MB, should be smaller then -Xmx, but big to get OOME quick
			while (wastedMemory.add(new byte[quickAllocationSize])) {
				// force OOME
			}
		} catch (OutOfMemoryError e1) {
			wastedMemory.clear();
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
	public void testCopy() throws Throwable {
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IProject source = projects[0];
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		IProject destination = projects[1];
		IFolder destFolder = destination.getFolder(sourceFolder.getName());
		IResource destFile = destFolder.getFile(sourceFile.getName());
		QualifiedName propName = new QualifiedName("test", "prop");
		String propValue = "this is the property value";

		ensureExistsInWorkspace(new IResource[] {source, sourceFolder, sourceFile}, true);

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

	public void testDeleteProperties() throws Throwable {
		/* create common objects */
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IFile target = projects[0].getFile("target");
		ensureExistsInWorkspace(target, true);

		/* server properties */
		QualifiedName propName = new QualifiedName("eclipse", "prop");
		String propValue = "this is the property value";
		manager.setProperty(target, propName, propValue);
		assertTrue("1.1", manager.getProperty(target, propName).equals(propValue));
		/* delete */
		manager.deleteProperties(target, IResource.DEPTH_INFINITE);
		assertTrue("1.3", manager.getProperty(target, propName) == null);

		//test deep deletion of project properties
		IProject source = projects[0];
		IFolder sourceFolder = source.getFolder("myfolder");
		IResource sourceFile = sourceFolder.getFile("myfile.txt");
		propName = new QualifiedName("test", "prop");
		propValue = "this is the property value";
		ensureExistsInWorkspace(new IResource[] {source, sourceFolder, sourceFile}, true);

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
	public void testFileRename() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("proj");
		IFolder folder = project.getFolder("folder");
		IFile file1a = folder.getFile("file1");
		ensureExistsInWorkspace(file1a, true);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		try {
			file1a.setPersistentProperty(key, "value");
		} catch (CoreException e) {
			fail("0.5", e);
		}
		try {
			file1a.move(IPath.fromOSString("file2"), true, getMonitor());
		} catch (CoreException e) {
			fail("0.6", e);
		}
		IFile file1b = folder.getFile("file1");
		ensureExistsInWorkspace(file1b, true);
		String value = null;
		try {
			value = file1b.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("0.8", e);
		}
		assertNull("1.0", value);
		file1a = folder.getFile("file2");
		try {
			value = file1a.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("1.9", e);
		}
		assertEquals("2.0", "value", value);

	}

	/**
	 * See bug 93849.
	 */
	public void testFolderRename() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("proj");
		IFolder folder1a = project.getFolder("folder1");
		ensureExistsInWorkspace(folder1a, true);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		try {
			folder1a.setPersistentProperty(key, "value");
		} catch (CoreException e) {
			fail("0.5", e);
		}
		try {
			folder1a.move(IPath.fromOSString("folder2"), true, getMonitor());
		} catch (CoreException e) {
			fail("0.6", e);
		}
		IFolder folder1b = project.getFolder("folder1");
		ensureExistsInWorkspace(folder1b, true);
		String value = null;
		try {
			value = folder1b.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("0.8", e);
		}
		assertNull("1.0", value);
		folder1a = project.getFolder("folder2");
		try {
			value = folder1a.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("1.9", e);
		}
		assertEquals("2.0", "value", value);

	}

	/**
	 * Do a stress test by adding a very large property to the store.
	 */
	public void testLargeProperty() {
		// create common objects
		IFile target = projects[0].getFile("target");
		try {
			target.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		QualifiedName name = new QualifiedName("stressTest", "prop");
		final int SIZE = 10000;
		StringBuilder valueBuf = new StringBuilder(SIZE);
		for (int i = 0; i < SIZE; i++) {
			valueBuf.append("a");
		}
		String value = valueBuf.toString();
		try {
			target.setPersistentProperty(name, value);
			//should fail
			fail("1.0");
		} catch (CoreException e) {
			// expected
		}

		// remove trash
		try {
			target.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

	/**
	 * See bug 93849.
	 */
	public void testProjectRename() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project1a = root.getProject("proj1");
		ensureExistsInWorkspace(project1a, true);
		QualifiedName key = new QualifiedName(PI_RESOURCES_TESTS, "key");
		try {
			project1a.setPersistentProperty(key, "value");
		} catch (CoreException e) {
			fail("0.5", e);
		}
		try {
			project1a.move(IPath.fromOSString("proj2"), true, getMonitor());
		} catch (CoreException e) {
			fail("0.6", e);
		}
		IProject project1b = root.getProject("proj1");
		ensureExistsInWorkspace(project1b, true);
		String value = null;
		try {
			value = project1b.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("0.8", e);
		}
		assertNull("1.0", value);

		project1a = root.getProject("proj2");
		try {
			value = project1a.getPersistentProperty(key);
		} catch (CoreException e) {
			fail("1.9", e);
		}
		assertEquals("2.0", "value", value);
	}

	public void testProperties() throws Throwable {
		IProgressMonitor monitor = null;

		// create common objects
		IPropertyManager manager = new PropertyManager2((Workspace) ResourcesPlugin.getWorkspace());
		IFile target = projects[0].getFile("target");
		target.create(null, false, monitor);

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

		// remove trash
		target.delete(false, monitor);
	}

	public void testSimpleUpdate() {

		// create common objects
		IFile target = projects[0].getFile("target");
		try {
			target.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

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
			try {
				target.setPersistentProperty(names[i], values[i]);
			} catch (CoreException e) {
				fail("1." + i, e);
			}
		}

		// verify
		for (int i = 0; i < N; i++) {
			try {
				assertTrue("2.0", target.getPersistentProperty(names[i]).equals(values[i]));
			} catch (CoreException e) {
				fail("3." + i, e);
			}
		}

		for (int j = 0; j < 20; j++) {

			// change properties
			for (int i = 0; i < N; i++) {
				try {
					values[i] = values[i] + " - changed";
					target.setPersistentProperty(names[i], values[i]);
				} catch (CoreException e) {
					fail("4." + i, e);
				}
			}

			// verify
			for (int i = 0; i < N; i++) {
				try {
					assertTrue("5.0", target.getPersistentProperty(names[i]).equals(values[i]));
				} catch (CoreException e) {
					fail("6." + i, e);
				}
			}

		}

		// remove trash
		try {
			target.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

}
