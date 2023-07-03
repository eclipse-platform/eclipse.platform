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
 *******************************************************************************/

package org.eclipse.ui.internal.provisional.cheatsheets;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * An editor which provides the UI for a task within a composite cheat sheet.
 * A task editor is responsible for saving the state of the task whenever
 * it changes.
 *
 */

public abstract class TaskEditor {

	/**
	 * Creates the widget
	 * @param parent
	 * @param toolkit
	 */
	public abstract void createControl(Composite parent, FormToolkit toolkit);

	/**
	 * @return the Control created by a previous call to CreateControl()
	 */
	public abstract Control getControl();

	/**
	 * Starts editing the provided task. The editor is responsible
	 * for saving its state. createControl() will always be called before setInput().
	 * @param task The task associated with this editor
	 * @param memento The state of this task saved from a previous invocation.
	 * The memento will be <b>null</b> if the task has not been previously started
	 * or if it is being restarted. If the editor is being restored from a previous
	 * session the memento will contain the last saved state.
	 */
	public abstract void setInput(IEditableTask task, IMemento memento);

	/**
	 * Saves the object state within a memento.
	 *
	 * @param memento a memento to receive the object state
	 */
	public abstract void saveState(IMemento memento);

}
