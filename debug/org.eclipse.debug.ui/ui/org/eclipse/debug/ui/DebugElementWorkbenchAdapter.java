/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.views.launch.DebugElementHelper;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * Common function for debug element workbench adapters.
 * <p>
 * Clients may subclass this class to provide custom adapters for elements in a debug
 * model. The debug platform provides <code>IWorkbenchAdapters</code> for the standard debug
 * elements. Clients may override the default content in the debug view by providing an
 * <code>IWorkbenchAdapter</code> or <code>IDeferredWorkbenchAdapter</code> for a debug
 * element.
 * </p>
 * @since 3.1
 * @deprecated Custom content in the debug views is no longer supported by
 * 	 {@link IWorkbenchAdapter}. Custom content is currently supported
 * 	 by a provisional internal viewer framework.
 */
@Deprecated
public abstract class DebugElementWorkbenchAdapter implements IWorkbenchAdapter, IWorkbenchAdapter2 {

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return DebugElementHelper.getImageDescriptor(object);
	}

	@Override
	public String getLabel(Object o) {
		return DebugElementHelper.getLabel(o);
	}

	@Override
	public RGB getForeground(Object element) {
		return DebugElementHelper.getForeground(element);
	}

	@Override
	public RGB getBackground(Object element) {
		return DebugElementHelper.getBackground(element);
	}

	@Override
	public FontData getFont(Object element) {
		return DebugElementHelper.getFont(element);
	}

}
