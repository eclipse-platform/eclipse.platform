/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates context specific cell modifiers and editors for elements.
 *
 * @since 3.3
 */
public interface IElementEditor {

	/**
	 * Returns a cell editor to use for the specified column and object or <code>null</code>
	 * if none.
	 *
	 * @param context presentation context
	 * @param columnId column id
	 * @param element object to be edited
	 * @param parent parent control to create the cell editor in
	 * @return cell editor or <code>null</code>
	 */
	CellEditor getCellEditor(IPresentationContext context, String columnId, Object element, Composite parent);

	/**
	 * Returns a cell modifier for the specified element in the given context
	 * or <code>null</code> if none.
	 * @param context Presentation context
	 * @param element Model element.
	 *
	 * @return cell modifier or <code>null</code>
	 */
	ICellModifier getCellModifier(IPresentationContext context, Object element);

}
