/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.provisional;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * Provides content for elements in an asynchronous viewer. Note that implementations
 * must provide content asynchronously.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 */
public interface IAsynchronousContentAdapter {

	/**
	 * Asynchronously retrieves the children of the given parent reporting to the
	 * given monitor. If unable to retrieve children, an exception should be reported
	 * to the monitor with an appropriate status.
	 *
	 * @param parent the element to retrieve children for
	 * @param context the context in which children have been requested
	 * @param monitor request monitor to report children to
	 */
	void retrieveChildren(Object parent, IPresentationContext context, IChildrenRequestMonitor result);

	/**
	 * Asynchronously determines whether the given element contains children in the specified
	 * context reporting the result to the given monitor. If unable to determine
	 * whether the element has children, an exception should be reported to the monitor
	 * with an appropriate status.
	 *
	 * @param element the element on which children may exist
	 * @param context the context in which children may exist
	 * @param monitor request monitor to report the result to
	 */
	void isContainer(Object element, IPresentationContext context, IContainerRequestMonitor result);

}
