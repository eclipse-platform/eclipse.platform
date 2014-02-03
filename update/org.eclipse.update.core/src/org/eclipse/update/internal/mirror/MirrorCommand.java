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
package org.eclipse.update.internal.mirror;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.SiteFeatureReference;
import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.standalone.ScriptedCommand;
import org.eclipse.update.standalone.StandaloneUpdateApplication;

/**
 * Mirrors a remote site locally.
 */
public class MirrorCommand extends ScriptedCommand {

	private String featureId;
	private String featureVersion;
	private String fromSiteUrl;
	private String toSiteDir;
	private String mirrorURL;
	private boolean ignoreNonPresentPlugins = false;
	private MirrorSite mirrorSite;

	public MirrorCommand(
		String featureId,
		String featureVersion,
		String fromSiteUrl,
		String toSiteDir,
		String mirrorUrl, 
		String ignoreNonPresentPlugins) {
		this.featureId = featureId;
		this.featureVersion = featureVersion;
		this.fromSiteUrl = fromSiteUrl;
		this.toSiteDir = toSiteDir;
		this.mirrorURL = mirrorUrl;
		this.ignoreNonPresentPlugins = (ignoreNonPresentPlugins != null) && (ignoreNonPresentPlugins.equals("true")); //$NON-NLS-1$
	}

	/**
	 * true if success
	 */
	public boolean run(IProgressMonitor monitor) {
		if (!validateParameters()) {
			return false;
		}

		try {
			if (getMirrorSite() == null)
				return false;

			URL remoteSiteUrl = new URL(fromSiteUrl);
			ISite remoteSite =
				SiteManager.getSite(remoteSiteUrl, new NullProgressMonitor());

			ISiteFeatureReference featureReferencesToMirror[] =
				findFeaturesToMirror(remoteSite);
			if (featureReferencesToMirror.length == 0) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(
					Utilities.newCoreException(
						"No matching features found on " + remoteSiteUrl + ".", //$NON-NLS-1$ //$NON-NLS-2$
						null));
				return false;
			}

			mirrorSite.mirrorAndExpose(
				remoteSite,
				featureReferencesToMirror,
				null,
				mirrorURL);
			return true;
		} catch (MalformedURLException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
			return false;
		} catch (CoreException ce) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(ce);
			return false;
		} finally {
			JarContentReference.shutdown();
		}
	}
	private boolean validateParameters() {
		if (fromSiteUrl == null || fromSiteUrl.length() <= 0) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(
				Utilities.newCoreException("from parameter is missing.", null)); //$NON-NLS-1$
			return false;
		}
		try {
			new URL(fromSiteUrl);
		} catch (MalformedURLException mue) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(
				Utilities.newCoreException("from must be a valid URL", null)); //$NON-NLS-1$
			return false;
		}
		if (toSiteDir == null || toSiteDir.length() <= 0) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(
				Utilities.newCoreException("to parameter is missing.", null)); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	private MirrorSite getMirrorSite()
		throws MalformedURLException, CoreException {
		// Create mirror site
		if (mirrorSite == null) {
			if (toSiteDir != null) {
				MirrorSiteFactory factory = new MirrorSiteFactory();
				System.out.print("Analyzing features already mirrored ..."); //$NON-NLS-1$
				try {
					mirrorSite =
						(MirrorSite) factory.createSite(new File(toSiteDir));
					mirrorSite.setIgnoreNonPresentPlugins(ignoreNonPresentPlugins);
				} catch (InvalidSiteTypeException iste) {
				}
				System.out.println("  Done."); //$NON-NLS-1$
			}
			if (mirrorSite == null) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(
					Utilities.newCoreException(
						"Mirror site at " + toSiteDir + " cannot be accessed.", //$NON-NLS-1$ //$NON-NLS-2$
						null));
				return null;
			}
		}
		return mirrorSite;

	}
	/**
	 * Returns subset of feature references on remote site
	 * as specified by optional featureId and featureVersion
	 * parameters
	 * @param remoteSite
	 * @return ISiteFeatureReference[]
	 * @throws CoreException
	 */
	private ISiteFeatureReference[] findFeaturesToMirror(ISite remoteSite)
		throws CoreException {
		ISiteFeatureReference remoteSiteFeatureReferences[] =
			remoteSite.getRawFeatureReferences();
		SiteFeatureReferenceModel existingFeatureModels[] =
			mirrorSite.getFeatureReferenceModels();
		Collection featureReferencesToMirror = new ArrayList();

		PluginVersionIdentifier featureVersionIdentifier = null;

		if (featureId == null) {
			System.out.println(
				"Parameter feature not specified.  All features on the remote site will be mirrored."); //$NON-NLS-1$
		}
		if (featureVersion == null) {
			System.out.println(
				"Parameter version not specified.  All versions of features on the remote site will be mirrored."); //$NON-NLS-1$
		} else {
			featureVersionIdentifier =
				new PluginVersionIdentifier(featureVersion);
		}
		for (int i = 0; i < remoteSiteFeatureReferences.length; i++) {
			VersionedIdentifier remoteFeatureVersionedIdentifier =
				remoteSiteFeatureReferences[i].getVersionedIdentifier();

			if (featureId != null
				&& !featureId.equals(
					remoteFeatureVersionedIdentifier.getIdentifier())) {
				// id does not match
				continue;
			}
			if (featureVersionIdentifier != null
				&& !featureVersionIdentifier.isPerfect(
					remoteFeatureVersionedIdentifier.getVersion())) {
				// version does not match
				continue;
			}

			for (int e = 0; e < existingFeatureModels.length; e++) {
				if (existingFeatureModels[e]
					.getVersionedIdentifier()
					.equals(remoteFeatureVersionedIdentifier)) {
					System.out.println(
						"Feature " //$NON-NLS-1$
							+ remoteFeatureVersionedIdentifier
							+ " already mirrored and exposed."); //$NON-NLS-1$
					// feature already mirrored and exposed in site.xml
					continue;
				}
			}

			// Check feature type
			String type =
				((SiteFeatureReference) remoteSiteFeatureReferences[i])
					.getType();
			if (type != null
				&& !ISite.DEFAULT_PACKAGED_FEATURE_TYPE.equals(type)) {
				// unsupported
				throw Utilities.newCoreException(
					"Feature " //$NON-NLS-1$
						+ remoteFeatureVersionedIdentifier
						+ " is of type " //$NON-NLS-1$
						+ type
						+ ".  Only features of type " //$NON-NLS-1$
						+ ISite.DEFAULT_PACKAGED_FEATURE_TYPE
						+ " are supported.", //$NON-NLS-1$
					null);
			}

			featureReferencesToMirror.add(remoteSiteFeatureReferences[i]);
			System.out.println(
				"Feature " //$NON-NLS-1$
					+ remoteSiteFeatureReferences[i].getVersionedIdentifier()
					+ " will be mirrored."); //$NON-NLS-1$
		}
		return (ISiteFeatureReference[]) featureReferencesToMirror.toArray(
			new ISiteFeatureReference[featureReferencesToMirror.size()]);
	}

}
