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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.debug.internal.ui.elements.adapters.DefaultVariableCellModifier;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @since 3.3
 */
public class VariableEditor implements IElementEditor {

	@Override
	public CellEditor getCellEditor(IPresentationContext context, String columnId, Object element, Composite parent) {
		return new TextCellEditor(parent);
	}

	@Override
	public ICellModifier getCellModifier(IPresentationContext context, Object element) {
		return new DefaultVariableCellModifier();
	}

}
