package org.eclipse.update.internal.ui.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class FeatureReferenceAdapter extends FeatureAdapter {
	private IFeatureReference featureRef;

	public FeatureReferenceAdapter(IFeatureReference featureRef) {
		this(featureRef, false);
	}
	
	public FeatureReferenceAdapter(IFeatureReference featureRef, boolean included) {
		this.featureRef = featureRef;
		setIncluded(included);
	}
	
	public IFeature getFeature() throws CoreException {
		return featureRef.getFeature();
	}
	
	public ISite getSite() {
		return featureRef.getSite();
	}
	
	public URL getURL() {
		return featureRef.getURL();
	}
	
	public boolean isOptional() {
		return featureRef.isOptional();
	}

	public IFeatureAdapter[] getIncludedFeatures() {
		try {
			IFeatureReference[] included =
				getFeature().getIncludedFeatureReferences();
			FeatureReferenceAdapter[] result =
				new FeatureReferenceAdapter[included.length];
			for (int i = 0; i < included.length; i++) {
				result[i] = new FeatureReferenceAdapter(included[i], true);
			}
			return result;
		} catch (CoreException e) {
			return new IFeatureAdapter[0];
		}
	}

	public IFeatureReference getFeatureReference() {
		return featureRef;
	}
}