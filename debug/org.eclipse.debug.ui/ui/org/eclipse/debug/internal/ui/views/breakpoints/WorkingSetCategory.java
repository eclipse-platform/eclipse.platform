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
package org.eclipse.debug.internal.ui.views.breakpoints;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * Represents a breakpoint category for a specific working set.
 */
public class WorkingSetCategory extends PlatformObject implements IWorkbenchAdapter, IWorkbenchAdapter2 {

	private IWorkingSet fWorkingSet;

	/**
	 * Constructs a new workings set category for the given working set.
	 *
	 * @param workingSet
	 */
	public WorkingSetCategory(IWorkingSet workingSet) {
		fWorkingSet = workingSet;
	}

	@Override
	public Object[] getChildren(Object o) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return fWorkingSet.getImageDescriptor();
	}

	@Override
	public String getLabel(Object o) {
		StringBuilder name = new StringBuilder(fWorkingSet.getName());
		if (isDefault()) {
			name.append(DebugUIViewsMessages.WorkingSetCategory_0);
		}
		return name.toString();
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	/**
	 * Returns the working set for this category.
	 *
	 * @return
	 */
	public IWorkingSet getWorkingSet() {
		return fWorkingSet;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WorkingSetCategory) {
			WorkingSetCategory category = (WorkingSetCategory) obj;
			return category.getWorkingSet().equals(fWorkingSet);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fWorkingSet.hashCode();
	}

	@Override
	public RGB getForeground(Object element) {
		return null;
	}

	@Override
	public RGB getBackground(Object element) {
		return null;
	}

	@Override
	public FontData getFont(Object element) {
		if (isDefault()) {
			FontData[] fontData = JFaceResources.getDefaultFont().getFontData();
			if (fontData != null && fontData.length > 0) {
				FontData data = fontData[0];
				data.setStyle(SWT.BOLD);
				return data;
			}
		}
		return null;
	}

	/**
	 * Whether this is the default breakpoint working set.
	 *
	 * @return whether this is the default breakpoint working set
	 */
	private boolean isDefault() {
		return fWorkingSet.equals(BreakpointSetOrganizer.getDefaultWorkingSet());
	}

	@Override
	public String toString() {
		return fWorkingSet.getName();
	}
}

