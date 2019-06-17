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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * @version 	1.0
 * @author 	${user}
 */
public class ImportTest extends EclipseTest {

	/**
	 * Constructor for ImportTest.
	 */
	public ImportTest() {
		super();
	}

	/**
	 * Constructor for ImportTest.
	 * @param name
	 */
	public ImportTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ImportTest.class);
		return new CVSTestSetup(suite);
		//return new CVSTestSetup(new ImportTest("testReadOnly"));
	}
	
	// Assert that the two containers have equal contents
	protected void assertEquals(IContainer container1, IContainer container2) throws CoreException {
		assertEquals(container1.getName(), container2.getName());
		List<IResource> members1 = new ArrayList<>();
		members1.addAll(Arrays.asList(container1.members()));
		members1.remove(container1.findMember("CVS"));
		
		List<IResource> members2 = new ArrayList<>();
		members2.addAll(Arrays.asList(container2.members()));
		members2.remove(container2.findMember("CVS"));
		
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
	@Override
	protected void assertEquals(IProject container1, IProject container2) throws CoreException {
		List<IResource> members1 = new ArrayList<>();
		members1.addAll(Arrays.asList(container1.members()));
		members1.remove(container1.findMember(".project"));
		members1.remove(container1.findMember("CVS"));
		
		List<IResource> members2 = new ArrayList<>();
		members2.addAll(Arrays.asList(container2.members()));
		members2.remove(container2.findMember(".project"));
		members2.remove(container2.findMember("CVS"));
		
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
	
	public void testImportAndCheckout() throws TeamException, CoreException {
		// Create a test project and import it into cvs
		IProject project = getUniqueTestProject("testImport");
		buildResources(project, new String[] { "file1.txt", "folder1/", "folder1/a.txt" }, true);
		importProject(project);
		
		// Check it out under a different name and validate that the results are the same
		IProject copy = getWorkspace().getRoot().getProject(project.getName() + "Copy");
		checkout(getRepository(), copy, project.getName(), null, DEFAULT_MONITOR);
		assertValidCheckout(copy);
		assertEquals(project, copy);
	}
	
	public void testCheckout() throws TeamException, CoreException, IOException {
		// Create a project and checkout a copy
		IProject project = createProject("testCheckout", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IProject copy = checkoutCopy(project, "-copy");
		
		// 0. checkout the project again
		project = checkoutProject(project, null, null);
		assertEquals(project, copy, true, true);
		
		// 1. Delete the project but not it's contents and checkout the project again
		waitForDecorator();
		project.delete(false, false, DEFAULT_MONITOR);
		project = checkoutProject(project, null, null);
		assertEquals(project, copy, true, true);
		
		// 2. Delete the project and its contents and use the module name instead of the project
		waitForDecorator();
		project.delete(true, false, DEFAULT_MONITOR);
		project = checkoutProject(null, project.getName(), null);
		assertEquals(project, copy, true, true);
		
		// 3. Create a project in a custom location and check out over it
		waitForDecorator();
		project.delete(true, false, DEFAULT_MONITOR);
		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		//desc.setLocation(new Path("C:\\temp\\project"));
		project.create(desc, DEFAULT_MONITOR);
		project = checkoutProject(project, null, null);
		assertEquals(project, copy, true, true);
		
		// 4. Checkout something that doesn't contain a .project
		waitForDecorator();
		project.delete(true, false, DEFAULT_MONITOR);
		project = checkoutProject(null, project.getName() + "/folder1", null);
		//assertEquals(project, copy.getFolder("folder1"));
		

	}
	
	public void testRootShare() throws CoreException {
		// Create a test project 
		IProject project = getUniqueTestProject(getName());
		buildResources(project, new String[] { "file1.txt", "folder1/", "folder1/a.txt" }, true);
		// Share the project
		shareProject(getRepository(), project, null, DEFAULT_MONITOR);
		addResources(project, new String[] { "file1.txt", "folder1/", "folder1/a.txt" }, true);
		// Checkout a copy ans test
		IProject copy = checkoutProject(getUniqueTestProject(getName() + "copy"), project.getName(), null);
		assertEquals(project, copy);
	}
	
	public void testNonRootShare() throws CoreException {
		// Create a test project 
		IProject project = getUniqueTestProject(getName());
		buildResources(project, new String[] { "file1.txt", "folder1/", "folder1/a.txt" }, true);
		// Share it as a non-root folder in the repository
		String moduleName = "root" + Long.toString(System.currentTimeMillis()) + "/" + project.getName();
		shareProject(getRepository(), project, moduleName, DEFAULT_MONITOR);
		addResources(project, new String[] { "file1.txt", "folder1/", "folder1/a.txt" }, true);
		// Checkout a copy and test
		IProject copy = checkoutProject(getUniqueTestProject(getName() + "copy"), moduleName, null);
		assertEquals(project, copy);
	}
}
