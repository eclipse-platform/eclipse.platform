/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.core.ant;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;

public class EclipseAntMain extends Main {

	private final Project fEclipseAntProject;

	public EclipseAntMain(Project eclipseAntProject) {
		super();
		fEclipseAntProject = eclipseAntProject;
	}

	public static void run(String[] args, Project eclipseAntProject) {
		Main projectHelpMain = new EclipseAntMain(eclipseAntProject);
		projectHelpMain.startAnt(args, null, null);
	}

	@Override
	protected void exit(int exitCode) {
		// disallow system exit
	}

	@Override
	protected void addBuildListeners(Project project) {
		for (BuildListener listener : fEclipseAntProject.getBuildListeners()) {
			project.addBuildListener(listener);
		}
	}
}
