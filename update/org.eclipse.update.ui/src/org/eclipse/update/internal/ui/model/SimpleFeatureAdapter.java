/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.update.internal.ui.model;

import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.core.runtime.CoreException;

/**
 * @version 	1.0
 * @author
 */
public class SimpleFeatureAdapter extends FeatureAdapter {
	protected IFeature feature;
	private boolean optional;
	public SimpleFeatureAdapter(IFeature feature) {
		this(feature, false);
	}
	public SimpleFeatureAdapter(IFeature feature, boolean optional) {
		this.optional = optional;
	}
	
	public IFeature getFeature() throws CoreException {
		return feature;
	}
	
	public URL getURL() {
		return feature.getURL();
	}
	
	public ISite getSite() {
		return feature.getSite();
	}
	
	public IFeatureAdapter[] getIncludedFeatures() {
		try {
			IFeatureReference[] included = getFeature().getIncludedFeatureReferences();
			SimpleFeatureAdapter[] result =
				new SimpleFeatureAdapter[included.length];
			for (int i = 0; i < included.length; i++) {
				result[i] =
					new SimpleFeatureAdapter(included[i].getFeature(), included[i].isOptional());
				result[i].setIncluded(true);
			}
			return result;
		} catch (CoreException e) {
			return new IFeatureAdapter[0];
		}
	}
	public boolean isOptional() {
		return optional;
	}
}