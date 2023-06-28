/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.debug.internal.ui.elements.adapters.WatchExpressionCellModifier;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @since 3.6
 */
public class WatchExpressionEditor implements IElementEditor {

	@Override
	public CellEditor getCellEditor(IPresentationContext context, String columnId, Object element, Composite parent) {
		return new TextCellEditor(parent);
	}

	@Override
	public ICellModifier getCellModifier(IPresentationContext context, Object element) {
		return new WatchExpressionCellModifier();
	}

}
