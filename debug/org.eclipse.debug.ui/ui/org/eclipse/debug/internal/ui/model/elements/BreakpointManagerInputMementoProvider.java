/*****************************************************************
 * Copyright (c) 2009 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *****************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Breakpoint manager input memento provider.
 *
 * @since 3.6
 */
public class BreakpointManagerInputMementoProvider extends DebugElementMementoProvider {

	@Override
	protected String getElementName(Object element, IPresentationContext context) throws CoreException {
		return  "BreakpointInputMemento"; //$NON-NLS-1$
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(id);
	}
}
