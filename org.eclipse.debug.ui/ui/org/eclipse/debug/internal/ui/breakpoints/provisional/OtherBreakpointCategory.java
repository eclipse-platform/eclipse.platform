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
package org.eclipse.debug.internal.ui.breakpoints.provisional;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Category for breakpoints in "other" categories.   Clients which provide
 * custom content in the Breakpoints view may instantiate this object to
 * represent elements in a breakpoint organizer that do not fall into any known
 * category.
 *
 * @since 3.6
 *
 * @see IBreakpointContainer
 * @see org.eclipse.debug.ui.IBreakpointOrganizerDelegate
 */
public class OtherBreakpointCategory extends PlatformObject implements IWorkbenchAdapter {

	private static Map<IBreakpointOrganizer, IAdaptable[]> fOthers = new HashMap<>();
	private IBreakpointOrganizer fOrganizer;


	public static IAdaptable[] getCategories(IBreakpointOrganizer organizer) {
		IAdaptable[] others = fOthers.get(organizer);
		if (others == null) {
			others = new IAdaptable[]{new OtherBreakpointCategory(organizer)};
			fOthers.put(organizer, others);
		}
		return others;
	}

	/**
	 * Constructs an 'other' category for the given organizer.
	 *
	 * @param organizer breakpoint organizer
	 */
	private OtherBreakpointCategory(IBreakpointOrganizer organizer) {
		fOrganizer = organizer;
	}

	@Override
	public Object[] getChildren(Object o) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_VIEW_BREAKPOINTS);
	}

	@Override
	public String getLabel(Object o) {
		return fOrganizer.getOthersLabel();
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OtherBreakpointCategory) {
			OtherBreakpointCategory category = (OtherBreakpointCategory) obj;
			return fOrganizer.equals(category.fOrganizer);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fOrganizer.hashCode();
	}
}
