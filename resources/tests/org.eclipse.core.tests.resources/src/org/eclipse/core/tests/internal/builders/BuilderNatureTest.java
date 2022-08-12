/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests relationship between natures and builders.  Builders that are owned
 * by a nature can only be run if their owning nature is defined on the project
 * being built.
 */
public class BuilderNatureTest extends AbstractBuilderTest {

	public BuilderNatureTest(String testName) {
		super(testName);
	}

	protected InputStream projectFileWithoutSnow() {
		String contents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>P1</name>\n" + "	<comment></comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<buildCommand>\n" + "			<name>org.eclipse.core.tests.resources.snowbuilder</name>\n" + "			<arguments>\n" + "				<dictionary>\n" + "					<key>BuildID</key>\n" + "					<value>SnowBuild</value>\n" + "				</dictionary>\n" + "			</arguments>\n" + "		</buildCommand>\n" + "	</buildSpec>\n" + "	<natures>\n" + "		<nature>org.eclipse.core.tests.resources.waterNature</nature>\n" + "	</natures>\n" + "</projectDescription>";

		return new ByteArrayInputStream(contents.getBytes());
	}

	protected InputStream projectFileWithoutWater() {
		String contents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>P1</name>\n" + "	<comment></comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<buildCommand>\n" + "			<name>org.eclipse.core.tests.resources.snowbuilder</name>\n" + "			<arguments>\n" + "				<dictionary>\n" + "					<key>BuildID</key>\n" + "					<value>SnowBuild</value>\n" + "				</dictionary>\n" + "			</arguments>\n" + "		</buildCommand>\n" + "	</buildSpec>\n" + "	<natures>\n" + "		<nature>org.eclipse.core.tests.resources.snowNature</nature>\n" + "	</natures>\n" + "</projectDescription>";

		return new ByteArrayInputStream(contents.getBytes());
	}

	public void testBasic() {
		//add the water and snow natures to the project, and ensure
		//the snow builder gets run
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("P1");
		ensureExistsInWorkspace(project, true);
		SnowBuilder builder = SnowBuilder.getInstance();
		builder.reset();
		try {
			setAutoBuilding(true);
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("0.99", e);
		}
		builder.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		builder.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		builder.addExpectedLifecycleEvent(SnowBuilder.SNOW_BUILD_EVENT);
		builder.assertLifecycleEvents("1.0");
	}

	/**
	 * Get the project in a state where the snow nature is disabled,
	 * then ensure the snow builder is not run but remains on the build spec
	 */
	public void testDisabledNature() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("P1");
		ensureExistsInWorkspace(project, true);
		try {
			setAutoBuilding(true);
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("0.99", e);
		}
		//remove the water nature, thus invalidating snow nature
		SnowBuilder builder = SnowBuilder.getInstance();
		builder.reset();
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		try {
			//setting description file will also trigger build
			descFile.setContents(projectFileWithoutWater(), IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//assert that builder was skipped
		builder.assertLifecycleEvents("1.0");

		//now re-enable the nature and ensure that the delta was null
		builder.reset();
		builder.addExpectedLifecycleEvent(SnowBuilder.SNOW_BUILD_EVENT);
		try {
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("2.99", e);
		}
		builder.assertLifecycleEvents("2.0");
		assertTrue("2.1", builder.wasDeltaNull());
	}

	/**
	 * Get the project in a state where the snow nature is missing,
	 * then ensure the snow builder is removed from the build spec.
	 */
	public void testMissingNature() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("P1");
		ensureExistsInWorkspace(project, true);
		try {
			setAutoBuilding(true);
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("0.99", e);
		}
		//remove the snow nature through normal API
		SnowBuilder builder = SnowBuilder.getInstance();
		builder.reset();
		try {
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER});
			project.setDescription(desc, IResource.NONE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//make sure the snow builder wasn't run
		builder.assertLifecycleEvents("2.0");

		//make sure the build spec doesn't include snow builder
		try {
			ICommand[] commands = project.getDescription().getBuildSpec();
			for (ICommand command : commands) {
				if (command.getBuilderName().equals(SnowBuilder.BUILDER_NAME)) {
					assertTrue("2.1", false);
				}
			}
		} catch (CoreException e) {
			fail("2.99", e);
		}

		//now add the snow nature back and ensure snow builder runs
		builder.reset();
		builder.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		builder.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		builder.addExpectedLifecycleEvent(SnowBuilder.SNOW_BUILD_EVENT);
		try {
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.KEEP_HISTORY, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("3.99", e);
		}
		builder.assertLifecycleEvents("3.0");

		//now remove the snow nature by hacking .project
		//the deconfigure method won't run, but the builder should still be removed.
		builder.reset();
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		try {
			//setting description file will also trigger build
			descFile.setContents(projectFileWithoutSnow(), IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("4.99", e);
		}
		//assert that builder was skipped
		builder.assertLifecycleEvents("4.0");

		//make sure the build spec doesn't include snow builder
		try {
			ICommand[] commands = project.getDescription().getBuildSpec();
			for (ICommand command : commands) {
				if (command.getBuilderName().equals(SnowBuilder.BUILDER_NAME)) {
					assertTrue("4.1", false);
				}
			}
		} catch (CoreException e) {
			fail("5.99", e);
		}

		//now re-enable the nature and ensure that the delta was null
		builder.reset();
		builder.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		builder.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		builder.addExpectedLifecycleEvent(SnowBuilder.SNOW_BUILD_EVENT);
		try {
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {NATURE_WATER, NATURE_SNOW});
			project.setDescription(desc, IResource.FORCE, getMonitor());
			waitForBuild();
		} catch (CoreException e) {
			fail("6.99", e);
		}
		builder.assertLifecycleEvents("5.0");
		assertTrue("5.1", builder.wasDeltaNull());
	}
}
