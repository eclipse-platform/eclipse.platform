/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

package org.eclipse.debug.core.model;

import org.eclipse.debug.core.DebugException;


/**
 * Extended capabilities for memory block retrieval. Supports the retrieval
 * of memory blocks based on an expression and context.
 *
 * @since 3.1
 */
public interface IMemoryBlockRetrievalExtension extends IMemoryBlockRetrieval {

	/**
	 * Retrieves and returns a memory block.
	 *
	 * @param expression expression to be evaluated to an address
	 * @param context context for evaluating the expression.  This is typically
	 *  a debug element.
	 * @return a memory block based on the given expression and context
	 * @throws DebugException if unable to retrieve the specified memory
	 */

	IMemoryBlockExtension getExtendedMemoryBlock(String expression, Object context) throws DebugException;
}
