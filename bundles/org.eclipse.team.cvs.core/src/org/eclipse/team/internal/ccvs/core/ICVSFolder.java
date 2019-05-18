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
package org.eclipse.team.internal.ccvs.core;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

/**
 * The CVS analog of a directory. CVS folders have access to synchronization information
 * that describes the association between the folder and the remote repository.
 * 
 * @see ICVSResource
 * @see ICVSFile
 */
public interface ICVSFolder extends ICVSResource {
	
	public static final int FILE_MEMBERS = 1;
	public static final int FOLDER_MEMBERS = 2;
	public static final int IGNORED_MEMBERS = 4;
	public static final int UNMANAGED_MEMBERS = 8;
	public static final int MANAGED_MEMBERS = 16;
	public static final int EXISTING_MEMBERS = 32;
	public static final int PHANTOM_MEMBERS = 64;
	public static final int ALL_MEMBERS = FILE_MEMBERS 
		| FOLDER_MEMBERS 
		| IGNORED_MEMBERS 
		| UNMANAGED_MEMBERS 
		| MANAGED_MEMBERS 
		| EXISTING_MEMBERS
		| PHANTOM_MEMBERS;
	public static final int ALL_EXISTING_MEMBERS = FILE_MEMBERS 
		| FOLDER_MEMBERS 
		| IGNORED_MEMBERS 
		| UNMANAGED_MEMBERS 
		| MANAGED_MEMBERS 
		| EXISTING_MEMBERS;
	public static final int ALL_UNIGNORED_MEMBERS = FILE_MEMBERS
		| FOLDER_MEMBERS
		| UNMANAGED_MEMBERS
		| MANAGED_MEMBERS
		| EXISTING_MEMBERS
		| PHANTOM_MEMBERS;
	
	/**
	 * Answers and array of <code>ICVSResource</code> elements that are immediate 
	 * children of this remote resource, in no particular order. The server may be contacted.
	 * 
	 * @param monitor a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * 
	 * @return array of immediate children of this remote resource. 
	 */
	public ICVSResource[] fetchChildren(IProgressMonitor monitor) throws CVSException;
	
	/**
	 * Answer the immediate children of the resource that are known
	 * at the time of invocation. The server is never contacted.
	 * The flags indicate the type of members to be included.
	 * Here are the rules for specifying just one flag:
	 * 
	 *   a) FILE_MEMBERS and FOLDER_MEMBERS will return managed 
	 *     and unmanaged resource of the corresponding type
	 *   b) IGNORED_MEMBERS, MANAGED_RESOURCES and UNMANAGED_RESOURCES
	 *     will return files and folders of the given type
	 *   c) EXISTING_MEMBERS and PHANTOM_MEMBERS will return existing 
	 *     and phantom resource of the corresponding type
	 * 
	 * Note: Unmanaged resources are those that are neither managed or ignored.
	 * 
	 * If all of the flags from either group a), group b) or group c)
	 * are not present, the same rule for default types applies. 
	 * For example,
	 * - FILE_MEMBERS | FOLDER_MEMBERS will return all managed
	 *   and unmanaged, existing and phantom files and folders. 
	 * - IGNORED_MEMBERS | UNMANAGED_MEMBERS will return all
	 *   ignored or unmanaged, existing or phantom files and folders
	 * If a flag from each group is present, the result is the
	 * union of the sets. For example,
	 * - FILE_MEMBERS | IGNORED_MEMBERS | EXISTING_MEMBERS will return all
	 *   existing ignored files.
	 */
	public ICVSResource[] members(int flags) throws CVSException;
	
	/**
	 * Answers a child folder of this resource with the given name or <code>null</code> if 
	 * the given folder does not have a child with that name.
	 */
	public ICVSFolder getFolder(String name) throws CVSException;
	
	/**
	 * Answers a child file of this resource with the given name or <code>null</code> if 
	 * the given folder does not have a child with that name.
	 */
	public ICVSFile getFile(String name) throws CVSException;

	/**
	 * Return the child resource at the given path relative to
	 * the receiver.
	 */
	public ICVSResource getChild(String path) throws CVSException;
	
	/**
	 * Create the folder if it did not exist before. Does only
	 * work if the direct subfolder did exist.
	 * 
	 * @throws CVSException if for some reason it was not possible to create the folder
	 */
	public void mkdir() throws CVSException;

	/**
	 * Answers the folder's synchronization information or <code>null</code> if the folder
	 * is not a CVS folder.
	 * <p>
	 * To modify the folder sync info the caller must call <code>setFolderSyncInfo</code> with
	 * new sync information.</p>
	 */
	public FolderSyncInfo getFolderSyncInfo() throws CVSException;
	
	/**
	 * Set the folder sync information for this folder. Setting the folder information
	 * to <code>null</code> is not supported. The only mechanism for removing an existing
	 * CVS folder is to delete the resource.
	 */
	public void setFolderSyncInfo(FolderSyncInfo folderInfo) throws CVSException;	
	
	/**
	 * Accepts the visitor on all files and all subFolder in the folder. Files are
	 * visited first, then all the folders..
	 */
	public void acceptChildren(ICVSResourceVisitor visitor) throws CVSException;
	
	/**
	 * Answers <code>true</code> if the folder has valid CVS synchronization information and
	 * <code>false</code> otherwise. 
	 * 
	 * Note: This method does not throw an exception so this method does not differentiate
	 * between a folder not be shared with CVS and a folder that is shared but whose sync info has
	 * become corrupt. Use getFolderSyncInfo() to differentiate between these situations.
	 * 
	 * Also Note: A folder that is a CVS folder may not exist in the workspace. The purpose of
	 * such a folder is to act as a remotely existing folder that does not exist locally. 
	 * This is normally done in order to remember outgoing file deletions when a parent
	 * folder is deleted.
	 * Creating the folder will result in a folder that is mapped to a remote folder.
	 */
	public boolean isCVSFolder() throws CVSException;
	
	/**
	 * Runs the given action as an atomic cvs local workspace operation 
	 * rooted at this cvs folder.
	 * <p>
	 * After running a method that modifies cvs resource state in the 
	 * local workspace, registered listeners receive after-the-fact 
	 * notification in the form of a resource state change event. In addition,
	 * any resource state information persistance is batched.
	 * This method allows clients to call a number of
	 * methods that modify resources and only have resource
	 * change event notifications reported at the end of the entire
	 * batch.
	 * </p>
	 * <p>
	 * If this method is called in the dynamic scope of another such
	 * call, this method simply runs the action.
	 * </p>
	 *
	 * @param job the action to perform
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CVSException if the operation failed.
	 */
	public void run(ICVSRunnable job, IProgressMonitor monitor) throws CVSException;
}
