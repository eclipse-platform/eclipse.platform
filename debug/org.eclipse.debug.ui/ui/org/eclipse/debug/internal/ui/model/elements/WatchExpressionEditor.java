/*******************************************************************************
 * Copyright (c) 2010, 2026 Wind River Systems and others.
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
 *     IBM Corporation - Add content assist support
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import java.util.Optional;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.ui.elements.adapters.WatchExpressionCellModifier;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IWatchExpressionCellEditorFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * @since 3.6
 */
public class WatchExpressionEditor implements IElementEditor {

	@Override
	public CellEditor getCellEditor(IPresentationContext context, String columnId, Object element, Composite parent) {
		return Optional.ofNullable(Adapters.adapt(getDebugContext(context), IWatchExpressionCellEditorFactory.class))
				.flatMap(factory -> factory.createCellEditor(parent))
				.orElseGet(() -> new TextCellEditor(parent));
	}

	@Override
	public ICellModifier getCellModifier(IPresentationContext context, Object element) {
		return new WatchExpressionCellModifier();
	}

	/**
	 * Returns the active debug context associated with the given presentation
	 * context, or {@code null} if there is none.
	 */
	private IAdaptable getDebugContext(IPresentationContext context) {
		IWorkbenchPart part = context.getPart();
		if (part != null) {
			IWorkbenchPartSite site = part.getSite();
			if (site != null) {
				return DebugUITools.getPartDebugContext(site);
			}
		}
		return DebugUITools.getDebugContext();
	}

}
