/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.resources.mapping.*;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.mapping.DiffTreeChangesSection.ITraversalFactory;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.IMemento;

public class ChangeSetCompareAdapter extends SynchronizationCompareAdapter implements ITraversalFactory {

	public void save(ResourceMapping[] mappings, IMemento memento) {
		// Don't save
	}

	public ResourceMapping[] restore(IMemento memento) {
		// Don't restore
		return new ResourceMapping[0];
	}

	public ResourceTraversal[] getTraversals(ISynchronizationScope scope) {
		return scope.getTraversals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
	}
	
	public String getName(ResourceMapping mapping) {
		Object modelObject = mapping.getModelObject();
		if (modelObject instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) modelObject;
			return changeSet.getName();
		}
		return super.getName(mapping);
	}

}
