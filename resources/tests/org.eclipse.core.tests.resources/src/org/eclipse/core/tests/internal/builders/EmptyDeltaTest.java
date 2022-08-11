/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests the callOnEmptyDelta attribute of the builder extension
 */
public class EmptyDeltaTest extends AbstractBuilderTest {

	public EmptyDeltaTest(String name) {
		super(name);
	}

	public void testBuildEvents() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("TestBuildEvents");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(EmptyDeltaBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Set up a plug-in lifecycle verifier for testing purposes
		EmptyDeltaBuilder verifier = null;
		//do an initial incremental build
		try {
			new EmptyDeltaBuilder().reset();
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier = EmptyDeltaBuilder.getInstance();
			verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
			verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("3.1");
		} catch (CoreException e) {
			fail("3.2", e);
			return;
		}
		// Now do another incremental build. Even though the delta is empty, it should be called
		try {
			verifier.reset();
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents("3.3");
		} catch (CoreException e) {
			fail("3.4", e);
		}
	}

}
