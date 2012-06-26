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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.core.URLEntry;
import org.eclipse.update.internal.core.ExtendedSite;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default site parser.
 * Parses the site manifest file as defined by the platform. Defers
 * to a model factory to create the actual concrete model objects. The 
 * update framework supplies two factory implementations:
 * <ul>
 * <li>@see org.eclipse.update.core.model.SiteModelFactory
 * <li>@see org.eclipse.update.core.BaseSiteFactory
 * </ul>
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class DefaultSiteParser extends DefaultHandler {
	
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	
	private SAXParser parser;
	private SiteModelFactory factory;

	private MultiStatus status;

	private boolean DESCRIPTION_SITE_ALREADY_SEEN = false;

	private static final int STATE_IGNORED_ELEMENT = -1;
	private static final int STATE_INITIAL = 0;
	private static final int STATE_SITE = 1;
	private static final int STATE_FEATURE = 2;
	private static final int STATE_ARCHIVE = 3;
	private static final int STATE_CATEGORY = 4;
	private static final int STATE_CATEGORY_DEF = 5;
	private static final int STATE_DESCRIPTION_SITE = 6;
	private static final int STATE_DESCRIPTION_CATEGORY_DEF = 7;
	private static final String PLUGIN_ID = UpdateCore.getPlugin().getBundle().getSymbolicName();

	private static final String SITE = "site"; //$NON-NLS-1$
	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String ARCHIVE = "archive"; //$NON-NLS-1$
	private static final String CATEGORY_DEF = "category-def"; //$NON-NLS-1$
	private static final String CATEGORY = "category"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String MIRROR = "mirror"; //$NON-NLS-1$
	//private static final String ASSOCIATE_SITES = "associateSites"; //$NON-NLS-1$
	private static final String ASSOCIATE_SITE = "associateSite"; //$NON-NLS-1$

	private static final String DEFAULT_INFO_URL = "index.html"; //$NON-NLS-1$
	private static final String FEATURES = "features/"; //$NON-NLS-1$
    
	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are
	// populating in this plugin descriptor
	Stack objectStack = new Stack();

	private int currentState;

	/**
	 * Constructs a site parser.
	 */
	public DefaultSiteParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			UpdateCore.log(e);
		} catch (SAXException e) {
			UpdateCore.log(e);
		}

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Created"); //$NON-NLS-1$
	}

	public void init(SiteModelFactory factory) {
		// PERF: separate instance creation from parsing
		this.factory = factory;
		stateStack = new Stack();
		objectStack = new Stack();
		status = null;
		DESCRIPTION_SITE_ALREADY_SEEN = false;
	}

	/**
	 * Parses the specified input steam and constructs a site model.
	 * The input stream is not closed as part of this operation.
	 * 
	 * @param in input stream
	 * @return site model
	 * @exception SAXException
	 * @exception IOException
	 * @since 2.0
	 */
	public SiteModel parse(InputStream in) throws SAXException, IOException {
		stateStack.push(new Integer(STATE_INITIAL));
		currentState = ((Integer) stateStack.peek()).intValue();
		parser.parse(new InputSource(in), this);
		if (objectStack.isEmpty())
			throw new SAXException(Messages.DefaultSiteParser_NoSiteTag);	
		else {
			if (objectStack.peek() instanceof SiteModel) {
				return (SiteModel) objectStack.pop();
			} else {
				String stack = ""; //$NON-NLS-1$
				Iterator iter = objectStack.iterator();
				while (iter.hasNext()) {
					stack = stack + iter.next().toString() + "\r\n"; //$NON-NLS-1$
				}
				throw new SAXException(NLS.bind(Messages.DefaultSiteParser_WrongParsingStack, (new String[] { stack })));
			}
		}
	}

	/**
	 * Returns all status objects accumulated by the parser.
	 *
	 * @return multi-status containing accumulated status, or <code>null</code>.
	 * @since 2.0
	 */
	public MultiStatus getStatus() {
		return status;
	}

	/**
	 * Handle start of element tags
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			debug("State: " + currentState); //$NON-NLS-1$
			debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		switch (currentState) {
			case STATE_IGNORED_ELEMENT :
				internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { localName, getState(currentState) })));
				break;
			case STATE_INITIAL :
				handleInitialState(localName, attributes);
				break;

			case STATE_SITE :
				handleSiteState(localName, attributes);
				break;

			case STATE_FEATURE :
				handleFeatureState(localName, attributes);
				break;

			case STATE_ARCHIVE :
				handleSiteState(localName, attributes);
				break;

			case STATE_CATEGORY :
				handleCategoryState(localName, attributes);
				break;

			case STATE_CATEGORY_DEF :
				handleCategoryDefState(localName, attributes);
				break;

			case STATE_DESCRIPTION_SITE :
				handleSiteState(localName, attributes);
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
				handleSiteState(localName, attributes);
				break;

			default :
				internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownStartState, (new String[] { getState(currentState) })));
				break;
		}
		int newState = ((Integer) stateStack.peek()).intValue();
		if (newState != STATE_IGNORED_ELEMENT)
			currentState = newState;

	}

	/**
	 * Handle end of element tags
	 * @see DefaultHandler#endElement(String, String, String)
	 * @since 2.0
	 */
	public void endElement(String uri, String localName, String qName) {

		String text = null;
		URLEntryModel info = null;

		int state = ((Integer) stateStack.peek()).intValue();
		switch (state) {
			case STATE_IGNORED_ELEMENT :
			case STATE_ARCHIVE :
			case STATE_CATEGORY :
				stateStack.pop();
				break;

			case STATE_INITIAL :
				internalError(Messages.DefaultSiteParser_ParsingStackBackToInitialState);	
				break;

			case STATE_SITE :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					SiteModel site = (SiteModel) objectStack.peek();
					site.getDescriptionModel().setAnnotation(text);					
				}
				//do not pop the object
				break;

			case STATE_FEATURE :
				stateStack.pop();
				objectStack.pop();
				break;

			case STATE_CATEGORY_DEF :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					CategoryModel category = (CategoryModel) objectStack.peek();
					category.getDescriptionModel().setAnnotation(text);
				}
				objectStack.pop();
				break;

			case STATE_DESCRIPTION_SITE :
				stateStack.pop();
				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					// add text, preserving at most one space between text fragments
					String newText = (String) objectStack.pop();
					if (trailingSpace(newText) && !leadingSpace(text)) {
						text = " " + text; //$NON-NLS-1$
					}
					text = newText.trim() + text;
					if (leadingSpace(newText) && !leadingSpace(text)) {
						text = " " + text; //$NON-NLS-1$
					}
				}
				text = text.trim();

				info = (URLEntryModel) objectStack.pop();
				if (text != null)
					info.setAnnotation(text);

				SiteModel siteModel = (SiteModel) objectStack.peek();
				// override description.
				// do not raise error as previous description may be default one
				// when parsing site tag
				if (DESCRIPTION_SITE_ALREADY_SEEN)
					debug(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet, (new String[] { getState(state) })));
				siteModel.setDescriptionModel(info);
				DESCRIPTION_SITE_ALREADY_SEEN = true;
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
				stateStack.pop();
				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					// add text, preserving at most one space between text fragments
					String newText = (String) objectStack.pop();
					if (trailingSpace(newText) && !leadingSpace(text)) {
						text = " " + text; //$NON-NLS-1$
					}
					text = newText.trim() + text;
					if (leadingSpace(newText) && !leadingSpace(text)) {
						text = " " + text; //$NON-NLS-1$
					}
				}
				text = text.trim();

				info = (URLEntryModel) objectStack.pop();
				if (text != null)
					info.setAnnotation(text);

				CategoryModel category = (CategoryModel) objectStack.peek();
				if (category.getDescriptionModel() != null)
					internalError(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet, (new String[] { getState(state), category.getLabel() })));
				else
					category.setDescriptionModel(info);
				break;

			default :
				internalError(NLS.bind(Messages.DefaultSiteParser_UnknownEndState, (new String[] { getState(state) })));
				break;
		}

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End Element:" + uri + ":" + localName + ":" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Handle character text
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	public void characters(char[] ch, int start, int length) {
		String text = new String(ch, start, length);
		//only push if description
		int state = ((Integer) stateStack.peek()).intValue();
		if (state == STATE_DESCRIPTION_SITE || state == STATE_DESCRIPTION_CATEGORY_DEF)
			objectStack.push(text);

	}

	/**
	 * Handle errors
	 * @see DefaultHandler#error(SAXParseException)
	 * @since 2.0
	 */
	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	/**
	 * Handle fatal errors
	 * @see DefaultHandler#fatalError(SAXParseException)
	 * @exception SAXException
	 * @since 2.0
	 */
	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	private void handleInitialState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(SITE)) {
			stateStack.push(new Integer(STATE_SITE));
			processSite(attributes);
		} else {
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { elementName, getState(currentState) })));
			// what we received was not a site.xml, no need to continue
			throw new SAXException(Messages.DefaultSiteParser_InvalidXMLStream); 
		}

	}

	private void handleSiteState(String elementName, Attributes attributes) {
		if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
		} else if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { elementName, getState(currentState) }))); 			
	}

	private void handleFeatureState(String elementName, Attributes attributes) {
		if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
		} else if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(CATEGORY)) {
			stateStack.push(new Integer(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { elementName, getState(currentState) }))); 			
	}

	private void handleCategoryDefState(String elementName, Attributes attributes) {
		if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_CATEGORY_DEF));
			processInfo(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { elementName, getState(currentState) }))); 			
	}

	private void handleCategoryState(String elementName, Attributes attributes) {
		if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
		} else if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(CATEGORY)) {
			stateStack.push(new Integer(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] { elementName, getState(currentState) }))); 			
	}

	/* 
	 * process site info
	 */
	private void processSite(Attributes attributes) throws SAXException {
		// create site map
		SiteModel site = factory.createSiteMapModel();

		// if URL is specified, it replaces the URL of the site
		// used to calculate the location of features and archives
		String siteURL = attributes.getValue("url"); //$NON-NLS-1$
		if (siteURL != null && !("".equals(siteURL.trim()))) { //$NON-NLS-1$
			if (!siteURL.endsWith("/") && !siteURL.endsWith(File.separator)) { //$NON-NLS-1$
				siteURL += "/"; //$NON-NLS-1$
			}
			site.setLocationURLString(siteURL);
		}

		// provide default description URL
		// If <description> is specified, for the site,  it takes precedence		
		URLEntryModel description = factory.createURLEntryModel();
		description.setURLString(DEFAULT_INFO_URL);
		site.setDescriptionModel(description);

		// verify we can parse the site ...if the site has
		// a different type throw an exception to force reparsing
		// with the matching parser
		String type = attributes.getValue("type"); //$NON-NLS-1$
		if (!factory.canParseSiteType(type)) {
			throw new SAXException(new InvalidSiteTypeException(type));
		}
		site.setType(type);
		
		// get mirrors, if any
		String mirrorsURL = attributes.getValue("mirrorsURL"); //$NON-NLS-1$
		if (mirrorsURL != null && mirrorsURL.trim().length() > 0) {
			URLEntryModel[] mirrors = getMirrors(mirrorsURL, factory);
			if (mirrors != null)
				site.setMirrorSiteEntryModels(mirrors);
			else 
				site.setMirrorsURLString(mirrorsURL);
		}
		
		String pack200 = attributes.getValue("pack200"); //$NON-NLS-1$
		if(site instanceof ExtendedSite && pack200 != null && new Boolean(pack200).booleanValue()){
			((ExtendedSite) site).setSupportsPack200(true);
		}
		
		if ( (site instanceof ExtendedSite) && (attributes.getValue("digestURL") != null)) { //$NON-NLS-1$
			ExtendedSite extendedSite = (ExtendedSite) site;
			extendedSite.setDigestExist(true);
			extendedSite.setDigestURL(attributes.getValue("digestURL")); //$NON-NLS-1$
			
			if ( (attributes.getValue("availableLocales") != null) && (!attributes.getValue("availableLocales").trim().equals(""))) {  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				StringTokenizer locals = new StringTokenizer(attributes.getValue("availableLocales"), ",");  //$NON-NLS-1$//$NON-NLS-2$
				String[] availableLocals = new String[locals.countTokens()];
				int i = 0;
				while(locals.hasMoreTokens()) {
					availableLocals[i++] = locals.nextToken();
				}
				extendedSite.setAvailableLocals(availableLocals);
			}
		}
		
		if ( (site instanceof ExtendedSite) && (attributes.getValue("associateSitesURL") != null)) { //$NON-NLS-1$
			IURLEntry[] associateSites = getAssociateSites(attributes.getValue("associateSitesURL"), factory); //$NON-NLS-1$
			if (associateSites != null)
				((ExtendedSite)site).setAssociateSites(associateSites);
			else 
				site.setMirrorsURLString(mirrorsURL);
		}
		
		objectStack.push(site);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End process Site tag: siteURL:" + siteURL + " type:" + type);//$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process feature info
	 */
	private void processFeature(Attributes attributes) {
		SiteFeatureReferenceModel feature = factory.createFeatureReferenceModel();
		
        // feature location on the site
        String urlInfo = attributes.getValue("url"); //$NON-NLS-1$
        // identifier and version
        String id = attributes.getValue("id"); //$NON-NLS-1$
        String ver = attributes.getValue("version"); //$NON-NLS-1$
        
        boolean noURL = (urlInfo == null || urlInfo.trim().equals("")); //$NON-NLS-1$
        boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$
        boolean noVersion = (ver == null || ver.trim().equals("")); //$NON-NLS-1$
        
        // We need to have id and version, or the url, or both.
 		if (noURL) {
            if (noId || noVersion)
                internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "url", getState(currentState) })));	//$NON-NLS-1$
            else // default url
                urlInfo = FEATURES + id + '_' + ver; // 
        }
        
		feature.setURLString(urlInfo);

		String type = attributes.getValue("type"); //$NON-NLS-1$
		feature.setType(type);

		// if one is null, and not the other
		if (noId ^ noVersion) {
			String[] values = new String[] { id, ver, getState(currentState)};
			UpdateCore.warn(NLS.bind(Messages.DefaultFeatureParser_IdOrVersionInvalid, values));
		} else {
			feature.setFeatureIdentifier(id);
			feature.setFeatureVersion(ver);
		}

		// get label if it exists
		String label = attributes.getValue("label"); //$NON-NLS-1$
		if (label != null) {
			if ("".equals(label.trim())) //$NON-NLS-1$
				label = null;
		}
		feature.setLabel(label);

		// OS
		String os = attributes.getValue("os"); //$NON-NLS-1$
		feature.setOS(os);

		// WS
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		feature.setWS(ws);

		// NL
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		feature.setNL(nl);

		// arch
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		feature.setArch(arch);

		//patch
		String patch = attributes.getValue("patch"); //$NON-NLS-1$
		feature.setPatch(patch);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addFeatureReferenceModel(feature);
		feature.setSiteModel(site);

		objectStack.push(feature);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End Processing DefaultFeature Tag: url:" + urlInfo + " type:" + type); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process archive info
	 */
	private void processArchive(Attributes attributes) {
		ArchiveReferenceModel archive = factory.createArchiveReferenceModel();
		String id = attributes.getValue("path"); //$NON-NLS-1$
		if (id == null || id.trim().equals("")) { //$NON-NLS-1$
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "path", getState(currentState) }))); //$NON-NLS-1$
		}

		archive.setPath(id);

		String url = attributes.getValue("url"); //$NON-NLS-1$
		if (url == null || url.trim().equals("")) { //$NON-NLS-1$
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "archive", getState(currentState) })));	//$NON-NLS-1$
		} else {
			archive.setURLString(url);

			SiteModel site = (SiteModel) objectStack.peek();
			site.addArchiveReferenceModel(archive);
		}
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End processing Archive: path:" + id + " url:" + url);//$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process the Category  info
	 */
	private void processCategory(Attributes attributes) {
		String category = attributes.getValue("name"); //$NON-NLS-1$
		SiteFeatureReferenceModel feature = (SiteFeatureReferenceModel) objectStack.peek();
		feature.addCategoryName(category);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End processing Category: name:" + category); //$NON-NLS-1$
	}

	/* 
	 * process category def info
	 */
	private void processCategoryDef(Attributes attributes) {
		CategoryModel category = factory.createSiteCategoryModel();
		String name = attributes.getValue("name"); //$NON-NLS-1$
		String label = attributes.getValue("label"); //$NON-NLS-1$
		category.setName(name);
		category.setLabel(label);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addCategoryModel(category);
		objectStack.push(category);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End processing CategoryDef: name:" + name + " label:" + label); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* 
	 * process URL info with element text
	 */
	private void processInfo(Attributes attributes) {
		URLEntryModel inf = factory.createURLEntryModel();
		String infoURL = attributes.getValue("url"); //$NON-NLS-1$
		inf.setURLString(infoURL);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Processed Info: url:" + infoURL); //$NON-NLS-1$

		objectStack.push(inf);
	}

	/*
	 * 
	 */
	private static void debug(String s) {
		UpdateCore.debug("DefaultSiteParser" + s); //$NON-NLS-1$
	}

	/*
	 * 
	 */
	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = ""; //$NON-NLS-1$
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$

		String msg;
		if (name.equals("")) //$NON-NLS-1$
			msg = NLS.bind(Messages.DefaultSiteParser_ErrorParsing, (new String[] { ex.getMessage() }));
		else {
			String[] values = new String[] { name, Integer.toString(ex.getLineNumber()), Integer.toString(ex.getColumnNumber()), ex.getMessage()};
			msg = NLS.bind(Messages.DefaultSiteParser_ErrorlineColumnMessage, values);
		}
		error(new Status(IStatus.ERROR, PLUGIN_ID, Platform.PARSE_PROBLEM, msg, ex));
	}

	/*
	 * Handles an error state specified by the status.  The collection of all logged status
	 * objects can be accessed using <code>getStatus()</code>.
	 *
	 * @param error a status detailing the error condition
	 */
	private void error(IStatus error) {

		if (status == null) {
			status = new MultiStatus(PLUGIN_ID, Platform.PARSE_PROBLEM, Messages.DefaultSiteParser_ErrorParsingSite, null);
		}

		status.add(error);
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			UpdateCore.log(error);
	}

	/*
	 * 
	 */
	private void internalErrorUnknownTag(String msg) {
		stateStack.push(new Integer(STATE_IGNORED_ELEMENT));
		internalError(msg);
	}

	/*
	 * 
	 */
	private void internalError(String message) {
		error(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, message, null));
	}

	/*
	 * return the state as String
	 */
	private String getState(int state) {

		switch (state) {
			case STATE_IGNORED_ELEMENT :
				return "Ignored"; //$NON-NLS-1$

			case STATE_INITIAL :
				return "Initial"; //$NON-NLS-1$

			case STATE_SITE :
				return "Site"; //$NON-NLS-1$

			case STATE_FEATURE :
				return "Feature"; //$NON-NLS-1$

			case STATE_ARCHIVE :
				return "Archive"; //$NON-NLS-1$

			case STATE_CATEGORY :
				return "Category"; //$NON-NLS-1$

			case STATE_CATEGORY_DEF :
				return "Category Def"; //$NON-NLS-1$

			case STATE_DESCRIPTION_CATEGORY_DEF :
				return "Description / Category Def"; //$NON-NLS-1$

			case STATE_DESCRIPTION_SITE :
				return "Description / Site"; //$NON-NLS-1$

			default :
				return Messages.DefaultSiteParser_UnknownState; 
		}
	}
	private boolean leadingSpace(String str) {
		if (str.length() <= 0) {
			return false;
		}
		return Character.isWhitespace(str.charAt(0));
	}
	private boolean trailingSpace(String str) {
		if (str.length() <= 0) {
			return false;
		}
		return Character.isWhitespace(str.charAt(str.length() - 1));
	}
	
	static URLEntryModel[] getMirrors(String mirrorsURL, SiteModelFactory factory) {
	    
		try {
			String countryCode = Locale.getDefault().getCountry().toLowerCase();
			int timeZone = (new GregorianCalendar()).get(Calendar.ZONE_OFFSET)/(60*60*1000);

			if (mirrorsURL.indexOf("?") != -1) { //$NON-NLS-1$
				mirrorsURL = mirrorsURL + "&"; //$NON-NLS-1$
			} else {
				mirrorsURL = mirrorsURL + "?"; //$NON-NLS-1$
			}			
			mirrorsURL = mirrorsURL + "countryCode=" + countryCode + "&timeZone=" + timeZone + "&responseType=xml"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		    DocumentBuilderFactory domFactory = 
		    DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document document = builder.parse(mirrorsURL);
		    if (document == null)
		    	return null;
		    NodeList mirrorNodes = document.getElementsByTagName(MIRROR); 
		    URLEntryModel[] mirrors = new URLEntryModel[mirrorNodes.getLength()];
		    for (int i=0; i<mirrorNodes.getLength(); i++) {
		    	Element mirrorNode = (Element)mirrorNodes.item(i);
				mirrors[i] = factory.createURLEntryModel();
				String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
				String label = mirrorNode.getAttribute("label"); //$NON-NLS-1$
				mirrors[i].setURLString(infoURL);
				mirrors[i].setAnnotation(label);

				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
					debug("Processed mirror: url:" + infoURL + " label:" + label); //$NON-NLS-1$ //$NON-NLS-2$
		    }
		    return mirrors;
		}
		catch (Exception e) {
		    // log if absolute url
		    if (mirrorsURL != null &&
		    		(mirrorsURL.startsWith("http://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("https://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("file://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("ftp://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("jar://"))) //$NON-NLS-1$
		    	UpdateCore.log(Messages.DefaultSiteParser_mirrors, e);
			return null;
		}
	}
	
	private static IURLEntry[] getAssociateSites(String associateSitesURL, SiteModelFactory factory) {
	    
		try {
		    DocumentBuilderFactory domFactory = 
		    DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document document = builder.parse(associateSitesURL);
		    if (document == null)
		    	return null;
		    NodeList mirrorNodes = document.getElementsByTagName(ASSOCIATE_SITE); 
		    URLEntry[] mirrors = new URLEntry[mirrorNodes.getLength()];
		    for (int i=0; i<mirrorNodes.getLength(); i++) {
		    	Element mirrorNode = (Element)mirrorNodes.item(i);
				mirrors[i] = new URLEntry();
				String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
				String label = mirrorNode.getAttribute("label"); //$NON-NLS-1$
				mirrors[i].setURLString(infoURL);
				mirrors[i].setAnnotation(label);

				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
					debug("Processed mirror: url:" + infoURL + " label:" + label); //$NON-NLS-1$ //$NON-NLS-2$
		    }
		    return mirrors;
		}
		catch (Exception e) {
		    // log if absolute url
		    if (associateSitesURL != null &&
		    		(associateSitesURL.startsWith("http://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("https://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("file://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("ftp://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("jar://"))) //$NON-NLS-1$
		    	UpdateCore.log(Messages.DefaultSiteParser_mirrors, e);
			return null;
		}
	}
	
}
