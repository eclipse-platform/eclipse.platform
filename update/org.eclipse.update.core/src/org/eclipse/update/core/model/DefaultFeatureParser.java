/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;


import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Default feature parser.
 * Parses the feature manifest file as defined by the platform. Defers
 * to a model factory to create the actual concrete model objects. The 
 * update framework supplies two factory implementations:
 * <ul>
 * <li>@see org.eclipse.update.core.model.FeatureModelFactory
 * <li>@see org.eclipse.update.core.BaseFeatureFactory
 * </ul>
 * 
 * @since 2.0
 */
public class DefaultFeatureParser extends DefaultHandler {

	private SAXParser parser;
	private FeatureModelFactory factory;
	private MultiStatus status;

	private boolean URL_ALREADY_SEEN = false;

	private static final int STATE_IGNORED_ELEMENT = -1;
	private static final int STATE_INITIAL = 0;
	private static final int STATE_INCLUDES = 1;
	private static final int STATE_FEATURE = 2;
	private static final int STATE_HANDLER = 3;
	private static final int STATE_DESCRIPTION = 4;
	private static final int STATE_COPYRIGHT = 5;
	private static final int STATE_LICENSE = 6;
	private static final int STATE_URL = 7;
	private static final int STATE_UPDATE = 8;
	private static final int STATE_DISCOVERY = 9;
	private static final int STATE_REQUIRES = 10;
	private static final int STATE_IMPORT = 11;
	private static final int STATE_PLUGIN = 12;
	private static final int STATE_DATA = 13;
	private static final String PLUGIN_ID = UpdateCore.getPlugin().getBundle().getSymbolicName();

	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String INCLUDES = "includes"; //$NON-NLS-1$
	private static final String HANDLER = "install-handler"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String COPYRIGHT = "copyright"; //$NON-NLS-1$
	private static final String LICENSE = "license"; //$NON-NLS-1$
	private static final String URL = "url"; //$NON-NLS-1$
	private static final String UPDATE = "update"; //$NON-NLS-1$
	private static final String DISCOVERY = "discovery"; //$NON-NLS-1$
	private static final String REQUIRES = "requires"; //$NON-NLS-1$
	private static final String IMPORT = "import"; //$NON-NLS-1$
	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String DATA = "data"; //$NON-NLS-1$
	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are
	// populating in this plugin descriptor
	Stack objectStack = new Stack();

	private int currentState;
	
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();

	/**
	 * Constructs a feature parser.
	 * 
	 * @param factory feature model factory
	 * @since 2.0
	 */
	public DefaultFeatureParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			UpdateCore.log(e);
		} catch (SAXException e) {
			UpdateCore.log(e);
		}
	}

	public void init(FeatureModelFactory factory) {
		// PERF: separate instance creation from parsing
		this.factory = factory;
		stateStack = new Stack();
		objectStack = new Stack();
		status = null;
		URL_ALREADY_SEEN = false;
		//parser.reset();
	}

	/**
	 * Parses the specified input steam and constructs a feature model.
	 * The input stream is not closed as part of this operation.
	 * 
	 * @param in input stream
	 * @return feature model
	 * @exception SAXException
	 * @exception IOException
	 * @since 2.0
	 */
	public FeatureModel parse(InputStream in) throws SAXException, IOException {
		stateStack.push(new Integer(STATE_INITIAL));
		currentState = ((Integer) stateStack.peek()).intValue();
		parser.parse(new InputSource(in), this);
		if (objectStack.isEmpty())
			throw new SAXException(Policy.bind("DefaultFeatureParser.NoFeatureTag"));
		//$NON-NLS-1$
		else {
			if (objectStack.peek() instanceof FeatureModel) {
				return (FeatureModel) objectStack.pop();
			} else {
				String stack = ""; //$NON-NLS-1$
				Iterator iter = objectStack.iterator();
				while (iter.hasNext()) {
					stack = "\r\n" + iter.next().toString() + stack; //$NON-NLS-1$
				}
				throw new SAXException(Policy.bind("DefaultFeatureParser.WrongParsingStack", stack));
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
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName);
		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		switch (currentState) {
			case STATE_IGNORED_ELEMENT :
				internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", localName, getState(currentState)));
				//$NON-NLS-1$
				break;

			case STATE_INITIAL :
				handleInitialState(localName, attributes);
				break;

			case STATE_FEATURE :
			case STATE_INCLUDES :
			case STATE_HANDLER :
			case STATE_DESCRIPTION :
			case STATE_COPYRIGHT :
			case STATE_LICENSE :
				handleFeatureState(localName, attributes);
				break;

			case STATE_URL :
				if (URL_ALREADY_SEEN)
					internalError(Policy.bind("DefaultFeatureParser.TooManyURLtag")); //$NON-NLS-1$
				handleURLState(localName, attributes);
				break;

			case STATE_UPDATE :
			case STATE_DISCOVERY :
				handleUpdateDiscoveryState(localName, attributes);
				break;

			case STATE_REQUIRES :
				handleRequiresState(localName, attributes);
				break;

			case STATE_IMPORT :
				handleImportState(localName,attributes);
				break;
				
			case STATE_PLUGIN :
			case STATE_DATA :
				handleFeatureState(localName, attributes);
				break;

			default :
				internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownStartState", Integer.toString(currentState)));
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

		// variables used
		URLEntryModel info = null;
		FeatureModel featureModel = null;
		String text = null;
		int innerState = 0;

		int state = ((Integer) stateStack.peek()).intValue();
		switch (state) {
			case STATE_IGNORED_ELEMENT :
				stateStack.pop();
				break;

			case STATE_INITIAL :
				internalError(Policy.bind("DefaultFeatureParser.ParsingStackBackToInitialState"));
				//$NON-NLS-1$
				break;

			case STATE_FEATURE :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					FeatureModel feature = (FeatureModel) objectStack.peek();
					feature.getDescriptionModel().setAnnotation(text);
				}
				//do not pop
				break;

			case STATE_INCLUDES :
				stateStack.pop();
				if (objectStack.peek() instanceof IncludedFeatureReferenceModel) {
					IncludedFeatureReferenceModel includedFeatureRefModel = ((IncludedFeatureReferenceModel) objectStack.pop());
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						featureModel.addIncludedFeatureReferenceModel(includedFeatureRefModel);
					}
				}
				break;

			case STATE_HANDLER :
				stateStack.pop();
				if (objectStack.peek() instanceof InstallHandlerEntryModel) {
					InstallHandlerEntryModel handlerModel = (InstallHandlerEntryModel) objectStack.pop();
					featureModel = (FeatureModel) objectStack.peek();
					if (featureModel.getInstallHandlerModel() != null)
						internalError(Policy.bind("DefaultFeatureParser.ElementAlreadySet", getState(state)));
					//$NON-NLS-1$
					//$NON-NLS-1$
					else
						featureModel.setInstallHandlerModel(handlerModel);
				}
				break;

			case STATE_DESCRIPTION :
				stateStack.pop();

				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop() + text;
				}
				if (objectStack.peek() instanceof URLEntryModel) {
					info = (URLEntryModel) objectStack.pop();
					text = cleanupText(text);
					if (text != null)
						info.setAnnotation(text);

					innerState = ((Integer) stateStack.peek()).intValue();
					switch (innerState) {
						case STATE_FEATURE :
							if (objectStack.peek() instanceof FeatureModel) {
								featureModel = (FeatureModel) objectStack.peek();
								if (featureModel.getDescriptionModel() != null)
									internalError(Policy.bind("DefaultFeatureParser.ElementAlreadySet", getState(state)));
								//$NON-NLS-1$
								else
									featureModel.setDescriptionModel(info);
							}
							break;

						default :
							internalError(Policy.bind("DefaultFeatureParser.StateIncludeWrongElement", getState(innerState), getState(state)));
							//$NON-NLS-1$
							//$NON-NLS-1$
							break;

					}
				}
				break;

			case STATE_COPYRIGHT :
				stateStack.pop();
				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop() + text;
				}
				if (objectStack.peek() instanceof URLEntryModel) {
					info = (URLEntryModel) objectStack.pop();
					text = cleanupText(text);
					if (text != null) {
						info.setAnnotation(text);
					}

					innerState = ((Integer) stateStack.peek()).intValue();
					switch (innerState) {
						case STATE_FEATURE :
							if (objectStack.peek() instanceof FeatureModel) {
								featureModel = (FeatureModel) objectStack.peek();
								if (featureModel.getCopyrightModel() != null)
									internalError(Policy.bind("DefaultFeatureParser.ElementAlreadySet", getState(state)));
								//$NON-NLS-1$
								else
									featureModel.setCopyrightModel(info);
							}
							break;

						default :
							internalError(Policy.bind("DefaultFeatureParser.StateIncludeWrongElement", getState(innerState), getState(state)));
							//$NON-NLS-1$
							break;

					}
				}
				break;

			case STATE_LICENSE :
				stateStack.pop();

				text = ""; //$NON-NLS-1$
				while (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop() + text;
				}
				if (objectStack.peek() instanceof URLEntryModel) {
					info = (URLEntryModel) objectStack.pop();
					text = cleanupText(text);
					if (text != null) {
						info.setAnnotation(text);
					}

					innerState = ((Integer) stateStack.peek()).intValue();
					switch (innerState) {
						case STATE_FEATURE :
							if (objectStack.peek() instanceof FeatureModel) {
								featureModel = (FeatureModel) objectStack.peek();
								if (featureModel.getLicenseModel() != null)
									internalError(Policy.bind("DefaultFeatureParser.ElementAlreadySet", getState(state)));
								//$NON-NLS-1$
								else
									featureModel.setLicenseModel(info);
							}
							break;

						default :
							internalError(Policy.bind("DefaultFeatureParser.StateIncludeWrongElement", getState(innerState), getState(state)));
							//$NON-NLS-1$
							break;

					}
				}
				break;

			case STATE_URL :
				stateStack.pop();
				URL_ALREADY_SEEN = true;
				break;

			case STATE_UPDATE :
				stateStack.pop();
				if (objectStack.peek() instanceof URLEntryModel) {
					info = (URLEntryModel) objectStack.pop();
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						if (featureModel.getUpdateSiteEntryModel() != null) {
							internalError(Policy.bind("DefaultFeatureParser.ElementAlreadySet", getState(state)));
							//$NON-NLS-1$
						} else {
							featureModel.setUpdateSiteEntryModel(info);
						}
					}
				}
				break;

			case STATE_DISCOVERY :
				stateStack.pop();
				if (objectStack.peek() instanceof URLEntryModel) {
					info = (URLEntryModel) objectStack.pop();
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						featureModel.addDiscoverySiteEntryModel(info);
					}
				}
				break;

			case STATE_REQUIRES :
				stateStack.pop();
				if (objectStack.peek() instanceof FeatureModel) {
					featureModel = (FeatureModel) objectStack.peek();
					ImportModel[] importModels = featureModel.getImportModels();
					if (importModels.length == 0) {
						internalError(Policy.bind("DefaultFeatureParser.RequireStateWithoutImportElement"));
						//$NON-NLS-1$
						//$NON-NLS-1$
					} else {
						boolean patchMode = false;
						for (int i = 0; i < importModels.length; i++) {
							ImportModel importModel = importModels[i];
							if (importModel.isPatch()) {
								if (patchMode == false)
									patchMode = true;
								else {
									internalError(Policy.bind("DefaultFeatureParser.MultiplePatchImports"));
									break;
								}
							}
						}
					}
				}
				break;

			case STATE_IMPORT :
				stateStack.pop();
				if (objectStack.peek() instanceof ImportModel) {
					ImportModel importModel = (ImportModel) objectStack.pop();
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						featureModel.addImportModel(importModel);
					}
				}
				break;

			case STATE_PLUGIN :
				stateStack.pop();
				if (objectStack.peek() instanceof PluginEntryModel) {
					PluginEntryModel pluginEntry = (PluginEntryModel) objectStack.pop();
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						featureModel.addPluginEntryModel(pluginEntry);
					}
				}
				break;

			case STATE_DATA :
				stateStack.pop();
				if (objectStack.peek() instanceof NonPluginEntryModel) {
					NonPluginEntryModel nonPluginEntry = (NonPluginEntryModel) objectStack.pop();
					if (objectStack.peek() instanceof FeatureModel) {
						featureModel = (FeatureModel) objectStack.peek();
						featureModel.addNonPluginEntryModel(nonPluginEntry);
					}
				}
				break;

			default :
				internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownEndState") + state);
				//$NON-NLS-1$
				//$NON-NLS-1$
				break;

		}

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("End Element:" + uri + ":" + localName + ":" + qName);
		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/*
	 * Method cleanupText.
	 * Removes pre white space and post white space
	 * return null if the text only contains whitespaces (\t \r\n and spaces)
	 * 
	 * @param text or null
	 * @return String
	 */
	private String cleanupText(String text) {
		text = text.trim();
		if ("".equals(text)) return null;
		return text;
	}

	/**
	 * Handle character text
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	public void characters(char[] ch, int start, int length) {
		String text = "";
		boolean valid = true;

		if (valid) {
			text = new String(ch, start, length);
		}

		//only push if not unknown state
		int state = ((Integer) stateStack.peek()).intValue();
		if (state == STATE_DESCRIPTION || state == STATE_COPYRIGHT || state == STATE_LICENSE)
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
		if (elementName.equals(FEATURE)) {
			stateStack.push(new Integer(STATE_FEATURE));
			processFeature(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}

	private void handleFeatureState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(HANDLER)) {
			stateStack.push(new Integer(STATE_HANDLER));
			processHandler(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION));
			processInfo(attributes);
		} else if (elementName.equals(COPYRIGHT)) {
			stateStack.push(new Integer(STATE_COPYRIGHT));
			processInfo(attributes);
		} else if (elementName.equals(LICENSE)) {
			stateStack.push(new Integer(STATE_LICENSE));
			processInfo(attributes);
		} else if (elementName.equals(URL)) {
			stateStack.push(new Integer(STATE_URL));
			//No process as URL tag does not contain any element itself
		} else if (elementName.equals(INCLUDES)) {
			stateStack.push(new Integer(STATE_INCLUDES));
			processIncludes(attributes);
		} else if (elementName.equals(REQUIRES)) {
			stateStack.push(new Integer(STATE_REQUIRES));
			processRequire(attributes);
		} else if (elementName.equals(PLUGIN)) {
			stateStack.push(new Integer(STATE_PLUGIN));
			processPlugin(attributes);
		} else if (elementName.equals(DATA)) {
			stateStack.push(new Integer(STATE_DATA));
			processData(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}

	private void handleURLState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(UPDATE)) {
			stateStack.push(new Integer(STATE_UPDATE));
			processURLInfo(attributes);
		} else if (elementName.equals(DISCOVERY)) {
			stateStack.push(new Integer(STATE_DISCOVERY));
			processURLInfo(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}

	private void handleRequiresState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(IMPORT)) {
			stateStack.push(new Integer(STATE_IMPORT));
			processImport(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}
	private void handleUpdateDiscoveryState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(HANDLER)) {
			stateStack.push(new Integer(STATE_HANDLER));
			processHandler(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION));
			processInfo(attributes);
		} else if (elementName.equals(COPYRIGHT)) {
			stateStack.push(new Integer(STATE_COPYRIGHT));
			processInfo(attributes);
		} else if (elementName.equals(LICENSE)) {
			stateStack.push(new Integer(STATE_LICENSE));
			processInfo(attributes);
		} else if (elementName.equals(URL)) {
			stateStack.push(new Integer(STATE_URL));
			//No process as URL tag does not contain any element itself
		} else if (elementName.equals(INCLUDES)) {
			stateStack.push(new Integer(STATE_INCLUDES));
			processIncludes(attributes);
		} else if (elementName.equals(REQUIRES)) {
			stateStack.push(new Integer(STATE_REQUIRES));
			processRequire(attributes);
		} else if (elementName.equals(PLUGIN)) {
			stateStack.push(new Integer(STATE_PLUGIN));
			processPlugin(attributes);
		} else if (elementName.equals(DATA)) {
			stateStack.push(new Integer(STATE_DATA));
			processData(attributes);
		} else if (elementName.equals(UPDATE)) {
			stateStack.push(new Integer(STATE_UPDATE));
			processURLInfo(attributes);
		} else if (elementName.equals(DISCOVERY)) {
			stateStack.push(new Integer(STATE_DISCOVERY));
			processURLInfo(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}



	private void handleImportState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(HANDLER)) {
			stateStack.push(new Integer(STATE_HANDLER));
			processHandler(attributes);
		} else if (elementName.equals(DESCRIPTION)) {
			stateStack.push(new Integer(STATE_DESCRIPTION));
			processInfo(attributes);
		} else if (elementName.equals(COPYRIGHT)) {
			stateStack.push(new Integer(STATE_COPYRIGHT));
			processInfo(attributes);
		} else if (elementName.equals(LICENSE)) {
			stateStack.push(new Integer(STATE_LICENSE));
			processInfo(attributes);
		} else if (elementName.equals(URL)) {
			stateStack.push(new Integer(STATE_URL));
			//No process as URL tag does not contain any element itself
		} else if (elementName.equals(INCLUDES)) {
			stateStack.push(new Integer(STATE_INCLUDES));
			processIncludes(attributes);
		} else if (elementName.equals(REQUIRES)) {
			stateStack.push(new Integer(STATE_REQUIRES));
			processRequire(attributes);
		} else if (elementName.equals(PLUGIN)) {
			stateStack.push(new Integer(STATE_PLUGIN));
			processPlugin(attributes);
		} else if (elementName.equals(DATA)) {
			stateStack.push(new Integer(STATE_DATA));
			processData(attributes);
		} else if (elementName.equals(IMPORT)) {
			stateStack.push(new Integer(STATE_IMPORT));
			processImport(attributes);
		} else
			internalErrorUnknownTag(Policy.bind("DefaultFeatureParser.UnknownElement", elementName, getState(currentState)));
		//$NON-NLS-1$
	}

	/*
	 * Process feature information
	 */
	private void processFeature(Attributes attributes) {

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().equals("") //$NON-NLS-1$
		|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			internalError(Policy.bind("DefaultFeatureParser.IdOrVersionInvalid", new String[] { id, ver, getState(currentState)}));
			//$NON-NLS-1$
		} else {
			// create feature model
			FeatureModel feature = factory.createFeatureModel();

			feature.setFeatureIdentifier(id);
			feature.setFeatureVersion(ver);

			// label
			String label = attributes.getValue("label"); //$NON-NLS-1$
			feature.setLabel(label);

			// provider
			String provider = attributes.getValue("provider-name"); //$NON-NLS-1$
			feature.setProvider(provider);

			//image
			String imageURL = attributes.getValue("image"); //$NON-NLS-1$
			feature.setImageURLString(imageURL);

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

			// primary
			String primary = attributes.getValue("primary"); //$NON-NLS-1$
			feature.setPrimary(primary != null && primary.trim().equalsIgnoreCase("true"));

			// exclusive
			String exclusive = attributes.getValue("exclusive"); //$NON-NLS-1$
			feature.setExclusive(exclusive != null && exclusive.trim().equalsIgnoreCase("true"));
			//$NON-NLS-1$

			// application
			String application = attributes.getValue("application"); //$NON-NLS-1$
			feature.setApplication(application);

			// affinity
			String affinity = attributes.getValue("colocation-affinity"); //$NON-NLS-1$
			feature.setAffinityFeature(affinity);

			// primary plugin
			String plugin = attributes.getValue("plugin");
			feature.setPrimaryPluginID(plugin);

			objectStack.push(feature);

			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
				debug("End process DefaultFeature tag: id:" //$NON-NLS-1$
				+id + " ver:" //$NON-NLS-1$
				+ver + " label:" //$NON-NLS-1$
				+label + " provider:" //$NON-NLS-1$
				+provider);
				debug("End process DefaultFeature tag: image:" + imageURL); //$NON-NLS-1$
				debug("End process DefaultFeature tag: ws:" //$NON-NLS-1$
				+ws + " os:" //$NON-NLS-1$
				+os + " nl:" //$NON-NLS-1$
				+nl + " application:" //$NON-NLS-1$
				+application);
			}
		}
	}

	/* 
	 * process URL info with element text
	 */
	private void processHandler(Attributes attributes) {
		InstallHandlerEntryModel handler = factory.createInstallHandlerEntryModel();

		String handlerURL = attributes.getValue("url"); //$NON-NLS-1$
		handler.setURLString(handlerURL);

		String library = attributes.getValue("library"); //$NON-NLS-1$
		handler.setLibrary(library);

		String clazz = attributes.getValue("handler"); //$NON-NLS-1$
		handler.setHandlerName(clazz);

		objectStack.push(handler);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Processed Handler: url:" //$NON-NLS-1$
			+handlerURL + " library:" //$NON-NLS-1$
			+library + " class:" //$NON-NLS-1$
			+clazz);
	}

	/* 
	 * process URL info with element text
	 */
	private void processInfo(Attributes attributes) {
		URLEntryModel inf = factory.createURLEntryModel();
		String infoURL = attributes.getValue("url"); //$NON-NLS-1$
		inf.setURLString(infoURL);

		objectStack.push(inf);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Processed Info: url:" + infoURL); //$NON-NLS-1$
	}

	/*
	 * Process includes information
	 */
	private void processIncludes(Attributes attributes) {

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().equals("") //$NON-NLS-1$
		|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			internalError(Policy.bind("DefaultFeatureParser.IdOrVersionInvalid", new String[] { id, ver, getState(currentState)}));
			//$NON-NLS-1$
		}

		IncludedFeatureReferenceModel includedFeature = factory.createIncludedFeatureReferenceModel();
		includedFeature.setFeatureIdentifier(id);
		includedFeature.setFeatureVersion(ver);

		// name
		String name = attributes.getValue("name");
		includedFeature.setLabel(name);

		// optional
		String optional = attributes.getValue("optional");
		boolean isOptional = "true".equalsIgnoreCase(optional);
		includedFeature.isOptional(isOptional);

		// search location
		String locationName = attributes.getValue("search-location");
		// bug 27030
		if (locationName == null)
			locationName = attributes.getValue("search_location");
		int searchLocation = IUpdateConstants.SEARCH_ROOT;
		if ("both".equalsIgnoreCase(locationName))
			searchLocation = IUpdateConstants.SEARCH_ROOT & IUpdateConstants.SEARCH_SELF;
		if ("self".equalsIgnoreCase(locationName))
			searchLocation = IUpdateConstants.SEARCH_SELF;				
		includedFeature.setSearchLocation(searchLocation);

		// os arch ws
		String os = attributes.getValue("os");
		includedFeature.setOS(os);

		String ws = attributes.getValue("ws");
		includedFeature.setWS(ws);

		String arch = attributes.getValue("arch");
		includedFeature.setArch(arch);

		objectStack.push(includedFeature);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			debug("End process Includes tag: id:" //$NON-NLS-1$
			+id + " ver:" + ver); //$NON-NLS-1$
			debug("name =" + name + " optional=" + optional + " search-location=" + locationName);
			debug("os=" + os + " ws=" + ws + " arch=" + arch);
		}
	}

	/* 
	 * process URL info with label attribute
	 */
	private void processURLInfo(Attributes attributes) {
		URLEntryModel inf = factory.createURLEntryModel();
		String infoURL = attributes.getValue("url"); //$NON-NLS-1$
		String label = attributes.getValue("label"); //$NON-NLS-1$
		String type = attributes.getValue("type"); //$NON-NLS-1$
		inf.setURLString(infoURL);
		inf.setAnnotation(label);
		
		if ("web".equalsIgnoreCase(type))
			inf.setType(IURLEntry.WEB_SITE);
		else 
			inf.setType(IURLEntry.UPDATE_SITE);

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			debug("Processed URLInfo: url:" + infoURL + " label:" + label+" type:"+type);
		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		objectStack.push(inf);
	}

	/* 
	 * process import info
	 */
	private void processImport(Attributes attributes) {
		String pluginID = attributes.getValue("plugin"); //$NON-NLS-1$
		String featureID = attributes.getValue("feature"); //$NON-NLS-1$
		String idMatch = attributes.getValue("id-match"); //$NON-NLS-1$

		if (!(pluginID == null ^ featureID == null)) {
			internalError(Policy.bind("DefaultFeatureParser.PluginAndFeatureId"));
			return;
		}

		// since 2.0.2 , manage feature and plugin import
		String id = null;
		if (pluginID == null) {
			id = featureID;
		} else {
			id = pluginID;
		}

		if (id == null || id.trim().equals("")) //$NON-NLS-1$
			internalError(Policy.bind("DefaultFeatureParser.MissingId", getState(currentState)));
		//$NON-NLS-1$
		//$NON-NLS-1$
		else {
			ImportModel imp = factory.createImportModel();
			String ver = attributes.getValue("version"); //$NON-NLS-1$
			String match = attributes.getValue("match"); //$NON-NLS-1$
			String patch = attributes.getValue("patch");

			imp.setPatch(patch != null && patch.equalsIgnoreCase("true"));

			if (ver == null) {
				if (imp.isPatch()) {
					internalError(Policy.bind("DefaultFeatureParser.MissingPatchVersion"));
				}
				ver = "0.0.0";
				match = "greaterOrEqual";
			} else if (match == null) {
				if (imp.isPatch())
					match = "perfect";
				else
					match = "compatible";
			}

			imp.setIdentifier(id);
			imp.setVersion(ver);
			imp.setFeatureImport(featureID != null);
			imp.setMatchingRuleName(match);
			imp.setMatchingIdRuleName(idMatch);

			if (imp.isPatch()) {
				// patch reference must be perfect.
				if (match != null && !match.equalsIgnoreCase("perfect")) {
					internalError(Policy.bind("DefaultFeatureParser.wrongMatchForPatch"));
				}
				if (imp.isFeatureImport() == false) {
					imp.setPatch(false);
					internalError(Policy.bind("DefaultFeatureParser.patchWithPlugin"));
				}
			}

			// os arch ws
			String os = attributes.getValue("os");
			imp.setOS(os);

			String ws = attributes.getValue("ws");
			imp.setWS(ws);

			String arch = attributes.getValue("arch");
			imp.setOSArch(arch);

			objectStack.push(imp);

			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
				debug("Processed import: id:" + id + " ver:" + ver);
				//$NON-NLS-1$ //$NON-NLS-2$
				debug("Processed import: match:" + match); //$NON-NLS-1$
			}

		}
	}

	/* 
	 * process import info
	 */
	private void processRequire(Attributes attributes) {
	}

	/* 
	 * process plugin entry info
	 */
	private void processPlugin(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$
		if (id == null || id.trim().equals("") //$NON-NLS-1$
		|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			internalError(Policy.bind("DefaultFeatureParser.IdOrVersionInvalid", new String[] { id, ver, getState(currentState)}));
			//$NON-NLS-1$
		} else {
			PluginEntryModel pluginEntry = factory.createPluginEntryModel();
			pluginEntry.setPluginIdentifier(id);
			pluginEntry.setPluginVersion(ver);

			String fragment = attributes.getValue("fragment"); //$NON-NLS-1$
			pluginEntry.isFragment(fragment != null && fragment.trim().equalsIgnoreCase("true"));
			//$NON-NLS-1$			

			//setOS
			String os = attributes.getValue("os"); //$NON-NLS-1$
			pluginEntry.setOS(os);

			//setWS
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			pluginEntry.setWS(ws);

			//setNL
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			pluginEntry.setNL(nl);

			// setArch
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			pluginEntry.setArch(arch);

			// setUnpack
			String unpack = attributes.getValue("unpack"); //$NON-NLS-1$
			pluginEntry.setUnpack(!"false".equalsIgnoreCase(unpack));

			// download size
			long download_size = ContentEntryModel.UNKNOWN_SIZE;
			String download = attributes.getValue("download-size"); //$NON-NLS-1$
			if (download != null && !download.trim().equals("")) { //$NON-NLS-1$
				try {
					download_size = Long.valueOf(download).longValue();
				} catch (NumberFormatException e) {
					// use UNKNOWN_SIZE
				}
			}
			pluginEntry.setDownloadSize(download_size);

			// install size	
			long install_size = ContentEntryModel.UNKNOWN_SIZE;
			String install = attributes.getValue("install-size"); //$NON-NLS-1$
			if (install != null && !install.trim().equals("")) { //$NON-NLS-1$
				try {
					install_size = Long.valueOf(install).longValue();
				} catch (NumberFormatException e) {
					// use UNKNOWN_SIZE
				}
			}
			pluginEntry.setInstallSize(install_size);

			objectStack.push(pluginEntry);

			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
				debug("Processed Plugin: id:" + id + " ver:" + ver + " fragment:" + fragment);
				//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				debug("Processed Plugin: os:" + os + " ws:" + ws + " nl:" + nl);
				//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				debug("Processed Plugin: download size:" //$NON-NLS-1$
				+download_size + " install size:" //$NON-NLS-1$
				+install_size);
			}

		}
	}

	/* 
	 * process non-plug-in entry info
	 */
	private void processData(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		if (id == null || id.trim().equals("")) { //$NON-NLS-1$
			internalError(Policy.bind("DefaultFeatureParser.MissingId", getState(currentState)));
			//$NON-NLS-1$
			//$NON-NLS-1$
		} else {
			NonPluginEntryModel dataEntry = factory.createNonPluginEntryModel();
			dataEntry.setIdentifier(id);

			//setOS
			String os = attributes.getValue("os"); //$NON-NLS-1$
			dataEntry.setOS(os);

			//setWS
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			dataEntry.setWS(ws);

			//setNL
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			dataEntry.setNL(nl);

			// setArch
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			dataEntry.setArch(arch);

			// download size
			long download_size = ContentEntryModel.UNKNOWN_SIZE;
			String download = attributes.getValue("download-size"); //$NON-NLS-1$
			if (download != null && !download.trim().equals("")) { //$NON-NLS-1$
				try {
					download_size = Long.valueOf(download).longValue();
				} catch (NumberFormatException e) {
					// use UNKNOWN_SIZE
				}
			}
			dataEntry.setDownloadSize(download_size);

			// install size	
			long install_size = ContentEntryModel.UNKNOWN_SIZE;
			String install = attributes.getValue("install-size"); //$NON-NLS-1$
			if (install != null && !install.trim().equals("")) { //$NON-NLS-1$
				try {
					install_size = Long.valueOf(install).longValue();
				} catch (NumberFormatException e) {
					// use UNKNOWN_SIZE
				}
			}
			dataEntry.setInstallSize(install_size);

			objectStack.push(dataEntry);

			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
				debug("Processed Data: id:" + id); //$NON-NLS-1$
				debug("Processed Data: download size:" //$NON-NLS-1$
				+download_size + " install size:" //$NON-NLS-1$
				+install_size);
			}

		}
	}

	private void debug(String s) {
		UpdateCore.debug("DefaultFeatureParser: " + s); //$NON-NLS-1$
	}

	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = ""; //$NON-NLS-1$
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$

		String msg;
		if (name.equals("")) { //$NON-NLS-1$
			msg = Policy.bind("DefaultFeatureParser.ErrorParsing", ex.getMessage());
			//$NON-NLS-1$
		} else {
			String[] values = new String[] { name, Integer.toString(ex.getLineNumber()), Integer.toString(ex.getColumnNumber()), ex.getMessage()};
			msg = Policy.bind("DefaultFeatureParser.ErrorlineColumnMessage", values);
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
			status = new MultiStatus(PLUGIN_ID, Platform.PARSE_PROBLEM, Policy.bind("DefaultFeatureParser.ErrorParsingFeature"), null);
			//$NON-NLS-1$
		}

		status.add(error);
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING)
			UpdateCore.log(error);
	}

	private void internalErrorUnknownTag(String msg) {
		stateStack.push(new Integer(STATE_IGNORED_ELEMENT));
		internalError(msg);
	}

	private void internalError(String message) {
		error(new Status(IStatus.ERROR, PLUGIN_ID, Platform.PARSE_PROBLEM, message, null));
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

			case STATE_FEATURE :
				return "Feature"; //$NON-NLS-1$

			case STATE_HANDLER :
				return "Install Handler"; //$NON-NLS-1$

			case STATE_DESCRIPTION :
				return "description"; //$NON-NLS-1$

			case STATE_INCLUDES :
				return "includes"; //$NON-NLS-1$

			case STATE_COPYRIGHT :
				return "Copyright"; //$NON-NLS-1$

			case STATE_LICENSE :
				return "License"; //$NON-NLS-1$

			case STATE_URL :
				return "URL"; //$NON-NLS-1$

			case STATE_UPDATE :
				return "Update URL"; //$NON-NLS-1$

			case STATE_DISCOVERY :
				return "Discovery URL"; //$NON-NLS-1$

			case STATE_REQUIRES :
				return "Require"; //$NON-NLS-1$

			case STATE_IMPORT :
				return "Import"; //$NON-NLS-1$

			case STATE_PLUGIN :
				return "Plugin"; //$NON-NLS-1$

			case STATE_DATA :
				return "Data"; //$NON-NLS-1$

			default :
				return Policy.bind("DefaultFeatureParser.UnknownState", Integer.toString(state));
				//$NON-NLS-1$
		}

	}

	/**
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
		super.ignorableWhitespace(arg0, arg1, arg2);
	}
}
