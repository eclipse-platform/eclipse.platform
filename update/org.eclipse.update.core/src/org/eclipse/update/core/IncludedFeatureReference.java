/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;

/**
 * This is a utility class representing the options of a nested feature.
 * Feature will include other features. This class will represent the options of the inclusion.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0.1
 */
public class IncludedFeatureReference
	extends IncludedFeatureReferenceModel
	implements IIncludedFeatureReference {

	/**
	 * Construct a included feature reference
	 * 
	 * @since 2.1
	 */
	public IncludedFeatureReference() {
		super();
	}

	/**
	 * Construct a feature options 
	 * 
	 * @param name string representation of the feature
	 * @param isOptional <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * @param matchingRule the matching rule
	 * @param searchLocation the location to search for this feature's updates.
	 * @since 2.0.2
	 */
	public IncludedFeatureReference(IIncludedFeatureReference includedFeatureRef) {
		super((IncludedFeatureReferenceModel) includedFeatureRef);
	}

	/**
	 * Constructor IncludedFeatureReference.
	 * @param iFeatureReference
	 */
	public IncludedFeatureReference(IFeatureReference featureReference) {
		super(featureReference);
	}

	/*
	 * Method isDisabled.
	 * @return boolean
	 */
	private boolean isDisabled() {
		IConfiguredSite cSite = getSite().getCurrentConfiguredSite();
		if (cSite == null)
			return false;
		IFeatureReference[] configured = cSite.getConfiguredFeatures();
		for (int i = 0; i < configured.length; i++) {
			if (this.equals(configured[i]))
				return false;
		}
		return true;
		//		// FIXME: the above code was commented out and returned false. 
		//		// Should this be commented out again?
		//		return false;
	}

	/*
	 * Method isInstalled.
	 * @return boolean
	 */
	private boolean isUninstalled() {
		if (!isDisabled())
			return false;
		IFeatureReference[] installed = getSite().getFeatureReferences();
		for (int i = 0; i < installed.length; i++) {
			if (this.equals(installed[i]))
				return false;
		}
		// if we reached this point, the configured site exists and it does not
		// contain this feature reference, so clearly the feature is uninstalled
		return true;
	}

	/**
	 * @see org.eclipse.update.core.IIncludedFeatureReference#getFeature(boolean,
	 * IConfiguredSite)
	 * @deprecated use getFeature(IProgressMonitor)
	 */
	public IFeature getFeature(
		boolean perfectMatch,
		IConfiguredSite configuredSite)
		throws CoreException {
		return getFeature(null);
	}

	/**
	 * @see org.eclipse.update.core.IIncludedFeatureReference#getFeature(boolean,
	 * IConfiguredSite,IProgressMonitor)
	 * @deprecated use getFeature(IProgressMonitor)
	 */
	public IFeature getFeature(
		boolean perfectMatch,
		IConfiguredSite configuredSite,
		IProgressMonitor monitor)
		throws CoreException {
			return getFeature(monitor);
	}

	/**
	 * @see org.eclipse.update.core.IFeatureReference#getFeature()
	 * @deprecated use getFeature(IProgressMonitor)
	 */
	public IFeature getFeature() throws CoreException {
		return getFeature(null);
	}
	/**
	 * @see org.eclipse.update.core.IFeatureReference#getFeature
	 * (IProgressMonitor)
	 */
	public IFeature getFeature(IProgressMonitor monitor) throws CoreException {
		if (isUninstalled())
			throw new CoreException(new Status(IStatus.ERROR, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, Policy.bind("IncludedFeatureReference.featureUninstalled",
					getFeatureIdentifier()), null));
		else
			return super.getFeature(monitor);
	}
}
