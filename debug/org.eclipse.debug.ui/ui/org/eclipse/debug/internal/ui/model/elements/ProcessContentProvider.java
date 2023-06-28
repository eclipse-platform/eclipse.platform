/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Default content provider for process objects. Even though process objects
 * have no children by default, they still need a content provider to ensure
 * proper display (see bug
 *
 * @since 3.6
 */
public class ProcessContentProvider extends ElementContentProvider {

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) {
		return EMPTY;
	}

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) {
		return 0;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_DEBUG_VIEW.equals(id);
	}

}
