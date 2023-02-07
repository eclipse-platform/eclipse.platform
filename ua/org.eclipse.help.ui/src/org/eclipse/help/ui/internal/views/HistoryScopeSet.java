/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import org.eclipse.jface.preference.IPreferenceStore;

public class HistoryScopeSet extends ScopeSet {
	private static final String KEY_EXPRESSION = "expression"; //$NON-NLS-1$
	public static final String EXT = ".hist"; //$NON-NLS-1$

	public HistoryScopeSet(String expression) {
		this(expression, expression);
	}

	public HistoryScopeSet(String name, String expression) {
		super(name);
		if (expression!=null)
			setExpression(expression);
	}

	public HistoryScopeSet(HistoryScopeSet set) {
		super(set, set.getName());
		setExpression(set.getExpression());
	}

	@Override
	public void copyFrom(ScopeSet set) {
		String expression = getExpression();
		super.copyFrom(set);
		setExpression(expression);
	}

	public String getExpression() {
		IPreferenceStore store = getPreferenceStore();
		return store.getString(KEY_EXPRESSION);
	}

	@Override
	public boolean isImplicit() {
		return true;
	}

	@Override
	protected String getExtension() {
		return EXT;
	}

	@Override
	protected String encodeFileName(String name) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);

			if (c == '_' || Character.isLetterOrDigit(c)) {
				buf.append(c);
			} else {
				buf.append('_');
				buf.append((int) c);
				buf.append('_');
			}
		}
		return buf.toString();
	}

	public void setExpression(String expression) {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(KEY_EXPRESSION, expression);
	}
}
