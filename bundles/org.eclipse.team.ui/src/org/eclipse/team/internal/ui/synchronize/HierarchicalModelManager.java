/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelManager#getSupportedModelProviders()
	 */
	@Override
	protected ISynchronizeModelProviderDescriptor[] getSupportedModelProviders() {
		return new ISynchronizeModelProviderDescriptor[] {
		        new FlatModelProvider.FlatModelProviderDescriptor(),
				new HierarchicalModelProvider.HierarchicalModelProviderDescriptor(),
				new CompressedFoldersModelProvider.CompressedFolderModelProviderDescriptor() };
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelManager#createModelProvider(java.lang.String)
	 */
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
