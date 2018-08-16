/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Unsorted static helper-methods 
 */
public class Util {
	
	/**
	 * Return the last segment of the given path
	 * @param path
	 * @return String
	 */
	public static String getLastSegment(String path) {
		int index = path.lastIndexOf(Session.SERVER_SEPARATOR);
		if (index == -1) {
			return path;
		}
		if (index == path.length() - 1) {
			return getLastSegment(path.substring(0, index));
		}
		return path.substring(index + 1);
		
	}
	
	/**
	 * Return the given path with the last segment removed
	 * @param path
	 * @return String
	 */
	public static String removeLastSegment(String path) {
		int index = path.lastIndexOf(Session.SERVER_SEPARATOR);
		if (index == -1)
			return ""; //$NON-NLS-1$
		else
			return path.substring(0, index);

	}
	/**
	 * Return the path without a trailing /
	 * @param path
	 * @return String
	 */
	public static String asPath(String path) {
		if (path.endsWith(Session.SERVER_SEPARATOR)) {
			return path.substring(0, path.length() - Session.SERVER_SEPARATOR.length());
		}
		return path;
	}

	/**
	 * Get the extention of the path of resource relative to the path of root
	 * 
	 * @throws CVSException
	 *             if root is not a root-folder of resource
	 */
	public static String getRelativePath(String rootName, String resourceName) 
		throws CVSException {

		if (!resourceName.startsWith(rootName) || rootName.length() > resourceName.length()) {
			throw new CVSException(CVSMessages.Util_Internal_error__resource_does_not_start_with_root_3); 
		}
		
		// Otherwise we would get an ArrayOutOfBoundException
		// in case of two equal Resources
		if (rootName.length() == resourceName.length()) {
			return ""; //$NON-NLS-1$
		}
		
		// Remove leading slash if there is one
		String result = resourceName.substring(rootName.length());
		if (result.startsWith("/")) { //$NON-NLS-1$
			result = result.substring(1);
		}
		return result;
	}
	
	/**
	 * Append the prefix and suffix to form a valid CVS path.
	 */
	public static String appendPath(String prefix, String suffix) {
		if (prefix.length() == 0 || prefix.equals(Session.CURRENT_LOCAL_FOLDER)) {
			return suffix;
		} else if (prefix.endsWith(Session.SERVER_SEPARATOR)) {
			if (suffix.startsWith(Session.SERVER_SEPARATOR))
				return prefix + suffix.substring(1);
			else
				return prefix + suffix;
		} else if (suffix.startsWith(Session.SERVER_SEPARATOR))
			return prefix + suffix;
		else
			return prefix + Session.SERVER_SEPARATOR + suffix;
	}

	public static void logError(String message, Throwable throwable) {
		CVSProviderPlugin.log(IStatus.ERROR, message, throwable);
	}

	/**
	 * If the number of segments in the relative path of <code>resource</code> to <code>root</code> is 
	 * greater than <code>split</code> then the returned path is truncated to <code>split</code> number
	 * of segments and '...' is shown as the first segment of the path.
	 */
	public static String toTruncatedPath(ICVSResource resource, ICVSFolder root, int split) {
		try {
			String stringPath = resource.getRelativePath(root);
			if (stringPath.equals(Session.CURRENT_LOCAL_FOLDER)) {
				return resource.getName();
			}
			String truncatedPath = toTruncatedPath(stringPath, split);
			return truncatedPath;
		} catch(CVSException e) {
			return resource.getName();
		}
	}

	public static String toTruncatedPath(String stringPath, int split) {
		// Search backwards until split separators are found
		int count = 0;
		int index = stringPath.length();
		while (count++ < split && index != -1) {
			index = stringPath.lastIndexOf(Session.SERVER_SEPARATOR, index - 1);
		}
		if (index == -1) {
			return stringPath;
		} else {
			return NLS.bind(CVSMessages.Util_truncatedPath, new String[] { stringPath.substring(index) }); 
		}
	}
	
	/**
	 * Helper method that will time out when making a socket connection.
	 * This is required because there is no way to provide a timeout value
	 * when creating a socket and in some instances, they don't seem to
	 * timeout at all.
	 */
	public static Socket createSocket(final String host, final int port, IProgressMonitor monitor) throws UnknownHostException, IOException {
		int timeout = CVSProviderPlugin.getPlugin().getTimeout();
		if (timeout == 0) timeout = CVSProviderPlugin.DEFAULT_TIMEOUT;
		ResponsiveSocketFactory factory = new ResponsiveSocketFactory(monitor, timeout);
		return factory.createSocket(host, port);
	}
	
	/**
	 * Helper method that will time out when running an external command.
	 * This is required because there is no way to provide a timeout value
	 * when executing an external command and in some instances, they don't seem to
	 * timeout at all.
	 */
	public static Process createProcess(final String[] command, IProgressMonitor monitor) throws IOException {
		
		// Start a thread to execute the command and get a handle to the process
		final Process[] process = new Process[] { null };
		final Exception[] exception = new Exception[] {null };
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Process newProcess = Runtime.getRuntime().exec(command);
					synchronized (process) {
						if (Thread.interrupted()) {
							// we we're either cancelled or timed out so just destroy the process
							newProcess.destroy();
						} else {
							process[0] = newProcess;
						}
					}
				} catch (IOException e) {
					exception[0] = e;
				}
			}
		});
		thread.start();
		
		// Wait the appropriate number of seconds
		int timeout = CVSProviderPlugin.getPlugin().getTimeout();
		if (timeout == 0) timeout = CVSProviderPlugin.DEFAULT_TIMEOUT;
		for (int i = 0; i < timeout; i++) {
			try {
				// wait for the thread to complete or 1 second, which ever comes first
				thread.join(1000);
			} catch (InterruptedException e) {
				// I think this means the thread was interupted but not necessarily timed out
				// so we don't need to do anything
			}
			synchronized (process) {
				// if the user cancelled, clean up before preempting the operation
				if (monitor.isCanceled()) {
					if (thread.isAlive()) {
						thread.interrupt();
					}
					if (process[0] != null) {
						process[0].destroy();
					}
					// this method will throw the proper exception
					Policy.checkCanceled(monitor);
				}
			}
		}
		// If the thread is still running (i.e. we timed out) signal that it is too late
		synchronized (process) {
			if (thread.isAlive()) {
				thread.interrupt();
			}
		}
		if (exception[0] != null) {
			throw (IOException)exception[0];
		}
		if (process[0] == null) {
			throw new InterruptedIOException(NLS.bind(CVSMessages.Util_processTimeout, new String[] { command[0] })); 
		}
		return process[0];
	}
	
	public static String[] parseIntoSubstrings(String string, String delimiter) {
		List result = new ArrayList();
		int start = 0;
		int index = string.indexOf(delimiter);
		String next;
		while (index != -1) {
			next = string.substring(start, index);
			result.add(next);
			start = index + 1;
			index = string.indexOf(delimiter, start);
		}
		if (start >= string.length()) {
			next = "";//$NON-NLS-1$
		} else {
			next = string.substring(start);
		}
		result.add(next);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	/**
	 * Return the substring at the given index (starting at 0) where each
	 * element is delimited by the provided delimiter.
	 * 
	 * @param bytes
	 * @param delimiter
	 * @param index
	 * @param includeRest
	 * @return String
	 */
	public static String getSubstring(byte[] bytes, byte delimiter, int index, boolean includeRest) {
		byte[] bytesForSlot = getBytesForSlot(bytes, delimiter, index, includeRest);
		if (bytesForSlot == null) {
			return null;
		}
		return new String(bytesForSlot);
	}
	
	/**
	 * Return the offset the the Nth delimeter from the given start index.
	 * @param bytes
	 * @param delimiter
	 * @param start
	 * @param n
	 * @return int
	 */
	public static int getOffsetOfDelimeter(byte[] bytes, byte delimiter, int start, int n) {
		int count = 0;
		for (int i = start; i < bytes.length; i++) {
			if (bytes[i] == delimiter) count++;
			if (count == n) return i;
		}
		// the Nth delimeter was not found
		return -1;
	}
	
	/**
	 * Method getBytesForSlot.
	 * @param syncBytes
	 * @param SEPARATOR_BYTE
	 * @param i
	 * @param b
	 * @return byte[]
	 */
	public static byte[] getBytesForSlot(byte[] bytes, byte delimiter, int index, boolean includeRest) {
		// Find the starting index
		int start;
		if (index == 0) {
			// make start -1 so that end determination will start at offset 0.
			start = -1;
		} else {
			start = getOffsetOfDelimeter(bytes, delimiter, 0, index);
			if (start == -1) return null;
		}
		// Find the ending index
		int end = getOffsetOfDelimeter(bytes, delimiter, start + 1, 1);
		// Calculate the length
		int length;
		if (end == -1 || includeRest) {
			length = bytes.length - start - 1;
		} else {
			length = end - start - 1;
		}
		byte[] result = new byte[length];
		System.arraycopy(bytes, start + 1, result, 0, length);
		return result;
	}
	
	/**
	 * Method equals.
	 * @param syncBytes
	 * @param oldBytes
	 * @return boolean
	 */
	public static boolean equals(byte[] syncBytes, byte[] oldBytes) {
		if (syncBytes == null || oldBytes == null) return syncBytes == oldBytes;
		if (syncBytes.length != oldBytes.length) return false;
		for (int i = 0; i < oldBytes.length; i++) {
			if (oldBytes[i] != syncBytes[i]) return false;
		}
		return true;
	}
	
	/**
	 * Workaround a CVS bug where a CVS Folder with no immediately contained files has an incorrect
	 * Tag type stored in the TAG file.  In this case, the tag type is always BRANCH (Tv1)
	 * 
	 * The fix is for folders with no files, use the tag type for the containing project.  Since projects almost
	 * always have files the TAG file is usually correct.
	 * 
	 * For the case where the folder tag name does not match the project tag name we can not do much so we just
	 * return the folder tag which will currently always be a branch.
	 * 
	 * @param resource The IResource being tested.  Can not be null.
	 * @param tag The CVSTag as reported by CVS for the IResource.  May be null.
	 * @return CVSTag The corrected tag for the resource.  May be null.
	 */
	public static CVSTag getAccurateFolderTag(IResource resource, CVSTag tag) {

		// Determine if the folder contains files as immediate children.
		if (resource.getType() != IResource.FOLDER) {
			return tag;
		}

		IResource[] members = null;
		try {
			members = ((IFolder) resource).members();
		} catch (CoreException e1) {
			return tag;
		}
		
		for (int i = 0; i < members.length; i++) {
			if (members[i].getType() == IResource.FILE) {
				return tag;
			}
		}
	
		// Folder contains no files so this may not really be a branch.
		// Make the type the same as the project tag type if both are the same tag name.
		IProject project = resource.getProject();
		if (project == null) {
			return tag;
		}
		
		ICVSFolder projectFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
		FolderSyncInfo projectSyncInfo;
		try {
			projectSyncInfo = projectFolder.getFolderSyncInfo();
		} catch (CVSException e) {
			return tag;
		}
		
		if (projectSyncInfo == null) {
			return tag;
		}
		
		CVSTag projectTag = projectSyncInfo.getTag();
								
		if (projectTag != null && projectTag.getName().equals(tag.getName())) {
			return projectTag;
		} else {
			return tag;
		}
	}	
	
	/**
	 * Workaround for CVS "bug" where CVS ENTRIES file does not contain correct
	 * Branch vs. Version info.  Entries files always record a Tv1 so all entries would
	 * appear as branches.
	 * 	
	 * By comparing the revision number to the tag name
	 * you can determine if the tag is a branch or version.
	 * 
	 * @param cvsResource the resource to test.  Must nut be null.
	 * @return the correct cVSTag.  May be null.
	 */
	public static CVSTag getAccurateFileTag(ICVSResource cvsResource) throws CVSException {

		CVSTag tag = null;
		ResourceSyncInfo info = cvsResource.getSyncInfo();
		if(info != null) {
			tag = info.getTag();
		}

		FolderSyncInfo parentInfo = cvsResource.getParent().getFolderSyncInfo();
		CVSTag parentTag = null;
		if(parentInfo != null) {
			parentTag = parentInfo.getTag();
		}

		if(tag != null) {
			if(tag.getName().equals(info.getRevision())) {
				tag = new CVSTag(tag.getName(), CVSTag.VERSION);
			} else if(parentTag != null){
				tag = new CVSTag(tag.getName(), parentTag.getType());
			}
		}
		
		return tag;						
	}

	/**
	 * Workaround for CVS "bug" where CVS ENTRIES file does not contain correct
	 * Branch vs. Version info.  Entries files always record a Tv1 so all entries would
	 * appear as branches.
	 * 	
	 * By comparing the revision number to the tag name and to the existing tags
	 * you can determine if the tag is a branch or version.
	 * 
	 * @param cvsResource the resource to test.  Must nut be null.
	 * @return the correct cVSTag.  May be null.
	 */
	public static CVSTag getAccurateFileTag(ICVSResource cvsResource, CVSTag[] existing) throws CVSException {
		List tags= Arrays.asList(existing);
		CVSTag tag = null;
		ResourceSyncInfo info = cvsResource.getSyncInfo();
		if(info != null) {
			tag = info.getTag();
		}

		FolderSyncInfo parentInfo = cvsResource.getParent().getFolderSyncInfo();
		CVSTag parentTag = null;
		if(parentInfo != null) {
			parentTag = parentInfo.getTag();
		}

		if(tag != null) {
			if(tag.getName().equals(info.getRevision())) {
				tag = new CVSTag(tag.getName(), CVSTag.VERSION);
			} else if(parentTag != null){
				tag = new CVSTag(tag.getName(), parentTag.getType());
			} else if (!tags.contains(tag)) {
				if (!tags.contains(tag)) {
					CVSTag newTag= new CVSTag(tag.getName(), CVSTag.VERSION);
					if (tags.contains(newTag)) {
						tag = newTag;
					}
				}
			}
		}
		return tag;
	}
	
	/**
	 * Return the fullest path that we can obtain for the given resource
	 * @param resource
	 * @return
	 */
	public static String getFullestPath(ICVSResource resource) {
		IResource local = resource.getIResource();
		if (local != null) {
			return local.getFullPath().toString();
		}
		try {
			String remotePath = resource.getRepositoryRelativePath();
			if (remotePath != null) {
				return remotePath;
			}
		} catch (CVSException e) {
			// Ignore and try the next method;
		}
		return resource.getName();
	}
	
	public static String getVariablePattern(String pattern, String variableName) {
		return "(" + variableName + ":" + pattern + ":" + variableName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @param localRevision
	 * @return
	 */
	public static int[] convertToDigits(String localRevision) {
		try {
			String digitStrings[] = localRevision.split("\\."); //$NON-NLS-1$
			int[] digits = new int[digitStrings.length];
			for (int i = 0; i < digitStrings.length; i++) {
				String digitString = digitStrings[i];
				digits[i] = Integer.parseInt(digitString);
			}
			return digits;
		} catch (NumberFormatException e) {
			CVSProviderPlugin.log(CVSException.wrapException(e));
			return new int[0];
		}
	}

	public static String toTruncatedPath(ICVSStorage file, ICVSFolder localRoot, int i) {
		if (file instanceof ICVSResource) {
			return toTruncatedPath((ICVSResource)file, localRoot, i);
		}
		return file.getName();
	}
	
	/**
	 * If the status/log returns that the file is in the Attic, then remove the
	 * Attic segment. This is because files added to a branch that are not in
	 * the main trunk (HEAD) are added to the Attic but cvs does magic on
	 * update to put them in the correct location.
	 * (e.g. /project/Attic/file.txt -> /project/file.txt)
	 */ 
	public static String removeAtticSegment(String path) {
		int lastSeparator = path.lastIndexOf(Session.SERVER_SEPARATOR);
		if (lastSeparator == -1) return path;
		int secondLastSeparator = path.lastIndexOf(Session.SERVER_SEPARATOR, lastSeparator - 1);
		if (secondLastSeparator == -1) return path;
		String secondLastSegment = path.substring(secondLastSeparator + 1, lastSeparator);
		if (secondLastSegment.equals("Attic")) { //$NON-NLS-1$
			return path.substring(0, secondLastSeparator) + path.substring(lastSeparator);
		}
		return path;
	}
	
	/**
	 * Flatten the text in the multiline comment
	 */
	public static String flattenText(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(Session.SERVER_SEPARATOR); 
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}
}
