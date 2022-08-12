/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.resources.team;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * Provides internal access to the workspace resource tree for the purposes of
 * implementing the move and delete operations. Implementations of
 * <code>IMoveDeleteHook</code> call these methods.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IResourceTree {

	/**
	 * Constant indicating that no file timestamp was supplied.
	 *
	 * @see #movedFile(IFile, IFile)
	 */
	long NULL_TIMESTAMP = 0L;

	/**
	 * Adds the current state of the given file to the local history.
	 * Does nothing if the file does not exist in the workspace resource tree,
	 * or if it exists in the workspace resource tree but not in the local file
	 * system.
	 * <p>
	 * This method is used to capture the state of a file in the workspace
	 * local history before it is overwritten or deleted.
	 * </p>
	 *
	 * @param file the file to be captured
	 */
	void addToLocalHistory(IFile file);

	/**
	 * Returns whether the given resource and its descendents to the given depth
	 * are considered to be in sync with the local file system. Returns
	 * <code>false</code> if the given resource does not exist in the workspace
	 * resource tree, but exists in the local file system; and conversely.
	 *
	 * @param resource the resource of interest
	 * @param depth the depth (one of <code>IResource.DEPTH_ZERO</code>,
	 *   <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>)
	 * @return <code>true</code> if the resource is synchronized, and
	 *    <code>false</code> in all other cases
	 */
	boolean isSynchronized(IResource resource, int depth);

	/**
	 * Computes the timestamp for the given file in the local file system.
	 * Returns <code>NULL_TIMESTAMP</code> if the timestamp of the file in
	 * the local file system cannot be determined. The file need not exist in
	 * the workspace resource tree; however, if the file's project does not
	 * exist in the workspace resource tree, this method returns
	 * <code>NULL_TIMESTAMP</code> because the project's local content area
	 * is indeterminate.
	 * <p>
	 * Note that the timestamps used for workspace resource tree file
	 * synchronization are not necessarily interchangeable with
	 * <code>java.io.File</code> last modification time.The ones computed by
	 * <code>computeTimestamp</code> may have a higher resolution in some
	 * operating environments.
	 * </p>
	 *
	 * @param file the file of interest
	 * @return the local file system timestamp for the file, or
	 *    <code>NULL_TIMESTAMP</code> if it could not be computed
	 */
	long computeTimestamp(IFile file);

	/**
	 * Returns the timestamp for the given file as recorded in the workspace
	 * resource tree. Returns <code>NULL_TIMESTAMP</code> if the given file
	 * does not exist in the workspace resource tree, or if the timestamp is
	 * not known.
	 * <p>
	 * Note that the timestamps used for workspace resource tree file
	 * synchronization are not necessarily interchangeable with
	 * <code>java.io.File</code> last modification time.The ones computed by
	 * <code>computeTimestamp</code> may have a higher resolution in some
	 * operating environments.
	 * </p>
	 *
	 * @param file the file of interest
	 * @return the workspace resource tree timestamp for the file, or
	 *    <code>NULL_TIMESTAMP</code> if the file does not exist in the
	 *    workspace resource tree, or if the timestamp is not known
	 */
	long getTimestamp(IFile file);

	/**
	 * Updates the timestamp for the given file in the workspace resource tree.
	 * The file is the local file system is not affected. Does nothing if the
	 * given file does not exist in the workspace resource tree.
	 * <p>
	 * The given timestamp should be that of the corresponding file in the local
	 * file system (as computed by <code>computeTimestamp</code>). A discrepancy
	 * between the timestamp of the file in the local file system and the
	 * timestamp recorded in the workspace resource tree means that the file is
	 * out of sync (<code>isSynchronized</code> returns <code>false</code>).
	 * </p>
	 * <p>
	 * This operation should be used after <code>movedFile/Folder/Project</code>
	 * to correct the workspace resource tree record when file timestamps change
	 * in the course of a move operation.
	 * </p>
	 * <p>
	 * Note that the timestamps used for workspace resource tree file
	 * synchronization are not necessarily interchangeable with
	 * <code>java.io.File</code> last modification time.The ones computed by
	 * <code>computeTimestamp</code> may have a higher resolution in some
	 * operating environments.
	 * </p>
	 *
	 * @param file the file of interest
	 * @param timestamp the local file system timestamp for the file, or
	 *    <code>NULL_TIMESTAMP</code> if unknown
	 * @see #computeTimestamp(IFile)
	 */
	void updateMovedFileTimestamp(IFile file, long timestamp);

	/**
	 * Declares that the operation has failed for the specified reason.
	 * This method may be called multiple times to report multiple
	 * failures. All reasons will be accumulated and taken into consideration
	 * when deciding the outcome of the hooked operation as a whole.
	 *
	 * @param reason the reason the operation (or sub-operation) failed
	 */
	void failed(IStatus reason);

	/**
	 * Declares that the given file has been successfully deleted from the
	 * local file system, and requests that the corresponding deletion should
	 * now be made to the workspace resource tree. No action is taken if the
	 * given file does not exist in the workspace resource tree.
	 * <p>
	 * This method clears out any markers, session properties, and persistent
	 * properties associated with the given file.
	 * </p>
	 *
	 * @param file the file that was just deleted from the local file system
	 */
	void deletedFile(IFile file);

	/**
	 * Declares that the given folder and all its descendents have been
	 * successfully deleted from the local file system, and requests that the
	 * corresponding deletion should now be made to the workspace resource tree.
	 * No action is taken if the given folder does not exist in the workspace
	 * resource tree.
	 * <p>
	 * This method clears out any markers, session properties, and persistent
	 * properties associated with the given folder or its descendents.
	 * </p>
	 *
	 * @param folder the folder that was just deleted from the local file system
	 */
	void deletedFolder(IFolder folder);

	/**
	 * Declares that the given project's content area in the local file system
	 * has been successfully dealt with in an appropriate manner, and requests
	 * that the corresponding deletion should now be made to the workspace
	 * resource tree. No action is taken if the given project does not exist in
	 * the workspace resource tree.
	 * <p>
	 * This method clears out everything associated with this project and any of
	 * its descendent resources, including: markers; session properties;
	 * persistent properties; local history; and project-specific plug-ins
	 * working data areas. The project's content area is not affected.
	 * </p>
	 *
	 * @param project the project being deleted
	 */
	void deletedProject(IProject project);

	/**
	 * Declares that the given source file has been successfully moved to the
	 * given destination in the local file system, and requests that the
	 * corresponding changes should now be made to the workspace resource tree.
	 * No action is taken if the given source file does not exist in the
	 * workspace resource tree.
	 * <p>
	 * The destination file must not already exist in the workspace resource
	 * tree.
	 * </p><p>
	 * This operation carries over the file timestamp unchanged. Use
	 * <code>updateMovedFileTimestamp</code> to update the timestamp
	 * of the file if its timestamp changed as a direct consequence of the move.
	 * </p>
	 *
	 * @param source the handle of the source file that was moved
	 * @param destination the handle of where the file moved to
	 * @see #computeTimestamp(IFile)
	 */
	void movedFile(IFile source, IFile destination);

	/**
	 * Declares that the given source folder and its descendents have been
	 * successfully moved to the given destination in the local file system,
	 * and requests that the corresponding changes should now be made to the
	 * workspace resource tree for the folder and all its descendents. No action
	 * is taken if the given source folder does not exist in the workspace
	 * resource tree.
	 * <p>
	 * This operation carries over file timestamps unchanged. Use
	 * <code>updateMovedFileTimestamp</code> to update the timestamp of files
	 * whose timestamps changed as a direct consequence of the move.
	 * </p><p>
	 * The destination folder must not already exist in the workspace resource
	 * tree.
	 * </p>
	 *
	 * @param source the handle of the source folder that was moved
	 * @param destination the handle of where the folder moved to
	 */
	void movedFolderSubtree(IFolder source, IFolder destination);

	/**
	 * Declares that the given source project and its files and folders have
	 * been successfully relocated in the local file system if required, and
	 * requests that the rename and/or relocation should now be made to the
	 * workspace resource tree for the project and all its descendents. No
	 * action is taken if the given project does not exist in the workspace
	 * resource tree.
	 * <p>
	 * This operation carries over file timestamps unchanged. Use
	 * <code>updateMovedFileTimestamp</code> to update the timestamp of files whose
	 * timestamps changed as a direct consequence of the move.
	 * </p><p>
	 * If the project is being renamed, the destination project must not
	 * already exist in the workspace resource tree.
	 * </p><p>
	 * Local history is not preserved if the project is renamed. It is preserved
	 * when the project's content area is relocated without renaming the
	 * project.
	 * </p>
	 *
	 * @param source the handle of the source project that was moved
	 * @param description the new project description
	 * @return <code>true</code> if the move succeeded, and <code>false</code>
	 *    otherwise
	 */
	boolean movedProjectSubtree(IProject source, IProjectDescription description);

	/**
	 * Deletes the given file in the standard manner from both the local file
	 * system and from the workspace resource tree.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of  <code>file.delete(updateFlags, monitor)</code> because all
	 * regular API operations that modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param file the file to delete
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 */
	void standardDeleteFile(IFile file, int updateFlags, IProgressMonitor monitor);

	/**
	 * Deletes the given folder and its descendents in the standard manner from
	 * both the local file system and from  the workspace resource tree.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of  <code>folder.delete(updateFlags, monitor)</code> because all
	 * regular API operations that modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param folder the folder to delete
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 */
	void standardDeleteFolder(IFolder folder, int updateFlags, IProgressMonitor monitor);

	/**
	 * Deletes the given project and its descendents in the standard manner from
	 * both the local file system and from the workspace resource tree.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of  <code>project.delete(updateFlags, monitor)</code> because all
	 * regular API operations that modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param project the project to delete
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 */
	void standardDeleteProject(IProject project, int updateFlags, IProgressMonitor monitor);

	/**
	 * Moves the given file in the standard manner from both the local file
	 * system and from the workspace resource tree.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of <code>source.move(destination.getProjectRelativePath(),
	 * updateFlags, monitor)</code> because all regular API  operations that
	 * modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param source the handle of the source file to move
	 * @param destination the handle of where the file will move to
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 */
	void standardMoveFile(IFile source, IFile destination, int updateFlags, IProgressMonitor monitor);

	/**
	 * Moves the given folder and its descendents in the standard manner from
	 * both the local file system and from the workspace resource tree.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of <code>source.move(destination.getProjectRelativePath(),
	 * updateFlags, monitor)</code> because all regular API  operations that
	 * modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param source the handle of the source folder to move
	 * @param destination the handle of where the folder will move to
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 */
	void standardMoveFolder(IFolder source, IFolder destination, int updateFlags, IProgressMonitor monitor);

	/**
	 * Renames and/or relocates the given project in the standard manner.
	 * <p>
	 * Implementations of <code>IMoveDeleteHook</code> must invoke this method
	 * in lieu of <code>source.move(description, updateFlags, monitor)</code>
	 * because all regular API  operations that modify resources are off limits.
	 * </p><p>
	 * If the operation fails, the reason for the failure is automatically
	 * collected by an internal call to <code>failed</code>.
	 * </p>
	 *
	 * @param source the handle of the source folder to move
	 * @param description the new project description
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 */
	void standardMoveProject(IProject source, IProjectDescription description, int updateFlags, IProgressMonitor monitor);
}
