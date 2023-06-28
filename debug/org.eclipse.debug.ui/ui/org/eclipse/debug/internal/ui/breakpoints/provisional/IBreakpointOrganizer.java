/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.breakpoints.provisional;

import org.eclipse.debug.ui.IBreakpointOrganizerDelegate;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A breakpoint organizer is represents a breakpoint organizer delegate in
 * the breakpoint view.  Clients can retrieve the current active breakpoint
 * organizers from the breakpoint view's presentation context using the
 * {@link IBreakpointUIConstants#PROP_BREAKPOINTS_ORGANIZERS} property.
 * <p>
 * This interface is not intended to be implemented. Clients contributing a breakpoint
 * organizer are intended to implement <code>IBreakpointOrganizerDelegate</code>.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.6 (internal interface since 3.1)
 *
 * @see IBreakpointUIConstants#PROP_BREAKPOINTS_ORGANIZERS
 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext
 */
public interface IBreakpointOrganizer extends IBreakpointOrganizerDelegate {

	/**
	 * Returns a label for this breakpoint organizer.
	 *
	 * @return a label for this breakpoint organizer
	 */
	String getLabel();

	/**
	 * Returns an image descriptor for this breakpoint organizer or <code>null</code>.
	 *
	 * @return an image descriptor for this breakpoint organizer or <code>null</code>
	 */
	ImageDescriptor getImageDescriptor();

	/**
	 * Returns a unique identifier for this breakpoint organizer.
	 *
	 * @return a unique identifier for this breakpoint organizer
	 */
	String getIdentifier();

	/**
	 * Returns the label for breakpoints that do not fall into a category
	 * for this organizer.
	 *
	 * @return label for breakpoints that do not fall into a category
	 * for this organizer
	 */
	String getOthersLabel();

}
