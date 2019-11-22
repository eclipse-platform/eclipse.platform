/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class WorkspaceTest extends ResourceTest {
	protected static IProgressMonitor DEFAULT_MONITOR = new NullProgressMonitor();
	protected static final IProgressMonitor DEFAULT_PROGRESS_MONITOR = new NullProgressMonitor();

	public static Test suite(Class<?> c) {
		String testName = System.getProperty("eclipse.team.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(c);
			return suite;
		}
		try {
			return (Test)c.getConstructor(String.class).newInstance(testName);
		} catch (Exception e) {
			fail(e.getMessage());
			// Above will throw so below is never actually reached
			return null;
		}
	}

	public WorkspaceTest() {
		super();
	}
	public WorkspaceTest(String name) {
		super(name);
	}

	protected IProject getNamedTestProject(String name) throws CoreException {
		IProject target = getWorkspace().getRoot().getProject(name);
		if (!target.exists()) {
			target.create(null);
			target.open(null);
		}
		assertExistsInFileSystem(target);
		return target;
	}

	protected IProject getUniqueTestProject(String prefix) throws CoreException {
		// manage and share with the default stream create by this class
		return getNamedTestProject(prefix + "-" + Long.toString(System.currentTimeMillis()));
	}

	/*
	 * This method creates a project with the given resources
	 */
	protected IProject createProject(String prefix, String[] resources) throws CoreException {
		IProject project = getUniqueTestProject(prefix);
		buildResources(project, resources, true);
		return project;
	}

	/*
	 * Create a test project using the currently running test case as the project name prefix
	 */
	protected IProject createProject(String[] resources) throws CoreException {
		return createProject(getName(), resources);
	}

	protected IStatus getTeamTestStatus(int severity) {
		return new Status(severity, "org.eclipse.team.tests.core", 0, "team status", null);
	}

	/**
	 * Creates filesystem 'resources' with the given names and fills them with random text.
	 * @param container An object that can hold the newly created resources.
	 * @param hierarchy A list of files &amp; folder names to use as resources
	 * @param includeContainer A flag that controls whether the container is included in the list of resources.
	 * @return IResource[] An array of resources filled with variable amounts of random text
	 * @throws CoreException
	 */
	public IResource[] buildResources(IContainer container, String[] hierarchy, boolean includeContainer) throws CoreException {
		List<IResource> resources = new ArrayList<>(hierarchy.length + 1);
		if (includeContainer)
			resources.add(container);
		resources.addAll(Arrays.asList(buildResources(container, hierarchy)));
		IResource[] result = resources.toArray(new IResource[resources.size()]);
		ensureExistsInWorkspace(result, true);
		for (IResource r : result) {
			if (r.getType() == IResource.FILE) {
				((IFile) r).setContents(getRandomContents(100), true, false, null);
			}
		}
		return result;
	}
	public IResource[] buildEmptyResources(IContainer container, String[] hierarchy, boolean includeContainer) throws CoreException {
		List<IResource> resources = new ArrayList<>(hierarchy.length + 1);
		resources.addAll(Arrays.asList(buildResources(container, hierarchy)));
		if (includeContainer)
			resources.add(container);
		IResource[] result = resources.toArray(new IResource[resources.size()]);
		ensureExistsInWorkspace(result, true);
		return result;
	}
	/**
	 * Creates an InputStream filled with random text in excess of a specified minimum.
	 * @param sizeAtLeast 	The minimum number of chars to fill the input stream with.
	 * @return InputStream The input stream containing random text.
	 */
	protected static InputStream getRandomContents(int sizeAtLeast) {
		StringBuilder randomStuff = new StringBuilder(sizeAtLeast + 100);
		while (randomStuff.length() < sizeAtLeast) {
			randomStuff.append(getRandomSnippet());
		}
		return new ByteArrayInputStream(randomStuff.toString().getBytes());
	}
	/**
	 * Produces a random chunk of text from a finite collection of pre-written phrases.
	 * @return String Some random words.
	 */
	public static String getRandomSnippet() {
		switch ((int) Math.round(Math.random() * 10)) {
			case 0 :
				return "este e' o meu conteudo (portuguese)";
			case 1 :
				return "Dann brauchen wir aber auch einen deutschen Satz!";
			case 2 :
				return "I'll be back";
			case 3 :
				return "don't worry, be happy";
			case 4 :
				return "there is no imagination for more sentences";
			case 5 :
				return "customize yours";
			case 6 :
				return "foo";
			case 7 :
				return "bar";
			case 8 :
				return "foobar";
			case 9 :
				return "case 9";
			default :
				return "these are my contents";
		}
	}


	public void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			System.err.println("Testing was rudely interrupted.");
		}
	}

	public void appendText(IResource resource, String text, boolean prepend) throws CoreException, IOException {
		IFile file = (IFile) resource;
		InputStream in = file.getContents();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			if (prepend) {
				bos.write(text.getBytes());
			}
			int i;
			while ((i = in.read()) != -1) {
				bos.write(i);
			}
			if (!prepend) {
				bos.write(text.getBytes());
			}
		} finally {
			in.close();
		}
		file.setContents(new ByteArrayInputStream(bos.toByteArray()), false, false, DEFAULT_MONITOR);
	}
	/*
	 * Get the resources for the given resource names
	 */
	public IResource[] getResources(IContainer container, String[] hierarchy) throws CoreException {
		IResource[] resources = new IResource[hierarchy.length];
		for (int i=0;i<resources.length;i++) {
			resources[i] = container.findMember(hierarchy[i]);
			if (resources[i] == null) {
				resources[i] = buildResources(container, new String[] {hierarchy[i]})[0];
			}
		}
		return resources;
	}

	// Assert that the two containers have equal contents
	protected void assertEquals(IContainer container1, IContainer container2) throws CoreException {
		assertEquals(container1.getName(), container2.getName());
		List<IResource> members1 = new ArrayList<>();
		members1.addAll(Arrays.asList(container1.members()));

		List<IResource> members2 = new ArrayList<>();
		members2.addAll(Arrays.asList(container2.members()));

		assertTrue(members1.size() == members2.size());
		for (int i=0;i<members1.size();i++) {
			IResource member1 = members1.get(i);
			IResource member2 = container2.findMember(member1.getName());
			assertNotNull(member2);
			assertEquals(member1, member2);
		}
	}

	// Assert that the two files have equal contents
	protected void assertEquals(IFile file1, IFile file2) throws CoreException {
		assertEquals(file1.getName(), file2.getName());
		assertTrue(compareContent(file1.getContents(), file2.getContents()));
	}

	// Assert that the two projects have equal contents ignoreing the project name
	// and the .vcm_meta file
	protected void assertEquals(IProject container1, IProject container2) throws CoreException {
		List<IResource> members1 = new ArrayList<>();
		members1.addAll(Arrays.asList(container1.members()));
		members1.remove(container1.findMember(".project"));

		List<IResource> members2 = new ArrayList<>();
		members2.addAll(Arrays.asList(container2.members()));
		members2.remove(container2.findMember(".project"));

		assertTrue("Number of children differs for " + container1.getFullPath(), members1.size() == members2.size());
		for (int i=0;i<members1.size();i++) {
			IResource member1 = members1.get(i);
			IResource member2 = container2.findMember(member1.getName());
			assertNotNull(member2);
			assertEquals(member1, member2);
		}
	}
	protected void assertEquals(IResource resource1, IResource resource2) throws CoreException {
		assertEquals(resource1.getType(), resource2.getType());
		if (resource1.getType() == IResource.FILE)
			assertEquals((IFile)resource1, (IFile)resource2);
		else
			assertEquals((IContainer)resource1, (IContainer)resource2);
	}
}
