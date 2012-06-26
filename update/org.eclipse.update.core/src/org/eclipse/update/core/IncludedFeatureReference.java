/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 182625, Missing constructor
 *******************************************************************************/
package org.eclipse.update.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;

/**
 * This is a utility class representing the options of a nested feature.
 * Feature will include other features. This class will represent the options of the inclusion.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
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
	 * @param includedFeatureRef reference to clone
	 * @since 2.0.2
	 */
	public IncludedFeatureReference(IIncludedFeatureReference includedFeatureRef) {
		super((IncludedFeatureReferenceModel) includedFeatureRef);
	}

	/**
	 * Constructor IncludedFeatureReference.
	 * @param featureReference
	 */
	public IncludedFeatureReference(IFeatureReference featureReference) {
		super(featureReference);
	}
	
	public IncludedFeatureReference(IncludedFeatureReferenceModel includedFeatureRefModel){
		super(includedFeatureRefModel);
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
			throw new CoreException(new Status(IStatus.ERROR, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, NLS.bind(Messages.IncludedFeatureReference_featureUninstalled, (new String[] { getFeatureIdentifier() })), null));
		else
			return super.getFeature(monitor);
	}
}
