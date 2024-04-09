/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.EditorsUI;

public class TextEditorPropertyAction extends Action implements IPropertyChangeListener {

	private final MergeSourceViewer[] viewers;
	private final String preferenceKey;
	private final IPreferenceStore store;

	public TextEditorPropertyAction(String label, MergeSourceViewer[] viewers, String preferenceKey) {
		super(label, IAction.AS_CHECK_BOX);
		this.viewers = viewers;
		this.preferenceKey = preferenceKey;
		this.store = EditorsUI.getPreferenceStore();
		if (store != null)
			store.addPropertyChangeListener(this);
		synchronizeWithPreference();
		addActionToViewers();
	}

	private void addActionToViewers() {
		for (MergeSourceViewer viewer : viewers) {
			viewer.addTextAction(this);
		}
	}

	public MergeSourceViewer[] getViewers() {
		return viewers;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(getPreferenceKey())) {
			synchronizeWithPreference();
		}
	}

	protected void synchronizeWithPreference() {
		boolean checked = false;
		if (store != null) {
			checked = store.getBoolean(getPreferenceKey());
		}
		if (checked != isChecked()) {
			if (toggleState(checked))
				setChecked(checked);
		}
	}

	public String getPreferenceKey() {
		return preferenceKey;
	}

	@Override
	public void run() {
		toggleState(isChecked());
		if (store != null)
			store.setValue(getPreferenceKey(), isChecked());
	}

	public void dispose() {
		if (viewers != null) {
			for (int i = 0; i < viewers.length; i++) {
				viewers[i] = null;
			}
		}
		if (store != null) {
			store.removePropertyChangeListener(this);
		}
	}

	/**
	 * @param checked
	 *            new state
	 * @return <code>true</code> if state has been changed, toggle has been
	 *         successful
	 */
	protected boolean toggleState(boolean checked) {
		// No-op by default
		return false;
	}

}
