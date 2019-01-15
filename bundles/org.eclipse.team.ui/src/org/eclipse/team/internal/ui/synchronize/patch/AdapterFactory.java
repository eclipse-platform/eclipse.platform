/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.patch.HunkDiffNode;
import org.eclipse.compare.internal.patch.PatchFileDiffNode;
import org.eclipse.compare.internal.patch.PatchProjectDiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class AdapterFactory implements IAdapterFactory {

	private IWorkbenchAdapter modelAdapter = new PatchWorkbenchAdapter();
	private ISynchronizationCompareAdapter compareAdapter;

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == ResourceMapping.class) {
			if (adaptableObject instanceof PatchProjectDiffNode) {
				return (T) new DiffProjectResourceMapping(
						((PatchProjectDiffNode) adaptableObject)
								.getDiffProject());
			}
			if (adaptableObject instanceof PatchFileDiffNode) {
				return (T) new FilePatchResourceMapping(
						((PatchFileDiffNode) adaptableObject).getDiffResult());
			}
			if (adaptableObject instanceof HunkDiffNode) {
				return (T) new HunkResourceMapping(((HunkDiffNode) adaptableObject)
						.getHunkResult());
			}
		}
		if (adapterType == IWorkbenchAdapter.class)
			return (T) modelAdapter;
		if (adapterType == ISynchronizationCompareAdapter.class
				&& adaptableObject instanceof PatchModelProvider) {
			if (compareAdapter == null) {
				compareAdapter = new PatchCompareAdapter();
			}
			return (T) compareAdapter;
		}
		if (adapterType == IResource.class) {
			if (adaptableObject instanceof PatchFileDiffNode) {
				return (T) ((PatchFileDiffNode) adaptableObject).getResource();
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ResourceMapping.class, IWorkbenchAdapter.class,
				IResource.class };
	}
}
