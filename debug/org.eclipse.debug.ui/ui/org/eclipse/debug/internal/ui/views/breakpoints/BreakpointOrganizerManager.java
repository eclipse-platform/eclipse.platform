/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * Manager which provides access to the breakpoint organizers
 * contributed via the org.eclipse.debug.ui.breakpointOrganizers
 * extension point.
 * <p>
 * Manages the default breakpoint working set and places newly
 * create breakpoints in to that set.
 * </p>
 * @since 3.1
 */
public class BreakpointOrganizerManager {

	private static BreakpointOrganizerManager fgManager;

	// map for lookup by id
	private Map<String, IBreakpointOrganizer> fOrganizers = new HashMap<>();
	// cached sorted list by label
	private List<IBreakpointOrganizer> fSorted = null;

	/**
	 * Returns the singleton instance of the breakpoint container
	 * factory manager.
	 * @return the singleton {@link BreakpointOrganizerManager}
	 */
	public static BreakpointOrganizerManager getDefault() {
		if (fgManager == null) {
			fgManager= new BreakpointOrganizerManager();
		}
		return fgManager;
	}

	/**
	 * Creates and initializes a new breakpoint container factory.
	 */
	private BreakpointOrganizerManager() {
		loadOrganizers();
		// force the working set organizers to initialize their listeners
		start("org.eclipse.debug.ui.workingSetOrganizer"); //$NON-NLS-1$
		start("org.eclipse.debug.ui.breakpointWorkingSetOrganizer"); //$NON-NLS-1$
	}

	/**
	 * Forces instantiation of organizer delegate.
	 *
	 * @param organizerId organizer to start
	 */
	private void start(String organizerId) {
		IBreakpointOrganizer organizer = getOrganizer(organizerId);
		IPropertyChangeListener listener = event -> {
		};
		organizer.addPropertyChangeListener(listener);
		organizer.removePropertyChangeListener(listener);
	}

	/**
	 * Loads all contributed breakpoint organizers.
	 */
	private void loadOrganizers() {
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_BREAKPOINT_ORGANIZERS);
		for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
			IBreakpointOrganizer organizer = new BreakpointOrganizerExtension(element);
			if (validateOrganizer(organizer)) {
				fOrganizers.put(organizer.getIdentifier(), organizer);
			}
		}
	}

	/**
	 * Validates the given organizer. Checks that certain required attributes
	 * are available.
	 * @param organizer the organizer to check
	 * @return whether the given organizer is valid
	 */
	protected static boolean validateOrganizer(IBreakpointOrganizer organizer) {
		String id = organizer.getIdentifier();
		String label = organizer.getLabel();
		return id != null && id.length() > 0 && label != null && label.length() > 0;
	}

	/**
	 * Returns all contributed breakpoint organizers.
	 *
	 * @return all contributed breakpoint organizers
	 */
	public IBreakpointOrganizer[] getOrganizers() {
		if (fSorted == null) {
			Collection<IBreakpointOrganizer> collection = fOrganizers.values();
			fSorted = new ArrayList<>();
			fSorted.addAll(collection);
			Collections.sort(fSorted, Comparator.comparing(IBreakpointOrganizer::getLabel));
		}
		return fSorted.toArray(new IBreakpointOrganizer[fSorted.size()]);
	}

	/**
	 * Returns the specified breakpoint organizer or <code>null</code>
	 * @param id organizer identifier
	 * @return breakpoint organizer or <code>null</code>
	 */
	public IBreakpointOrganizer getOrganizer(String id) {
		return fOrganizers.get(id);
	}

	/**
	 * Shuts down the organizer manager, disposing organizers.
	 */
	public void shutdown() {
		for (IBreakpointOrganizer organizer : getOrganizers()) {
			organizer.dispose();
		}
	}

}
