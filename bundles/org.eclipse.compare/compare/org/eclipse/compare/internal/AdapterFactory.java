/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public class AdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(final Object adaptableObject, Class<T> adapterType) {
		if (IContributorResourceAdapter.class.equals(adapterType)
				&& adaptableObject instanceof CompareEditorInput) {
			return (T) (IContributorResourceAdapter) adaptable -> {
				Object ei = ((CompareEditorInput) adaptableObject)
						.getAdapter(IEditorInput.class);
				if (ei instanceof IFileEditorInput) {
					return ((IFileEditorInput) ei).getFile();
				}
				return null;
			};
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IContributorResourceAdapter.class };
	}
}