/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * This interface defines a single operation for performing a three-way merge on three
 * input streams. The merged result is written to an output stream.
 * <p>
 * Clients must implement this interface when contributing new mergers to the
 * <code>org.eclipse.compare.streamMergers</code> extension point.
 * </p>
 * @deprecated Clients should use <code>org.eclipse.team.core.mapping.IStorageMerger</code> instead.
 * @since 3.0
 */
public interface IStreamMerger {

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
     * Performs a merge operation on the given input streams and writes the merge result to the output stream.
     * On success a status <code>IStatus.OK</code> is returned, on error a status <code>IStatus.ERROR</code>. 
     * If the merge operation cannot deal with conflicts, the code of the error status has the value <code>IStreamMerger.CONFLICT</code>.
     * For text oriented mergers the encoding for the input and output streams is honored.
     * It is the responsibility of callers to close input and output streams. 
     * 
     * @param output the byte stream to which the merge result is written; the merger will not close the stream
     * @param outputEncoding the encoding to use when writing to the output stream
     * @param ancestor the byte stream from which the common ancestor is read
     * @param ancestorEncoding the encoding of the ancestor input byte stream
     * @param target the byte stream containing the target of the merge
     * @param targetEncoding the encoding of the target input byte stream
     * @param other the byte stream containing the target of the merge
     * @param otherEncoding the encoding of the other input byte stream
     * @param monitor reports progress of the merge operation
     * @return returns the completion status of the operation
     */
	IStatus merge(OutputStream output, String outputEncoding,
			InputStream ancestor, String ancestorEncoding,
			InputStream target, String targetEncoding,
			InputStream other, String otherEncoding,
	        	IProgressMonitor monitor);
}
