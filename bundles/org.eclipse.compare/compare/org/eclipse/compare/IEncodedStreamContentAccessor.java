/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.core.runtime.CoreException;

/**
 * Extension for <code>IStreamContentAccessor</code>. Extends the original
 * concept of a <code>IStreamContentAccessor</code> to answer the Charset (encoding) used for the stream.
 * 
 * @since 3.0
 */
public interface IEncodedStreamContentAccessor extends IStreamContentAccessor {
	
	/**
	 * Returns the name of a charset encoding to be used when decoding this 
	 * stream accessor's contents into characters. Returns <code>null</code> if a proper 
	 * encoding cannot be determined.
	 * <p>
	 * <b>Note</b>:  this method does not check whether the result is a supported
	 * charset name. Callers should be prepared to handle 
	 * <code>UnsupportedEncodingException</code> where this charset is used. 
	 * </p>
	 * @return the name of a charset, or <code>null</code>
	 * @exception CoreException if an error happens while determining 
	 * the charset. See any refinements for more information.
	 * @see IStreamContentAccessor#getContents
	 * @since 3.0
	 */
	String getCharset() throws CoreException;
}
