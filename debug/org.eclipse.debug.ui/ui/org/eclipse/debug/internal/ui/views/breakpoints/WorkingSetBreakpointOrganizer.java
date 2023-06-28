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
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Breakpoint organizers for resource working sets.
 *
 * @since 3.1
 */
public class WorkingSetBreakpointOrganizer extends AbstractBreakpointOrganizerDelegate implements IPropertyChangeListener {

	IWorkingSetManager fWorkingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();

	/**
	 * Constructs a working set breakpoint organizer. Listens for changes in
	 * working sets and fires property change notification.
	 */
	public WorkingSetBreakpointOrganizer() {
		fWorkingSetManager.addPropertyChangeListener(this);
	}

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		List<IAdaptable> result = new ArrayList<>();
		List<IResource> parents = new ArrayList<>();
		IResource res = breakpoint.getMarker().getResource();
		parents.add(res);
		while (res != null) {
			res = res.getParent();
			if (res != null) {
				parents.add(res);
			}
		}
		for (IWorkingSet workingSet : fWorkingSetManager.getWorkingSets()) {
			if (!IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(workingSet.getId())) {
				for (IAdaptable element : workingSet.getElements()) {
					IResource resource = element.getAdapter(IResource.class);
					if (resource != null) {
						if (parents.contains(resource)) {
							result.add(new WorkingSetCategory(workingSet));
							break;
						}
					}
				}
			}
		}
		return result.toArray(new IAdaptable[result.size()]);
	}

	@Override
	public void dispose() {
		fWorkingSetManager.removePropertyChangeListener(this);
		fWorkingSetManager = null;
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		IWorkingSet set = null;
		if (event.getNewValue() instanceof IWorkingSet) {
			set = (IWorkingSet) event.getNewValue();
		} else if (event.getOldValue() instanceof IWorkingSet) {
			set = (IWorkingSet) event.getOldValue();
		}
		if (set != null && !IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId())) {
			fireCategoryChanged(new WorkingSetCategory(set));
		}
	}
}
