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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import java.util.HashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.SnowBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;

/**
 */
public class SnowNature extends TestNature {
	/**
	 * Constructor for SnowNature.
	 */
	public SnowNature() {
		super();
	}

	/**
	 * @see IProjectNature#configure()
	 */
	@Override
	public void configure() throws CoreException {
		//install the snow builder
		IProject project = getProject();
		IProjectDescription desc = project.getDescription();
		ICommand[] oldSpec = desc.getBuildSpec();
		ICommand[] newSpec = new ICommand[oldSpec.length + 1];
		System.arraycopy(oldSpec, 0, newSpec, 0, oldSpec.length);
		ICommand newCommand = desc.newCommand();
		newCommand.setBuilderName(SnowBuilder.BUILDER_NAME);
		HashMap<String, String> args = new HashMap<>(20);
		args.put(TestBuilder.BUILD_ID, SnowBuilder.SNOW_BUILD_EVENT);
		newCommand.setArguments(args);
		newSpec[oldSpec.length] = newCommand;
		desc.setBuildSpec(newSpec);
		project.setDescription(desc, IResource.FORCE | IResource.KEEP_HISTORY, null);
	}

	/**
	 * @see IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
		//remove the snow builder
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(SnowBuilder.BUILDER_NAME)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				getProject().setDescription(description, null);
				return;
			}
		}
	}
}
