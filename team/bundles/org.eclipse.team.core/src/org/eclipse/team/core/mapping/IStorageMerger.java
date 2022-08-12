/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.mapping;

import java.io.OutputStream;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * This interface defines a single operation for performing a three-way merge on three
 * instances of {@link IStorage}. The merged result is written to an output stream.
 * <p>
 * Clients must implement this interface when contributing new mergers to the
 * <code>org.eclipse.team.core.storageMergers</code> extension point.
 * </p>
 *
 * @since 3.2
 */
public interface IStorageMerger {

	/**
	 * Indicates the successful completion of the merge operation (value <code>IStatus.OK</code>)
	 */
	public static final int OK= IStatus.OK;

	/**
	 * Indicates that a change conflict prevented the merge from successful completion (value <code>1</code>)
	 */
	public static final int CONFLICT= 1;

	/**
	 * Status code describing an internal error (value <code>2</code>)
	 */
	public static final int INTERNAL_ERROR= 2;

	/**
	 * Indicates that at least one of the encodings associated with the input was unsupported (value <code>3</code>)
	 */
	public static final int UNSUPPORTED_ENCODING= 3;

	/**
	* Performs a merge operation on the given storage instances and writes the merge result to the output stream.
	* On success a status <code>IStatus.OK</code> is returned, on error a status <code>IStatus.ERROR</code>.
	* If the merge operation cannot deal with conflicts, the code of the error status has the value <code>IStreamMerger.CONFLICT</code>.
	* For text oriented mergers the encoding for the input and output is honored if they implement
	* {@link IEncodedStorage}.
	* It is the responsibility of callers to close the output stream.
	* <p>
	* The provided ancestor may be <code>null</code> if this merger
	* returns <code>true</code> from {@link #canMergeWithoutAncestor()}.
	*
	* @param output the byte stream to which the merge result is written; the merger will not close the stream
	* @param outputEncoding the encoding to use when writing to the output stream
	* @param ancestor the storage from which the common ancestor is read
	* @param target the storage containing the target of the merge
	* @param other the storage containing the target of the merge
	* @param monitor reports progress of the merge operation
	* @return returns the completion status of the operation
	* @throws CoreException if an error occurs
	*/
	IStatus merge(OutputStream output, String outputEncoding,
			IStorage ancestor, IStorage target, IStorage other,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Return whether this merger can merge the two contributors
	 * without an ancestor. This is typically not possible but may be
	 * for some file types (for instances, files that contain a
	 * timestamp based list of events).
	 * @return whether this merger can merge the two contributors
	 * without an ancestor
	 */
	boolean canMergeWithoutAncestor();

}
