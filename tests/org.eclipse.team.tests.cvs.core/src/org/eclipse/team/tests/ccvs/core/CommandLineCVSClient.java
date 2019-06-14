/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.junit.Assert;

public class CommandLineCVSClient implements ICVSClient {
	public static final ICVSClient INSTANCE = new CommandLineCVSClient();
	private static final String cvsExecutable =
		System.getProperty("eclipse.cvs.command");
		
	@Override
	public void executeCommand(ICVSRepositoryLocation repositoryLocation,
		IContainer localRoot, String command, String[] globalOptions,
		String[] localOptions, String[] arguments) throws CVSException {
		execute(repositoryLocation.getLocation(false), localRoot.getLocation().toFile(), command,
			globalOptions, localOptions, arguments);
		try {
			localRoot.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new CVSClientException("CoreException during refreshLocal: " + e.getMessage());
		}
	}
	
	public static void execute(
		String repositoryLocation, File localRoot, String command,
		String[] globalOptions, String[] localOptions,
		String[] arguments) throws CVSException {
		// test arguments
		Assert.assertNotNull(repositoryLocation);
		Assert.assertNotNull(localRoot);
		Assert.assertNotNull(command);
		Assert.assertNotNull(globalOptions);
		Assert.assertNotNull(localOptions);
		Assert.assertNotNull(arguments);
		Assert.assertTrue(localRoot.exists());

		// build command line
		StringBuffer commandLineBuf = new StringBuffer(cvsExecutable);
		commandLineBuf.append(" -d \"");
		commandLineBuf.append(repositoryLocation);
		commandLineBuf.append('"');
		appendStrings(commandLineBuf, globalOptions);
		commandLineBuf.append(' ');
		commandLineBuf.append(command);
		appendStrings(commandLineBuf, localOptions);
		appendStrings(commandLineBuf, arguments);
		
		// execute command
		JUnitTestCase.waitMsec(1500);
		int returnCode = CVSTestSetup.executeCommand(commandLineBuf.toString(), null, localRoot);
		if (returnCode != 0) {
			throw new CVSClientException("Command line client returned non-zero code: " + returnCode);
		}
	}
		
	private static void appendStrings(StringBuffer commandLine, String[] strings) {	
		for (String string : strings) {
			if (string != null && string.length() != 0) {
				commandLine.append(" \"");
				commandLine.append(string);
				commandLine.append('"');
			}
		}
	}
}
