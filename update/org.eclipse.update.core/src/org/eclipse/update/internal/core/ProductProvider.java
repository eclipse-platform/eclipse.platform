/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.internal.configurator.FeatureEntry;

/**
 * Maps primary features to IProduct
 */
public class ProductProvider implements IProductProvider {
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProductProvider#getProducts()
	 */
	public IProduct[] getProducts() {
		IPlatformConfiguration configuration = ConfiguratorUtils.getCurrentPlatformConfiguration();
		if (configuration == null)
			return new IProduct[0];
		IPlatformConfiguration.IFeatureEntry[] features = configuration.getConfiguredFeatureEntries();
		ArrayList primaryFeatures = new ArrayList();
		for (int i = 0; i < features.length; i++)
			if (features[i].canBePrimary() && (features[i] instanceof FeatureEntry))
				primaryFeatures.add(new FeatureEntryWrapper((FeatureEntry) features[i]));
		// TODO handle unmanaged plugins later
		return (IProduct[]) primaryFeatures.toArray(new IProduct[primaryFeatures.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProductProvider#getName()
	 */
	public String getName() {
		return Messages.ProductProvider;
	}
}
