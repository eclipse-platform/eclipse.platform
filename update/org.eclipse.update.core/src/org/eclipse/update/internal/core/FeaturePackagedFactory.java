/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;

/**
 * Factory for Feature Packaged
 */
public class FeaturePackagedFactory extends BaseFeatureFactory {

	/*
	 * @see IFeatureFactory#createFeature(URL,ISite,IProgressMonitor)
	 */
	public IFeature createFeature(URL url,ISite site, IProgressMonitor monitor) throws CoreException {
		Feature feature = null;
		InputStream featureStream = null;
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(null,2);
		monitor.worked(1);
			
					
		try {	
			IFeatureContentProvider contentProvider = new FeaturePackagedContentProvider(url, site);	
			ContentReference manifest = contentProvider.getFeatureManifestReference(null/*IProgressMonitor*/);
			featureStream = manifest.getInputStream();
			feature = (Feature)parseFeature(featureStream);
			monitor.worked(1);
	
			// if there is no update URL for the Feature
			// use the Site URL
			if (feature.getUpdateSiteEntry()==null){
				URLEntryModel entryModel = createURLEntryModel();
				URL siteUrl = site.getURL();
				if (siteUrl!=null){
					entryModel.setURLString(siteUrl.toExternalForm());
					entryModel.resolve(siteUrl,null);
					feature.setUpdateSiteEntryModel(entryModel);
				}
			}	
			feature.setFeatureContentProvider(contentProvider);
			feature.setSite(site);						
			URL baseUrl = null;
			try {
				baseUrl = new URL(manifest.asURL(),"."); // make sure we have URL to feature directory //$NON-NLS-1$
			} catch(MalformedURLException e) {	
			}
			feature.resolve(baseUrl, baseUrl);
			feature.markReadOnly();			
		}  catch (CoreException e){
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw Utilities.newCoreException(NLS.bind(Messages.FeatureFactory_CreatingError, (new String[] { url.toExternalForm() })), e);
		}finally {
			try {
				if (featureStream!=null)	
					featureStream.close();
			} catch (IOException e) {
			}
		}
		return feature;
	}
    
    public IncludedFeatureReferenceModel createIncludedFeatureReferenceModel() {
     return new UpdateSiteIncludedFeatureReference();
    }
}
