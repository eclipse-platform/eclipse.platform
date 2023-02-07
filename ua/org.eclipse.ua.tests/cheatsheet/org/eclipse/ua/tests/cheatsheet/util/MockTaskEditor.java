/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.util;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;

// Task Editor used in the persistence tests

public class MockTaskEditor extends TaskEditor {

	public static final String NO_MEMENTO = "No Memento";

	private String value;

	private static final String KEY = "key";

	@Override
	public Control getControl() {
		// Not used by tests
		return null;
	}

	@Override
	public void setInput(IEditableTask task, IMemento memento) {
		if (memento == null) {
			setValue(NO_MEMENTO);
		} else {
			setValue(memento.getString(KEY));
		}
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(KEY, getValue());
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}


}
