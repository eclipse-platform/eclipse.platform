/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables;


import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 *
 */
public abstract class VariableViewToggleAction implements IViewActionDelegate, IActionDelegate2 {

	private IViewPart fView;
	private IAction fAction;

	public VariableViewToggleAction() {
		super();
	}

	@Override
	public void init(IViewPart view) {
		fView = view;
		boolean checked = getPreferenceValue(view);
		fAction.setChecked(checked);
		run(fAction);
	}

	@Override
	public void init(IAction action) {
		fAction = action;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void run(IAction action) {
		IPreferenceStore store = getPreferenceStore();
		String key = getView().getSite().getId() + "." + getPreferenceKey(); //$NON-NLS-1$
		store.setValue(key, action.isChecked());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	protected IPreferenceStore getPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Returns the value of this filters preference (on/off) for the given
	 * view.
	 *
	 * @param part
	 * @return boolean
	 */
	protected boolean getPreferenceValue(IViewPart part) {
		String baseKey = getPreferenceKey();
		String viewKey = part.getSite().getId();
		String compositeKey = viewKey + "." + baseKey; //$NON-NLS-1$
		IPreferenceStore store = getPreferenceStore();
		boolean value = false;
		if (store.contains(compositeKey)) {
			value = store.getBoolean(compositeKey);
		} else {
			value = store.getBoolean(baseKey);
		}
		return value;
	}

	/**
	 * Returns the key for this action's preference
	 *
	 * @return String
	 */
	protected abstract String getPreferenceKey();

	protected IViewPart getView() {
		return fView;
	}

	protected StructuredViewer getStructuredViewer() {
		IDebugView view = getView().getAdapter(IDebugView.class);
		if (view != null) {
			Viewer viewer = view.getViewer();
			if (viewer instanceof StructuredViewer) {
				return (StructuredViewer)viewer;
			}
		}
		return null;
	}
}
