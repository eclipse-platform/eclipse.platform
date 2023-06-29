/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.debug.internal.ui.views.launch.LaunchView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * An action handler that delegates to an action in the debug view.
 */
public abstract class DebugActionHandler implements IHandler2 {

	private String fActionId;

	/**
	 * Constructs a new handler for the given action identifier.
	 *
	 * @param actionId action identifier
	 */
	public DebugActionHandler(String actionId) {
		fActionId = actionId;
	}

	/**
	 * Returns the delegate handler or <code>null</code> if none.
	 *
	 * @return handler or <code>null</code>
	 */
	protected IHandler2 getDelegate() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null) {
			IViewReference reference = window.getActivePage().findViewReference(IDebugUIConstants.ID_DEBUG_VIEW);
			if (reference != null) {
				IViewPart view = reference.getView(false);
				if (view instanceof LaunchView) {
					return ((LaunchView)view).getHandler(fActionId);
				}
			}
		}
		return null;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			return delegate.execute(event);
		}
		return null;
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			delegate.addHandlerListener(handlerListener);
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isEnabled() {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			return delegate.isEnabled();
		}
		return false;
	}

	@Override
	public boolean isHandled() {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			return delegate.isHandled();
		}
		return false;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			delegate.removeHandlerListener(handlerListener);
		}
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		IHandler2 delegate = getDelegate();
		if (delegate != null) {
			delegate.setEnabled(evaluationContext);
		}
	}
}
