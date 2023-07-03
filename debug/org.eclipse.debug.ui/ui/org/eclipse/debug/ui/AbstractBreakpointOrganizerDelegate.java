/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.OtherBreakpointCategory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Common function for breakpoint organizer delegates.
 * <p>
 * Clients implementing <code>IBreakpointOrganizerDelegate</code> must subclass this class.
 * </p>
 * @since 3.1
 */
public abstract class AbstractBreakpointOrganizerDelegate implements IBreakpointOrganizerDelegate {

	// property change listeners
	private ListenerList<IPropertyChangeListener> fListeners = new ListenerList<>();

	@Override
	public void addBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		// do noting, not supported by default
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	/*
	 * Subclasses that override should return super.canAdd(...) when they are not
	 * able to add the breakpoint.
	 */
	@Override
	public boolean canAdd(IBreakpoint breakpoint, IAdaptable category) {
		return category instanceof OtherBreakpointCategory;
	}

	/*
	 * Subclasses that override should return super.canRemove(...) when they are not
	 * able to remove the breakpoint.
	 */
	@Override
	public boolean canRemove(IBreakpoint breakpoint, IAdaptable category) {
		return category instanceof OtherBreakpointCategory;
	}

	@Override
	public void dispose() {
		fListeners = new ListenerList<>();
	}

	@Override
	public void removeBreakpoint(IBreakpoint breakpoint, IAdaptable category) {
		// do nothing, not supported by default
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Fires a property change notification for the given category.
	 *
	 * @param category category that has changed
	 */
	protected void fireCategoryChanged(IAdaptable category) {
		if (fListeners.isEmpty()) {
			return;
		}
		final PropertyChangeEvent event = new PropertyChangeEvent(this, P_CATEGORY_CHANGED, category, null);
		for (IPropertyChangeListener iPropertyChangeListener : fListeners) {
			final IPropertyChangeListener listener = iPropertyChangeListener;
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					DebugUIPlugin.log(exception);
				}
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			};
			SafeRunner.run(runnable);
		}
	}

	@Override
	public IAdaptable[] getCategories() {
		return null;
	}
}
