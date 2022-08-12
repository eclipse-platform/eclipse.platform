/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.examples.filesystem.ui.FileSystemHistoryPageSource;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.mapping.ExampleModelProvider;
import org.eclipse.team.examples.model.mapping.ModelMerger;
import org.eclipse.team.examples.model.mapping.ModelResourceMapping;
import org.eclipse.team.examples.model.ui.mapping.CompareAdapter;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class AdapterFactory implements IAdapterFactory {

	private IWorkbenchAdapter modelAdapter = new ModelWorkbenchAdapter();
	private ModelMerger modelMerger;
	private CompareAdapter compareAdapter;
	private static Object historyPageSource = new FileSystemHistoryPageSource();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IWorkbenchAdapter.class && adaptableObject instanceof ModelObject)
			return adapterType.cast(modelAdapter);
		if (adapterType == ResourceMapping.class && adaptableObject instanceof ModelObject)
			return adapterType.cast(ModelResourceMapping.create((ModelObject)adaptableObject));
		if (adapterType == IResourceMappingMerger.class && adaptableObject instanceof ExampleModelProvider) {
			if (modelMerger == null) {
				modelMerger = new ModelMerger((ExampleModelProvider)adaptableObject);
			}
			return adapterType.cast(modelMerger);
		}
		if (adapterType == ISynchronizationCompareAdapter.class && adaptableObject instanceof ExampleModelProvider) {
			if (compareAdapter == null) {
				compareAdapter = new CompareAdapter((ExampleModelProvider)adaptableObject);
			}
			return adapterType.cast(compareAdapter);
		}

		if (adapterType == IHistoryPageSource.class){
			return adapterType.cast(historyPageSource);
		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IWorkbenchAdapter.class, ResourceMapping.class, IResourceMappingMerger.class, ISynchronizationCompareAdapter.class };
	}

}
