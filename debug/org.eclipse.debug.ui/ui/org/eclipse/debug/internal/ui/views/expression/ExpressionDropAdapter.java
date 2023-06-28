/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	   Wind River Systems - Pawel Piech - Initial Implementation - Drag/Drop to Expressions View (Bug 184057), Integration with non-standard debug models (Bug 209883)
 *     IBM Corporation - further implementation and documentation
 *     Wind River Systems - integration with non-standard debug models
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.expression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.core.ExpressionManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter2;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapterExtension;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Drop Adapter allowing expressions, variables and text to be dropped in the Expression View.
 * When IVariables or text is dropped new watch expressions are created at the drop location.
 * When IExpressions are dropped, they are moved to the drop location
 *
 * @see org.eclipse.debug.internal.ui.views.variables.VariablesDragAdapter
 * @see ExpressionManager
 * @since 3.4
 */
public class ExpressionDropAdapter extends ViewerDropAdapter {

	private IWorkbenchPartSite fSite;
	private TransferData fCurrentTransferType = null;
	private boolean fInsertBefore;
	private int fDropType;

	private static final int DROP_TYPE_DEFAULT = 0;
	private static final int DROP_TYPE_VARIABLE = 1;
	private static final int DROP_TYPE_EXPRESSION = 2;
	private static final int DROP_TYPE_WATCH_ADAPTABLE_ELEMENT = 3;

	/**
	 * Constructor takes the viewer this drop adapter applies to.
	 * @param viewer the viewer to add drop to
	 */
	protected ExpressionDropAdapter(IWorkbenchPartSite site, TreeModelViewer viewer) {
		super(viewer);
		fSite = site;
		setFeedbackEnabled(true);
		setSelectionFeedbackEnabled(false);
		setScrollExpandEnabled(false);
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		fDropType = DROP_TYPE_DEFAULT;
		event.detail = DND.DROP_NONE;

		for (TransferData dataType : event.dataTypes) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(dataType)) {
				if (isExpressionDrop()){
					event.currentDataType = dataType;
					event.detail = DND.DROP_MOVE;
					fDropType = DROP_TYPE_EXPRESSION;
					break;
				} else if (isVariableDrop()){
					event.currentDataType = dataType;
					event.detail = DND.DROP_COPY;
					fDropType = DROP_TYPE_VARIABLE;
					break;
				} else if (isWatchAdaptableElementDrop()){
					event.currentDataType = dataType;
					event.detail = DND.DROP_COPY;
					fDropType = DROP_TYPE_WATCH_ADAPTABLE_ELEMENT;
					break;
				}
			} else if (TextTransfer.getInstance().isSupportedType(dataType)) {
				event.currentDataType = dataType;
				event.detail = DND.DROP_COPY;
				fDropType = DROP_TYPE_DEFAULT;
				break;
			}
		}

		super.dragEnter(event);
	}

	/**
	 * @return whether the selection transfer contains only IExpressions
	 */
	private boolean isExpressionDrop() {
		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (getTargetExpression(element) == null){
				return false;
			}
		}
		return true;
	}

	/**
	 * @return whether the selection transfer contains only IVariables
	 */
	private boolean isVariableDrop() {
		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (!(element instanceof IVariable)){
				return false;
			}
		}
		return true;
	}

	/**
	 * @return whether the selection transfer contains only objects adaptable
	 * to IWatchExpressionFactoryAdapter2
	 */
	private boolean isWatchAdaptableElementDrop() {
		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (!(element instanceof IAdaptable &&
				((IAdaptable)element).getAdapter(IWatchExpressionFactoryAdapter2.class) != null))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		super.dragOver(event);
		// Allow scrolling (but not expansion)
		event.feedback |= DND.FEEDBACK_SCROLL;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
			switch (fDropType) {
			case DROP_TYPE_EXPRESSION:
				return validateExpressionDrop(target);
			case DROP_TYPE_VARIABLE:
				return validateVariableDrop(target);
			case DROP_TYPE_WATCH_ADAPTABLE_ELEMENT:
				return validateWatchAdaptableDrop(target);
			default:
				break;
			}
		} else if (TextTransfer.getInstance().isSupportedType(transferType)) {
			return validateTextDrop(target);
		}
		return false;
	}

	/**
	 * Validates if an IExpression drop is valid by checking if the target
	 * is an IExpression.
	 * @param target target of the drop
	 * @return whether the drop is valid
	 */
	private boolean validateExpressionDrop(Object target){
		return target instanceof IExpression ||
			   ((target instanceof IAdaptable) && ((IAdaptable)target).getAdapter(IExpression.class) != null);
	}

	private IExpression getTargetExpression(Object target) {
		if (target instanceof IExpression) {
			return (IExpression)target;
		} else if (target instanceof IAdaptable) {
			return ((IAdaptable)target).getAdapter(IExpression.class);
		}
		return null;
	}

	/**
	 * Validates if the drop is valid by validating the local selection transfer
	 * to ensure that a watch expression can be created for each contained IVariable.
	 * @param target target of the drop
	 * @return whether the drop is valid
	 */
	private boolean validateVariableDrop(Object target) {
		// Target must be null or an IExpression, you cannot add a new watch expression inside another
		if (target != null && getTargetExpression(target) == null) {
			return false;
		}

		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		int enabled = 0;
		int size = -1;
		if (selection != null) {
			size = selection.size();
			IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
			Iterator<?> iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof IVariable){
					IVariable variable = (IVariable) element;
					if (variable instanceof IndexedVariablePartition) {
						break;
					} else if (manager.hasWatchExpressionDelegate(variable.getModelIdentifier()) && isFactoryEnabled(variable)) {
						enabled++;
					} else {
						break;
					}
				}
			}
		}
		return enabled == size;
	}

	/**
	 * Validates if the drop is valid by validating the local selection transfer
	 * to ensure that a watch expression can be created for each contained element.
	 * @param target target of the drop
	 * @return whether the drop is valid
	 */
	private boolean validateWatchAdaptableDrop(Object target) {
		// Target must be null or an IExpression, you cannot add a new watch expression inside another
		if (target != null && getTargetExpression(target) == null) {
			return false;
		}

		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		int enabled = 0;
		int size = -1;
		if (selection != null) {
			size = selection.size();
			Iterator<?> iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (isFactory2Enabled(element)) {
					enabled++;
				} else {
					break;
				}
			}
		}
		return enabled == size;
	}

	/**
	 * Validates if the drop is valid by validating the drop location.
	 * Only valid if the target is <code>null</code> or an <code>IExpression</code>.
	 * You cannot add a new watch expression inside another.
	 * @param target target of the drop
	 * @return whether the drop is valid
	 */
	private boolean validateTextDrop(Object target){
		return target == null || getTargetExpression(target) != null;
	}

	/**
	 * Returns whether the factory adapter for the given variable is currently enabled.
	 *
	 * @param variable the variable to ask for the adapter
	 * @return whether the factory is enabled
	 */
	private boolean isFactoryEnabled(IVariable variable) {
		IWatchExpressionFactoryAdapter factory = getFactory(variable);
		if (factory instanceof IWatchExpressionFactoryAdapterExtension) {
			IWatchExpressionFactoryAdapterExtension ext = (IWatchExpressionFactoryAdapterExtension) factory;
			return ext.canCreateWatchExpression(variable);
		}
		return true;
	}

	/**
	 * Returns whether the factory adapter for the given element is currently enabled.
	 *
	 * @param element the element to ask for the adapter
	 * @return whether the factory is enabled
	 */
	private boolean isFactory2Enabled(Object element) {
		IWatchExpressionFactoryAdapter2 factory = getFactory2(element);
		if (factory != null) {
			return factory.canCreateWatchExpression(element);
		}
		return false;
	}

	@Override
	public void drop(DropTargetEvent event) {
		fCurrentTransferType = event.currentDataType;
		// Unless insert after is explicitly set, insert before
		fInsertBefore = getCurrentLocation() != LOCATION_AFTER;
		super.drop(event);
	}

	@Override
	public boolean performDrop(Object data) {
		if (LocalSelectionTransfer.getTransfer().isSupportedType(fCurrentTransferType)) {
			IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
			if (fDropType == DROP_TYPE_EXPRESSION){
				return performExpressionDrop(selection);
			} else if (fDropType == DROP_TYPE_VARIABLE || fDropType == DROP_TYPE_WATCH_ADAPTABLE_ELEMENT){
				return performVariableOrWatchAdaptableDrop(selection);
			}
		} else if (TextTransfer.getInstance().isSupportedType(fCurrentTransferType)) {
			if (data != null) {
				return performTextDrop((String)data);
			}
		}
		return false;
	}

	/**
	 * Performs the drop when the selection is a collection of IExpressions.
	 * Moves the given expressions from their original locations to the
	 * location of the current target.
	 * @param selection the dragged selection
	 * @return whether the drop could be completed
	 */
	private boolean performExpressionDrop(IStructuredSelection selection) {
		IExpression targetExpression = getTargetExpression(getCurrentTarget());
		if (targetExpression != null){
			IExpression[] expressions = new IExpression[selection.size()];
			Object[] selectionElements = selection.toArray();
			for (int i = 0; i < selectionElements.length; i++) {
				expressions[i] = getTargetExpression(selectionElements[i]);
			}

			IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
			if (manager instanceof ExpressionManager){
				((ExpressionManager)manager).moveExpressions(expressions, targetExpression, fInsertBefore);
			}
			return true;
		}
		return false;

	}

	/**
	 * If the dragged data is a structured selection, get any IVariables in it
	 * and create expressions for each of them.  Insert the created expressions
	 * at the currently selected target or add them to the end of the collection
	 * if no target is selected.
	 *
	 * @param selection Structured selection containing IVariables
	 * @return whether the drop was successful
	 */
	private boolean performVariableOrWatchAdaptableDrop(IStructuredSelection selection) {
		List<IExpression> expressions = new ArrayList<>(selection.size());
		for (Iterator<?> itr = selection.iterator(); itr.hasNext();) {
			Object element = itr.next();
			String expressionText = createExpressionString(element);
			if (expressionText != null){
				IExpression expression = createExpression(expressionText);
				if (expression != null){
					expressions.add(expression);
				} else {
					DebugUIPlugin.log(new Status(IStatus.ERROR,DebugUIPlugin.getUniqueIdentifier(),"Drop failed.  Watch expression could not be created for the text " + expressionText)); //$NON-NLS-1$
					return false;
				}
			} else {
				return false;
			}
		}
		if (expressions.size() == selection.size()){
			IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
			if (manager instanceof ExpressionManager){
				IExpression targetExpression = getTargetExpression(getCurrentTarget());
				if (targetExpression != null){
					((ExpressionManager)manager).insertExpressions(expressions.toArray(new IExpression[expressions.size()]), targetExpression, fInsertBefore);
				} else {
					((ExpressionManager)manager).addExpressions(expressions.toArray(new IExpression[expressions.size()]));
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * Performs the drop when text was dragged.  Creates a new watch expression from
	 * the text.  Inserts the expression at the currently selected target or adds it
	 * to the end of the collection if no target is selected.
	 *
	 * @param text string to use to create the expression
	 * @return whether the drop was successful
	 */
	private boolean performTextDrop(String text){
		IExpression expression = createExpression(text);
		if (expression != null){
			IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
			if (manager instanceof ExpressionManager){
				IExpression targetExpression = getTargetExpression(getCurrentTarget());
				if (targetExpression != null){
					((ExpressionManager)manager).insertExpressions(new IExpression[]{expression}, targetExpression, fInsertBefore);
				} else {
					((ExpressionManager)manager).addExpression(expression);
				}
				return true;
			}
		}
		DebugUIPlugin.log(new Status(IStatus.ERROR,DebugUIPlugin.getUniqueIdentifier(),"Drop failed.  Watch expression could not be created for the text " + text)); //$NON-NLS-1$
		return false;
	}

	/**
	 * Creates a new watch expression from an IVariable using the watch expression factory
	 * adapter for that variable.
	 *
	 * @param variable the variable to use to create the watch expression
	 * @return the string to be used to create expression, return <code>null</code>
	 * if no expression is to be created
	 */
	private String createExpressionString(Object element) {
		try {
			if (element instanceof IVariable) {
				IVariable variable = (IVariable)element;
				IWatchExpressionFactoryAdapter factory = getFactory(variable);
				String exp = variable.getName();
				if (factory != null) {
					//if a factory exists, use it to create expression,
					//otherwise just use variable name
					exp = factory.createWatchExpression(variable);
				}
				return exp;
			} else {
				IWatchExpressionFactoryAdapter2 factory2 = getFactory2(element);
				if (factory2 != null) {
					return factory2.createWatchExpression(element);
				}
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
		return null;
	}

	/**
	 * Creates a new watch expression from a string using the default expression manager.
	 *
	 * @param exp the string to use to create the expression
	 */
	private IExpression createExpression(String exp) {
		IWatchExpression expression = DebugPlugin.getDefault().getExpressionManager().newWatchExpression(exp);
		IAdaptable object = DebugUITools.getPartDebugContext(fSite);
		IDebugElement context = null;
		if (object instanceof IDebugElement) {
			context = (IDebugElement) object;
		} else if (object instanceof ILaunch) {
			context = ((ILaunch) object).getDebugTarget();
		}
		expression.setExpressionContext(context);
		return expression;
	}


	/**
	 * Returns the factory adapter for the given variable or <code>null</code> if none.
	 *
	 * @param variable
	 * @return factory or <code>null</code>
	 */
	private IWatchExpressionFactoryAdapter getFactory(IVariable variable) {
		return variable.getAdapter(IWatchExpressionFactoryAdapter.class);
	}

	/**
	 * Returns the factory adapter for the given element or <code>null</code> if none.
	 *
	 * @param element
	 * @return factory or <code>null</code>
	 */
	private IWatchExpressionFactoryAdapter2 getFactory2(Object element) {
		if (element instanceof IAdaptable) {
			return ((IAdaptable)element).getAdapter(IWatchExpressionFactoryAdapter2.class);
		}
		return null;
	}

}
