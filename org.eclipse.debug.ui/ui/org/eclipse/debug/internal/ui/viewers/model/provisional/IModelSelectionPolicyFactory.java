/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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


/**
 * A model selection policy factory creates a model selection policy for an element based on
 * a specific presentation context. A model selection policy factory is provided for
 * a model element by registering a model selection policy factory adapter for
 * an element.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IModelSelectionPolicy
 * @since 3.2
 */
public interface IModelSelectionPolicyFactory {
	/**
	 * Creates and returns a model selection policy for the given element in the specified
	 * context or <code>null</code> if none.
	 *
	 * @param element model element to create a selection policy for
	 * @param context presentation context
	 * @return model selection policy or <code>null</code>
	 */
	IModelSelectionPolicy createModelSelectionPolicyAdapter(Object element, IPresentationContext context);
}
