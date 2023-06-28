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
package org.eclipse.debug.ui;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Represents a breakpoint's type to support organization of breakpoints
 * by type in the breakpoints view. A default breakpoint type adapter
 * is provided for breakpoints by the debug platform, but clients may override
 * the default adapter by providing their own adapter for breakpoints.
 * <p>
 * A breakpoint type category is an adaptable that must provide an
 * <code>IWorkbenchAdapter</code> such that it can be rendered in the
 * breakpoints view.
 * </p>
 * <p>
 * Implementations should ensure that <code>equals</code> and <code>hashCode</code>
 * are implemented properly to reflect the equality of type categories.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see org.eclipse.debug.ui.BreakpointTypeCategory
 * @since 3.1
 */
public interface IBreakpointTypeCategory extends IAdaptable {

}
