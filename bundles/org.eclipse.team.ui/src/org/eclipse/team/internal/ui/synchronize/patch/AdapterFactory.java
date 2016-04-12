/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.patch.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class AdapterFactory implements IAdapterFactory {

	private IWorkbenchAdapter modelAdapter = new PatchWorkbenchAdapter();
	private ISynchronizationCompareAdapter compareAdapter;

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == ResourceMapping.class) {
			if (adaptableObject instanceof PatchProjectDiffNode) {
				return new DiffProjectResourceMapping(
						((PatchProjectDiffNode) adaptableObject)
								.getDiffProject());
			}
			if (adaptableObject instanceof PatchFileDiffNode) {
				return new FilePatchResourceMapping(
						((PatchFileDiffNode) adaptableObject).getDiffResult());
			}
			if (adaptableObject instanceof HunkDiffNode) {
				return new HunkResourceMapping(((HunkDiffNode) adaptableObject)
						.getHunkResult());
			}
		}
		if (adapterType == IWorkbenchAdapter.class)
			return modelAdapter;
		if (adapterType == ISynchronizationCompareAdapter.class
				&& adaptableObject instanceof PatchModelProvider) {
			if (compareAdapter == null) {
				compareAdapter = new PatchCompareAdapter();
			}
			return compareAdapter;
		}
		if (adapterType == IResource.class) {
			if (adaptableObject instanceof PatchFileDiffNode) {
				return ((PatchFileDiffNode) adaptableObject).getResource();
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { ResourceMapping.class, IWorkbenchAdapter.class,
				IResource.class };
	}
}
