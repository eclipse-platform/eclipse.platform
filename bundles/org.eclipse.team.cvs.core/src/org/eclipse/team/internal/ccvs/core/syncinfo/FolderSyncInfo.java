/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.syncinfo;

 
import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.CVSEntryLineTag;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Value (immutable) object that represents workspace state information about the contents of a
 * folder that was retreived from a CVS repository. It is a specialized representation of the files from
 * the CVS sub-directory that contain folder specific connection information (e.g. Root, Repository, Tag).
 *  
 * @see ICVSFolder#getFolderSyncInfo()
 */
public class FolderSyncInfo {

	// The Repository value for virtual directories (i.e. local with no corresponding remote)
	public static final String VIRTUAL_DIRECTORY = "CVSROOT/Emptydir"; //$NON-NLS-1$

	// relative path of this folder in the repository, project1/folder1/folder2
	protected String repository;
	
	// :pserver:user@host:/home/user/repo
	protected String root;
	
	// sticky tag (e.g. version, date, or branch tag applied to folder)
	private CVSEntryLineTag tag;
	
	// if true then it means only part of the folder was fetched from the repository, and CVS will not create 
	// additional files in that folder.
	protected boolean isStatic;

	/**
	 * Construct a folder sync object.
	 * 
	 * @param repo the relative path of this folder in the repository, cannot be <code>null</code>.
	 * @param root the location of the repository, cannot be <code>null</code>.
	 * @param tag the tag set for the folder or <code>null</code> if there is no tag applied.
	 * @param isStatic to indicate is only part of the folder was fetched from the server.
	 */
	public FolderSyncInfo(String repo, String root, CVSTag tag, boolean isStatic) {
		Assert.isNotNull(repo);
		Assert.isNotNull(root);
		this.repository = repo;
		// intern the root so that caching of FolderSyncInfos for folders will use less space
		this.root = root.intern();
		ensureRepositoryRelativeToRoot();
		this.isStatic = isStatic;
		setTag(tag);
	}

	/**
	 * Method ensureRepositoryRelativeToRoot.
	 */
	private void ensureRepositoryRelativeToRoot() {
		String rootDir;
		try {
			rootDir = getRootDirectory();
		} catch (CVSException e) {
			// Ignore the for now. Using the root will show the error to the user.
			return;
		}
		if (repository.startsWith(rootDir)) {
			repository = repository.substring(rootDir.length());
		}
		if (repository.startsWith(ResourceSyncInfo.SEPARATOR)) {
			repository = repository.substring(ResourceSyncInfo.SEPARATOR.length());
		}
	}
	
	public boolean equals(Object other) {
		if(other == this) return true;
		if (!(other instanceof FolderSyncInfo)) return false;
			
		FolderSyncInfo syncInfo = ((FolderSyncInfo)other);
		if (!getRoot().equals(syncInfo.getRoot())) return false;
		if (!getRepository().equals(syncInfo.getRepository())) return false;
		if (getIsStatic() != syncInfo.getIsStatic()) return false;
		if ((getTag() == null) || (syncInfo.getTag() == null)) {
			if ((getTag() == null) && (syncInfo.getTag() != null) && (syncInfo.getTag().getType() != CVSTag.HEAD)) {
				return false;
			} else if ((syncInfo.getTag() == null) && (getTag() != null) && (getTag().getType() != CVSTag.HEAD)) {
				return false;
			}
		} else if (!getTag().equals(syncInfo.getTag())) {
			return false;
		}
		return true;
	}
	/**
	 * Gets the root, cannot be <code>null</code>.
	 * 
	 * @return Returns a String
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * Answer the directory portion of the root. For example, if
	 *    root = :pserver:user@host:/home/user/repo
	 * then /home/user/repo is return.
	 * <p>
	 * The root does not neccesarily contain a user name, in which cas the format is
	 * :pserver:host:/home/user/repo.
	 *
	 * 
	 * @return String
	 */
	private String getRootDirectory() throws CVSException {
		try {
			String root = getRoot();
			int index = root.lastIndexOf(CVSRepositoryLocation.HOST_SEPARATOR);
			if (index == -1) {
				// If the username is missing, we have to find the third ':'.
				index = root.indexOf(CVSRepositoryLocation.COLON);
				if (index == 0) {
					// This indicates that the conection method is present.
					// It is surrounded by two colons so skip them.
					index = root.indexOf(CVSRepositoryLocation.COLON, index + 1);
					index = root.indexOf(CVSRepositoryLocation.COLON, index + 1);
				}
				if (index == -1) {
					// The host colon is missing.
					// Look for a slash to find the path
					index = root.indexOf(ResourceSyncInfo.SEPARATOR);
					// Decrement the index since the slash is part of the path
					if (index != -1) index--;
				}
			} else {
				// If the username was there, we find the first ':' past the '@'
				index = root.indexOf(CVSRepositoryLocation.COLON, index + 1);
			}
			index++;
			// strip off a leading port if there is one
			char c = root.charAt(index);
			while (Character.isDigit(c)) {
				c = root.charAt(++index);
			}
			return root.substring(index);
		} catch (IndexOutOfBoundsException e) {
			IStatus status = new CVSStatus(IStatus.ERROR,CVSMessages.FolderSyncInfo_Maleformed_root_4, e);
			throw new CVSException(status); 
		}
	}
	
	/**
	 * Gets the tag, may be <code>null</code>.
	 * 
	 * @return Returns a String
	 */
	public CVSEntryLineTag getTag() {
		return tag;
	}

	/**
	 * Gets the repository, may be <code>null</code>.
	 * 
	 * @return Returns a String
	 */
	public String getRepository() {
		return repository;
	}

	/**
	 * Gets the isStatic.
	 * 
	 * @return Returns a boolean
	 */
	public boolean getIsStatic() {
		return isStatic;
	}

	/**
	 * Answers a full path to the folder on the remote server. This by appending the repository to the
	 * repository location speficied in the root.
	 * 
	 * Example:
	 * 	root = :pserver:user@host:/home/user/repo
	 * 	repository = folder1/folder2
	 * 
	 * Returns:
	 * 	/home/users/repo/folder1/folder2
	 * 
	 * Note: CVS supports repository root directories that end in a slash (/).
	 * For these directories, the remote location must contain two slashes (//)
	 * between the root directory and the rest of the path. For example:
	 * 	root = :pserver:user@host:/home/user/repo/
	 * 	repository = folder1/folder2
	 * must return:
	 * 	/home/users/repo//folder1/folder2
	 * 
	 * @return the full path of this folder on the server.
	 * @throws a CVSException if the root or repository is malformed.
	 */
	public String getRemoteLocation() throws CVSException {
		return Util.appendPath(getRootDirectory(), getRepository());
	}
	
	/*
	 * Provide a hashCode() method that gaurentees that equal object will have the
	 * same hashCode
	 */
	public int hashCode() {
		return getRoot().hashCode() | getRepository().hashCode();
	}
	
	/**
	 * Sets the tag for the folder.
	 * 
	 * @param tag The tag to set
	 */
	protected void setTag(CVSTag tag) {
		if (tag == null || tag.equals(CVSTag.DEFAULT)) {
			this.tag = null;
		} else {
			this.tag = new CVSEntryLineTag(tag);
		}
	}
	
	public String toString() {
		return getRoot() + "/" + getRepository() + "/" + getTag(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public MutableFolderSyncInfo cloneMutable() {
		MutableFolderSyncInfo newSync = new MutableFolderSyncInfo(this);
		return newSync;
	}

	/**
	 * Return true if this FolderSyncInfo is mapped to the same remote directory
	 * as the other FolderSyncInfo passed as a parameter.
	 * 
	 * @param remoteInfo
	 * @return
	 */
	public boolean isSameMapping(FolderSyncInfo other) {
		if (other == null) return false;
		return (this.getRoot().equals(other.getRoot()) 
			&& this.getRepository().equals(other.getRepository())) ;
	}

/**
	 * Convert a FolderSyncInfo into a byte array that can be stored
	 * in the workspace synchronizer
	 */
	public byte[] getBytes() throws CVSException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeUTF(getRoot());
			dos.writeUTF(getRepository());
			CVSEntryLineTag t = getTag();
			if (t == null) {
				dos.writeUTF(""); //$NON-NLS-1$
			} else {
				dos.writeUTF(t.toString());
			}
			dos.writeBoolean(getIsStatic());
			dos.close();
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}
		return out.toByteArray();
	}

	/**
	 * Convert a byte array that was created using getBytes(FolderSyncInfo)
	 * into a FolderSyncInfo
	 */
	public static FolderSyncInfo getFolderSyncInfo(byte[] bytes) throws CVSException {
		Assert.isNotNull(bytes, "getFolderSyncInfo cannot be called with null parameter"); //$NON-NLS-1$
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(in);
		String root;
		String repository;
		CVSEntryLineTag tag;
		boolean isStatic;
		try {
			root = dis.readUTF();
			repository = dis.readUTF();
			String tagName = dis.readUTF();
			if (tagName.length() == 0) {
				tag = null;
			} else {
				tag = new CVSEntryLineTag(tagName);
			}
			isStatic = dis.readBoolean();
		} catch (IOException e) {
			Status status = new Status(Status.ERROR, CVSProviderPlugin.ID, NLS.bind(CVSMessages.FolderSyncInfo_InvalidSyncInfoBytes, new String(bytes)), e);
			CVSException ex = new CVSException(status);
			throw ex;
		}
		return new FolderSyncInfo(repository, root, tag, isStatic);
	}
	
	/**
	 * Return whether the local directory is mapped to an existing remote 
	 * directory or is just a local placeholder for child folders. a return type
	 * of <code>true</code> indicates that the local folder is not mapped to a
	 * remote folder.
	 * @return whether the directory is a local placeholder
	 */
	public boolean isVirtualDirectory() {
		return getRepository().equals(VIRTUAL_DIRECTORY);
	}

	public FolderSyncInfo asImmutable() {
		return this;
	}
}
