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
package org.eclipse.core.resources;

import org.eclipse.core.runtime.CoreException;

/**
 * This interface is implemented by objects that visit resource trees.  The fast
 * visitor is an optimized mechanism for tree traversal that creates a minimal
 * number of objects.  The visitor is provided with a callback interface,
 * instead of a resource.  Through the callback, the visitor can request
 * information about the resource being visited.
 * <p>
 * Usage:
 * </p>
 * <pre>
 * class Visitor implements IResourceProxyVisitor {
 * 	public boolean visit (IResourceProxy proxy) {
 * 		//	 your code here
 * 		return true;
 * 	}
 * }
 * ResourcesPlugin.getWorkspace().getRoot().accept(new Visitor(), IResource.NONE);
 * </pre>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see IResource#accept(IResourceVisitor)
 * @since 2.1
 */
public interface IResourceProxyVisitor {
	/**
	 * Visits the given resource.
	 *
	 * @param proxy for requesting information about the resource being visited;
	 * this object is only valid for the duration of the invocation of this
	 * method, and must not be used after this method has completed
	 * @return <code>true</code> if the resource's members should
	 *		be visited; <code>false</code> if they should be skipped
	 * @exception CoreException if the visit fails for some reason.
	 */
	boolean visit(IResourceProxy proxy) throws CoreException;
}
