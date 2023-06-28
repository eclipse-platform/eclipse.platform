/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - Fixed debug context service usage (Bug 258189)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.views.launch.DebugElementAdapterFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Performs source lookup in a window.
 *
 * @since 3.2
 */
public class SourceLookupService implements IDebugContextListener, ISourceDisplay {

	private IWorkbenchWindow fWindow;
	private IDebugContextService fDebugContextService;

	public SourceLookupService(IWorkbenchWindow window) {
		fWindow = window;
		fDebugContextService = DebugUITools.getDebugContextManager().getContextService(window);
		fDebugContextService.addDebugContextListener(this);
	}

	public void dispose() {
		fDebugContextService.removeDebugContextListener(this);
		fWindow = null;
	}

	@Override
	public synchronized void debugContextChanged(DebugContextEvent event) {
		if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
			if (isDebugViewActive(event) || canActivateDebugView()) {
				displaySource(event.getContext(), event.getDebugContextProvider().getPart(), false);
			}
		}
	}

	private boolean isDebugViewActive(DebugContextEvent event) {
		if (isDisposed()) {
			return false;
		}
		IWorkbenchPage activePage = fWindow.getActivePage();
		if (activePage != null) {
			IViewPart debugView = null;
			IWorkbenchPart part = event.getDebugContextProvider().getPart();
			if (part != null) {
				debugView = activePage.findView(part.getSite().getId());
			}
			if (debugView == null) {
				debugView = activePage.findView(IDebugUIConstants.ID_DEBUG_VIEW);
			}
			return debugView != null;
		}
		return false;
	}

	private boolean canActivateDebugView() {
		if (isDisposed()) {
			return false;
		}
		IPreferenceStore preferenceStore = DebugUIPlugin.getDefault().getPreferenceStore();
		String[] switchPreferences = {
				IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE,
				IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND,
		};
		for (String switchPreference : switchPreferences) {
			String preferenceValue = preferenceStore.getString(switchPreference);
			if (!MessageDialogWithToggle.NEVER.equals(preferenceValue)) {
				return true;
			}
		}
		boolean canActivateDebugView = preferenceStore.getBoolean(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW);
		return canActivateDebugView;
	}

	private boolean isDisposed() {
		return fWindow == null;
	}

	/**
	 * Displays source for the given selection and part, optionally forcing
	 * a source lookup.
	 *
	 * @param selection
	 * @param part
	 * @param force
	 */
	protected synchronized void displaySource(ISelection selection, IWorkbenchPart part, boolean force) {
		if (isDisposed()) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			if (structuredSelection.size() == 1) {
				Object context = (structuredSelection).getFirstElement();
				IWorkbenchPage page = null;
				if (part == null) {
					page = fWindow.getActivePage();
				} else {
					page = part.getSite().getPage();
				}
				displaySource(context, page, force);
			}
		}
	}

	@Override
	public void displaySource(Object context, IWorkbenchPage page, boolean forceSourceLookup) {
		if (context instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) context;
			ISourceDisplay adapter = adaptable.getAdapter(ISourceDisplay.class);
			if (adapter == null && !(context instanceof PlatformObject)) {
				// for objects that don't properly subclass PlatformObject to inherit default
				// adapters, just delegate to the adapter factory
				adapter = new DebugElementAdapterFactory().getAdapter(context, ISourceDisplay.class);
			}
			if (adapter != null) {
				adapter.displaySource(context, page, forceSourceLookup);
			}
		}
	}
}
