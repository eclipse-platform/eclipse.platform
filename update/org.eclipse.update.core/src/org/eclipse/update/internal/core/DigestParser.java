/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class DigestParser extends DefaultHandler {

	private InternalFeatureParser featureParser;
	
	private ArrayList featureModels;

	private SAXParser parser;

	private FeatureModelFactory factory;

	private String location;
	
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	
	public DigestParser() {
		super();
		featureParser = new InternalFeatureParser();
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
		init(factory, null);
	}
    
    /**
     * @param factory
     * @param location
     * @since 3.1
     */
    public void init(FeatureModelFactory factory, String location) {
    	
    	this.factory = factory;
    	this.location = location;
    	factory = new LiteFeatureFactory();
    	featureModels = new ArrayList();
    	featureParser.internalInit(factory, location);
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
	public LiteFeature[] parse(InputStream in) throws SAXException, IOException {
		
		parser.parse(new InputSource(in), this);	
		return (LiteFeature[])featureModels.toArray( new LiteFeature[featureModels.size()]);
	}


	/**
	 * Returns all status objects accumulated by the parser.
	 *
	 * @return multi-status containing accumulated status, or <code>null</code>.
	 * @since 2.0
	 */
	public MultiStatus getStatus() {
		return featureParser.getStatus();
	}

	/**
	 * Handle start of element tags
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(localName.equals("digest")) //$NON-NLS-1$
			return;
		if(localName.equals("feature")) //$NON-NLS-1$
			featureParser.internalInit(factory, location);
		
		featureParser.startElement(uri, localName, qName, attributes);
	}

	/**
	 * Handle end of element tags
	 * @see DefaultHandler#endElement(String, String, String)
	 * @since 2.0
	 */
	public void endElement(String uri, String localName, String qName) {
		if(localName.equals("digest")) //$NON-NLS-1$
			return;
		featureParser.endElement(uri, localName, qName);
		if(localName.equals("feature")) { //$NON-NLS-1$
			try {
				featureModels.add(featureParser.getFeatureModel());
			} catch (SAXException e) {
				e.printStackTrace();
			} 
		}
	}
	

	/**
	 * Handle character text
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	public void characters(char[] ch, int start, int length) {
		featureParser.characters(ch, start, length);
	}

	/**
	 * Handle errors
	 * @see DefaultHandler#error(SAXParseException)
	 * @since 2.0
	 */
	public void error(SAXParseException ex) {
		featureParser.error(ex);
	}

	/**
	 * Handle fatal errors
	 * @see DefaultHandler#fatalError(SAXParseException)
	 * @exception SAXException
	 * @since 2.0
	 */
	public void fatalError(SAXParseException ex) throws SAXException {
		featureParser.fatalError(ex);
	}

	/**
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
		featureParser.ignorableWhitespace(arg0, arg1, arg2);
	}
}
