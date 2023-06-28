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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Default implementation for a breakpoint type category.
 * <p>
 * Clients providing breakpoint type category adapters may instantiate
 * and subclass this class.
 * </p>
 * @since 3.1
 */
public class BreakpointTypeCategory extends PlatformObject implements IBreakpointTypeCategory, IWorkbenchAdapter {

	private String fName;
	private ImageDescriptor fImageDescriptor = DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJS_BREAKPOINT_TYPE);

	/**
	 * Constructs a type category for the given type name.
	 *
	 * @param name breakpoint type name
	 */
	public BreakpointTypeCategory(String name) {
		fName = name;
	}

	/**
	 * Constructs a type category for the given type name with the given
	 * image.
	 *
	 * @param name breakpoint type name
	 * @param descriptor image descriptor
	 */
	public BreakpointTypeCategory(String name, ImageDescriptor descriptor) {
		fName = name;
		if (descriptor != null) {
			fImageDescriptor = descriptor;
		}
	}

	/**
	 * Returns the name of this category's breakpoint type.
	 *
	 * @return the name of this category's breakpoint type
	 */
	protected String getName() {
		return fName;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof BreakpointTypeCategory) {
			BreakpointTypeCategory type = (BreakpointTypeCategory) object;
			return type.getName().equals(getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public Object[] getChildren(Object o) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return fImageDescriptor;
	}

	@Override
	public String getLabel(Object o) {
		return getName();
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public String toString() {
		return fName;
	}
}
