package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.core.Policy;
import org.xml.sax.SAXException;

/**
 * Default feature model factory.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead subclass the provided base implementation 
 * of this factory.
 * </p>
 * @see org.eclipse.update.core.BaseFeatureFactory
 * @since 2.0
 */

public class FeatureModelFactory {

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
	 * @exception ParsingException
	 * @exception IOException
	 * @exception SAXException
	 * @since 2.0
	 */
	public FeatureModel parseFeature(InputStream stream)
		throws CoreException, SAXException {
		DefaultFeatureParser parser = new DefaultFeatureParser(this);
		FeatureModel featureModel = null;
		try {
			featureModel = parser.parse(stream);
			if (parser.getStatus()!=null) {
				// some internalError were detected
				IStatus status = parser.getStatus();
				throw new CoreException(status);
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("FeatureModelFactory.ErrorAccesingFeatureStream"), e); //$NON-NLS-1$
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