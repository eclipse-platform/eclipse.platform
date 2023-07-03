/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Adapter factory for standard source containers.
 *
 * @since 3.0
 */
public class SourceContainerAdapterFactory implements IAdapterFactory {
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType.equals(IWorkbenchAdapter.class)) {
			return (T) new SourceContainerWorkbenchAdapter();
		}
		return null;
	}
	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] {IWorkbenchAdapter.class};
	}
}
