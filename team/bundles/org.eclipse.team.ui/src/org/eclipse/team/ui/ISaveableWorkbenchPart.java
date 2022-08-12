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
package org.eclipse.team.ui;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A saveable workbench part is a visual component that can be shown within
 * different contexts (e.g a dialog, workbench page).
 *
 * @see SaveablePartDialog
 * @since 3.0
 * @deprecated Clients should use a subclass of {@link CompareEditorInput}
 *      and {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}
 */
@Deprecated
public interface ISaveableWorkbenchPart extends ISaveablePart, IWorkbenchPart {

	/**
	 * The property id for <code>isDirty</code>.
	 */
	public static final int PROP_DIRTY = ISaveablePart.PROP_DIRTY;
}
