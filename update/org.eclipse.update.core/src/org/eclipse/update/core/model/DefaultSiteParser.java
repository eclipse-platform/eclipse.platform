package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.util.Iterator;
import java.util.Stack;

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;
import org.xml.sax.*;
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
 * @since 2.0
 */
public class DefaultSiteParser extends DefaultHandler {

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
	private static final String PLUGIN_ID =
		UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();

	private static final String SITE = "site"; //$NON-NLS-1$
	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String ARCHIVE = "archive"; //$NON-NLS-1$
	private static final String CATEGORY_DEF = "category-def"; //$NON-NLS-1$
	private static final String CATEGORY = "category"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$

	private static final String DEFAULT_INFO_URL = "index.html"; //$NON-NLS-1$

	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are
	// populating in this plugin descriptor
	Stack objectStack = new Stack();

	private int currentState;

	/**
	 * Constructs a site parser.
	 * 
	 * @param factory site model factory
	 */
	public DefaultSiteParser(SiteModelFactory factory) {
		super();
		this.parser = new SAXParser();
		this.parser.setContentHandler(this);
		this.parser.setErrorHandler(this); // 18350
		this.factory = factory;

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("Created"); //$NON-NLS-1$
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
		parser.parse(new InputSource(in));
		if (objectStack.isEmpty())
			throw new SAXException(Policy.bind("DefaultSiteParser.NoSiteTag"));
		//$NON-NLS-1$
		else {
			if (objectStack.peek() instanceof SiteModel) {
				return (SiteModel) objectStack.pop();
			} else {
				String stack = ""; //$NON-NLS-1$
				Iterator iter = objectStack.iterator();
				while (iter.hasNext()) {
					stack = stack + iter.next().toString() + "\r\n"; //$NON-NLS-1$
				}
				throw new SAXException(
					Policy.bind("DefaultSiteParser.WrongParsingStack", stack));
				//$NON-NLS-1$
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
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			debug("State: " + currentState); //$NON-NLS-1$
			debug(
				"Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName);
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		switch (currentState) {
			case STATE_IGNORED_ELEMENT :
				internalErrorUnknownTag(
					Policy.bind(
						"DefaultSiteParser.UnknownElement",
						localName,
						getState(currentState)));
				//$NON-NLS-1$
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
				handleArchiveState(localName, attributes);
				break;

			case STATE_CATEGORY :
				handleCategoryState(localName, attributes);
				break;

			case STATE_CATEGORY_DEF :
				handleCategoryDefState(localName, attributes);
				break;

			case STATE_DESCRIPTION_SITE :
				handleDescriptionSiteState(localName, attributes);
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
				handleDescriptionCategoryDefState(localName, attributes);
				break;

			default :
				internalErrorUnknownTag(
					Policy.bind("DefaultSiteParser.UnknownStartState", getState(currentState)));
				//$NON-NLS-1$
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
				internalError(Policy.bind("DefaultSiteParser.ParsingStackBackToInitialState"));
				//$NON-NLS-1$
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
					text = (String) objectStack.pop() + text;
				}

				info = (URLEntryModel) objectStack.pop();
				if (text != null)
					info.setAnnotation(text);

				SiteModel siteModel = (SiteModel) objectStack.peek();
				// override description.
				// do not raise error as previous description may be default one
				// when parsing site tag
				if (DESCRIPTION_SITE_ALREADY_SEEN)
					debug(Policy.bind("DefaultSiteParser.ElementAlreadySet", getState(state)));
				//$NON-NLS-1$
				siteModel.setDescriptionModel(info);
				DESCRIPTION_SITE_ALREADY_SEEN = true;
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
				stateStack.pop();
				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop() + text;
				}

				info = (URLEntryModel) objectStack.pop();
				if (text != null)
					info.setAnnotation(text);

				CategoryModel category = (CategoryModel) objectStack.peek();
				if (category.getDescriptionModel() != null)
					internalError(
						Policy.bind(
							"DefaultSiteParser.ElementAlreadySet2",
							getState(state),
							category.getLabel()));
				//$NON-NLS-1$
				else
					category.setDescriptionModel(info);
				break;

			default :
				internalError(
					Policy.bind("DefaultSiteParser.UnknownEndState", getState(state)));
				//$NON-NLS-1$
				break;
		}

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("End Element:" + uri + ":" + localName + ":" + qName);
		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Handle character text
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	public void characters(char[] ch, int start, int length) {
		String text = new String(ch, start, length).trim();
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

	private void handleInitialState(String elementName, Attributes attributes)
		throws SAXException {
		if (elementName.equals(SITE)) {
			stateStack.push(new Integer(STATE_SITE));
			processSite(attributes);
		} else {
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
			//$NON-NLS-1$
			// what we received was not a site.xml, no need to continue
			throw new SAXException(Policy.bind("DefaultSiteParser.InvalidXMLStream")); //$NON-NLS-1$
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
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}

	private void handleFeatureState(String elementName, Attributes attributes) {
		if (elementName.equals(FEATURE)) {
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
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}
	
	private void handleArchiveState(String elementName, Attributes attributes) {
		if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}
	
	private void handleCategoryState(String elementName, Attributes attributes) {
		if (elementName.equals(FEATURE)) {
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
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}
	
	private void handleCategoryDefState(
		String elementName,
		Attributes attributes) {
		if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_CATEGORY_DEF));
			processInfo(attributes);
		} else
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}
	
	private void handleDescriptionSiteState(
		String elementName,
		Attributes attributes) {
		if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(new Integer(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
	}
	
	private void handleDescriptionCategoryDefState(
		String elementName,
		Attributes attributes) {
		if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(new Integer(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION_CATEGORY_DEF));
			processInfo(attributes);
		} else
			internalErrorUnknownTag(
				Policy.bind(
					"DefaultSiteParser.UnknownElement",
					elementName,
					getState(currentState)));
		//$NON-NLS-1$ 			
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
			if (!siteURL.endsWith("/")
				&& !siteURL.endsWith(File.separator)) { //$NON-NLS-1$
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
		objectStack.push(site);

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("End process Site tag: siteURL:" + siteURL + " type:" + type);
		//$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process feature info
	 */
	private void processFeature(Attributes attributes) {
		SiteFeatureReferenceModel feature = factory.createFeatureReferenceModel();
		String urlInfo = attributes.getValue("url"); //$NON-NLS-1$
		if (urlInfo == null || urlInfo.trim().equals("")) //$NON-NLS-1$
			internalError(
				Policy.bind("DefaultSiteParser.Missing", "url", getState(currentState)));
		//$NON-NLS-1$  //$NON-NLS-2$

		feature.setURLString(urlInfo);

		String type = attributes.getValue("type"); //$NON-NLS-1$
		feature.setType(type);

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		// if one is null, and not the other
		if ((id == null || id.trim().equals("")) //$NON-NLS-1$
			^ (ver == null || ver.trim().equals(""))) { //$NON-NLS-1$
				String[] values = new String[] { id, ver, getState(currentState)};
				UpdateManagerPlugin.warn(Policy.bind("DefaultFeatureParser.IdOrVersionInvalid",values));
				//$NON-NLS-1$
		} else {
			feature.setFeatureIdentifier(id);
			feature.setFeatureVersion(ver);
		}
		
		SiteModel site = (SiteModel) objectStack.peek();
		site.addFeatureReferenceModel(feature);
		feature.setSiteModel(site);

		objectStack.push(feature);

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("End Processing DefaultFeature Tag: url:" + urlInfo + " type:" + type);
		//$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process archive info
	 */
	private void processArchive(Attributes attributes) {
		ArchiveReferenceModel archive = factory.createArchiveReferenceModel();
		String id = attributes.getValue("path"); //$NON-NLS-1$
		if (id == null || id.trim().equals("")) { //$NON-NLS-1$
			internalError(
				Policy.bind("DefaultSiteParser.Missing", "path", getState(currentState)));
			//$NON-NLS-1$  //$NON-NLS-2$
		}

		archive.setPath(id);

		String url = attributes.getValue("url"); //$NON-NLS-1$
		if (url == null || url.trim().equals("")) { //$NON-NLS-1$
			internalError(
				Policy.bind("DefaultSiteParser.Missing", "archive", getState(currentState)));
			//$NON-NLS-1$  //$NON-NLS-2$
		} else {
			archive.setURLString(url);

			SiteModel site = (SiteModel) objectStack.peek();
			site.addArchiveReferenceModel(archive);
		}
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("End processing Archive: path:" + id + " url:" + url);
		//$NON-NLS-1$ //$NON-NLS-2$

	}

	/* 
	 * process the Category  info
	 */
	private void processCategory(Attributes attributes) {
		String category = attributes.getValue("name"); //$NON-NLS-1$
		SiteFeatureReferenceModel feature = (SiteFeatureReferenceModel) objectStack.peek();
		feature.addCategoryName(category);

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
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

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("End processing CategoryDef: name:" + name + " label:" + label);
		//$NON-NLS-1$ //$NON-NLS-2$
	}

	/* 
	 * process URL info with element text
	 */
	private void processInfo(Attributes attributes) {
		URLEntryModel inf = factory.createURLEntryModel();
		String infoURL = attributes.getValue("url"); //$NON-NLS-1$
		inf.setURLString(infoURL);

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			debug("Processed Info: url:" + infoURL); //$NON-NLS-1$

		objectStack.push(inf);
	}

	/*
	 * 
	 */
	private void debug(String s) {
		UpdateManagerPlugin.debug("DefaultSiteParser"+s);
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
			msg = Policy.bind("DefaultSiteParser.ErrorParsing", ex.getMessage());
		//$NON-NLS-1$
		else {
			String[] values =
				new String[] {
					name,
					Integer.toString(ex.getLineNumber()),
					Integer.toString(ex.getColumnNumber()),
					ex.getMessage()};
			msg = Policy.bind("DefaultSiteParser.ErrorLineColumnMessage", values);
			//$NON-NLS-1$
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
			status =
				new MultiStatus(
					PLUGIN_ID,
					Platform.PARSE_PROBLEM,
					Policy.bind("DefaultSiteParser.ErrorParsingSite"),
					null);
			//$NON-NLS-1$
		}

		status.add(error);
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING)
			UpdateManagerPlugin.log(error);
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
				return Policy.bind("DefaultSiteParser.UnknownState"); //$NON-NLS-1$
		}
	}
}