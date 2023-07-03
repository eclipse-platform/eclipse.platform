/*******************************************************************************
 * Copyright (c) 2009, 2010 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.breakpoints.provisional;

/**
 * Constants to use with Breakpoint view.
 *
 * @since 3.6
 */
public interface IBreakpointUIConstants {
	/**
	 * Breakpoints presentation context property used to retrieve the array of
	 * breakpoint organizers.  The expected property type is
	 * <code>IBreakpointOrganizer[]</code>.  If property value is <code>null</code>,
	 * the breakpoint categories should not be shown.
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext#getProperty(String)
	 */
	String PROP_BREAKPOINTS_ORGANIZERS      = "BreakpointOrganizers";   //$NON-NLS-1$

	/**
	 * Breakpoints presentation context property used to retrieve a flag
	 * indicating whether the list of breakpoints should be filtered based
	 * on the active debug context.  The returned property value should
	 * be of type <code>java.lang.Boolean</code>.  If property value is
	 * <code>null</code>, then value should be treated the same as
	 * <code>Boolean.FALSE</code>.
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext#getProperty(String)
	 */
	String PROP_BREAKPOINTS_FILTER_SELECTION = "FilterSelection";       //$NON-NLS-1$

	/**
	 * Breakpoints presentation context property used to retrieve a flag
	 * indicating whether breakpoints view selection should be updated
	 * upon a breakpoint event in debug model.  The returned property value
	 * should be of type <code>java.lang.Boolean</code>.  If property value is
	 * <code>null</code>, then value should be treated the same as
	 * <code>Boolean.FALSE</code>.
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext#getProperty(String)
	 */
	String PROP_BREAKPOINTS_TRACK_SELECTION = "TrackSelection";         //$NON-NLS-1$

	/**
	 * Breakpoints presentation context property used to retrieve a
	 * comparator for sorting breakpoints.  The returned property value should
	 * be of type <code>java.util.Comparator</code>.  If property value is
	 * <code>null</code>, the breakpoints should not be sorted.
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext#getProperty(String)
	 */
	String PROP_BREAKPOINTS_ELEMENT_COMPARATOR = "ElementComparator";   //$NON-NLS-1$

	String PROP_BREAKPOINTS_ELEMENT_COMPARATOR_SORT = "ElementComparatorSort"; //$NON-NLS-1$

}
