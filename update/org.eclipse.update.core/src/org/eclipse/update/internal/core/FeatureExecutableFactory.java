package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureModel;

/**
 * FeatureFactory for Executable Features
 */
public class FeatureExecutableFactory extends BaseFeatureFactory {

	/*
	 * @see IFeatureFactory#createFeature(URL,ISite)
	 */
	public IFeature createFeature(URL url, ISite site) throws CoreException {

		TargetFeature feature = null;
		InputStream featureStream = null;

		if (url == null)
			return createFeature(site);

		// the URL should point to a directory
		url = validate(url);

		try {
			IFeatureContentProvider contentProvider =
				new FeatureExecutableContentProvider(url);
			IFeatureContentConsumer contentConsumer =
				new FeatureExecutableContentConsumer();

			URL nonResolvedURL =
				contentProvider.getFeatureManifestReference(null /*IProgressMonitor*/
			).asURL(); 
			URL resolvedURL = URLEncoder.encode(nonResolvedURL);
			featureStream = UpdateManagerPlugin.getPlugin().get(resolvedURL).getInputStream();

			feature = (TargetFeature) this.parseFeature(featureStream);
			feature.setSite(site);

			feature.setFeatureContentProvider(contentProvider);
			feature.setContentConsumer(contentConsumer);

			feature.resolve(url, getResourceBundle(url));
			feature.markReadOnly();
		} catch (CoreException e){
			throw e;
		} catch (Exception e){
			throw Utilities.newCoreException(
				Policy.bind("FeatureFactory.CreatingError", url.toExternalForm()),
				e);
			//$NON-NLS-1$
		} finally {
			try {
				featureStream.close();
			} catch (Exception e) {
			}
		}
		return feature;
	}

	/*
	 * @see FeatureModelFactory#createFeatureModel()
	 */
	public FeatureModel createFeatureModel() {
		return new TargetFeature();
	}

	/*
	 * Creates an empty feature on the site 
	 */
	private IFeature createFeature(ISite site) throws CoreException {
		TargetFeature feature = null;

		IFeatureContentProvider contentProvider =
			new FeatureExecutableContentProvider(null);
		IFeatureContentConsumer contentConsumer =
			new FeatureExecutableContentConsumer();
		feature = (TargetFeature) createFeatureModel();
		feature.setSite(site);
		feature.setFeatureContentProvider(contentProvider);
		feature.setContentConsumer(contentConsumer);

		// do not mark read only yet...	
		return feature;
	}

	/*
	 * validates a URL as a directory URL
	 */
	private URL validate(URL url) throws CoreException {

		if (url == null)
			throw Utilities.newCoreException(
				Policy.bind("FeatureExecutableFactory.NullURL"),
				null);
		//$NON-NLS-1$

		if (!(url.getFile().endsWith("/")
			|| url.getFile().endsWith(File.separator)
			|| url.getFile().endsWith(Feature.FEATURE_XML))) { //$NON-NLS-1$
			try {
				String path = url.getFile() + "/"; //$NON-NLS-1$
				url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("FeatureExecutableFactory.CannotCreateURL", url.toExternalForm()),
					e);
				//$NON-NLS-1$
			}
		}
		return url;
	}

}