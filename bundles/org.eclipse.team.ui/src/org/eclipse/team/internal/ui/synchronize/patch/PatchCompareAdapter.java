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

import org.eclipse.compare.internal.patch.PatchFileDiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.mapping.DiffTreeChangesSection.ITraversalFactory;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.IMemento;

public class PatchCompareAdapter extends SynchronizationCompareAdapter
		implements ITraversalFactory {

	@Override
	public ICompareInput asCompareInput(ISynchronizationContext context,
			Object o) {
		// PatchFileDiffNode can adapt to IFile
		if (o instanceof PatchFileDiffNode)
			return super.asCompareInput(context, ((PatchFileDiffNode) o)
					.getResource());
		if (o instanceof ICompareInput)
			return (ICompareInput) o;
		return super.asCompareInput(context, o);
	}

	@Override
	public void save(ResourceMapping[] mappings, IMemento memento) {
		// Don't save
	}

	@Override
	public ResourceMapping[] restore(IMemento memento) {
		// Don't restore
		return null;
	}

	@Override
	public ResourceTraversal[] getTraversals(ISynchronizationScope scope) {
		return scope.getTraversals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
	}
}
