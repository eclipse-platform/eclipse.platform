/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Manager for hierarchical models
 */
public class HierarchicalModelManager extends SynchronizeModelManager {

	public HierarchicalModelManager(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected ISynchronizeModelProviderDescriptor[] getSupportedModelProviders() {
		return new ISynchronizeModelProviderDescriptor[] {
				new FlatModelProvider.FlatModelProviderDescriptor(),
				new HierarchicalModelProvider.HierarchicalModelProviderDescriptor(),
				new CompressedFoldersModelProvider.CompressedFolderModelProviderDescriptor() };
	}

	@Override
	protected ISynchronizeModelProvider createModelProvider(String id) {
		if(id == null) {
			id = getDefaultProviderId();
		}
		if (id.endsWith(FlatModelProvider.FlatModelProviderDescriptor.ID)) {
			return new FlatModelProvider(getConfiguration(), getSyncInfoSet());
		} else if(id.endsWith(CompressedFoldersModelProvider.CompressedFolderModelProviderDescriptor.ID)) {
			return new CompressedFoldersModelProvider(getConfiguration(), getSyncInfoSet());
		} else {
			return new HierarchicalModelProvider(getConfiguration(), getSyncInfoSet());
		}
	}
}
