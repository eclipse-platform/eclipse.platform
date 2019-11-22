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
 *     Red Hat Incorporated - is/setExecutable() code
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.QuietOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.Connection;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.core.streams.*;

/**
 * Maintains CVS communication state for the lifetime of a connection
 * to a remote repository.  This class covers the initialization, use,
 * and eventual shutdown of a dialogue between a CVS client and a
 * remote server.  This dialogue may be monitored through the use of
 * a console.
 * 
 * Initially the Session is in a CLOSED state during which communication
 * with the server cannot take place.  Once OPENED, any number of commands
 * may be issued serially to the server, one at a time.  When finished, the
 * Session MUST be CLOSED once again to prevent eventual local and/or
 * remote resource exhaustion.  The session can either be discarded, or
 * re-opened for use with the same server though no state is persisted from
 * previous connections except for console attributes.
 * 
 * CVSExceptions are thrown only as a result of unrecoverable errors.  Once
 * this happens, commands must no longer be issued to the server.  If the
 * Session is in the OPEN state, it is still the responsibility of the
 * caller to CLOSE it before moving on.
 */
public class Session {
	public static final String CURRENT_LOCAL_FOLDER = "."; //$NON-NLS-1$
	public static final String CURRENT_REMOTE_FOLDER = ""; //$NON-NLS-1$
	public static final String SERVER_SEPARATOR = "/"; //$NON-NLS-1$

	// default file transfer buffer size (in bytes)
	private static final int TRANSFER_BUFFER_SIZE = 8192;
	// update progress bar in increments of this size (in bytes)
	//   no incremental progress shown for files smaller than this size
	private static final int TRANSFER_PROGRESS_INCREMENT = 32768;

	public static final boolean IS_CRLF_PLATFORM = Arrays.equals(
		System.getProperty("line.separator").getBytes(), new byte[] { '\r', '\n' }); //$NON-NLS-1$
	
	private CVSRepositoryLocation location;
	private ICVSFolder localRoot;
	private boolean outputToConsole;
	private Connection connection = null;
	private String validRequests = null;
	private Date modTime = null;
	private boolean noLocalChanges = false;
	private boolean createBackups = true;
	private int compressionLevel = 0;
	private List<String> expansions;
	private Collection /* of ICVSFile */ textTransferOverrideSet = null;
	
	// state need to indicate whether 
	private boolean ignoringLocalChanges = false;

	// The resource bundle key that provides the file sending message
	private String sendFileTitleMessage;
	private Map<String,ResponseHandler> responseHandlers;
	
	// List of errors accumulated while the command is executing
	private List<IStatus> errors = new ArrayList<>();
	
	private Command currentCommand;

	/**
	 * Creates a new CVS session, initially in the CLOSED state.
	 * By default, command output is directed to the console.
	 * 
	 * @param location the CVS repository location used for this session
	 * @param localRoot represents the current working directory of the client
	 */
	public Session(ICVSRepositoryLocation location, ICVSFolder localRoot) {
		this(location, localRoot, true);
	}
	
	/**
	 * Creates a new CVS session, initially in the CLOSED state.
	 * 
	 * @param location the CVS repository location used for this session
	 * @param localRoot represents the current working directory of the client
	 * @param outputToConsole if true, command output is directed to the console
	 */
	public Session(ICVSRepositoryLocation location, ICVSFolder localRoot, boolean outputToConsole) {
		this.location = (CVSRepositoryLocation) location;
		this.localRoot = localRoot;
		this.outputToConsole = outputToConsole;
	}
	
	/*
	 * Add a module expansion receivered from the server.
	 * This is only used by the ModuleExpansionsHandler
	 */
	protected void addModuleExpansion(String expansion) {
		expansions.add(expansion);
	}
	
	/*
	 * Add a module expansion receivered from the server.
	 * This is only used by the ExpandModules command
	 */
	protected void resetModuleExpansion() {
		if (expansions == null) 
			expansions = new ArrayList<>();
		else
			expansions.clear();
	}
	
	/**
	 * Opens, authenticates and initializes a connection to the server specified
	 * for the remote location.
	 *
	 * @param monitor the progress monitor
	 * @throws IllegalStateException if the Session is not in the CLOSED state
	 */
	public void open(IProgressMonitor monitor) throws CVSException {
		open(monitor, true /* write access*/);
	}
	
	public void open(IProgressMonitor monitor, boolean writeAccess) throws CVSException {
		if (connection != null) throw new IllegalStateException();
		monitor = Policy.subMonitorFor(monitor, 100);
		boolean opened = false;	
	
		try {
			connection = getLocationForConnection(writeAccess).openConnection(Policy.subMonitorFor(monitor, 50));
			
			// If we're connected to a CVSNT server or we don't know the platform, 
			// accept MT. Otherwise don't.
			boolean useMT = ! (location.getServerPlatform() == CVSRepositoryLocation.CVS_SERVER);
			if ( ! useMT) {
				removeResponseHandler("MT"); //$NON-NLS-1$
			}
			
			// tell the server the names of the responses we can handle
			connection.writeLine("Valid-responses " + makeResponseList()); //$NON-NLS-1$
			// Flush in order to recieve the valid requests
			connection.flush();
	
			// ask for the set of valid requests
			IStatus status = Request.VALID_REQUESTS.execute(this, Policy.subMonitorFor(monitor, 40));
			if (!status.isOK()) {
				throw new CVSException(status);
			}
			
			// set the root directory on the server for this connection
			connection.writeLine("Root " + getRepositoryRoot()); //$NON-NLS-1$

			// enable compression
			compressionLevel = CVSProviderPlugin.getPlugin().getCompressionLevel();
			if (compressionLevel != 0 && isValidRequest("gzip-file-contents")) { //$NON-NLS-1$
				// Enable the use of CVS 1.8 per-file compression mechanism.
				// The newer Gzip-stream request seems to be problematic due to Java's
				// GZIPInputStream tendency to block on read() rather than to return a
				// partially filled buffer.  The latter option would be better since it
				// can make more effective use of the code dictionary, if it can be made
				// to work...
				connection.writeLine("gzip-file-contents " + Integer.toString(compressionLevel)); //$NON-NLS-1$
			} else {
				compressionLevel = 0;
			}
			
			// get the server platform if it is unknown
			if (CVSProviderPlugin.getPlugin().isDetermineVersionEnabled() && location.getServerPlatform() == CVSRepositoryLocation.UNDETERMINED_PLATFORM) {
				Command.VERSION.execute(this, location, Policy.subMonitorFor(monitor, 10));
			}
			opened = true;
		} finally {
			if (connection != null && ! opened) {
				close();
			}
			monitor.done();
		}
	}		
	
	/*
	 * Return the location to be used for this connection
	 */
	private CVSRepositoryLocation getLocationForConnection(boolean writeAccess) {
		return location;
	}

	/**
	 * Closes a connection to the server.
	 *
	 * @throws IllegalStateException if the Session is not in the OPEN state
	 */
	public void close() {
		if (connection != null) {
			connection.close();
			connection = null;
			validRequests = null;
		}
	}
	
	/**
	 * Determines if the server supports the specified request.
	 * 
	 * @param request the request string to verify
	 * @return true iff the request is supported
	 */
	public boolean isValidRequest(String request) {
		return (validRequests == null) ||
			(validRequests.contains(" " + request + " ")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public boolean isCVSNT() {
		if (location.getServerPlatform() == CVSRepositoryLocation.UNDETERMINED_PLATFORM) {
			return location.getRootDirectory().indexOf(':') == 1;
		} else {
			return location.getServerPlatform() == CVSRepositoryLocation.CVSNT_SERVER;
		}
	}
	
	/**
	 * Returns the local root folder for this session.
	 * <p>
	 * Generally speaking, specifies the "current working directory" at
	 * the time of invocation of an equivalent CVS command-line client.
	 * </p>
	 * 
	 * @return the local root folder
	 */
	public ICVSFolder getLocalRoot() {
		return localRoot;
	}

	/**
	 * Return the list of module expansions communicated from the server.
	 * 
	 * The modules expansions are typically a directory path of length 1
	 * but can be of greater length on occasion. 
	 */
	public String[] getModuleExpansions() {
		if (expansions == null) return new String[0];
		return expansions.toArray(new String[expansions.size()]);
	}
	
	/**
	 * Returns the repository root folder for this session.
	 * <p>
	 * Specifies the unqualified path to the CVS repository root folder
	 * on the server.
	 * </p>
	 * 
	 * @return the repository root folder
	 */
	public String getRepositoryRoot() {
		return location.getRootDirectory();
	}
	
	/**
	 * Returns an object representing the CVS repository location for this session.
	 * 
	 * @return the CVS repository location
	 */
	public ICVSRepositoryLocation getCVSRepositoryLocation() {
		return location;
	}
	
	/**
	 * Receives a line of text minus the newline from the server.
	 * 
	 * @return the line of text
	 */
	public String readLine() throws CVSException {
		return connection.readLine();
	}

	/**
	 * Sends a line of text followed by a newline to the server.
	 * 
	 * @param line the line of text
	 */
	public void writeLine(String line) throws CVSException {
		connection.writeLine(line);
	}

	/**
	 * Sends an argument to the server.
	 * <p>e.g. sendArgument("Hello\nWorld\n  Hello World") sends:</p>
	 * <pre>
	 *   Argument Hello \n
	 *   Argumentx World \n
	 *   Argumentx Hello World \n
	 * </pre>
	 *
	 * @param arg the argument to send
	 */
	public void sendArgument(String arg) throws CVSException {
		connection.write("Argument "); //$NON-NLS-1$
		int oldPos = 0;
		for (;;) {
			int pos = arg.indexOf('\n', oldPos);
			if (pos == -1) break;
			connection.writeLine(stripTrainingCR(arg.substring(oldPos, pos)));
			connection.write("Argumentx "); //$NON-NLS-1$
			oldPos = pos + 1;
		}
		connection.writeLine(stripTrainingCR(arg.substring(oldPos)));
	}

	/*
	 * Remove any trailing CR from the string
	 */
	private String stripTrainingCR(String string) {
		if (string.endsWith("\r")) { //$NON-NLS-1$
			return string.substring(0, string.length() - 1);
		}
		return string;
	}

	/**
	 * Sends a request to the server and flushes any output buffers.
	 * 
	 * @param requestId the string associated with the request to be executed
	 */
	public void sendRequest(String requestId) throws CVSException {
		connection.writeLine(requestId);
		connection.flush();
	}

	/**
	 * Sends an Is-modified request to the server without the file contents.
	 * <p>e.g. if a file called "local_file" was modified, sends:</p>
	 * <pre>
	 *   Is-modified local_file \n
	 * </pre><p>
	 * This request is an optimized form of the Modified request and may not
	 * be supported by all servers.  Hence, if it is not supported, a Modified
	 * request is sent instead along with the file's contents.  According to
	 * the CVS protocol specification, this request is only safe for use with
	 * some forms of: admin, annotate, diff, editors, log, watch-add, watch-off,
	 * watch-on, watch-remove, and watchers.<br>
	 * It may be possible to use this for: add, export, remove and status.<br>
	 * Do not use with co, ci, history, init, import, release, rdiff, rtag, or update.
	 * </p><p>
	 * Note: The most recent Directory request must have specified the file's
	 *       parent folder.
	 * </p>
	 * 
	 * @param file the file that was modified
	 * @see #sendModified
	 */
	public void sendIsModified(ICVSFile file, boolean isBinary, IProgressMonitor monitor)
		throws CVSException {
		if (isValidRequest("Is-modified")) { //$NON-NLS-1$
			connection.writeLine("Is-modified " + file.getName()); //$NON-NLS-1$
		} else {
			sendModified(file, isBinary, monitor);
		}
	}

	/**
	 * Sends a Static-directory request to the server.
	 * <p>
	 * Indicates that the directory specified in the most recent Directory request
	 * is static.  No new files will be checked out into this directory unless
	 * explicitly requested.
	 * </p>
	 */
	public void sendStaticDirectory() throws CVSException {
		connection.writeLine("Static-directory"); //$NON-NLS-1$
	}

	/**
	 * Sends a Directory request to the server with a constructed path.
	 * <p>
	 * It may be necessary at times to guess the remote path of a directory since
	 * it does not exist yet.  In this case we construct a remote path based on the
	 * local path by prepending the local path with the repository root.  This may
	 * not work in the presence of modules, so only use it for creating new projects.
	 * </p><p>
	 * Note: A CVS repository root can end with a trailing slash. The CVS server
	 *       expects that the repository root sent contain this extra slash. Including
	 *       the foward slash in addition to the absolute remote path makes for a string
	 *       containing two consecutive slashes (e.g. /home/cvs/repo//projecta/a.txt).
	 *       This is valid in the CVS protocol.
	 * </p>
	 */
	public void sendConstructedDirectory(String localDir) throws CVSException {
		String path = Util.appendPath(getRepositoryRoot(), localDir);
		sendDirectory(localDir, path);
	}

	/**
	 * Sends a Directory request to the server.
	 * <p>e.g. sendDirectory("local_dir", "remote_dir") sends:</p>
	 * <pre>
	 *   Directory local_dir
	 *   repository_root/remote_dir
	 * </pre>
	 * 
	 * @param localDir the path of the local directory relative to localRoot
	 * @param remoteDir the path of the remote directory relative to repositoryRoot
	 */
	public void sendDirectory(String localDir, String remoteDir) throws CVSException {
		if (localDir.length() == 0) localDir = CURRENT_LOCAL_FOLDER; 
		connection.writeLine("Directory " + localDir); //$NON-NLS-1$
		connection.writeLine(remoteDir);
	}

	/**
	 * Sends a Directory request for the localRoot.
	 */
	public void sendLocalRootDirectory() throws CVSException {
		sendDirectory(CURRENT_LOCAL_FOLDER, localRoot.getRemoteLocation(localRoot)); 
	}

	/**
	 * Sends a Directory request for the localRoot with a constructed path.
	 * <p>
	 * Use this when creating a new project that does not exist in the repository.
	 * </p>
	 * @see #sendConstructedDirectory
	 */
	public void sendConstructedRootDirectory() throws CVSException {
		sendConstructedDirectory(""); //$NON-NLS-1$
	}

	/**
	 * Sends an Entry request to the server.
	 * <p>
	 * Indicates that a file is managed (but it may not exist locally).  Sends
	 * the file's entry line to the server to indicate the version that was
	 * previously checked out.
	 * </p><p>
	 * Note: The most recent Directory request must have specified the file's
	 *       parent folder.
	 * </p>
	 * 
	 * @param entryLine the formatted entry line of the managed file.
	 */
	public void sendEntry(byte[] syncBytes, String serverTimestamp) throws CVSException {
		connection.write("Entry "); //$NON-NLS-1$
		if (serverTimestamp == null) {
			serverTimestamp = ""; //$NON-NLS-1$
		}
		int start = Util.getOffsetOfDelimeter(syncBytes, (byte)'/', 0, 3);
		if (start == -1) {
			// something is wrong with the entry line so just send it as is
			// and let the server report the error.
			connection.writeLine(new String(syncBytes));
			return;
		}
		int end = Util.getOffsetOfDelimeter(syncBytes, (byte)'/', start + 1, 1);
		if (end == -1) {
			// something is wrong with the entry line so just send it as is
			// and let the server report the error.
			connection.writeLine(new String(syncBytes));
			return;
		}
		connection.write(new String(syncBytes, 0, start + 1));
		connection.write(serverTimestamp);
		connection.writeLine(new String(syncBytes, end, syncBytes.length - end));
	}

	/**
	 * Sends a global options to the server.
	 * <p>e.g. sendGlobalOption("-n") sends:</p>
	 * <pre>
	 *   Global_option -n \n
	 * </pre>
	 * 
	 * @param option the global option to send
	 */
	public void sendGlobalOption(String option) throws CVSException {
		connection.writeLine("Global_option " + option); //$NON-NLS-1$
	}

	/**
	 * Sends an Unchanged request to the server.
	 * <p>e.g. if a file called "local_file" was not modified, sends:
	 * <pre>
	 *   Unchanged local_file \n
	 * </pre></p><p>
	 * Note: The most recent Directory request must have specified the file's
	 *       parent folder.
	 * </p>
	 * 
	 * @param file the file that was not modified
	 */
	public void sendUnchanged(ICVSFile file) throws CVSException {
		connection.writeLine("Unchanged " + file.getName()); //$NON-NLS-1$
	}
	
	/**
	 * Sends the Notify request to the server
	 */
	public void sendNotify(ICVSFolder parent, NotifyInfo info)
		throws CVSException {
		
		String filename = info.getName();
		connection.writeLine("Notify " + filename); //$NON-NLS-1$
		connection.writeLine(info.getServerLine(parent));
	}
		
	/**
	 * Sends a Questionable request to the server.
	 * <p>
	 * Indicates that a file exists locally but is unmanaged.  Asks the server
	 * whether or not the file should be ignored in subsequent CVS operations.
	 * The reply to the request occurs in the form of special M-type message
	 * responses prefixed with '?' when the next command is executed.
	 * </p><p>
	 * Note: The most recent Directory request must have specified the file's
	 *       parent folder.
	 * </p>
	 * 
	 * @param resource the local file or folder
	 */
	public void sendQuestionable(ICVSResource resource) throws CVSException {
		connection.writeLine("Questionable " + resource.getName()); //$NON-NLS-1$
	}

	/**
	 * Sends a Sticky tag request to the server.
	 * <p>
	 * Indicates that the directory specified in the most recent Directory request
	 * has a sticky tag or date, and sends the tag's contents.
	 * </p>
	 * 
	 * @param tag the sticky tag associated with the directory
	 */
	public void sendSticky(String tag) throws CVSException {
		connection.writeLine("Sticky " + tag); //$NON-NLS-1$
	}

	/**
	 * Sends a Modified request to the server along with the file contents.
	 * <p>e.g. if a file called "local_file" was modified, sends:</p>
	 * <pre>
	 *   Modified local_file \n
	 *   file_permissions \n
	 *   file_size \n
	 *   [... file_contents ...]
	 * </pre><p>
	 * Under some circumstances, Is-modified may be used in place of this request.<br>
	 * Do not use with history, init, import, rdiff, release, rtag, or update.
	 * </p><p>
	 * Note: The most recent Directory request must have specified the file's
	 *       parent folder.
	 * </p>
	 * 
	 * @param file the file that was modified
	 * @param isBinary if true the file is sent without translating line delimiters
	 * @param monitor the progress monitor
	 * @see #sendIsModified
	 */
	public void sendModified(ICVSFile file, boolean isBinary, IProgressMonitor monitor)
		throws CVSException {
			sendModified(file, isBinary, true, monitor);
	}

	public void sendModified(ICVSFile file, boolean isBinary, boolean sendBinary, IProgressMonitor monitor)
				throws CVSException {
		
		String filename = file.getName();
		connection.writeLine("Modified " + filename); //$NON-NLS-1$
		// send the default permissions for now
		if (file.isExecutable()) {
			connection.writeLine(ResourceSyncInfo.getDefaultExecutablePermissions());
		} else {
			connection.writeLine(ResourceSyncInfo.getDefaultPermissions());
		} 
		sendFile(file, isBinary, sendBinary, monitor);
	}
	
		/**
		 * Sends a file to the remote CVS server, possibly translating line delimiters.
		 * <p>
		 * Line termination sequences are automatically converted to linefeeds only
		 * (required by the CVS specification) when sending non-binary files.  This
		 * may alter the actual size and contents of the file that is sent.
		 * </p><p>
		 * Note: Non-binary files must be small enough to fit in available memory.
		 * </p>
		 * @param file the file to be sent
		 * @param isBinary is true if the file should be sent without translation
		 * @param monitor the progress monitor
		 */
		public void sendFile(ICVSStorage file, boolean isBinary, boolean sendBinary, IProgressMonitor monitor) throws CVSException {
			// check overrides
			if (textTransferOverrideSet != null &&
				textTransferOverrideSet.contains(file)) isBinary = false;
	
			// update progress monitor
			final String title = NLS.bind(getSendFileTitleMessage(), (new Object[]{ Util.toTruncatedPath(file, localRoot, 3) })); 
			monitor.subTask(NLS.bind(CVSMessages.Session_transferNoSize, new String[] { title })); 
			try {
				InputStream in = null;
				long length;
				try {
					if (isBinary && !sendBinary)  {
						byte[] bytes = "hello".getBytes();  //$NON-NLS-1$
						sendUncompressedBytes(new ByteArrayInputStream(bytes), bytes.length);
						return;
					}
					
					if (compressionLevel == 0) {
						in = file.getContents();
						if (!isBinary && IS_CRLF_PLATFORM){
							// uncompressed text
							byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
							in = new CRLFtoLFInputStream(in);
							ByteCountOutputStream counter = new ByteCountOutputStream();
							try {
								for (int count; (count = in.read(buffer)) != -1;) counter.write(buffer, 0, count);
							} finally {
								counter.close();
							}
							in.close();
							length = counter.getSize();
							in = new CRLFtoLFInputStream(file.getContents());
						} else {
							// uncompressed binary
							length = file.getSize();
						}
						in = new ProgressMonitorInputStream(in, length, TRANSFER_PROGRESS_INCREMENT, monitor) {
							protected void updateMonitor(long bytesRead, long bytesTotal, IProgressMonitor monitor) {
								if (bytesRead == 0) return;
								Assert.isTrue(bytesRead <= bytesTotal);
								monitor.subTask(NLS.bind(CVSMessages.Session_transfer, (new Object[] { title, Long.toString(bytesRead >> 10), Long.toString(bytesTotal >> 10) })));
							}
						};
						sendUncompressedBytes(in, length);
					} else {
						monitor.subTask(NLS.bind(CVSMessages.Session_calculatingCompressedSize, new String[] { Util.toTruncatedPath(file, localRoot, 3) })); 
						in = file.getContents();
						byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
						ByteCountOutputStream counter = new ByteCountOutputStream();
						OutputStream zout = new GZIPOutputStream(counter);
						if (!isBinary && IS_CRLF_PLATFORM) in = new CRLFtoLFInputStream(in);
						try {
							for (int count; (count = in.read(buffer)) != -1;) zout.write(buffer, 0, count);
						} finally {
							zout.close();
						}
						in.close();
						in = file.getContents();
						in = new ProgressMonitorInputStream(in, file.getSize(), TRANSFER_PROGRESS_INCREMENT, monitor) {
							protected void updateMonitor(long bytesRead, long bytesTotal, IProgressMonitor monitor) {
								if (bytesRead == 0) return;
								Assert.isTrue(bytesRead <= bytesTotal);
								monitor.subTask(NLS.bind(CVSMessages.Session_transfer, (new Object[] { title, Long.toString(bytesRead >> 10), Long.toString(bytesTotal >> 10) })));
							}
						};
						if (!isBinary && IS_CRLF_PLATFORM) in = new CRLFtoLFInputStream(in);
						sendCompressedBytes(in, counter.getSize());
					}
				} finally {
					if (in != null) in.close();
				}
			} catch (IOException e) {
				throw CVSException.wrapException(e);
			}
		}

	/*
	 * Send the contents of the input stream to CVS.
	 * Length must equal the number of bytes that will be transferred
	 * across the wire, that is, the compressed file size.
	 */
	private void sendCompressedBytes(InputStream in, long length) throws IOException, CVSException {
		String sizeLine = "z" + Long.toString(length); //$NON-NLS-1$
		writeLine(sizeLine);
		OutputStream out = connection.getOutputStream();
		GZIPOutputStream zo = new GZIPOutputStream(out);
		byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
		for (int count;
		(count = in.read(buffer)) != -1;)
		zo.write(buffer, 0, count);
		zo.finish();
	}

	/*
	 * Send the contents of the input stream to CVS.
	 * Length must equal the number of bytes that will be transferred
	 * across the wire.
	 */
	private void sendUncompressedBytes(InputStream in, long length) throws IOException, CVSException {
		OutputStream out = connection.getOutputStream();
		String sizeLine = Long.toString(length);
		writeLine(sizeLine);
		byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
		for (int count; (count = in.read(buffer)) != -1;) out.write(buffer, 0, count);
	}




	/**
	 * Receives a file from the remote CVS server, possibly translating line delimiters.
	 * <p>
	 * Line termination sequences are automatically converted to platform format
	 * only when receiving non-binary files.  This may alter the actual size and
	 * contents of the file that is received.
	 * </p><p>
	 * Translation is performed on-the-fly, so the file need not fit in available memory.
	 * </p>
	 * @param file the file to be received
	 * @param isBinary is true if the file should be received without translation
	 * @param responseType one of the ICVSFile updated types (UPDATED, CREATED, MERGED, UPDATE_EXISTING)
	 * indicating what repsonse type provided the file contents
	 * @param monitor the progress monitor
	 */
	public void receiveFile(ICVSStorage file, boolean isBinary, int responseType, IProgressMonitor monitor)
	throws CVSException {
		// check overrides
		if (textTransferOverrideSet != null &&
			textTransferOverrideSet.contains(file)) isBinary = false;

		// update progress monitor
		final String title = NLS.bind(CVSMessages.Session_receiving, (new Object[]{ Util.toTruncatedPath(file, localRoot, 3) })); 
		monitor.subTask(NLS.bind(CVSMessages.Session_transferNoSize, new String[] { title })); 
		// get the file size from the server
		long size;
		boolean compressed = false;
		String sizeLine = null;
		try {
			sizeLine = readLine();
			if (sizeLine.charAt(0) == 'z') {
				compressed = true;
				sizeLine = sizeLine.substring(1);
			}
			size = Long.parseLong(sizeLine, 10);
		} catch (NumberFormatException e) {
			// In some cases, the server will give us an error line here
			if (sizeLine != null && sizeLine.startsWith("E")) { //$NON-NLS-1$
				handleErrorLine(sizeLine.substring(1).trim(), org.eclipse.core.runtime.Status.OK_STATUS);
				return;
			} else {
				IStatus status = new CVSStatus(IStatus.ERROR,CVSStatus.ERROR,CVSMessages.Session_badInt, e, localRoot);
				throw new CVSException(status); 
			}
		}
		// create an input stream that spans the next 'size' bytes from the connection
		InputStream in = new SizeConstrainedInputStream(connection.getInputStream(), size, true /*discardOnClose*/);
		// setup progress monitoring
		in = new ProgressMonitorInputStream(in, size, TRANSFER_PROGRESS_INCREMENT, monitor) {
			protected void updateMonitor(long bytesRead, long bytesTotal, IProgressMonitor monitor) {
				if (bytesRead == 0) return;
				monitor.subTask(NLS.bind(CVSMessages.Session_transfer, (new Object[] { title, Long.toString(bytesRead >> 10), Long.toString(bytesTotal >> 10) })));
			}
		};
		// if compression enabled, decompress on the fly
		if (compressed) {
			try {
				in = new GZIPInputStream(in);
			} catch (IOException e) {
				try {
					in.close();
				} catch (IOException e2) {
					// Don't care. We're right about to report this IOException
				}
				throw CVSException.wrapException(e);
			}
		}
		// if not binary, translate line delimiters on the fly
		if (! isBinary) {
			// switch from LF to CRLF if appropriate
			if (IS_CRLF_PLATFORM && CVSProviderPlugin.getPlugin().isUsePlatformLineend()) {
				// auto-correct for CRLF line-ends that come from the server
				in = new CRLFtoLFInputStream(in);
				// convert LF to CRLF
				in = new LFtoCRLFInputStream(in);
			} else {
				// be nice and warn about text files that contain CRLF
				in = new CRLFDetectInputStream(in, file);
			}
		}
		// write the file locally
		file.setContents(in, responseType, true, new NullProgressMonitor());
	}

	/**
	 * Stores the value of the last Mod-time response encountered.
	 * Valid only for the duration of a single CVS command.
	 */
	void setModTime(Date modTime) {
		this.modTime = modTime;
	}
	
	/**
	 * Returns the stored value of the last Mod-time response,
	 * or null if there was none while processing the current command.
	 */
	Date getModTime() {
		return modTime;
	}
	
	/**
	 * Stores true if the -n global option was specified for the current command.
	 * Valid only for the duration of a single CVS command.
	 */
	void setNoLocalChanges(boolean noLocalChanges) {
		this.noLocalChanges = noLocalChanges;
	}
	
	/**
	 * Returns true if the -n global option was specified for the current command,
	 * false otherwise.
	 */
	boolean isNoLocalChanges() {
		return noLocalChanges;
	}
	
	/**
	 * Callback hook for the ValidRequestsHandler to specify the set of valid
	 * requests for this session.
	 */
	void setValidRequests(String validRequests) {
		this.validRequests = " " + validRequests + " "; //$NON-NLS-1$  //$NON-NLS-2$
	}

	public boolean isOutputToConsole() {
		return outputToConsole;
	}

	/**
	 * Stores a flag as to whether .# files will be created. (Default is true)
	 * @param createBackups if true, creates .# files at the server's request
	 */
	void setCreateBackups(boolean createBackups) {
		this.createBackups = createBackups;
	}

	/**
	 * Returns a flag as to whether .# files will be created.
	 */
	boolean isCreateBackups() {
		return createBackups;
	}

	/**
	 * Gets the sendFileTitleKey.
	 * @return Returns a String
	 */
	String getSendFileTitleMessage() {
		if (sendFileTitleMessage == null)
			return CVSMessages.Session_sending; 
		return sendFileTitleMessage;
	}

	/**
	 * Sets the sendFileTitleKey.
	 * @param sendFileTitleKey The sendFileTitleKey to set
	 */
	public void setSendFileTitleKey(String sendFileTitleMessage) {
		this.sendFileTitleMessage = sendFileTitleMessage;
	}
	
	/**
	 * Remembers a set of files that must be transferred as 'text'
	 * regardless of what the isBinary parameter to sendFile() is.
	 * 
	 * @param textTransferOverrideSet the set of ICVSFiles to override, or null if none
	 */
	public void setTextTransferOverride(Collection textTransferOverrideSet) {
		this.textTransferOverrideSet = textTransferOverrideSet;
	}
	
	/**
	 * Filter the provided global options using parameters set on this session
	 * or globally. The session may add global options that correspond to user
	 * preferences or remove those that contradict requirements for this
	 * particular session.
	 *
	 * @param globalOptions the global options, read-only
	 * @return the filtered global options
	 */
	protected GlobalOption[] filterGlobalOptions(GlobalOption[] globalOptions) {
		if (! Command.DO_NOT_CHANGE.isElementOf(globalOptions)) {
			// Get the user preference for verbosity
			QuietOption quietOption = CVSProviderPlugin.getPlugin().getQuietness();
			if (quietOption != null) {
				globalOptions = quietOption.addToEnd(globalOptions);
			}
			// Get the user preference for read-only
			if (isWatchEditEnabled()) {
				if (!Command.MAKE_READ_ONLY.isElementOf(globalOptions)) {
					globalOptions = Command.MAKE_READ_ONLY.addToEnd(globalOptions);
				}
			}
		}
		return globalOptions;
	}

	private boolean isWatchEditEnabled() {
		// First, look at the global preference
		if (CVSProviderPlugin.getPlugin().getPluginPreferences().getBoolean(CVSProviderPlugin.READ_ONLY)) {
			return true;
		}
		// If there is a provider, use the providers setting for watch/edit
		try {
			IResource resource = getLocalRoot().getIResource();
			if (resource != null && resource.getType() != IResource.ROOT) {
				RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
				if (provider != null) {
					return ((CVSTeamProvider) provider).isWatchEditEnabled();
				}
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	/**
	 * Method setIgnoringLocalChanges.
	 * @param b
	 */
	protected void setIgnoringLocalChanges(boolean b) {
		ignoringLocalChanges = b;
	}
	/**
	 * Returns the ignoringLocalChanges.
	 * @return boolean
	 */
	protected boolean isIgnoringLocalChanges() {
		return ignoringLocalChanges;
	}

	/*
	 * Get the response handler map to be used for this session. The map is created by making a copy of the global
	 * reponse handler map.
	 */
	protected Map<String,ResponseHandler> getReponseHandlers() {
		if (responseHandlers == null) {
			responseHandlers = Request.getReponseHandlerMap();
		}
		return responseHandlers;
	}
	
	/*
	 * Makes a list of all valid responses; for initializing a session.
	 * @return a space-delimited list of all valid response strings
	 */
	private String makeResponseList() {
		StringBuilder result = new StringBuilder("ok error M E");  //$NON-NLS-1$
		Iterator elements = getReponseHandlers().keySet().iterator();
		while (elements.hasNext()) {
			result.append(' ');
			result.append((String) elements.next());
		}
		
		return result.toString();
	}
	public void registerResponseHandler(ResponseHandler handler) {
		getReponseHandlers().put(handler.getResponseID(), handler);
	}
	
	public void removeResponseHandler(String responseID) {
		getReponseHandlers().remove(responseID);
	}
	
	public ResponseHandler getResponseHandler(String responseID) {
		return getReponseHandlers().get(responseID);
	}

	/**
	 * Accumulate the added errors so they can be included in the status returned
	 * when the command execution is finished. OK status are ignored.
	 * @param status the status to be accumulated
	 */
	public void addError(IStatus status) {
		if (!status.isOK())
			errors.add(status);
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	public IStatus[] getErrors() {
		return errors.toArray(new IStatus[errors.size()]);
	}
	
	public void clearErrors() {
		errors.clear();
	}

	public void setCurrentCommand(Command c) {
		currentCommand = c;
	}
	
	public Command getCurrentCommand() {
		return currentCommand;
	}

	/**
	 * Report the given error line to any listeners
	 * @param line the error line
	 * @param status the status that indicates any problems encountered parsing the line
	 */
	public void handleErrorLine(String line, IStatus status) {
		ConsoleListeners.getInstance().errorLineReceived(this, line, status);
	}
	
	/**
	 * An error has occurred while processing responses from the 
	 * server. Place this error is the status that will be returned
	 * from the command and show the error in the console
	 * @param status the status that descibes the error
	 */
	public void handleResponseError(IStatus status) {
		addError(status);
		handleErrorLine(NLS.bind(CVSMessages.Session_0, new String[] { status.getMessage() }), status); 
	}
}
