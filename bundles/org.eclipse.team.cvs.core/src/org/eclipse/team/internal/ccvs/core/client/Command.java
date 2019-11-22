/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 179174 CVS client sets timestamps back when replacing
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;

/**
 * Abstract base class for command requests.
 * Provides a framework for implementing command execution.
 */
public abstract class Command extends Request {
	/*** Command singleton instances ***/
	public final static Add ADD = new Add();
	public final static Admin ADMIN = new Admin();
	public final static Annotate ANNOTATE = new Annotate();
	public final static Checkout CHECKOUT = new CheckoutWithOverwrite();
	public final static Commit COMMIT = new Commit();
	public final static Diff DIFF = new Diff();
	public final static RDiff RDIFF = new RDiff();
	public final static Editors EDITORS = new Editors();
	public final static Import IMPORT = new Import();
	public final static Log LOG = new Log();
	public final static Remove REMOVE = new Remove();
	public final static Status STATUS = new Status();
	public final static Tag TAG = new Tag();
	// The CUSTOM_TAG command has special handling for added and removed resources.
	// This behavior supports branching with local changes in the workspace
	public final static Tag CUSTOM_TAG = new Tag(true);
	public final static RTag RTAG = new RTag();
	public final static Update UPDATE = new Update();
	public final static Update REPLACE = new Replace();
	public final static SyncUpdate SYNCUPDATE = new SyncUpdate();
	public final static Version VERSION = new Version();
	public final static NOOPCommand NOOP = new NOOPCommand();
	
	// Empty argument array
	public final static String[] NO_ARGUMENTS = new String[0];

	/*** Global options ***/
	// Empty global option array
	public static final GlobalOption[] NO_GLOBAL_OPTIONS = new GlobalOption[0];
	// Do not change file contents
	public static final GlobalOption DO_NOT_CHANGE = new GlobalOption("-n");  //$NON-NLS-1$
	// Do not record this operation into CVS command history
	public static final GlobalOption DO_NOT_LOG = new GlobalOption("-l");  //$NON-NLS-1$
	// Make new working files read-only
	public static final GlobalOption MAKE_READ_ONLY = new GlobalOption("-r"); //$NON-NLS-1$
	// Trace command execution
	public static final GlobalOption TRACE_EXECUTION = new GlobalOption("-t"); //$NON-NLS-1$

	/*** Global options: quietness ***/
	// Don't be quiet (normal verbosity)
	public static final QuietOption VERBOSE = new QuietOption(""); //$NON-NLS-1$
	// Be somewhat quiet (suppress informational messages)
	public static final QuietOption PARTLY_QUIET = new QuietOption("-q"); //$NON-NLS-1$
	// Be really quiet (silent but for serious problems)
	public static final QuietOption SILENT = new QuietOption("-Q"); //$NON-NLS-1$

	/*** Local options: common to many commands ***/
	// Empty local option array
	public static final LocalOption[] NO_LOCAL_OPTIONS = new LocalOption[0];
	// valid for: annotate checkout commit diff export log rdiff remove rtag status tag update
	public static final LocalOption RECURSE = new LocalOption("-R"); //$NON-NLS-1$
	public static final LocalOption DO_NOT_RECURSE = new LocalOption("-l"); //$NON-NLS-1$	
	// valid for: checkout export update
	public static final LocalOption PRUNE_EMPTY_DIRECTORIES = new LocalOption("-P"); //$NON-NLS-1$
	// valid for: checkout export update
	public static final LocalOption MESSAGE_OPTION = new LocalOption("-m"); //$NON-NLS-1$

	/*** Local options: keyword substitution mode ***/
	// valid for: add admin checkout export import update
	private static final Map<String, KSubstOption> ksubstOptionMap = new HashMap<>();
	public static final KSubstOption KSUBST_BINARY = new KSubstOption("-kb"); //$NON-NLS-1$
	public static final KSubstOption KSUBST_TEXT = new KSubstOption("-ko"); //$NON-NLS-1$
	public static final KSubstOption KSUBST_TEXT_EXPAND = new KSubstOption("-kkv"); //$NON-NLS-1$
	public static final KSubstOption KSUBST_TEXT_EXPAND_LOCKER = new KSubstOption("-kkvl"); //$NON-NLS-1$
	public static final KSubstOption KSUBST_TEXT_VALUES_ONLY = new KSubstOption("-kv"); //$NON-NLS-1$
	public static final KSubstOption KSUBST_TEXT_KEYWORDS_ONLY = new KSubstOption("-kk"); //$NON-NLS-1$

	/*** Default command output listener ***/
	protected static final ICommandOutputListener DEFAULT_OUTPUT_LISTENER = new CommandOutputListener();
	
	/**
	 * Prevents client code from instantiating us.
	 */
	protected Command() { }

	/**
	 * Provides the default command output listener which is used to accumulate errors.
	 * 
	 * Subclasses can override this method in order to properly interpret information
	 * received from the server.
	 */
	protected ICommandOutputListener getDefaultCommandOutputListener() {
		return DEFAULT_OUTPUT_LISTENER;
	}

	/**
	 * Sends the command's arguments to the server.
	 * [template method]
	 * <p>
	 * The default implementation sends all arguments.  Subclasses may override
	 * this method to provide alternate behaviour.
	 * </p>
	 * 
	 * @param session the CVS session
	 * @param arguments the arguments that were supplied by the caller of execute()
	 */
	protected void sendArguments(Session session, String[] arguments) throws CVSException {
		for (int i = 0; i < arguments.length; ++i) {
			session.sendArgument(arguments[i]);
		}
	}

	/**
	 * Describes the local resource state to the server prior to command execution.
	 * [template method]
	 * <p>
	 * Commands must override this method to inform the server about the state of
	 * local resources using the Entries, Modified, Unchanged, and Questionable
	 * requests as needed.
	 * </p>
	 * <p>
	 * This method should return the resources that are of interest to the
	 * <code>Command#commandFinished()</code> method. In most cases, it
	 * is the same resources that are provided but in some cases (e.g. Commit)
	 * the resources to be passed to the above method are different.
	 * </p>
	 * 
	 * @param session the CVS session
	 * @param globalOptions the global options for the command
	 * @param localOptions the local options for the command
	 * @param resources the resource arguments for the command
	 * @param monitor the progress monitor
	 * @return ICVSResource[]
	 */
	protected abstract ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException;

	/**
	 * Cleans up after command execution.
	 * [template method] 
	 * <p>
	 * The default implementation is a no-op.  Subclasses may override this
	 * method to follow up command execution on the server with clean up
	 * operations on local resources.
	 * </p>
	 *
	 * @param session the CVS session
	 * @param globalOptions the global options for the command
	 * @param localOptions the local options for the command
	 * @param resources the resource arguments for the command
	 * @param monitor the progress monitor
	 * @param status the status accumulated so far. If the code == CVSStatus.SERVER_ERROR
	 *    then the command failed
	 * @return status the status past in plus any additional status accumulated during the finish
	 */
	protected IStatus commandFinished(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor,
		IStatus status) throws CVSException {
			return status;
	}

	/**
	 * Sends the local working directory path prior to command execution.
	 * [template method]
	 * <p>
	 * The default implementation sends the paths of local root directory
	 * (assuming it exists).  Subclasses may override this method to provide
	 * alternate behaviour.
	 * </p>
	 * 
	 * @param session the CVS session
	 */
	protected void sendLocalWorkingDirectory(Session session) throws CVSException {
		ICVSFolder localRoot = session.getLocalRoot();
		if (localRoot.isCVSFolder()) {
			session.sendLocalRootDirectory();
		} else {
			session.sendConstructedRootDirectory();
		}
	}

	/**
	 * Computes an array of ICVSResources corresponding to command arguments.
	 * [template method]
	 * <p>
	 * The default implementation assumes that all arguments supplied to the
	 * command represent resources in the local root that are to be manipulated.
	 * Subclasses must override this method if this assumption does not hold.
	 * </p>
	 * @param session the CVS session
	 * @param localOptions the command local options
	 * @param arguments the command arguments
	 * @return the resource arguments for the command
	 */
	protected ICVSResource[] computeWorkResources(Session session,
		LocalOption[] localOptions, String[] arguments) throws CVSException {
		ICVSFolder localRoot = session.getLocalRoot();

		if (arguments.length == 0) {
			// As a convenience, passing no arguments to the CVS command
			// implies the command will operate on the local root folder.
			return new ICVSResource[] { localRoot };
		} else {
			// Assume all arguments represent resources that are descendants
			// of the local root folder.
			ICVSResource[] resources = new ICVSResource[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				ICVSResource resource = localRoot.getChild(arguments[i]);				
				// file does not exist, it could have been deleted. It doesn't matter
				// which type we return since only the name of the resource is used
				// and sent to the server.
				if(resource==null) {
					if(localRoot.getName().length()==0) {
						// Return a folder because it is the safest choice when
						// localRoot is a handle to the IWorkspaceRoot!
						resource = localRoot.getFolder(arguments[i]);
					} else {
						resource = localRoot.getFile(arguments[i]);
					}
				}
				resources[i] = resource;
			}
			return resources;
		}
	}

	/**
	 * Send an array of Resources.
	 * @param localOptions 
	 * 
	 * @see Command#sendFileStructure(ICVSResource,IProgressMonitor,boolean,boolean,boolean)
	 */
	protected void sendFileStructure(Session session, ICVSResource[] resources,
		LocalOption[] localOptions, boolean emptyFolders, IProgressMonitor monitor) throws CVSException {
		checkResourcesManaged(session, resources);
		
		new FileStructureVisitor(session, localOptions, emptyFolders, true).visit(session, resources, monitor);
	}

	/**
	 * Checks that all work resources are managed.
	 * @param session TODO
	 * @param resources the resource arguments for the command
	 * 
	 * @throws CVSException if some resources are not managed
	 */
	protected void checkResourcesManaged(Session session, ICVSResource[] resources) throws CVSException {
		for (int i = 0; i < resources.length; ++i) {
			ICVSFolder folder;
			if (resources[i].isFolder()) {
				folder = (ICVSFolder) resources[i];
			}
			else {
				folder = resources[i].getParent();
			}
			if (!folder.isCVSFolder() && folder.exists()) {
				IStatus status = new CVSStatus(IStatus.ERROR,CVSStatus.ERROR,NLS.bind(CVSMessages.Command_argumentNotManaged, new String[] { folder.getName() }),session.getLocalRoot());
				throw new CVSException(status);
			}
		}
	}
		
	/**
	 * Executes a CVS command.
	 * <p>
	 * Dispatches the commands, retrieves the results, and determines whether or
	 * not an error occurred.  A listener may be supplied to capture message text
	 * that would normally be written to the standard error and standard output
	 * streams of a command line CVS client.
	 * </p>
	 * @param session the open CVS session
	 * @param globalOptions the array of global options, or NO_GLOBAL_OPTIONS
	 * @param localOptions the array of local options, or NO_LOCAL_OPTIONS
	 * @param arguments the array of arguments (usually filenames relative to localRoot), or NO_ARGUMENTS
	 * @param listener the command output listener, or null to discard all messages
	 * @param monitor the progress monitor
	 * @return a status code indicating success or failure of the operation
	 * @throws CVSException if a fatal error occurs (e.g. connection timeout)
	 */
	public final IStatus execute(final Session session, final GlobalOption[] globalOptions,
		final LocalOption[] localOptions, final String[] arguments, final ICommandOutputListener listener,
		IProgressMonitor pm) throws CVSException {		
		final IStatus[] status = new IStatus[1];
		ICVSRunnable job = monitor -> {
			// update the global and local options
			GlobalOption[] gOptions = filterGlobalOptions(session, globalOptions);
			LocalOption[] lOptions = filterLocalOptions(session, gOptions, localOptions);
			
			// print the invocation string to the console
			if (session.isOutputToConsole() || Policy.isDebugProtocol()) {
				IPath commandRootPath;
				IResource resource = session.getLocalRoot().getIResource();
				if (resource == null) {
					commandRootPath = Path.EMPTY;
				} else {
					commandRootPath = resource.getFullPath();
				}
				String line = constructCommandInvocationString(commandRootPath, gOptions, lOptions, arguments);
				ConsoleListeners.getInstance().commandInvoked(session, line);
				if (Policy.isDebugProtocol()) Policy.printProtocolLine("CMD> " + line); //$NON-NLS-1$
			}
			
			// run the command
			try {
				session.setCurrentCommand(Command.this);
				status[0] = doExecute(session, gOptions, lOptions, arguments, listener, monitor);
				notifyConsoleOnCompletion(session, status[0], null);
			} catch (CVSException e1) {
				notifyConsoleOnCompletion(session, null, e1);
				throw e1;
			} catch (RuntimeException e2) {
				notifyConsoleOnCompletion(session, null, e2);
				throw e2;
			}
		};
		if (isWorkspaceModification()) {
			session.getLocalRoot().run(job, pm);
		} else {
			job.run(pm);
		}
		return status[0];
	}
	
	/**
	 * Return whether this command modifies the workspace.
	 * If <code>true</code> is returned, a scheduling rule on
	 * the session local root is obtained. Otherwise, no
	 * scheduling rule is obtained. By default, <code>true</code>
	 * is returned
	 * @return whether this command modifies the workspace
	 */
	protected boolean isWorkspaceModification() {
		return true;
	}

	private void notifyConsoleOnCompletion(Session session, IStatus status, Exception exception) {
		ConsoleListeners.getInstance().commandCompleted(session, status, exception);
		if (Policy.isDebugProtocol()) {
			if (status != null) Policy.printProtocolLine("RESULT> " + status.toString()); //$NON-NLS-1$
			else Policy.printProtocolLine("RESULT> " + exception.toString()); //$NON-NLS-1$
		}
	}

	protected IStatus doExecute(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, String[] arguments, ICommandOutputListener listener,
		IProgressMonitor monitor) throws CVSException {
		ICVSResource[] resources = null;
		/*** setup progress monitor ***/
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		Policy.checkCanceled(monitor);
		try {
			/*** prepare for command ***/
			// clear stale command state from previous runs
			session.setNoLocalChanges(DO_NOT_CHANGE.isElementOf(globalOptions));
			session.setModTime(null);

			/*** initiate command ***/
			// send global options
			for (GlobalOption globalOption : globalOptions) {
				globalOption.send(session);
			}
			Policy.checkCanceled(monitor);
			// send local options
			for (LocalOption localOption : localOptions) {
				localOption.send(session);
			}
			Policy.checkCanceled(monitor);
			// compute the work resources
			resources = computeWorkResources(session, localOptions, arguments);			
			Policy.checkCanceled(monitor);
			// send local working directory state contributes 48% of work
			resources = sendLocalResourceState(session, globalOptions, localOptions,
					resources, Policy.infiniteSubMonitorFor(monitor, 48));
			Policy.checkCanceled(monitor);
			// escape file names, see bug 149683
			for(int i = 0; i < arguments.length; i++){
				if(arguments[i].startsWith("-")){ //$NON-NLS-1$
					arguments[i] = "./" + arguments[i]; //$NON-NLS-1$
				}
			}
			// send arguments
			sendArguments(session, arguments);
			// send local working directory path
			sendLocalWorkingDirectory(session);

			// if no listener was provided, use the command's default in order to get error reporting
			if (listener == null) listener = getDefaultCommandOutputListener();

			/*** execute command and process responses ***/
			// Processing responses contributes 50% of work.
			IStatus status = executeRequest(session, listener, Policy.subMonitorFor(monitor, 50));

			// Finished adds last 2% of work.
			status = commandFinished(session, globalOptions, localOptions, resources, Policy.subMonitorFor(monitor, 2),
				status);
			return status;
		} finally {			
			monitor.done();
		}
	}
	
	/**
	 * Constucts the CVS command invocation string corresponding to the arguments.
	 * 
	 * @param globalOptions the global options
	 * @param localOption the local options
	 * @param arguments the arguments
	 * @return the command invocation string
	 */
	private String constructCommandInvocationString(IPath commandRootPath, GlobalOption[] globalOptions,
		LocalOption[] localOptions, String[] arguments) {
		StringBuilder commandLine = new StringBuilder("cvs"); //$NON-NLS-1$
		for (int i = 0; i < globalOptions.length; ++i) {
			String option = globalOptions[i].toString();
			if (option.length() == 0) continue;
			commandLine.append(' ');
			commandLine.append(option);
		}
		commandLine.append(' ');
		commandLine.append(getRequestId());
		for (int i = 0; i < localOptions.length; ++i) {
			String option = localOptions[i].toString();
			if (option.length() == 0) continue;
			commandLine.append(' ');
			commandLine.append(option);
		}
		for (int i = 0; i < arguments.length; ++i) {
			if (arguments[i].length() == 0) continue;
			commandLine.append(" \""); //$NON-NLS-1$
			IPath completePath = commandRootPath;
			if (!arguments[i].equals(Session.CURRENT_LOCAL_FOLDER)) {
				completePath = completePath.append(arguments[i]);
			}
			commandLine.append(completePath.toString());
			commandLine.append('"'); 
		}
		return commandLine.toString();
	}	

	/**
	 * Superclass for all CVS command options
	 */
	protected static abstract class Option {
		protected String option, argument;
		protected Option(String option, String argument) {
			this.option = option;
			this.argument = argument;
		}
		/**
		 * Determines if this option is an element of an array of options
		 * @param array the array of options
		 * @return true iff the array contains this option
		 */
		public boolean isElementOf(Option[] array) {
			return findOption(array, option) != null;
		}
		/**
		 * Returns the option part of the option
		 */
		String getOption() {
			return option;
		}
		/**
		 * Compares two options for equality.
		 * @param other the other option
		 */
		public boolean equals(Object other) {
			if (this == other) return true;
			if (other instanceof Option) {
				Option otherOption = (Option) other;
				return option.equals(otherOption.option);
			}
			return false;
		}
		/**
		 * Sends the option to a CVS server
		 * @param session the CVS session
		 */
		public abstract void send(Session session) throws CVSException;		
		/*
		 * To make debugging a tad easier.
		 */
		public String toString() {
			if (argument != null && argument.length() != 0) {
				return option + " \"" + argument + '"'; //$NON-NLS-1$
			} else {
				return option;
			}
		}
	}	
	/**
	 * Option subtype for global options that are common to all commands.
	 */
	public static class GlobalOption extends Option {
		protected GlobalOption(String option) {
			super(option, null);
		}
		public void send(Session session) throws CVSException {
			session.sendGlobalOption(option);
		}
		/**
		 * Add the given global option to the end of the provided list
		 *
		 * @param newOption
		 * @param options
		 * @return GlobalOption[]
		 */
		protected GlobalOption[] addToEnd(GlobalOption[] options) {
			GlobalOption[] globalOptions = new GlobalOption[options.length + 1];
			System.arraycopy(options, 0, globalOptions, 0, options.length);
			globalOptions[globalOptions.length - 1] = this;
			return globalOptions;
		}
	}
	/**
	 * Option subtype for global quietness options.
	 */
	public static final class QuietOption extends GlobalOption {
		private QuietOption(String option) {
			super(option);
		}
		public void send(Session session) throws CVSException {
			if (option.length() != 0) super.send(session);
		}
	}
	/**
	 * Option subtype for local options that vary from command to command.
	 */
	public static class LocalOption extends Option {
		protected LocalOption(String option) {
			super(option, null);
		}
		protected LocalOption(String option, String argument) {
			super(option, argument);
		}
		public void send(Session session) throws CVSException {
			session.sendArgument(option);
			if (argument != null) session.sendArgument(argument);
		}
		public LocalOption[] addTo(LocalOption[] options) {
			if (this.isElementOf(options)) {
				return options;
			}
			LocalOption[] newOptions = new LocalOption[options.length + 1];
			System.arraycopy(options, 0, newOptions, 0, options.length);
			newOptions[options.length] = this;
			return newOptions;
		}
		public LocalOption[] removeFrom(LocalOption[] options) {
			if (!this.isElementOf(options)) {
				return options;
			}
			List<LocalOption> result = new ArrayList<>();
			for (LocalOption option : options) {
				if (!option.equals(this)) {
					result.add(option);
				}
			}
			return result.toArray(new LocalOption[result.size()]);
		}
	}
	/**
	 * Options subtype for keyword substitution options.
	 */
	public static class KSubstOption extends LocalOption {
		private boolean isUnknownMode;
		private KSubstOption(String option) {
			this(option, false);
		}
		private KSubstOption(String option, boolean isUnknownMode) {
			super(option);
			this.isUnknownMode = isUnknownMode;
			ksubstOptionMap.put(option, this);
		}
		/**
		 * Gets the KSubstOption instance for the specified mode.
		 * 
		 * @param mode the mode, e.g. -kb
		 * @return an instance for that mode
		 */
		public static KSubstOption fromMode(String mode) {
			if (mode.length() == 0) mode = "-kkv"; // use default //$NON-NLS-1$
			KSubstOption option = ksubstOptionMap.get(mode);
			if (option == null) option = new KSubstOption(mode, true);
			return option;
		}
		/**
		 * Gets the KSubstOption instance for the specified file.
		 *
		 * @param file the file to get the option for
		 * @return an instance for that mode
		 */
		public static KSubstOption fromFile(IFile file) {
			if (CVSProviderPlugin.isText(file))
				return getDefaultTextMode();
			return KSUBST_BINARY;
		}
		/**
		 * Returns an array of all valid modes.
		 */
		public static KSubstOption[] getAllKSubstOptions() {
			return ksubstOptionMap.values().toArray(new KSubstOption[ksubstOptionMap.size()]);
		}
		/**
		 * Returns the entry line mode string for this instance. Note that it might return blank strings
		 * for certain options. For UI, use {@link #toMode()} which will always return the a string
		 * containing the keyword substitution.
		 */
		public String toEntryLineMode() {
			if (KSUBST_TEXT_EXPAND.equals(this)) return ""; //$NON-NLS-1$
			return getOption();
		}
		
		/**
		 * Returns the entry line mode string for this instance.
		 */
		public String toMode(){
			return getOption();
		}
		
		/**
		 * Returns true if the substitution mode requires no data translation
		 * during file transfer.
		 */
		public boolean isBinary() {
			return KSUBST_BINARY.equals(this);
		}
		/**
		 * Returns a short localized text string describing this mode.
		 */
		public String getShortDisplayText() {
			if (isUnknownMode)
				return NLS.bind(CVSMessages.KSubstOption_unknown_short, new String[] { option }); 
			if (option.equals("-kb")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kb_short;
			if (option.equals("-kkv")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kkv_short;
			if (option.equals("-ko")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__ko_short;
			if (option.equals("-kk")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kk_short;
			if (option.equals("-kv")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kv_short;
			if (option.equals("-kkvl")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kkvl_short;
			return NLS.bind(CVSMessages.KSubstOption_unknown_short, new String[] { option }); 
		}
		/**
		 * Returns a long localized text string describing this mode.
		 */
		public String getLongDisplayText() {
			if (isUnknownMode)
				return NLS.bind(CVSMessages.KSubstOption_unknown_long, new String[] { option }); 
			if (option.equals("-kb")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kb_long;
			if (option.equals("-kkv")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kkv_long;
			if (option.equals("-ko")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__ko_long;
			if (option.equals("-kk")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kk_long;
			if (option.equals("-kv")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kv_long;
			if (option.equals("-kkvl")) //$NON-NLS-1$
				return CVSMessages.KSubstOption__kkvl_long;
			return NLS.bind(CVSMessages.KSubstOption_unknown_long, new String[] { option }); 
		}
		/**
		 * Return the text mode that will be used by default
		 */
		public static KSubstOption getDefaultTextMode() {
			return CVSProviderPlugin.getPlugin().getDefaultTextKSubstOption();
		}
	}

	/**
	 * Makes a -m log message option.
	 * Valid for: add commit import
	 */
	public static LocalOption makeArgumentOption(LocalOption option, String argument) {
		if(argument == null) {
			argument = ""; //$NON-NLS-1$
		}
		return new LocalOption(option.getOption(), argument);  
	}
	
	/**
	 * Makes a -r or -D option for a tag.
	 * Valid for: checkout export history rdiff update
	 */
	public static LocalOption makeTagOption(CVSTag tag) {
		int type = tag.getType();
		switch (type) {
			case CVSTag.BRANCH:
			case CVSTag.VERSION:
				return new LocalOption("-r", tag.getName()); //$NON-NLS-1$
			case CVSTag.DATE:
				return new LocalOption("-D", tag.getName()); //$NON-NLS-1$
			default:
				// tag must not be HEAD
				throw new IllegalArgumentException(CVSMessages.Command_invalidTag); 
		}
	}

	/**
	 * Find a specific option in an array of options
	 * @param array the array of options
	 * @param option the option string to search for
	 * @return the first element matching the option string, or null if none
	 */
	public static Option findOption(Option[] array, String option) {
		for (int i = 0; i < array.length; ++i) {
			if (array[i].getOption().equals(option)) return array[i];
		}
		return null;
	}

	/**
	 * Collect all arguments of a specific option from an array of options
	 * @param array the array of options
	 * @param option the option string to search for
	 * @return an array of all arguments of belonging to matching options
	 */
	protected static String[] collectOptionArguments(Option[] array, String option) {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < array.length; ++i) {
			if (array[i].getOption().equals(option)) {
				list.add(array[i].argument);
			}
		}
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * Allows commands to filter the set of global options to be sent.
	 * This method invokes the method of the same name on the session
	 * itself in order to get any session wide or globally set options.
	 * Subclasses that override this method should call the superclass.
	 * 
	 * @param session the session
	 * @param globalOptions the global options, read-only
	 * @return the filtered global options
	 */
	protected GlobalOption[] filterGlobalOptions(Session session, GlobalOption[] globalOptions) {
		return session.filterGlobalOptions(globalOptions);
	}
	
	/**
	 * Allows commands to filter the set of local options to be sent.
	 * Subclasses that override this method should call the superclass.
	 * 
	 * @param session the session
	 * @param globalOptions the global options, read-only
	 * @param localOptions the local options, read-only
	 * @return the filtered local options
	 */
	protected LocalOption[] filterLocalOptions(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions) {
		return localOptions;
	}
	
	/**
	 * Execute a CVS command on an array of ICVSResource. This method simply converts
	 * the ICVSResource to String paths relative to the local root of the session and
	 * invokes <code>execute(Session, GlobalOption[], LocalOption[], String[], ICommandOutputListener, IProgressMonitor)</code>.
	 *
	 * @param session the open CVS session
	 * @param globalOptions the array of global options, or NO_GLOBAL_OPTIONS
	 * @param localOptions the array of local options, or NO_LOCAL_OPTIONS
	 * @param arguments the array of ICVSResource to be operated on
	 * @param listener the command output listener, or null to discard all messages
	 * @param monitor the progress monitor
	 * @return a status code indicating success or failure of the operation
	 * @throws CVSException if a fatal error occurs (e.g. connection timeout)
	 * 
	 * @see Command#execute(Session, GlobalOption[], LocalOption[], String[], ICommandOutputListener, IProgressMonitor)
	 */
	public final IStatus execute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, ICVSResource[] arguments, 
		ICommandOutputListener listener, IProgressMonitor pm) throws CVSException {
		
		String[] stringArguments = convertArgumentsForOpenSession(arguments, session);
		return execute(session, globalOptions, localOptions, stringArguments, listener, pm);
	}
	
	protected String[] convertArgumentsForOpenSession(ICVSResource[] arguments, Session openSession) throws CVSException {
		// Convert arguments
		List<String> stringArguments = new ArrayList<>(arguments.length);
		for (ICVSResource argument : arguments) {
			stringArguments.add(argument.getRelativePath(openSession.getLocalRoot()));
		}
		return stringArguments.toArray(new String[stringArguments.size()]);
	}
	
	/**
	 * Method mergeStatus.
	 * @param status
	 * @param cVSStatus
	 * @return IStatus
	 */
	protected IStatus mergeStatus(IStatus accumulatedStatus, IStatus newStatus) {
		if (accumulatedStatus.isMultiStatus()) {
			((MultiStatus)accumulatedStatus).merge(newStatus);
			return accumulatedStatus;
		}
		if (accumulatedStatus.isOK()) return newStatus;
		if (newStatus.isOK()) return accumulatedStatus;
		MultiStatus result = new MultiStatus(CVSProviderPlugin.ID, IStatus.INFO,
				new IStatus[] {accumulatedStatus, newStatus},
				NLS.bind(CVSMessages.Command_warnings, new String[] { getDisplayText() }), null);
		return result;
	}
}
