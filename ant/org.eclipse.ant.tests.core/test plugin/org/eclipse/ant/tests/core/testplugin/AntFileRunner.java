/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.testplugin;


import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Responsible for running test ant build files.
 */
public class AntFileRunner {
	
	private static final String BASE_DIR_PREFIX = "-Dbasedir="; //$NON-NLS-1$

	public void run(IFile buildFile, String[] targets, String[] args, String baseDir, boolean captureOutput) throws CoreException {
	
		AntRunner runner = new AntRunner();

		String[] runnerArgs = args;

		if (baseDir.length() > 0) {
			// Ant requires the working directory to be specified
			// as one of the arguments, so it needs to be appended.
			int length = 1;
			if (args != null) {
				length = args.length + 1;
			} 
			
			runnerArgs = new String[length];
			if (args != null) {
				System.arraycopy(args, 0, runnerArgs, 0, args.length);
			}
			runnerArgs[length - 1] = BASE_DIR_PREFIX + baseDir;
		}
		runner.setArguments(runnerArgs);

		if (buildFile != null) {
			runner.setBuildFileLocation(buildFile.getLocation().toFile().toString());
		}
		if (targets != null && targets.length > 0) {
			runner.setExecutionTargets(targets);
		}
		if (captureOutput) {
			runner.addBuildLogger(AbstractAntTest.ANT_TEST_BUILD_LOGGER);
		}

		runner.run(null);
	}
	
	public void run(String[] args, String baseDir) throws Exception {
	
		AntRunner runner = new AntRunner();

		String[] runnerArgs = args;

		if (baseDir.length() > 0) {
			// Ant requires the working directory to be specified
			// as one of the arguments, so it needs to be appended.
			runnerArgs = new String[args.length + 1];
			System.arraycopy(args, 0, runnerArgs, 0, args.length);
			runnerArgs[args.length] = BASE_DIR_PREFIX + baseDir;
		}
		runner.setArguments(runnerArgs);

		runner.run(args);
	}
}
