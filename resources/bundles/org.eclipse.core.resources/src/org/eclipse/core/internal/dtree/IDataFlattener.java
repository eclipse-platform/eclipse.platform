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
package org.eclipse.core.internal.dtree;

import java.io.*;
import org.eclipse.core.runtime.IPath;

/**
 * The <code>IElementInfoFlattener</code> interface supports
 * reading and writing element info objects.
 */
public interface IDataFlattener {
	/**
	 * Reads a data object from the given input stream.
	 * @param path the path of the element to be read
	 * @param input the stream from which the element info should be read.
	 * @return the object associated with the given path,
	 *   which may be <code>null</code>.
	 */
	Object readData(IPath path, DataInput input) throws IOException;

	/**
	 * Writes the given data to the output stream.
	 * <p> N.B. The bytes written must be sufficient for the
	 * purposes of reading the object back in.
	 * @param path the element's path in the tree
	 * @param data the object associated with the given path,
	 *   which may be <code>null</code>.
	 */
	void writeData(IPath path, Object data, DataOutput output) throws IOException;
}
