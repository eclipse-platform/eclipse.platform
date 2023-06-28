/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Common function for a column presentation.
 * <p>
 * Clients implementing <code>IColumnPresentation</code> must subclass this class.
 * </p>
 * @since 3.2
 */
public abstract class AbstractColumnPresentation implements IColumnPresentation2 {

	private IPresentationContext fContext;

	/**
	 * Empty array of strings
	 */
	protected static final String[] EMPTY = new String[0];

	@Override
	public void init(IPresentationContext context) {
		fContext = context;
	}

	@Override
	public void dispose() {
		fContext = null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(String id) {
		return null;
	}

	/**
	 * Returns the context this column presentation is installed in.
	 *
	 * @return presentation context
	 */
	protected IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	public int getInitialColumnWidth(String id, int treeWidgetWidth, String[] visibleColumnIds) {
		return -1;
	}
}
