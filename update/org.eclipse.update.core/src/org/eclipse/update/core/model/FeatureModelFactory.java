/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.core.Messages;
import org.xml.sax.SAXException;

/**
 * Default feature model factory.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead subclass the provided base implementation 
 * of this factory.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.BaseFeatureFactory
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class FeatureModelFactory {


	//private static DefaultFeatureParser parser = new DefaultFeatureParser();

	/**
	 * Creates a default model factory.
	 * 
	 * @since 2.0
	 */
	public FeatureModelFactory() {
		super();
	}

	/**
	 * Creates and populates a default feature from stream.
	 * The parser assumes the stream contains a default feature manifest
	 * (feature.xml) as documented by the platform.
	 * 
	 * @param stream feature stream
	 * @return populated feature model
	 * @exception CoreException
	 * @exception SAXException
	 * @since 2.0
	 */
	public FeatureModel parseFeature(InputStream stream)
		throws CoreException, SAXException {
        return parseFeature(stream, null);
	}
    
    /**
     * Creates and populates a default feature from stream.
     * The parser assumes the stream contains a default feature manifest
     * (feature.xml) as documented by the platform.
     * 
     * @param stream feature stream
     * @param location feature location
     * @return populated feature model
     * @exception CoreException
     * @exception SAXException
     * @since 3.1
     */
    public FeatureModel parseFeature(InputStream stream, String location)
        throws CoreException, SAXException {
    	DefaultFeatureParser parser = new DefaultFeatureParser();
        parser.init(this, location);
        FeatureModel featureModel = null;
        try {
            featureModel = parser.parse(stream);
            if (parser.getStatus()!=null) {
                // some internalError were detected
                IStatus status = parser.getStatus();
                throw new CoreException(status);
            }
        } catch (IOException e) {
            throw Utilities.newCoreException(Messages.FeatureModelFactory_ErrorAccesingFeatureStream, e); 
        }
        return featureModel;
    }

	/**
	 * Create a default feature model.
	 * 
	 * @see FeatureModel
	 * @return feature model
	 * @since 2.0
	 */
	public FeatureModel createFeatureModel() {
		return new FeatureModel();
	}

	/**
	 * Create a default included feature reference model.
	 * 
	 * @see IncludedFeatureReferenceModel
	 * @return feature model
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel createIncludedFeatureReferenceModel() {
		return new IncludedFeatureReferenceModel();
	}


	/**
	 * Create a default install handler model.
	 * 
	 * @see InstallHandlerEntryModel
	 * @return install handler entry model
	 * @since 2.0
	 */
	public InstallHandlerEntryModel createInstallHandlerEntryModel() {
		return new InstallHandlerEntryModel();
	}

	/**
	 * Create a default import dependency model.
	 * 
	 * @see ImportModel
	 * @return import dependency model
	 * @since 2.0
	 */
	public ImportModel createImportModel() {
		return new ImportModel();
	}

	/**
	 * Create a default plug-in entry model.
	 * 
	 * @see PluginEntryModel
	 * @return plug-in entry model
	 * @since 2.0
	 */
	public PluginEntryModel createPluginEntryModel() {
		return new PluginEntryModel();
	}

	/**
	 * Create a default non-plug-in entry model.
	 * 
	 * @see NonPluginEntryModel
	 * @return non-plug-in entry model
	 * @since 2.0
	 */
	public NonPluginEntryModel createNonPluginEntryModel() {
		return new NonPluginEntryModel();
	}

	/**
	 * Create a default annotated URL model.
	 * 
	 * @see URLEntryModel
	 * @return annotated URL model
	 * @since 2.0
	 */
	public URLEntryModel createURLEntryModel() {
		return new URLEntryModel();
	}
}
