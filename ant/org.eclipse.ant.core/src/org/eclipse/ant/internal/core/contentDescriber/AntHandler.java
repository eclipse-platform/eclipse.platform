/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.core.contentDescriber;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An xml event handler for detecting the project top-level element in an Ant buildfile.
 * Also records whether a default attribute is present for the project and if any target 
 * elements are present.
 * 
 * @since 3.1
 */
public final class AntHandler extends DefaultHandler {
    /**
     * An exception indicating that the parsing should stop.
     * 
     * @since 3.1
     */
    private class StopParsingException extends SAXException {
        /**
         * All serializable objects should have a stable serialVersionUID
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs an instance of <code>StopParsingException</code> with a
         * <code>null</code> detail message.
         */
        public StopParsingException() {
            super((String) null);
        }
    }

    private static final String DEFAULT_ATTRIBUTE= "default"; //$NON-NLS-1$
    private static final String PROJECT = "project"; //$NON-NLS-1$
    private static final String TARGET= "target"; //$NON-NLS-1$
    
    /**
     * This is the name of the top-level element found in the XML file. This
     * member variable is <code>null</code> unless the file has been parsed
     * successful to the point of finding the top-level element.
     */
    private String fTopElementFound = null;
    private SAXParserFactory fFactory;

    private boolean fDefaultAttributeFound= false;
    private boolean fTargetFound = false;

    private int fLevel= -1;

    /**
     * Creates a new SAX parser for use within this instance.
     * 
     * @return The newly created parser.
     * @throws ParserConfigurationException
     *             If a parser of the given configuration cannot be created.
     * @throws SAXException
     *             If something in general goes wrong when creating the parser.
     */
    private final SAXParser createParser(SAXParserFactory parserFactory) throws ParserConfigurationException, SAXException, SAXNotRecognizedException, SAXNotSupportedException {
        // Initialize the parser.
        final SAXParser parser = parserFactory.newSAXParser();
        final XMLReader reader = parser.getXMLReader();
        // disable DTD validation (bug 63625)
        try {
            //  be sure validation is "off" or the feature to ignore DTD's will not apply
            reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
        } catch (SAXNotRecognizedException e) {
            // not a big deal if the parser does not recognize the features
        } catch (SAXNotSupportedException e) {
            // not a big deal if the parser does not support the features
        }
        return parser;
    }

    private SAXParserFactory getFactory() {
        synchronized (this) {
            if (fFactory != null) {
                return fFactory;
            }
            fFactory= SAXParserFactory.newInstance();
            fFactory.setNamespaceAware(true);
        }
        return fFactory;
    }

    protected boolean parseContents(InputSource contents) throws IOException, ParserConfigurationException, SAXException {
        // Parse the file into we have what we need (or an error occurs).
        try {
            fFactory = getFactory();
            if (fFactory == null) {
                return false;
            }
            final SAXParser parser = createParser(fFactory);
            // to support external entities specified as relative URIs (see bug 63298)
            contents.setSystemId("/"); //$NON-NLS-1$
            parser.parse(contents, this);
        } catch (StopParsingException e) {
            // Abort the parsing normally. Fall through...
        }
        return true;
    }

    /*
     * Resolve external entity definitions to an empty string.  This is to speed
     * up processing of files with external DTDs.  Not resolving the contents 
     * of the DTD is ok, as only the System ID of the DTD declaration is used.
     * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        return new InputSource(new StringReader("")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public final void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
    	fLevel++;
        if (fTopElementFound == null) {
            fTopElementFound = elementName;
            if (!hasRootProjectElement()) {
                throw new StopParsingException();
            }
            if (attributes != null) {
                fDefaultAttributeFound= attributes.getValue(DEFAULT_ATTRIBUTE) != null;
                if (fDefaultAttributeFound) {
                    throw new StopParsingException();
                }
            }
        }
        if (fLevel == 1 && TARGET.equals(elementName)) {
            fTargetFound= true;
            throw new StopParsingException();
        }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	super.endElement(uri, localName, qName);
    	fLevel--;
    }

    protected boolean hasProjectDefaultAttribute() {
        return fDefaultAttributeFound;
    }

    protected boolean hasRootProjectElement() {
       return PROJECT.equals(fTopElementFound);
    }
    
    protected boolean hasTargetElement() {
       return fTargetFound;
    }
}