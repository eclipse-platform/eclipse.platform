/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
 * A model proxy factory creates model proxies for elements based on
 * specific presentation contexts. A model proxy factory is provided for
 * a model element by registering a model proxy factory adapter for
 * an element.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IModelProxy
 * @see IModelDelta
 * @since 3.2
 */
public interface IModelProxyFactory {
	/**
	 * Creates and returns a model proxy for the given element in the specified
	 * context or <code>null</code> if none.
	 *
	 * @param element model element to create a model proxy for
	 * @param context presentation context
	 * @return model proxy or <code>null</code>
	 */
	IModelProxy createModelProxy(Object element, IPresentationContext context);
}
