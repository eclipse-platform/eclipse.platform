/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.junit.Assert;

public class EclipseCVSClient implements ICVSClient {
	public static final ICVSClient INSTANCE = new EclipseCVSClient();
	private static final HashMap<String, Command> commandPool = new HashMap<>();
	static {
		commandPool.put("update", Command.UPDATE);
		commandPool.put("co", Command.CHECKOUT);
		commandPool.put("ci", Command.COMMIT);
		commandPool.put("import", Command.IMPORT);
		commandPool.put("add", Command.ADD);
		commandPool.put("remove", Command.REMOVE);
		commandPool.put("status", Command.STATUS);
		commandPool.put("log", Command.LOG);
		commandPool.put("tag", Command.TAG);
		commandPool.put("rtag", Command.RTAG);
		commandPool.put("admin", Command.ADMIN);
		commandPool.put("diff", Command.DIFF);
	}
	
	@Override
	public void executeCommand(ICVSRepositoryLocation repositoryLocation,
		IContainer localRoot, String command, String[] globalOptions,
		String[] localOptions, String[] arguments) throws CVSException {
		execute(repositoryLocation, CVSWorkspaceRoot.getCVSFolderFor(localRoot), command,
			globalOptions, localOptions, arguments);
	}
	
	public static void execute(
		ICVSRepositoryLocation cvsRepositoryLocation, ICVSFolder cvsLocalRoot,
		String command, String[] globalOptions, String[] localOptions,
		String[] arguments) throws CVSException {
		// test arguments
		Assert.assertNotNull(cvsRepositoryLocation);
		Assert.assertNotNull(cvsLocalRoot);
		Assert.assertNotNull(command);
		Assert.assertNotNull(globalOptions);
		Assert.assertNotNull(localOptions);
		Assert.assertNotNull(arguments);
		Assert.assertTrue(cvsLocalRoot.exists());

		// get command instance
		Command cvsCommand = commandPool.get(command);
			
		// get global options
		List<CustomGlobalOption> globals = new ArrayList<>();
		for (String globalOption : globalOptions) {
			globals.add(new CustomGlobalOption(globalOption));
		}
		GlobalOption[] cvsGlobalOptions = globals.toArray(new GlobalOption[globals.size()]);
		
		// get local options
		List<CustomLocalOption> locals = new ArrayList<>();
		for (int i = 0; i < localOptions.length; i++) {
			String option = localOptions[i];
			String argument = null;
			if ((i < localOptions.length - 1) && (localOptions[i + 1].charAt(0) != '-')) {
				argument = localOptions[++i];
			}
			locals.add(new CustomLocalOption(option, argument));
		}
		LocalOption[] cvsLocalOptions = locals.toArray(new LocalOption[locals.size()]);
		
		// execute command
		IProgressMonitor monitor = new NullProgressMonitor();
		Session session = new Session(cvsRepositoryLocation, cvsLocalRoot);
		try {
			session.open(monitor, true /* open for modification */);
			IStatus status = cvsCommand.execute(session,
				cvsGlobalOptions, cvsLocalOptions, arguments, null, monitor);
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				throw new CVSClientException("Eclipse client returned non-ok status: " + status);
			}
		} finally {
			session.close();
			monitor.done();
		}
	}

	private static class CustomGlobalOption extends GlobalOption {
		public CustomGlobalOption(String option) {
			super(option);
		}
	}

	private static class CustomLocalOption extends LocalOption {
		public CustomLocalOption(String option, String arg) {
			super(option, arg);
		}
	}
}
