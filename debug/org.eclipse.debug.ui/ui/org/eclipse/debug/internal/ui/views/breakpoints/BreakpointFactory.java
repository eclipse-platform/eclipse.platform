/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

/**
 * Factory to restore breakpoints from mementos for breakpoint working sets.
 */
public class BreakpointFactory implements IElementFactory {

	@Override
	public IAdaptable createElement(IMemento memento) {
		String longString = memento.getString(BreakpointPersistableElementAdapter.TAG_MARKER_ID);
		String factoryId = memento.getString(BreakpointPersistableElementAdapter.TAG_RESOURCE_FACTORY_ID);
		if (factoryId != null && longString != null) {
			IElementFactory elementFactory = PlatformUI.getWorkbench().getElementFactory(factoryId);
			if (elementFactory != null) {
				IAdaptable adaptable = elementFactory.createElement(memento);
				if (adaptable instanceof IResource) {
					IResource resource = (IResource) adaptable;
					try {
						long id = Long.parseLong(longString);
						IMarker marker = resource.findMarker(id);
						if (marker != null) {
							return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
						}
					} catch (NumberFormatException e) {
					} catch (CoreException e) {
					}
				}
			}
		}
		return null;
	}

}
