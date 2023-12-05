/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.Map;
import java.util.stream.Stream;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * This class does not define any tests, just convenience methods for other builder tests.
 */
public abstract class AbstractBuilderTest extends ResourceTest {

	public AbstractBuilderTest(String name) {
		super(name);
	}

	/**
	 * Adds a new delta verifier builder to the given project.
	 */
	protected void addBuilder(IProject project, String builderName) throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] {createCommand(desc, builderName, "Project1Build1")});
		project.setDescription(desc, getMonitor());
	}

	/**
	 * Creates and returns a new command with the SortBuilder, and the TestBuilder.BUILD_ID
	 * parameter set to the given value.
	 */
	protected ICommand createCommand(IProjectDescription description, String buildID) {
		return createCommand(description, SortBuilder.BUILDER_NAME, buildID);
	}

	/**
	 * Creates and returns a new command with the given builder name, and the TestBuilder.BUILD_ID
	 * parameter set to the given value.
	 */
	protected ICommand createCommand(IProjectDescription description, String builderName, String buildID) {
		ICommand command = description.newCommand();
		Map<String, String> args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, buildID);
		command.setBuilderName(builderName);
		command.setArguments(args);
		return command;
	}

	/**
	 * Dirties the given file, forcing a build.
	 */
	protected void dirty(IFile file) throws CoreException {
		file.setContents(getRandomContents(), true, true, getMonitor());
	}

	/**
	 * Sets the workspace build order to just contain the given projects.
	 */
	protected void setBuildOrder(IProject... projects) throws CoreException {
		IWorkspace workspace = getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setBuildOrder(Stream.of(projects).map(IProject::getName).toArray(String[]::new));
		workspace.setDescription(desc);
	}

}
