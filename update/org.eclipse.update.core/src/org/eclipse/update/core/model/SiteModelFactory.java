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
 * Default site model factory.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead subclass the provided base implementation 
 * of this factory.
 * </p>
 * @see org.eclipse.update.core.BaseSiteFactory
 * @since 2.0
 */
public class SiteModelFactory {

	/**
	 * Creates a default site factory.
	 * 
	 * @since 2.0
	 */
	public SiteModelFactory() {
		super();
	}

	/**
	 * Indicates whether this factory can handle the specified site type. This
	 * method is intended to be overridden by subclasses.
	 * 
	 * @param type site type identifier
	 * @return <code>true</code> if the type can be handled, otherwise <code>false</code>
	 * @since 2.0
	 */
	public boolean canParseSiteType(String type) {
		// return true if type was not specified (ie. is null or empty string)
		return (type == null || type.trim().equals("")); //$NON-NLS-1$
	}

	/**
	 * Creates and populates a default site from stream.
	 * The parser assumes the stream contains a default site manifest
	 * (site.xml) as documented by the platform.
	 * 
	 * @param stream site stream
	 * @return populated site model
	 * @exception ParsingException
	 * @exception IOException
	 * @exception SAXException
	 * @since 2.0
	 */
	public SiteModel parseSite(InputStream stream)
		throws CoreException, InvalidSiteTypeException {
		SiteModel result = null;
		try {
			DefaultSiteParser parser = new DefaultSiteParser(this);
			result = parser.parse(stream);
			if (parser.getStatus()!=null) {
				// some internalError were detected
				IStatus status = parser.getStatus();
				throw new CoreException(status);
			}
		} catch (SAXException e) {
			if (e instanceof SAXException) {
				SAXException exception = (SAXException) e;
				// invalid Site type
				if (exception.getException() instanceof InvalidSiteTypeException) {
					throw (InvalidSiteTypeException) exception.getException();
				}
			}

			throw Utilities.newCoreException(Policy.bind("SiteModelObject.ErrorParsingSiteStream"),e); //$NON-NLS-1$
		} catch (IOException e){
			throw Utilities.newCoreException(Policy.bind("SiteModelObject.ErrorAccessingSiteStream"),e); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Create a default site model.
	 * 
	 * @see SiteModel
	 * @return site model
	 * @since 2.0
	 */
	public SiteModel createSiteMapModel() {
		return new SiteModel();
	}

	/**
	 * Create a default feature reference model.
	 * 
	 * @see FeatureReferenceModel
	 * @return feature reference model
	 * @since 2.0
	 */
	public FeatureReferenceModel createFeatureReferenceModel() {
		return new FeatureReferenceModel();
	}

	/**
	 * Create a default archive reference model.
	 * 
	 * @see ArchiveReferenceModel
	 * @return archive reference model
	 * @since 2.0
	 */
	public ArchiveReferenceModel createArchiveReferenceModel() {
		return new ArchiveReferenceModel();
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

	/**
	 * Create a default category model.
	 * 
	 * @see CategoryModel
	 * @return category model
	 * @since 2.0
	 */
	public CategoryModel createSiteCategoryModel() {
		return new CategoryModel();
	}
}