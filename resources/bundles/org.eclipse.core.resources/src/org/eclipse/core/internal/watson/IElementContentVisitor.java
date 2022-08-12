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

/**
 * An interface for objects which can visit an element of
 * an element tree and access that element's node info.
 * @see ElementTreeIterator
 */
public interface IElementContentVisitor {
	/** Visits a node (element).
	 * <p> Note that <code>elementContents</code> is equal to<code>tree.
	 * getElement(elementPath)</code> but takes no time.
	 * @param tree the element tree being visited
	 * @param elementContents the object at the node being visited on this call
	 * @param requestor callback object for requesting the path of the object being
	 * visited.
	 * @return true if this element's children should be visited, and false
	 * otherwise.
	 */
	boolean visitElement(ElementTree tree, IPathRequestor requestor, Object elementContents);
}
