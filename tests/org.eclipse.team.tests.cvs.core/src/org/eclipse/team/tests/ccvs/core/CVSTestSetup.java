/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;
import java.io.*;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSCommunicationException;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;

public class CVSTestSetup extends TestSetup {
	public static final String REPOSITORY_LOCATION;
	public static final boolean INITIALIZE_REPO;
	public static final boolean DEBUG;
	public static final boolean LOCAL_REPO;
	public static final String RSH;
	public static final int WAIT_FACTOR;
	public static final int COMPRESSION_LEVEL;
	public static final boolean FAIL_IF_EXCEPTION_LOGGED;
    public static final boolean RECORD_PROTOCOL_TRAFFIC;
    public static final boolean ENSURE_SEQUENTIAL_ACCESS;
    public static final boolean FAIL_ON_BAD_DIFF;
    public static final int TIMEOUT = 600;
	
	public static CVSRepositoryLocation repository;
	public static CVSTestLogListener logListener;
	
	// Static initializer for constants
	static {
		loadProperties();
		REPOSITORY_LOCATION = System.getProperty("eclipse.cvs.repository");
		INITIALIZE_REPO = Boolean.valueOf(System.getProperty("eclipse.cvs.initrepo", "false")).booleanValue();
		DEBUG = Boolean.valueOf(System.getProperty("eclipse.cvs.debug", "false")).booleanValue();
		RSH = System.getProperty("eclipse.cvs.rsh", "rsh");
		LOCAL_REPO = Boolean.valueOf(System.getProperty("eclipse.cvs.localRepo", "false")).booleanValue();
		WAIT_FACTOR = Integer.parseInt(System.getProperty("eclipse.cvs.waitFactor", "1"));
		COMPRESSION_LEVEL = Integer.parseInt(System.getProperty("eclipse.cvs.compressionLevel", "0"));
		FAIL_IF_EXCEPTION_LOGGED = Boolean.valueOf(System.getProperty("eclipse.cvs.failLog", "true")).booleanValue();
        RECORD_PROTOCOL_TRAFFIC = Boolean.valueOf(System.getProperty("eclipse.cvs.recordProtocolTraffic", "false")).booleanValue();
        ENSURE_SEQUENTIAL_ACCESS = Boolean.valueOf(System.getProperty("eclipse.cvs.sequentialAccess", "false")).booleanValue();
        FAIL_ON_BAD_DIFF = Boolean.valueOf(System.getProperty("eclipse.cvs.failOnBadDiff", "false")).booleanValue();
	}

	public static void loadProperties() {
		String propertiesFile = System.getProperty("eclipse.cvs.properties");
		if (propertiesFile == null) return;
		File file = new File(propertiesFile);
		if (file.isDirectory()) file = new File(file, "repository.properties");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				for (String line; (line = reader.readLine()) != null; ) {
					if (line.startsWith("#")) continue;					
					int sep = line.indexOf("=");
					String property = line.substring(0, sep).trim();
					String value = line.substring(sep + 1).trim();
					System.setProperty("eclipse.cvs." + property, value);
				}
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			System.err.println("Could not read repository properties file: " + file.getAbsolutePath());
		}
	}	

	/**
	 * Constructor for CVSTestSetup.
	 */
	public CVSTestSetup(Test test) {
		super(test);
	}

	public static void executeRemoteCommand(ICVSRepositoryLocation repository, String commandLine) {
		if (! LOCAL_REPO) {
			commandLine = RSH + " " + repository.getHost() + " -l " + repository.getUsername() + " " + commandLine;
		}
		int returnCode = executeCommand(commandLine, null, null);
		if (returnCode != -1 && returnCode != 0) {
			System.err.println("Remote command returned " + returnCode + ": " + commandLine);
		}
	}
	
	/**
	 * Executes a command.
	 * Returns the command's return code, or -1 on failure.
	 * 
	 * @param commandLine the local command line to run
	 * @param environment the new environment variables, or null to inherit from parent process
	 * @param workingDirectory the new workingDirectory, or null to inherit from parent process
	 */
	public static int executeCommand(String commandLine, String[] environment, File workingDirectory) {
		PrintStream debugStream = CVSTestSetup.DEBUG ? System.out : null;
		try {
			if (debugStream != null) {
				// while debugging, dump CVS command line client results to stdout
				// prefix distinguishes between message source stream
				debugStream.println();
				printPrefixedLine(debugStream, "CMD> ", commandLine);
				if (workingDirectory != null) printPrefixedLine(debugStream, "DIR> ", workingDirectory.toString());
			}
			Process cvsProcess = Runtime.getRuntime().exec(commandLine, environment, workingDirectory);
			// stream output must be dumped to avoid blocking the process or causing a deadlock
			startBackgroundPipeThread(cvsProcess.getErrorStream(), debugStream, "ERR> ");
			startBackgroundPipeThread(cvsProcess.getInputStream(), debugStream, "MSG> ");

			int returnCode = cvsProcess.waitFor();			
			if (debugStream != null) debugStream.println("RESULT> " + returnCode);
			return returnCode;
		} catch (IOException e) {
			printPrefixedLine(System.err, "Unable to execute command: ", commandLine);
			e.printStackTrace(System.err);
		} catch (InterruptedException e) {
			printPrefixedLine(System.err, "Unable to execute command: ", commandLine);
			e.printStackTrace(System.err);
		}
		return -1;
	}
	
	private static void startBackgroundPipeThread(final InputStream is, final PrintStream os,
		final String prefix) {
		new Thread() {
			public void run() {
				BufferedReader reader = null;
				try {
					try {
						reader = new BufferedReader(new InputStreamReader(is));
						for (;;) {
							String line = reader.readLine();
							if (line == null) break;
							if (os != null) printPrefixedLine(os, prefix, line);
						}
					} finally {
						if (reader != null) reader.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private static void printPrefixedLine(PrintStream os, String prefix, String line) {
		os.print(prefix);
		os.println(line.substring(0, Math.min(line.length(), 256))); // trim long lines
	}

	/*
	 * Use rsh to delete any contents of the repository and initialize it again
	 */
	private static void initializeRepository(CVSRepositoryLocation repository) {
		String repoRoot = repository.getRootDirectory();
		executeRemoteCommand(repository, "rm -rf " + repoRoot);
		executeRemoteCommand(repository, "cvs -d " + repoRoot + " init");
	}
	
	public void setUp() throws CoreException {
		if (repository == null) {
			repository = setupRepository(REPOSITORY_LOCATION);
		}
		CVSProviderPlugin.getPlugin().setCompressionLevel(COMPRESSION_LEVEL);
		CVSProviderPlugin.getPlugin().setTimeout(TIMEOUT);
		// Add a log listener so we can ensure that nothing is logged during a test
		if (logListener == null) {
			logListener = new CVSTestLogListener();
			Platform.addLogListener(logListener);
		}
	}

	protected CVSRepositoryLocation setupRepository(String location) throws CVSException {
		
		// Validate that we can connect, also creates and caches the repository location. This
		// is important for the UI tests.
		CVSRepositoryLocation repository = (CVSRepositoryLocation)KnownRepositories.getInstance().getRepository(location);
		KnownRepositories.getInstance().addRepository(repository, false);
		repository.setUserAuthenticator(new TestsUserAuthenticator());
		
		// Give some info about which repository the tests are running with
		System.out.println("Connecting to: " + repository.getHost() + ":" + repository.getMethod().getName());
		
		try {
			try {
				repository.validateConnection(new NullProgressMonitor());
			} catch (CVSCommunicationException e) {
				// Try once more, just in case it is a transient server problem
				repository.validateConnection(new NullProgressMonitor());
			} catch (OperationCanceledException e) {
				// This can occur if authentication fails
				throw new CVSException(new CVSStatus(IStatus.ERROR, "The connection was canceled, possibly due to an authentication failure."));
			}
		} catch (CVSException e) {
			System.out.println("Unable to connect to remote repository: " + repository.toString());
			System.out.println(e.getMessage());
			throw e;
		}
		
		// Initialize the repo if requested (requires rsh access)
		if( INITIALIZE_REPO ) {
			initializeRepository(repository);
		}
		
		return repository;
	}
	
	public void tearDown() throws CVSException {
		// Nothing to do here
	}

}

