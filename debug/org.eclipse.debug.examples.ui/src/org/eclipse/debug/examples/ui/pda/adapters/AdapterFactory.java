/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.adapters;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewActionProvider;


/**
 *
 * @since 3.2
 *
 */
public class AdapterFactory implements IAdapterFactory {

	private static IElementContentProvider fgTargetAdapter = new PDADebugTargetContentProvider();
	private static IModelProxyFactory fgFactory = new ModelProxyFactory();
	private static IViewActionProvider fgViewActionProvider = new PDAViewActionProvider();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IElementContentProvider.class.equals(adapterType)) {
			if (adaptableObject instanceof PDADebugTarget) {
				return (T) fgTargetAdapter;
			}
		}
		if (IModelProxyFactory.class.equals(adapterType)) {
			if (adaptableObject instanceof PDADebugTarget) {
				return (T) fgFactory;
			}
		}
		if (IViewActionProvider.class.equals(adapterType)) {
			if (adaptableObject instanceof PDAStackFrame) {
				return (T) fgViewActionProvider;
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{IElementContentProvider.class, IModelProxyFactory.class, IViewActionProvider.class};
	}

}
