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

/**
 * The <code>IElementInfoFlattener</code> interface supports
 * reading and writing element info objects.
 */
public interface IDataFlattener {
	/**
	 * Reads a data object from the given input stream.
	 * @param rootNode whether the node being read is the root of the tree
	 * @return the object read, which may be <code>null</code>.
	 */
	Object readData(boolean rootNode, DataInput input) throws IOException;

	/**
	 * Writes the given data to the output stream.
	 * <p> N.B. The bytes written must be sufficient for the
	 * purposes of reading the object back in.
	 * @param rootNode whether the node being written is the root of the tree
	 * @param data the object to write, which may be <code>null</code>.
	 */
	void writeData(boolean rootNode, Object data, DataOutput output) throws IOException;
}
