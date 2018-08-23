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
package org.eclipse.team.tests.ccvs.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.ui.IActionDelegate;

public class EnablementTest extends EclipseTest {
	
	/**
	 * Constructor for CVSProviderTest
	 */
	public EnablementTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public EnablementTest(String name) {
		super(name);
	}
	
	/**
	 * Create a test project for the given action delegate. The structure of
	 * this test project is used by the get resource methods to return resources
	 * of the proper type.
	 * 
	 * @param actionDelegate
	 * @throws CoreException
	 * @throws TeamException
	 */
	protected IProject createTestProject(IActionDelegate actionDelegate) throws CoreException, TeamException {
		String actionName = getName(actionDelegate);
		return createProject(actionName, new String[] { "file.txt", "folder1/", "folder1/a.txt" });
	}
	
	protected List getManagedResources(IProject testProject, boolean includeFolders, boolean multiple) {
		List result = new ArrayList();
		if (includeFolders) {
			result.add(testProject.getFolder("folder1"));
		} else {
			result.add(testProject.getFile("folder1/a.txt"));
		}
		if (multiple) {
			result.add(testProject.getFile("file.txt"));
		}
		return result;
	}
	
	protected List getAddedResources(IProject testProject) throws CoreException, TeamException {
		List result = new ArrayList();
		IFile file = testProject.getFile("added.txt");
		if (!file.exists()) {
			addResources(testProject, new String[] {"added.txt"}, false);
		}
		result.add(file);
		return result;
	}
	
	protected List getIgnoredResources(IProject testProject) throws CoreException, TeamException {
		List result = new ArrayList();
		IFile file = testProject.getFile("ignored.txt");
		if (!file.exists()) {
			file.create(getRandomContents(), false, null);
		}
		result.add(file);
		IFile ignoreFile = testProject.getFile(".cvsignore");
		InputStream contents = new ByteArrayInputStream("ignored.txt".getBytes());
		if (ignoreFile.exists()) {
			ignoreFile.setContents(contents, false, false, null);
		} else {
			ignoreFile.create(contents, false, null);
		}
		return result;
	}
	
	protected List getUnmanagedResources(IProject testProject) throws CoreException, TeamException {
		List result = new ArrayList();
		IFile file = testProject.getFile("unmanaged.txt");
		if (!file.exists()) {
			file.create(getRandomContents(), false, null);
		}
		result.add(file);
		return result;
	}

	/**
	 * Method getResourceWithUnmanagedParent.
	 * @param project
	 * @return Collection
	 */
	protected List getResourceWithUnmanagedParent(IProject project) throws CoreException {
		List result = new ArrayList();
		IFolder folder = project.getFolder("newFolder");
		if(!folder.exists()) folder.create(false, true, null);
		IFile file = folder.getFile("unmanaged.txt");
		if (!file.exists()) {
			file.create(getRandomContents(), false, null);
		}
		result.add(file);
		return result;
	}
		
	protected List getOverlappingResources(IProject testProject, boolean includeFiles) {
		List result = new ArrayList();
		result.add(testProject);
		result.add(testProject.getFolder("folder1"));
		if (includeFiles) {
			result.add(testProject.getFile("folder1/a.txt"));
		}
		return result;
	}

	protected ISelection asSelection(List resources) {
		return new StructuredSelection(resources);
	}
	
	protected String getName(IActionDelegate actionDelegate) {
		return actionDelegate.getClass().getName();
	}

	/**
	 * Assert that the enablement for the given IActionDelegate and ISelection
	 * match that provided as expectedEnablement.
	 * 
	 * @param actionDelegate
	 * @param selection
	 * @param expectedEnablement
	 */
	protected void assertEnablement(IActionDelegate actionDelegate, ISelection selection, boolean expectedEnablement) {
		IAction action = new Action() {};
		actionDelegate.selectionChanged(action, selection);
		assertEquals(getName(actionDelegate) + " enablement wrong!", expectedEnablement, action.isEnabled());
	}
}
