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
package org.eclipse.team.tests.core.mapping;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ScopeTests {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IProject project1, project2, project3;
	private IWorkingSet workingSet;
	private SubscriberScopeManager manager;


	@Before
	public void setUp() throws Exception {
		project1 = createProjectWithFile("p1", "file.txt");
		project2 = createProjectWithFile("p2", "file.txt");
		project3 = createProjectWithFile("p3", "file.txt");
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		workingSet = manager.createWorkingSet("TestWS", new IProject[] { project1 });
		manager.addWorkingSet(workingSet);
	}

	/*
	 * This method creates a project with the given resources
	 */
	private IProject createProjectWithFile(String name, String fileName) throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(name);
		createInWorkspace(project);
		createInWorkspace(project.getFile(fileName));
		return project;
	}

	@After
	public void tearDown() throws Exception {
		this.manager.dispose();
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		manager.removeWorkingSet(workingSet);
	}

	private void assertProperContainment(ISynchronizationScopeManager sm) throws OperationCanceledException, InterruptedException {
		waitForManager(sm);
		testProjectContainment(sm, project1);
		testProjectContainment(sm, project2);
		testProjectContainment(sm, project3);
	}

	private void waitForManager(ISynchronizationScopeManager sm) throws OperationCanceledException, InterruptedException {
		Job.getJobManager().join(sm, null);
	}

	private void testProjectContainment(ISynchronizationScopeManager sm, IProject project) {
		if (isInWorkingSet(project) && !isInScope(sm, project))
			fail(project.getName() + " is in the working set but not in the scope");
		if (!isInWorkingSet(project) && isInScope(sm, project))
			fail(project.getName() + " is in scope but not in the working set");
	}

	private boolean isInScope(ISynchronizationScopeManager sm, IProject project) {
		return sm.getScope().contains(project);
	}

	private boolean isInWorkingSet(IProject project) {
		IAdaptable[] elements = workingSet.getElements();
		for (IAdaptable adaptable : elements) {
			if (adaptable.equals(project))
				return true;
		}
		return false;
	}

	private ISynchronizationScopeManager createScopeManager() throws CoreException, OperationCanceledException, InterruptedException {
		ScopeTestSubscriber subscriber = new ScopeTestSubscriber();
		manager = new SubscriberScopeManager(subscriber.getName(), new ResourceMapping[] { Utils.getResourceMapping(workingSet) }, subscriber, true);
		manager.initialize(new NullProgressMonitor());
		waitForManager(manager);
		return manager;
	}

	@Test
	public void testScopeExpansion() throws CoreException, OperationCanceledException, InterruptedException {
		ISynchronizationScopeManager sm = createScopeManager();
		assertProperContainment(sm);
		workingSet.setElements( new IProject[] { project1, project2 });
		assertProperContainment(sm);
	}

	@Test
	public void testScopeContraction() throws OperationCanceledException, InterruptedException, CoreException {
		workingSet.setElements( new IProject[] { project1, project2 });
		ISynchronizationScopeManager sm = createScopeManager();
		assertProperContainment(sm);
		workingSet.setElements( new IProject[] { project1 });
		assertProperContainment(sm);
	}

}
