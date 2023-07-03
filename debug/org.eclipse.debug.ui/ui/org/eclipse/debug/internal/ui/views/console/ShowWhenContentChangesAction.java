/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.console;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Abstract action for toggling preference to automatically show
 * the console when a streams content changes.
 *
 * @since 3.3
 */
public abstract class ShowWhenContentChangesAction extends Action implements IPropertyChangeListener{

	/**
	 * Constructs an action to toggle console auto activation preferences
	 */
	public ShowWhenContentChangesAction(String name) {
		super(name, IAction.AS_CHECK_BOX);
		setToolTipText(name);
		getPreferenceStore().addPropertyChangeListener(this);
		update();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(getKey())) {
			update();
		}
	}

	protected abstract String getKey();

	private void update() {
		IPreferenceStore store = getPreferenceStore();
		if (store.getBoolean(getKey())) {
			// on
			setChecked(true);
		} else {
			// off
			setChecked(false);
		}
	}

	private IPreferenceStore getPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void run() {
		IPreferenceStore store = getPreferenceStore();
		boolean show = isChecked();
		store.removePropertyChangeListener(this);
		store.setValue(getKey(), show);
		store.addPropertyChangeListener(this);
	}

	/**
	 * Must be called to dispose this action.
	 */
	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);
	}

}
