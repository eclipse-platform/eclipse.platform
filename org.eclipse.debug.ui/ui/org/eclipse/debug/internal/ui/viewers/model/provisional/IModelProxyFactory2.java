/*******************************************************************************
 * Copyright (c) 2009 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Freescale Semiconductor - initial API and implementation � Bug 241336
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.viewers.TreePath;

/**
 * A model proxy factory creates model proxies for elements based on
 * specific presentation contexts. A model proxy factory is provided for
 * a model element by registering a model proxy factory adapter for
 * an element.
 * <p>
 * This interface is an alternative to the {@link IModelProxyFactory}
 * interface. Unlike its predecessor <code>IModelProxyFactory2</code> allows
 * the full path to the tree element to be specified when creating an
 * <code>IModelProxy<code> instance.  Using the full patch allows models to
 * provide proper model deltas even if the root element of this proxy is at
 * variable or unknown location in the viewer.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IModelProxyFactory
 * @see IModelProxy
 * @see IModelDelta
 *
 * @since 3.6
 */
public interface IModelProxyFactory2 {
	/**
	 * Creates and returns a model proxy for the given element in the specified
	 * context or <code>null</code> if none.
	 *
	 * @param input viewer input context
	 * @param path to model element to create a model proxy for
	 * @param context presentation context
	 * @return model proxy or <code>null</code>
	 */
	IModelProxy createTreeModelProxy(Object input, TreePath path, IPresentationContext context);
}
