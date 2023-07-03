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

/**
 *  A task editor with no interesting behavior.
 *  Used by parser tests
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;

public class TestTaskEditor extends TaskEditor {

	private Composite control;

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		control = new Composite(parent, SWT.NULL);
		control.setLayout(new GridLayout());
		Label label = new Label(control, SWT.NULL);
		label.setText("Task editor used by JUnit tests");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	@Override
	public Control getControl() {
		return control;
	}

	@Override
	public void setInput(IEditableTask task, IMemento memento) {
		// Do nothing
	}

	@Override
	public void saveState(IMemento memento) {
		// Do nothing
	}

}
