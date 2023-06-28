/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
 *     Wind Rvier Systems - added support for columns (bug 235646)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;

/**
 * Default content provider for the expression manager.
 */
public class ExpressionManagerContentProvider extends ElementContentProvider {

	/**
	 * An element representing the "Add new expression" entry in the
	 * expressions view.
	 *
	 * @since 3.6
	 */
	private static class AddNewExpressionElement implements IElementLabelProvider, IElementEditor, ICellModifier {

		@Override
		public void update(ILabelUpdate[] updates) {
			for (ILabelUpdate update : updates) {
				String[] columnIds = update.getColumnIds();
				if (columnIds == null) {
					updateLabel(update, 0);
				} else {
					for (int j = 0; j < columnIds.length; j++) {
						if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnIds[j])) {
							updateLabel(update, j);
						} else {
							update.setLabel(IInternalDebugCoreConstants.EMPTY_STRING, j);
						}
					}
				}

				update.done();
			}
		}

		@SuppressWarnings("deprecation")
		private void updateLabel(ILabelUpdate update, int columnIndex) {
			update.setLabel(DebugUIMessages.ExpressionManagerContentProvider_1, columnIndex);
			update.setImageDescriptor(DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_MONITOR_EXPRESSION), columnIndex);

			// Display the "Add new expression" element in italic to
			// distinguish it from user elements in view.
			FontData fontData = JFaceResources.getFontDescriptor(IDebugUIConstants.PREF_VARIABLE_TEXT_FONT).getFontData()[0];
			fontData.setStyle(SWT.ITALIC);
			update.setFontData(fontData, columnIndex);
		}

		@Override
		public CellEditor getCellEditor(IPresentationContext context, String columnId, Object element, Composite parent) {
			return new TextCellEditor(parent);
		}

		@Override
		public ICellModifier getCellModifier(IPresentationContext context, Object element) {
			return this;
		}

		@Override
		public boolean canModify(Object element, String property) {
			return (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(property));
		}

		@Override
		public Object getValue(Object element, String property) {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			// If an expression is entered, add a new watch expression to the
			// manager.
			if (value instanceof String &&
				!IInternalDebugCoreConstants.EMPTY_STRING.equals( ((String)value).trim()) )
			{
				String expressionText = DefaultLabelProvider.encodeEsacpedChars((String)value);
				IWatchExpression newExpression=
					DebugPlugin.getDefault().getExpressionManager().newWatchExpression(expressionText);
				DebugPlugin.getDefault().getExpressionManager().addExpression(newExpression);
				newExpression.setExpressionContext(getContext());
			}
		}

		private IDebugElement getContext() {
			IAdaptable object = DebugUITools.getDebugContext();
			IDebugElement context = null;
			if (object instanceof IDebugElement) {
				context = (IDebugElement) object;
			} else if (object instanceof ILaunch) {
				context = ((ILaunch) object).getDebugTarget();
			}
			return context;
		}

	}

	private static final AddNewExpressionElement ADD_NEW_EXPRESSION_ELEMENT = new AddNewExpressionElement();

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		// Add the "Add new expression" element only if columns are displayed.
		return ((IExpressionManager) element).getExpressions().length +
			(context.getColumns() != null ? 1 : 0);
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (context.getColumns() != null) {
			return getElements(((IExpressionManager) parent).getExpressions(), ADD_NEW_EXPRESSION_ELEMENT, index, length);
		} else {
			return getElements(((IExpressionManager) parent).getExpressions(), index, length);
		}
	}

	/**
	 * Returns a subrange of elements from the given elements array plus the last element.
	 *
	 * @see ElementContentProvider#getElements(Object[], int, int)
	 *
	 * @since 3.6
	 */
	private Object[] getElements(Object[] elements, Object lastElement, int index, int length) {

		int max = elements.length + 1;
		if (index < max && ((index + length) > max)) {
			length = max - index;
		}
		if ((index + length) <= max) {
			Object[] sub = new Object[length];
			System.arraycopy(elements, index, sub, 0, Math.min(elements.length - index, length));
			if (index + length > elements.length) {
				sub[length - 1] = lastElement;
			}
			return sub;
		}
		return null;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return id.equals(IDebugUIConstants.ID_EXPRESSION_VIEW);
	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return true;
	}

}
