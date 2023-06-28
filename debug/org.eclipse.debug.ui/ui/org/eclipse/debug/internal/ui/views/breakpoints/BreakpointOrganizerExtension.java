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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.debug.ui.IBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.IBreakpointOrganizerDelegateExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * A contributed breakpoint organizer.
 */
public class BreakpointOrganizerExtension implements IBreakpointOrganizer, IBreakpointOrganizerDelegateExtension {

	private IConfigurationElement fElement;
	private IBreakpointOrganizerDelegate fDelegate;
	private ImageDescriptor fDescriptor;

	// attributes
	public static final String ATTR_LABEL = "label"; //$NON-NLS-1$
	public static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	public static final String ATTR_ID = "id"; //$NON-NLS-1$
	public static final String ATTR_ICON = "icon"; //$NON-NLS-1$
	public static final String ATTR_OTHERS_LABEL = "othersLabel"; //$NON-NLS-1$

	public BreakpointOrganizerExtension(IConfigurationElement element) {
		fElement = element;
	}

	/**
	 * Returns the image descriptor for this organizer.
	 *
	 * @return image descriptor
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		if (fDescriptor == null) {
			fDescriptor = DebugUIPlugin.getImageDescriptor(fElement, ATTR_ICON);
			if (fDescriptor == null) {
				fDescriptor = ImageDescriptor.getMissingImageDescriptor();
			}
		}
		return fDescriptor;
	}

	/**
	 * Returns this organizer's label.
	 *
	 * @return this organizer's label
	 */
	@Override
	public String getLabel() {
		return fElement.getAttribute(ATTR_LABEL);
	}

	/**
	 * Returns this organizer's identifier.
	 *
	 * @return this organizer's identifier
	 */
	@Override
	public String getIdentifier() {
		return fElement.getAttribute(ATTR_ID);
	}

	/**
	 * Returns this organizer's delegate, instantiating it if required.
	 *
	 * @return this organizer's delegate
	 */
	protected IBreakpointOrganizerDelegate getOrganizer() {
		if (fDelegate == null) {
			try {
				fDelegate = (IBreakpointOrganizerDelegate) fElement.createExecutableExtension(ATTR_CLASS);
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		return fDelegate;
	}

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		return getOrganizer().getCategories(breakpoint);
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		getOrganizer().addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		getOrganizer().removePropertyChangeListener(listener);
	}

	@Override
	public void addBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		getOrganizer().addBreakpoint(breakpoint, category);
	}

	@Override
	public void removeBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		getOrganizer().removeBreakpoint(breakpoint, category);
	}

	@Override
	public boolean canAdd(IBreakpoint breakpoint, IAdaptable category) {
		return getOrganizer().canAdd(breakpoint, category);
	}

	@Override
	public boolean canRemove(IBreakpoint breakpoint, IAdaptable category) {
		return getOrganizer().canRemove(breakpoint, category);
	}

	@Override
	public void dispose() {
		// don't instantiate the delegate if it has not been used
		if (fDelegate != null) {
			fDelegate.dispose();
		}
	}

	@Override
	public String getOthersLabel() {
		String attribute = fElement.getAttribute(ATTR_OTHERS_LABEL);
		if (attribute == null) {
			return DebugUIViewsMessages.OtherBreakpointOrganizer_0;
		}
		return attribute;
	}

	@Override
	public IAdaptable[] getCategories() {
		return getOrganizer().getCategories();
	}

	@Override
	public void addBreakpoints(IBreakpoint[] breakpoints, IAdaptable category) {
		IBreakpointOrganizerDelegate organizer = getOrganizer();
		if (organizer instanceof IBreakpointOrganizerDelegateExtension) {
			((IBreakpointOrganizerDelegateExtension)organizer).addBreakpoints(breakpoints, category);
		} else {
			for (IBreakpoint breakpoint : breakpoints) {
				addBreakpoint(breakpoint, category);
			}
		}
	}

	@Override
	public void removeBreakpoints(IBreakpoint[] breakpoints, IAdaptable category) {
		IBreakpointOrganizerDelegate organizer = getOrganizer();
		if (organizer instanceof IBreakpointOrganizerDelegateExtension) {
			((IBreakpointOrganizerDelegateExtension)organizer).removeBreakpoints(breakpoints, category);
		} else {
			for (IBreakpoint breakpoint : breakpoints) {
				removeBreakpoint(breakpoint, category);
			}
		}

	}
}
