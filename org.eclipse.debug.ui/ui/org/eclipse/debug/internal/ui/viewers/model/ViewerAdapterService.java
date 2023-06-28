/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 * 	   Wind River Systems - Pawel Piech: Bug 213244 - VariableAdapterService should also call IAdaptable.getAdapter() for adaptables that implement this method directly.
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputProvider;
import org.eclipse.debug.internal.ui.views.launch.DebugElementAdapterFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Helper class to retrieve adapters associated with viewers.
 *
 * @since 3.4
 */
public class ViewerAdapterService {


	/**
	 * Returns the content provider for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return content provider or <code>null</code>
	 */
	public static IElementContentProvider getContentProvider(Object element) {
		return (IElementContentProvider)getAdapter(element, IElementContentProvider.class);
	}

	/**
	 * Returns the label provider for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return label provider or <code>null</code>
	 */
	public static IElementLabelProvider getLabelProvider(Object element) {
		IElementLabelProvider lp = (IElementLabelProvider)getAdapter(element, IElementLabelProvider.class);
		if (lp == null && element instanceof String) {
			// there are no adapters registered for Strings
			return new DebugElementAdapterFactory().getAdapter(element, IElementLabelProvider.class);
		}
		return lp;
	}

	/**
	 * Returns the column presentation factory for the given element or <code>null</code>.
	 *
	 * @param element element to retrieve adapter for
	 * @return column presentation factory of <code>null</code>
	 */
	public static IColumnPresentationFactory getColumnPresentationFactory(Object element) {
		return (IColumnPresentationFactory)getAdapter(element, IColumnPresentationFactory.class);
	}

	/**
	 * Returns the model proxy factory for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return model proxy factory or <code>null</code>
	 */
	public static IModelProxyFactory getModelProxyFactory(Object element) {
		return (IModelProxyFactory)getAdapter(element, IModelProxyFactory.class);
	}

	/**
	 * Returns the model proxy factory v.2 for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return model proxy factory or <code>null</code>
	 */
	public static IModelProxyFactory2 getModelProxyFactory2(Object element) {
		return (IModelProxyFactory2)getAdapter(element, IModelProxyFactory2.class);
	}

	/**
	 * Returns the memento provider for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return memento provider or <code>null</code>
	 */
	public static IElementMementoProvider getMementoProvider(Object element) {
		return (IElementMementoProvider)getAdapter(element, IElementMementoProvider.class);
	}

	/**
	 * Returns the element editor for the given element or <code>null</code>.
	 *
	 * @param element element to retrieve adapter for
	 * @return element editor or <code>null</code>
	 */
	public static IElementEditor getElementEditor(Object element) {
		return (IElementEditor)getAdapter(element, IElementEditor.class);
	}

	/**
	 * Creates and returns the selection policy associated with the given selection
	 * or <code>null</code> if none.
	 *
	 * @param selection or <code>null</code>
	 * @param context presentation context
	 * @return selection policy or <code>null</code>
	 */
	public static IModelSelectionPolicy getSelectionPolicy(ISelection selection, IPresentationContext context) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			IModelSelectionPolicyFactory factory = (IModelSelectionPolicyFactory) getAdapter(element, IModelSelectionPolicyFactory.class);
			if (factory != null) {
				return factory.createModelSelectionPolicyAdapter(element, context);
			}
		}
		return null;
	}

	/**
	 * Returns the viewer input provider for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return viewer input provider or <code>null</code>
	 */
	public static IViewerInputProvider getInputProvider(Object element) {
		return (IViewerInputProvider)getAdapter(element, IViewerInputProvider.class);
	}

	/**
	 * Returns an adapter of the specified type for the given object or <code>null</code>
	 * if none.
	 *
	 * @param element element to retrieve adapter for
	 * @param type adapter type
	 * @return adapter or <code>null</code>
	 */
	private static Object getAdapter(Object element, Class<?> type) {
		return DebugPlugin.getAdapter(element, type);
	}
}
