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
package org.eclipse.update.internal.operations;

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.operations.*;

public class JobRoot {
	private IInstallFeatureOperation job;
	private FeatureHierarchyElement[] elements;

	public JobRoot(IInstallFeatureOperation job) {
		this.job = job;
	}

	public IInstallFeatureOperation getJob() {
		return job;
	}

	public FeatureHierarchyElement[] getElements() {
		if (elements == null)
			computeElements();
		return elements;
	}

	/**
	 * Returns unconfigured features before an install.
	 * After installing the features, the caller must get the local features that match
	 * these unconfigured features and unconfigure them.
	 * @param config
	 * @param targetSite
	 * @return
	 */
	public IFeature[] getUnconfiguredOptionalFeatures(
		IInstallConfiguration config,
		IConfiguredSite targetSite) {

		ArrayList unconfiguredOptionalFeatures = new ArrayList();
		getUnconfiguredOptionalFeatures(unconfiguredOptionalFeatures, config, targetSite, getElements(), UpdateUtils.isPatch(job.getFeature()));
		IFeature[] unconfiguredOptionalFeaturesArray =
			new IFeature[unconfiguredOptionalFeatures.size()];
		unconfiguredOptionalFeatures.toArray(unconfiguredOptionalFeaturesArray);
		return unconfiguredOptionalFeaturesArray;
	}

	private void getUnconfiguredOptionalFeatures(
		ArrayList unconfiguredOptionalFeatures,
		IInstallConfiguration config,
		IConfiguredSite targetSite,
		FeatureHierarchyElement[] optionalElements,
		boolean isPatch) {
		for (int i = 0; i < optionalElements.length; i++) {
			FeatureHierarchyElement[] children =
				optionalElements[i].getChildren(true, isPatch, config);
			getUnconfiguredOptionalFeatures(
				unconfiguredOptionalFeatures,
				config,
				targetSite,
				children,
				isPatch);
			if (!optionalElements[i].isEnabled(config)) {
				unconfiguredOptionalFeatures.add(optionalElements[i].getFeature());
			}
		}
	}

	private void computeElements() {
		try {
			IFeature oldFeature = job.getOldFeature();
			IFeature newFeature = job.getFeature();
			ArrayList list = new ArrayList();
			boolean patch = UpdateUtils.isPatch(newFeature);
			FeatureHierarchyElement.computeElements(
				oldFeature,
				newFeature,
				oldFeature != null,
				patch,
				SiteManager.getLocalSite().getCurrentConfiguration(),
				list);
			elements = new FeatureHierarchyElement[list.size()];
			list.toArray(elements);
			for (int i = 0; i < elements.length; i++) {
				elements[i].setRoot(this);
			}
		} catch (CoreException e) {
			UpdateUtils.logException(e);
		}
	}
}
