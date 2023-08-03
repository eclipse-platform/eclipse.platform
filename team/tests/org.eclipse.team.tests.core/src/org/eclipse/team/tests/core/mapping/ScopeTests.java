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

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.ui.*;

public class ScopeTests extends TeamTest {

	public static Test suite() {
		return suite(ScopeTests.class);
	}
	private IProject project1, project2, project3;
	private IWorkingSet workingSet;
	private SubscriberScopeManager manager;

	public ScopeTests() {
		super();
	}

	public ScopeTests(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		project1 = createProject("p1", new String[]{"file.txt"});
		project2 = createProject("p2", new String[]{"file.txt"});
		project3 = createProject("p3", new String[]{"file.txt"});
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		workingSet = manager.createWorkingSet("TestWS", new IProject[] { project1 });
		manager.addWorkingSet(workingSet);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
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

	public void testScopeExpansion() throws CoreException, OperationCanceledException, InterruptedException {
		ISynchronizationScopeManager sm = createScopeManager();
		assertProperContainment(sm);
		workingSet.setElements( new IProject[] { project1, project2 });
		assertProperContainment(sm);
	}

	public void testScopeContraction() throws OperationCanceledException, InterruptedException, CoreException {
		workingSet.setElements( new IProject[] { project1, project2 });
		ISynchronizationScopeManager sm = createScopeManager();
		assertProperContainment(sm);
		workingSet.setElements( new IProject[] { project1 });
		assertProperContainment(sm);
	}

}
