/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.patch;

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;

/**
 * A file patch result provides the results of an attempt to apply an
 * {@link IFilePatch2} to the contents of a file. *
 * 
 * @see IFilePatch2
 * @since 3.3
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients can obtain patch results from an {@link IFilePatch2}.
 */
public interface IFilePatchResult {
	
	/**
	 * Return a stream the contains the original contents of the file before
	 * any portions of the patch have been applied.
	 * @return a stream to the original contents of the file before
	 * any portions of the patch have been applied
	 * @see #getPatchedContents()
	 */
	public InputStream getOriginalContents();
	
	/**
	 * Return a stream that contains the file with as much of the patch 
	 * applied as possible. if {@link #hasMatches()} returns <code>false</code>
	 * then the patched contents will match the original contents. Otherwise,
	 * at least a portion of the patch could be successfully applied. if
	 * {@link #hasRejects()} returns <code>false</code>, then the entire patch was
	 * applied. Otherwise, portions could not be applied. The portions that could
	 * not be applied can be obtained by calling {@link #getRejects()}.
	 * 
	 * @return a stream that contains the file with as much of the patch 
	 * applied as possible.
	 */
	public InputStream getPatchedContents();
	
	/**
	 * Return whether the patch has portions that were successfully applied.
	 * @return whether the patch has portions that were successfully applied
	 * @see #getPatchedContents()
	 */
	public boolean hasMatches();
	
	/**
	 * Return whether the patch has portions that were not successfully applied.
	 * @return whether the patch has portions that were not successfully applied
	 * @see #getPatchedContents()
	 */
	public boolean hasRejects();
	
	/**
	 * Return the portions of the patch (referred to a hunks) that could not
	 * be applied.
	 * @return the portions of the patch (referred to a hunks) that could not
	 * be applied
	 * @see #getPatchedContents()
	 */
	public IHunk[] getRejects();
	
	/**
	 * Returns the name of a charset encoding to be used when decoding the contents 
	 * of this result into characters. Returns <code>null</code> if a proper 
	 * encoding cannot be determined.
	 * <p>
	 * Note that this method does not check whether the result is a supported
	 * charset name. Callers should be prepared to handle 
	 * <code>UnsupportedEncodingException</code> where this charset is used. 
	 * </p>
	 *
	 * @return the name of a charset, or <code>null</code>
	 * @exception CoreException if an error happens while determining 
	 * the charset. See any refinements for more information.
	 */
	public String getCharset() throws CoreException;

}
