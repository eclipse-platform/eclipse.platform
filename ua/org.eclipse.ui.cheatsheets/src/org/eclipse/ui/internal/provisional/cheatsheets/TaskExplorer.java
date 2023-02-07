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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Classes that extend this class are responsible for rendering the
 * hierarchy of tasks in the composite cheat sheet. They must support task
 * selection via the selection provider, be able to accept focus, and create
 * control when asked.
 */

public abstract class TaskExplorer {
	/**
	 * @return the id of this TaskExplorer which must match the id used in the
	 *         extension point
	 */
	public abstract String getId();

	/**
	 * Create a control which will display the structure of the composite cheat
	 * sheet and allow tasks within the composite cheat sheet to be selected.
	 *
	 * @param parent
	 * @param toolkit
	 */
	public abstract void createControl(Composite parent, FormToolkit toolkit);

	/**
	 * Get the control created by a previous call to createControl
	 *
	 * @return the task explorer control
	 */
	public abstract Control getControl();

	/**
	 * Called when the explorer gains focus.
	 */
	public abstract void setFocus();

	/**
	 * Get the selection provider for this explorer. The selections returned by
	 * the selection provider should represent IGuideTasks.
	 *
	 * @return the selection provider for the task explorer
	 */
	public abstract ISelectionProvider getSelectionProvider();

	/**
	 * Sets the composite cheat sheet to be displayed. createControl will
	 * already have been called.
	 *
	 * @param compositeCheatSheet
	 */
	public abstract void setCompositeCheatSheet(ICompositeCheatSheet compositeCheatSheet);

	/**
	 * Called after this explorer is no longer in use. Any resources should be
	 * disposed of at this point.
	 */
	public abstract void dispose();

	/**
	 * Called when the state of a task changes and the representation of the
	 * task may need to be redrawn.
	 *
	 * @param task
	 */
	public abstract void taskUpdated(ICompositeCheatSheetTask task);

	/**
	 * Called to set the provided selection and optionally reveal it
	 * if the scroll bars are active and the selected tasks
	 * are partially or fully hidden.
	 *
	 * @param selection the new selection
	 * @param reveal if <code>true</code>, expose the task if hidden;
	 * otherwise, just select.
	 */
	public abstract void setSelection(ISelection selection, boolean reveal);
}
