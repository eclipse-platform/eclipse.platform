/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.ui.IWorkbenchPage;

/**
 * Displays source for a debug model element. The debug platform displays
 * source whenever a debug context containing a single element is
 * activated (a structured selection with one element). The debug platform
 * displays source by asking an element for its <code>ISourceDisplay</code>
 * adapter or using the element directly if it implements <code>ISourceDisplay</code>.
 * <p>
 * The debug platform provides a source display adapter for instances
 * of <code>IStackFrame</code>. The standard adapter uses the source locator associated
 * with the stack frame's launch to lookup source. Clients may provide their own
 * source display adapters as required.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see org.eclipse.core.runtime.IAdaptable
 * @since 3.3
 */
public interface ISourceDisplay {

	/**
	 * Displays source for the given element in the specified page.
	 *
	 * @param element debug model element to display source for
	 * @param page the page in which to display source
	 * @param forceSourceLookup whether source lookup should be performed,
	 *  ignoring any previously cached results for the same element
	 */
	void displaySource(Object element, IWorkbenchPage page, boolean forceSourceLookup);

}
