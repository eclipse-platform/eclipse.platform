/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources.team;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Primary interface for hooking the implementation of
 * <code>IResource.move</code> and <code>IResource.delete</code>.
 * <p>
 * This interface is intended to be implemented by the team component in
 * conjunction with the <code>org.eclipse.core.resources.moveDeleteHook</code>
 * standard extension point. Individual team providers may also implement this
 * interface. It is not intended to be implemented by other clients. The methods
 * defined on this interface are called from within the implementations of
 * <code>IResource.move</code> and <code>IResource.delete</code>. They are not
 * intended to be called from anywhere else.
 * </p>
 *
 * @since 2.0
 */
public interface IMoveDeleteHook {

	/**
	 * Implements <code>IResource.delete(int,IProgressMonitor)</code> where the
	 * receiver is a file. Returns <code>true</code> to accept responsibility
	 * for implementing this operation as per the API contract.
	 * <p>
	 * In broad terms, a full re-implementation should delete the file in the
	 * local file system and then call <code>tree.deletedFile</code> to complete
	 * the updating of the workspace resource tree to reflect this fact. If
	 * unsuccessful in deleting the file from the local file system, it
	 * should instead call <code>tree.failed</code> to report the reason for
	 * the failure. In either case, it should return <code>true</code> to
	 * indicate that the operation was attempted. The <code>FORCE</code> update
	 * flag needs to be honored: unless <code>FORCE</code> is specified, the
	 * implementation must use <code>tree.isSynchronized</code> to determine
	 * whether the file is in sync before attempting to delete it.
	 * The <code>KEEP_HISTORY</code> update flag needs to be honored as well;
	 * use <code>tree.addToLocalHistory</code> to capture the contents of the
	 * file before deleting it from the local file system.
	 * </p><p>
	 * An extending implementation should perform whatever pre-processing it
	 * needs to do and then call <code>tree.standardDeleteFile</code> to
	 * explicitly invoke the standard file deletion behavior, which deletes
	 * both the file from the local file system and updates the workspace
	 * resource tree. It should return <code>true</code> to indicate that the
	 * operation was attempted.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardDeleteFile</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param file the handle of the file to delete; the receiver of
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this method
	 *   attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#delete(int,IProgressMonitor)
	 */
	boolean deleteFile(IResourceTree tree, IFile file, int updateFlags, IProgressMonitor monitor);

	/**
	 * Implements <code>IResource.delete(int,IProgressMonitor)</code> where the
	 * receiver is a folder. Returns <code>true</code> to accept responsibility
	 * for implementing this operation as per the API contract.
	 * <p>
	 * In broad terms, a full re-implementation should delete the directory tree
	 * in the local file system and then call <code>tree.deletedFolder</code> to
	 * complete the updating of the workspace resource tree to reflect this fact.
	 * If unsuccessful in deleting the directory or any of its descendents from
	 * the local file system, it should instead call <code>tree.failed</code> to
	 * report each reason for failure. In either case it should return
	 * <code>true</code> to indicate that the operation was attempted.
	 * The <code>FORCE</code> update flag needs to be honored: unless
	 * <code>FORCE</code> is specified, the implementation must use
	 * <code>tree.isSynchronized</code> to  determine whether the folder
	 * subtree is in sync before attempting to delete it.
	 * The <code>KEEP_HISTORY</code> update flag needs to be honored as well;
	 * use <code>tree.addToLocalHistory</code> to capture the contents of any
	 * files being deleted.
	 * </p><p>
	 * A partial re-implementation should perform whatever pre-processing it
	 * needs to do and then call <code>tree.standardDeleteFolder</code> to
	 * explicitly invoke the standard folder deletion behavior, which deletes
	 * both the folder and its descendents from the local file system and
	 * updates the workspace resource tree. It should return <code>true</code>
	 * to indicate that the operation was attempted.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardDeleteFolder</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param folder the handle of the folder to delete; the receiver of
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this
	 *   method attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#delete(int,IProgressMonitor)
	 */
	boolean deleteFolder(IResourceTree tree, IFolder folder, int updateFlags, IProgressMonitor monitor);

	/**
	 * Implements <code>IResource.delete(int,IProgressMonitor)</code> where the
	 * receiver is a project. Returns <code>true</code> to accept responsibility
	 * for implementing this operation as per the API contract.
	 * <p>
	 * In broad terms, a full re-implementation should delete the project content area in
	 * the local file system if required (the files of a closed project should be deleted
	 * only if the <code>IResource.ALWAYS_DELETE_PROJECT_CONTENTS</code> update
	 * flag is specified; the files of an open project should be deleted unless the
	 * the <code>IResource.NEVER_DELETE_PROJECT_CONTENTS</code> update flag is
	 * specified). It should then call <code>tree.deletedProject</code> to complete
	 * the updating of the workspace resource tree to reflect this fact. If unsuccessful
	 * in deleting the project's files from the local file system, it should instead call
	 * <code>tree.failed</code> to report the reason for the failure. In either case, it
	 * should return <code>true</code> to indicate that the operation was attempted.
	 * The <code>FORCE</code> update flag may need to be honored if the project is open:
	 * unless <code>FORCE</code> is specified, the implementation must use
	 * <code>tree.isSynchronized</code> to  determine whether the project subtree is in
	 * sync before attempting to delete it.
	 * Note that local history is not maintained when a project is deleted,
	 * regardless of the setting of the <code>KEEP_HISTORY</code> update flag.
	 * </p><p>
	 * A partial re-implementation should perform whatever pre-processing it needs
	 * to do and then call <code>tree.standardDeleteProject</code> to explicitly
	 * invoke the standard project deletion behavior. It should return <code>true</code>
	 * to indicate that the operation was attempted.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardDeleteProject</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param project the handle of the project to delete; the receiver of
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.delete(int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this
	 *   method attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#delete(int,IProgressMonitor)
	 */
	boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor);

	/**
	 * Implements <code>IResource.move(IPath,int,IProgressMonitor)</code> where
	 * the receiver is a file. Returns <code>true</code> to accept
	 * responsibility for implementing this operation as per the API contract.
	 * <p>
	 * On entry to this hook method, the following is guaranteed about the
	 * workspace resource tree: the source file exists; the destination file
	 * does not exist; the container of the destination file exists and is
	 * accessible. In broad terms, a full re-implementation should move the file
	 * in the local file system and then call <code>tree.moveFile</code> to
	 * complete the updating of the workspace resource tree to reflect this
	 * fact. If unsuccessful in moving the file in the local file system,
	 * it should instead call <code>tree.failed</code> to report the reason for
	 * the failure. In either case, it should return <code>true</code> to
	 * indicate that the operation was attempted.
	 * The <code>FORCE</code> update flag needs to be honored: unless
	 * <code>FORCE</code> is specified, the implementation must use
	 * <code>tree.isSynchronized</code> to determine whether the file is in sync before
	 * attempting to move it.
	 * The <code>KEEP_HISTORY</code> update flag needs to be honored as well; use
	 * <code>tree.addToLocalHistory</code> to capture the contents of the file
	 * (naturally, this must be before moving the file from the local file system).
	 * </p><p>
	 * An extending implementation should perform whatever pre-processing it needs
	 * to do and then call <code>tree.standardMoveFile</code> to explicitly
	 * invoke the standard file moving behavior, which moves both the file in the
	 * local file system and updates the workspace resource tree. It should return
	 * <code>true</code> to indicate that the operation was attempted.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardMoveFile</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param source the handle of the file to move; the receiver of
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param destination the handle of where the file will move to; the handle
	 *    equivalent of the first parameter to
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this
	 *   method attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#move(org.eclipse.core.runtime.IPath,int,IProgressMonitor)
	 */
	boolean moveFile(IResourceTree tree, IFile source, IFile destination, int updateFlags, IProgressMonitor monitor);

	/**
	 * Implements <code>IResource.move(IPath,int,IProgressMonitor)</code> where
	 * the receiver is a project. Returns <code>true</code> to accept
	 * responsibility for implementing this operation as per the API contract.
	 * <p>
	 * On entry to this hook method, the following is guaranteed about the
	 * workspace resource tree: the source folder exists; the destination folder
	 * does not exist; the container of the destination folder exists and is
	 * accessible. In broad terms, a full re-implementation should move the
	 * directory tree in the local file system and then call
	 * <code>tree.movedFolder</code> to complete the updating of the workspace
	 * resource tree to reflect this fact. If unsuccessful in moving the
	 * directory or any of its descendents in the local file system,
	 * call <code>tree.failed</code> to report each reason for failure.
	 * In either case, return <code>true</code> to indicate that the operation
	 * was attempted.
	 * The <code>FORCE</code> update flag needs to be honored: unless
	 * <code>FORCE</code> is specified, the implementation must use
	 * <code>tree.isSynchronized</code> to determine whether the folder subtree is in sync
	 * before attempting to move it.
	 * The <code>KEEP_HISTORY</code> update flag needs to be honored as well; use
	 * <code>tree.addToLocalHistory</code> to capture the contents of any files being
	 * moved.
	 * </p><p>
	 * A partial re-implementation should perform whatever pre-processing it needs
	 * to do and then call <code>tree.standardMoveFolder</code> to explicitly
	 * invoke the standard folder move behavior, which move both the folder
	 * and its descendents in the local file system and updates the workspace resource
	 * tree. Return <code>true</code> to indicate that the operation was attempted.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardDeleteFolder</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param source the handle of the folder to move; the receiver of
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param destination the handle of where the folder will move to; the
	 *    handle equivalent of the first parameter to
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this
	 *   method attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#move(org.eclipse.core.runtime.IPath,int,IProgressMonitor)
	 */
	boolean moveFolder(IResourceTree tree, IFolder source, IFolder destination, int updateFlags, IProgressMonitor monitor);

	/**
	 * Implements <code>IResource.move(IPath,int,IProgressMonitor)</code> and
	 * <code>IResource.move(IProjectDescription,int,IProgressMonitor)</code>
	 * where the receiver is a project. Returns <code>true</code> to accept
	 * responsibility for implementing this operation as per the API contracts.
	 * <p>
	 * On entry to this hook method, the source project is guaranteed to exist
	 * and be open in the workspace resource tree. If the given description
	 * contains a different name from that of the given project, then the
	 * project is being renamed (and its content possibly relocated). If the
	 * given description contains the same name as the given project, then the
	 * project is being relocated but not renamed. When the project is being
	 * renamed, the destination project is guaranteed not to exist in the
	 * workspace resource tree.
	 * </p><p>
	 * Returning <code>false</code> is the easy way for the implementation to
	 * say "pass". It is equivalent to calling
	 * <code>tree.standardMoveProject</code> and returning <code>true</code>.
	 * </p><p>
	 * The implementation of this method runs "below" the resources API and is
	 * therefore very restricted in what resource API method it can call. The
	 * list of useable methods includes most resource operations that read but
	 * do not update the resource tree; resource operations that modify
	 * resources and trigger deltas must not be called from within the dynamic
	 * scope of the invocation of this method.
	 * </p>
	 *
	 * @param tree the workspace resource tree; this object is only valid
	 *    for the duration of the invocation of this method, and must not be
	 *    used after this method has completed
	 * @param source the handle of the open project to move; the receiver of
	 *    <code>IResource.move(IProjectDescription,int,IProgressMonitor)</code>
	 *    or <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param description the new description of the project; the first
	 *    parameter to
	 *    <code>IResource.move(IProjectDescription,int,IProgressMonitor)</code>, or
	 *    a copy of the project's description with the location changed to the
	 *    path given in the first parameter to
	 *    <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param updateFlags bit-wise or of update flag constants as per
	 *    <code>IResource.move(IProjectDescription,int,IProgressMonitor)</code>
	 *    or <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @param monitor the progress monitor, or <code>null</code> as per
	 *    <code>IResource.move(IProjectDescription,int,IProgressMonitor)</code>
	 *    or <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * @return <code>false</code> if this method declined to assume
	 *   responsibility for this operation, and <code>true</code> if this method
	 *   attempted to carry out the operation
	 * @exception OperationCanceledException if the operation is canceled.
	 * Cancelation can occur even if no progress monitor is provided.
	 * @see IResource#move(org.eclipse.core.runtime.IPath,int,IProgressMonitor)
	 * @see IResource#move(IProjectDescription,int,IProgressMonitor)
	 */
	boolean moveProject(IResourceTree tree, IProject source, IProjectDescription description, int updateFlags, IProgressMonitor monitor);
}
