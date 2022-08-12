/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipantFactory;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.team.internal.ui.mapping.DefaultResourceMappingMerger;
import org.eclipse.team.internal.ui.mapping.ResourceModelPersistenceAdapter;
import org.eclipse.team.internal.ui.mapping.ResourceModelScopeParticipantFactory;
import org.eclipse.team.internal.ui.synchronize.DiffNodeWorkbenchAdapter;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.ITeamStateProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class TeamAdapterFactory implements IAdapterFactory {
	private static final Class[] ADAPTER_CLASSES = new Class[] {
			IWorkbenchAdapter.class,
			IResourceMappingMerger.class,
			ISynchronizationCompareAdapter.class,
			ISynchronizationScopeParticipantFactory.class,
			ITeamStateProvider.class ,
			IFileRevision.class
		};

	private DiffNodeWorkbenchAdapter diffNodeAdapter = new DiffNodeWorkbenchAdapter();

	private static final ISynchronizationCompareAdapter COMPARE_ADAPTER = new ResourceModelPersistenceAdapter();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if(adaptableObject instanceof DiffNode && adapterType == IWorkbenchAdapter.class) {
			return (T) diffNodeAdapter;
		}
		if (adaptableObject instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) adaptableObject;
			if (provider.getDescriptor().getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID)) {
				if (adapterType == IResourceMappingMerger.class) {
					return (T) new DefaultResourceMappingMerger((ModelProvider)adaptableObject);
				}
				if (adapterType == ISynchronizationScopeParticipantFactory.class) {
					return (T) new ResourceModelScopeParticipantFactory((ModelProvider)adaptableObject);
				}
			}
		}
		if (adaptableObject instanceof ModelProvider && adapterType == ISynchronizationCompareAdapter.class) {
			return (T) COMPARE_ADAPTER;
		}
		if (adaptableObject instanceof RepositoryProviderType && adapterType == ITeamStateProvider.class) {
			RepositoryProviderType rpt = (RepositoryProviderType) adaptableObject;
			return (T) TeamUIPlugin.getPlugin().getDecoratedStateProvider(rpt);
		}

		if (IFileRevision.class == adapterType && adaptableObject instanceof FileRevisionEditorInput) {
			return (T) ((FileRevisionEditorInput) adaptableObject).getFileRevision();
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTER_CLASSES;
	}
}
