package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.text.DateFormat;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * This class is a collection of utility functions that can be 
 * used for install processing
 */
public class Utilities {

	private static Map entryMap;
	private static Stack bufferPool;
	private static final int BUFFER_SIZE = 4096;
	private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault());
	private static long tmpseed = (new Date()).getTime();
	private static String dirRoot = null;

	/**
	 * Returns a new working directory (in temporary space). Ensures
	 * the directory exists. Any directory levels that had to be created
	 * are marked for deletion on exit.
	 * 
	 * @return working directory
	 * @exception IOException
	 * @since 2.0
	 */
	public static synchronized File createWorkingDirectory() throws IOException {

		if (dirRoot == null) {
			dirRoot = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			// in Linux, returns '/tmp', we must add '/'
			if (!dirRoot.endsWith(File.separator))
				dirRoot += File.separator;

			dirRoot += "eclipse" + File.separator + ".update" + File.separator + Long.toString(tmpseed) + File.separator;
			//$NON-NLS-1$ //$NON-NLS-2$
		}

		String tmpName = dirRoot + Long.toString(++tmpseed) + File.separator;

		File tmpDir = new File(tmpName);
		verifyPath(tmpDir, false);
		if (!tmpDir.exists())
			throw new FileNotFoundException(tmpName);
		return tmpDir;
	}

	/**
	 * Create a new working file. The file is marked for deletion on exit.
	 * The file is optionally associated with a lookup key.
	 * 
	 * @see #lookupLocalFile(String)
	 * @param tmpDir directory location for new file. Any missing directory
	 * levels are created (and marked for deletion on exit)
	 * @param key optional lookup key, or <code>null</code>.
	 * @param name optional file name, or <code>null</code>. If name is not
	 * specified, a temporary name is generated.
	 * @return created working file
	 * @exception IOException
	 * @since 2.0
	 */
	public static synchronized File createLocalFile(File tmpDir, String key, String name) throws IOException {
		// create the local file
		File temp;
		String filePath;
		if (name != null) {
			// create file with specified name
			filePath = name.replace('/', File.separatorChar);
			if (filePath.startsWith(File.separator))
				filePath = filePath.substring(1);
			temp = new File(tmpDir, filePath);
		} else {
			// create file with temp name
			temp = File.createTempFile("eclipse", null, tmpDir); //$NON-NLS-1$
		}
		temp.deleteOnExit();
		verifyPath(temp, true);

		// create file association 
		if (key != null) {
			if (entryMap == null)
				entryMap = new HashMap();
			entryMap.put(key, temp);
		}
		return temp;
	}

	/**
	 * Returns a previously cached local file (in temporary area) matching the
	 * specified key. 
	 * 
	 * @param key lookup key
	 * @return cached file, or <code>null</code>.
	 * @since 2.0
	 */
	public static synchronized File lookupLocalFile(String key) {
		if (entryMap == null)
			return null;
		return (File) entryMap.get(key);
	}

	/**
	 * Removes the specified key from the local file map. The file is
	 * not actually deleted until VM termination.
	 * 
	 * @param key lookup key
	 * @since 2.0
	 */
	public static synchronized void removeLocalFile(String key) {
		if (entryMap != null)
			entryMap.remove(key);
	}

	/**
	 * Copies specified input stream to the output stream. Neither stream
	 * is closed as part of this operation.
	 * 
	 * @param is input stream
	 * @param os output stream
	 * @param monitor progress monitor
	 * @exception IOException
	 * @exception InstallAbortedException
	 * @since 2.0
	 */
	public static void copy(InputStream is, OutputStream os, InstallMonitor monitor) throws IOException, InstallAbortedException {
		byte[] buf = getBuffer();
		try {
			long currentLen = 0;
			int len = is.read(buf);
			while (len != -1) {
				currentLen += len;
				os.write(buf, 0, len);
				if (monitor != null) {
					monitor.setCopyCount(currentLen);
					if (monitor.isCanceled()) {
						String msg = Policy.bind("Feature.InstallationCancelled"); //$NON-NLS-1$
						throw new InstallAbortedException(msg, null);
					}
				}
				len = is.read(buf);
			}
		} finally {
			freeBuffer(buf);
		}
	}

	/**
	 * Creates a CoreException from some other exception.
	 * The type of the CoreException is <code>IStatus.ERROR</code>
	 * If the exception passed as a parameter is also a CoreException,
	 * the new CoreException will contain all the status of the passed
	 * CoreException.
	 * 
	 * @see IStatus#ERROR
	 * @param s exception string
	 * @param code the code reported
	 * @param e actual exception being reported
	 * @return a CoreException
	 * @since 2.0
	 */
	public static CoreException newCoreException(String s, int code, Throwable e) {
		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();

		// check the case of a multistatus
		IStatus status;
		if (e instanceof CoreException) {
			if (s == null)
				s = "";
			status = new MultiStatus(id, code, s, e);
			IStatus childrenStatus = ((CoreException) e).getStatus();
			((MultiStatus) status).add(childrenStatus);
			((MultiStatus) status).addAll(childrenStatus);
		} else {
			StringBuffer completeString = new StringBuffer("");
			if (s != null)
				completeString.append(s);
			if (e != null) {
				completeString.append(" [");
				completeString.append(e.toString());
				completeString.append("]");
			}
			status = new Status(IStatus.ERROR, id, code, completeString.toString(), e);
		}
		return new CoreException(status); //$NON-NLS-1$
	}

	/**
	 * Creates a CoreException from some other exception.
	 * The type of the CoreException is <code>IStatus.ERROR</code>
	 * If the exceptionpassed as a parameter is also a CoreException,
	 * the new CoreException will contain all the status of the passed
	 * CoreException.
	 * 
	 * @see IStatus#ERROR
	 * @param s exception string
	 * @param e actual exception being reported
	 * @return a CoreException
	 * @since 2.0
	 */
	public static CoreException newCoreException(String s, Throwable e) {
		return newCoreException(s, IStatus.OK, e);
	}

	/**
	 * Creates a CoreException from two other CoreException
	 * 
	 * @param s overall exception string
	 * @param s1 string for first detailed exception
	 * @param s2 string for second detailed exception
	 * @param e1 first detailed exception
	 * @param e2 second detailed exception
	 * @return a CoreException with multi-status
	 * @since 2.0
	 */
	public static CoreException newCoreException(String s, String s1, String s2, CoreException e1, CoreException e2) {
		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
		if (s == null)
			s = "";

		IStatus childStatus1 = ((CoreException) e1).getStatus();
		IStatus childStatus2 = ((CoreException) e2).getStatus();
		int code = (childStatus1.getCode()==childStatus2.getCode())?childStatus1.getCode():IStatus.OK;
		MultiStatus multi = new MultiStatus(id, code, s, null);
		
						
		multi.add(childStatus1);
		multi.addAll(childStatus1);
		multi.add(childStatus2);
		multi.addAll(childStatus2);

		return new CoreException(multi); //$NON-NLS-1$
	}

	/**
	 * Formats a Date based on the default Locale 
	 * If teh Date is <code>null</code> returns an empty String
	 * 
	 * @param date the Date to format
	 * @return the formatted Date as a String
	 * @since 2.0
	 */
	public static String format(Date date) {
		if (date == null)
			return "";
		return dateFormat.format(date);
	}

	/**
	 * Perform shutdown processing for temporary file handling.
	 * This method is called when platform is shutting down.
	 * It is not intended to be called at any other time under
	 * normal circumstances. A side-effect of calling this method
	 * is that the contents of the temporary directory managed 
	 * by this class are deleted. 
	 * 
	 * @since 2.0
	 */
	public static void shutdown() {
		if (dirRoot == null)
			return;

		File temp = new File(dirRoot); // temp directory root for this run
		cleanupTemp(temp);
		temp.delete();
	}

	private static void cleanupTemp(File root) {
		File[] files = root.listFiles();
		for (int i = 0; files != null && i < files.length; i++) {
			if (files[i].isDirectory())
				cleanupTemp(files[i]);
			files[i].delete();
		}
	}

	private static void verifyPath(File path, boolean isFile) {
		// if we are expecting a file back off 1 path element
		if (isFile) {
			if (path.getAbsolutePath().endsWith(File.separator)) {
				// make sure this is a file
				path = path.getParentFile();
				isFile = false;
			}
		}

		// already exists ... just return
		if (path.exists())
			return;

		// does not exist ... ensure parent exists
		File parent = path.getParentFile();
		verifyPath(parent, false);

		// ensure directories are made. Mark files or directories for deletion
		if (!isFile)
			path.mkdir();
		path.deleteOnExit();
	}

	private static synchronized byte[] getBuffer() {
		if (bufferPool == null) {
			return new byte[BUFFER_SIZE];
		}

		try {
			return (byte[]) bufferPool.pop();
		} catch (EmptyStackException e) {
			return new byte[BUFFER_SIZE];
		}
	}

	private static synchronized void freeBuffer(byte[] buf) {
		if (bufferPool == null)
			bufferPool = new Stack();
		bufferPool.push(buf);
	}

}