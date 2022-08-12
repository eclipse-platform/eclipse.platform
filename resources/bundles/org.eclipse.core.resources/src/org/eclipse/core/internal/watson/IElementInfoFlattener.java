/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.watson;

import java.io.*;
import org.eclipse.core.runtime.IPath;

/**
 * The <code>IElementInfoFlattener</code> interface supports
 * reading and writing element info objects.
 */
public interface IElementInfoFlattener {
	/**
	 * Reads an element info from the given input stream.
	 * @param elementPath the path of the element to be read
	 * @param input the stream from which the element info should be read.
	 * @return the object associated with the given elementPath,
	 *   which may be <code>null</code>.
	 */
	Object readElement(IPath elementPath, DataInput input) throws IOException;

	/**
	 * Writes the given element to the output stream.
	 * <p> N.B. The bytes written must be sufficient for the
	 * purposes of reading the object back in.
	 * @param elementPath the element's path in the tree
	 * @param element the object associated with the given path,
	 *   which may be <code>null</code>.
	 */
	void writeElement(IPath elementPath, Object element, DataOutput output) throws IOException;
}
