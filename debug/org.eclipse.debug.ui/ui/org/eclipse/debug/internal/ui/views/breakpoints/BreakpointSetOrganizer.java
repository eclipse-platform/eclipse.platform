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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.importexport.breakpoints.IImportExportConstants;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.IBreakpointOrganizerDelegateExtension;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Breakpoint organizers for breakpoint working sets.
 *
 * @since 3.1
 */
public class BreakpointSetOrganizer extends AbstractBreakpointOrganizerDelegate implements IBreakpointOrganizerDelegateExtension, IPropertyChangeListener, IBreakpointsListener {

	private IWorkingSetManager fWorkingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();

	/**
	 * A cache for mapping markers to the working set they belong to
	 * @since 3.2
	 */
	private BreakpointWorkingSetCache fCache = null;

	// Cache of the default working set, so we can know when it changes name
	private static IWorkingSet fDefaultWorkingSet = null;


	/**
	 * Constructs a working set breakpoint organizer. Listens for changes in
	 * working sets and fires property change notification.
	 */
	public BreakpointSetOrganizer() {
		fWorkingSetManager.addPropertyChangeListener(this);
		fCache = new BreakpointWorkingSetCache();
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		fDefaultWorkingSet = getDefaultWorkingSet();
	}

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		List<IAdaptable> result = new ArrayList<>();
		IWorkingSet[] workingSets = fWorkingSetManager.getWorkingSets();
		for (IWorkingSet set : workingSets) {
			if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId())) {
				IAdaptable[] elements = set.getElements();
				for (IAdaptable adaptable : elements) {
					if (adaptable.equals(breakpoint)) {
						result.add(new WorkingSetCategory(set));
						break;
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
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(IInternalDebugUIConstants.MEMENTO_BREAKPOINT_WORKING_SET_NAME)) {
			IWorkingSet defaultWorkingSet = getDefaultWorkingSet();
			if (defaultWorkingSet != null) {
				fireCategoryChanged(new WorkingSetCategory(defaultWorkingSet));
			} else {
				fireCategoryChanged(null);
			}
		}

		IWorkingSet set = null;
		Object newValue = event.getNewValue();
		if (newValue instanceof IWorkingSet) {
			set = (IWorkingSet) newValue;
		}
		else if (event.getOldValue() instanceof IWorkingSet) {
			set = (IWorkingSet) event.getOldValue();
		}
		if(set == null) {
			return;
		}
		//fix for bug 103731
		if (property.equals(IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE)) {
			if (newValue.equals(fDefaultWorkingSet)) {
				setDefaultWorkingSet((IWorkingSet) newValue);
			}
		}
		if (property.equals(IWorkingSetManager.CHANGE_WORKING_SET_REMOVE)) {
			if (event.getOldValue().equals(fDefaultWorkingSet)) {
				setDefaultWorkingSet(null);
			}
		}
		if(property.equals(IWorkingSetManager.CHANGE_WORKING_SET_ADD)) {
			IAdaptable[] breakpoints = set.getElements();
			for (IAdaptable breakpoint : breakpoints) {
				if (breakpoint instanceof IBreakpoint) {
					IMarker marker = ((IBreakpoint) breakpoint).getMarker();
					fCache.addEntry(marker, set.getName());
					fCache.flushMarkerCache(marker);
				}
			}
		}
		if (set != null	&& IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId())) {
			fireCategoryChanged(new WorkingSetCategory(set));
		}
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		Map<IWorkingSet, List<IBreakpoint>> setToBreakpoints = new HashMap<>();
		for (IBreakpoint breakpoint : breakpoints) {
			IMarker marker = breakpoint.getMarker();
			String[] names = getWorkingsetAttributeFromMarker(marker, IInternalDebugUIConstants.WORKING_SET_NAME);
			//add it to the default set if the listing is empty
			if (names.length == 0) {
				queueToSet(breakpoint, getDefaultWorkingSet(), setToBreakpoints);
			} else {
				for (int j = 1; j < names.length; j++) {
					IWorkingSet set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(names[j]);
					// if we cannot find the one we want, try to get the default
					if (set == null) {
						set = getDefaultWorkingSet();
					}
					queueToSet(breakpoint, set, setToBreakpoints);
				}
			}
		}
		for (Entry<IWorkingSet, List<IBreakpoint>> entry : setToBreakpoints.entrySet()) {
			IWorkingSet set = entry.getKey();
			List<IBreakpoint> list = entry.getValue();
			addBreakpointsToSet(list.toArray(new IBreakpoint[list.size()]), set);
		}
	}

	private void queueToSet(IBreakpoint breakpoint, IWorkingSet set, Map<IWorkingSet, List<IBreakpoint>> queue) {
		List<IBreakpoint> list = queue.get(set);
		if (list == null) {
			list = new ArrayList<>();
			queue.put(set, list);
		}
		list.add(breakpoint);
	}

	/**
	 * Adds a breakpoint to a working set
	 * @param breakpoints the breakpoints to add
	 * @param set the set to add it to or <code>null</code> if none
	 *
	 * @since 3.2
	 */
	private void addBreakpointsToSet(IBreakpoint[] breakpoints, IWorkingSet set) {
		if (set != null) {
			IAdaptable[] elements = set.getElements();
			Set<IAdaptable> collection = new HashSet<>(elements.length);
			List<IAdaptable> list = new ArrayList<>(elements.length + breakpoints.length);
			for (IAdaptable element : elements) {
				collection.add(element);
				list.add(element);
			}
			for (IBreakpoint breakpoint : breakpoints) {
				if (!collection.contains(breakpoint)) {
					list.add(breakpoint);
					fCache.addEntry(breakpoint.getMarker(), set.getName()); //fix for bug 103731
					fCache.flushMarkerCache(breakpoint.getMarker());
				}
			}
			set.setElements(list.toArray(new IAdaptable[list.size()]));
		}
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints,
			IMarkerDelta[] deltas) {
		IWorkingSet[] workingSets = fWorkingSetManager.getWorkingSets();
		IWorkingSet set = null;
		for (IWorkingSet workingSet : workingSets) {
			set = workingSet;
			if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId())) {
				clean(set);
			}
		}
	}

	/**
	 * Removes deleted breakpoints from the given working set.
	 *
	 * @param workingSet
	 *            breakpoint working set
	 */
	private void clean(IWorkingSet workingSet) {
		IAdaptable[] elements = workingSet.getElements();
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		boolean update = false;
		for (int i = 0; i < elements.length; i++) {
			IAdaptable adaptable = elements[i];
			if (adaptable instanceof IBreakpoint) {
				IBreakpoint breakpoint = (IBreakpoint) adaptable;
				if (!manager.isRegistered(breakpoint)) {
					update = true;
					elements[i] = null;
				}
			}
		}
		if (update) {
			List<IAdaptable> newElements = new ArrayList<>(elements.length);
			for (IAdaptable adaptable : elements) {
				if (adaptable != null) {
					newElements.add(adaptable);
				}
			}
			workingSet.setElements(newElements.toArray(new IAdaptable[newElements.size()]));
		}
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
	}

	/**
	 * Returns the active default breakpoint working set, or <code>null</code>
	 * if none.
	 *
	 * @return the active default breakpoint working set, or <code>null</code>
	 */
	public static IWorkingSet getDefaultWorkingSet() {
		IPreferenceStore preferenceStore = DebugUIPlugin.getDefault().getPreferenceStore();
		String name = preferenceStore.getString(IInternalDebugUIConstants.MEMENTO_BREAKPOINT_WORKING_SET_NAME);
		if (name != null) {
			return PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
		}
		return null;
	}

	/**
	 * Sets the active default breakpoint working set, or <code>null</code> if
	 * none.
	 *
	 * @param set
	 *            default working set or <code>null</code>
	 */
	public static void setDefaultWorkingSet(IWorkingSet set) {
		String name = IInternalDebugCoreConstants.EMPTY_STRING;
		if (set != null) {
			// only consider breakpoint working sets
			if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId())) {
				name = set.getName();
			}
		}
		fDefaultWorkingSet = set;
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if(node != null) {
			try {
				node.put(IInternalDebugUIConstants.MEMENTO_BREAKPOINT_WORKING_SET_NAME, name);
				node.flush();
			} catch (BackingStoreException e) {
				DebugUIPlugin.log(e);
			}
		}
	}

	@Override
	public boolean canRemove(IBreakpoint breakpoint, IAdaptable category) {
		if (category instanceof WorkingSetCategory) {
			WorkingSetCategory wsc = (WorkingSetCategory) category;
			return IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(wsc.getWorkingSet().getId());
		}
		return super.canRemove(breakpoint, category);
	}

	@Override
	public boolean canAdd(IBreakpoint breakpoint, IAdaptable category) {
		if (category instanceof WorkingSetCategory) {
			WorkingSetCategory wsc = (WorkingSetCategory) category;
			return IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(wsc.getWorkingSet().getId());
		}
		return super.canAdd(breakpoint, category);
	}

	@Override
	public void addBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		addBreakpoints(new IBreakpoint[]{breakpoint}, category);
	}

	/**
	 * Gets the working set names from the marker
	 *
	 * @param marker them marker to get the names from
	 * @param type the type attribute to look up
	 * @return the listing of markers or an empty String array, never <code>null</code>
	 *
	 * @since 3.2
	 */
	private String[] getWorkingsetAttributeFromMarker(IMarker marker, String type) {
		try {
			String name = (String) marker.getAttribute(type);
			if (name != null) {
				return name.split("\\" + IImportExportConstants.DELIMITER); //$NON-NLS-1$
			}
		}
		catch (CoreException e) {DebugPlugin.log(e);}
		return new String[] {};
	}

	@Override
	public void removeBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		if (category instanceof WorkingSetCategory) {
			IWorkingSet set = ((WorkingSetCategory) category).getWorkingSet();
			IAdaptable[] elements = set.getElements();
			List<IAdaptable> list = new ArrayList<>();
			for (IAdaptable adaptable : elements) {
				if (!adaptable.equals(breakpoint)) {
					list.add(adaptable);
				}
			}
			fCache.removeMappedEntry(breakpoint.getMarker(), set.getName());
			fCache.flushMarkerCache(breakpoint.getMarker());
			set.setElements(list.toArray(new IAdaptable[list.size()]));
		}
	}

	@Override
	public IAdaptable[] getCategories() {
		IWorkingSet[] workingSets = fWorkingSetManager.getWorkingSets();
		List<IAdaptable> all = new ArrayList<>();
		for (IWorkingSet set : workingSets) {
			if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set
					.getId())) {
				all.add(new WorkingSetCategory(set));
			}
		}
		return all.toArray(new IAdaptable[all.size()]);
	}

	@Override
	public void addBreakpoints(IBreakpoint[] breakpoints, IAdaptable category) {
		if (category instanceof WorkingSetCategory) {
			IWorkingSet set = ((WorkingSetCategory) category).getWorkingSet();
			addBreakpointsToSet(breakpoints, set);
		}
	}

	@Override
	public void removeBreakpoints(IBreakpoint[] breakpoints, IAdaptable category) {
		if (category instanceof WorkingSetCategory) {
			IWorkingSet set = ((WorkingSetCategory) category).getWorkingSet();
			IAdaptable[] elements = set.getElements();
			List<IAdaptable> list = new ArrayList<>(elements.length);
			Collections.addAll(list, elements);
			for (IBreakpoint breakpoint : breakpoints) {
				fCache.removeMappedEntry(breakpoint.getMarker(), set.getName());
				fCache.flushMarkerCache(breakpoint.getMarker());
				list.remove(breakpoint);
			}
			set.setElements(list.toArray(new IAdaptable[list.size()]));
		}
	}
}
