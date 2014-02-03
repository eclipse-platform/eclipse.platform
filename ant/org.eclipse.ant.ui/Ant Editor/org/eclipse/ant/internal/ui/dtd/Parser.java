/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.ant.internal.ui.dtd.schema.SchemaFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;


/**
 * Simple parser for DTDs. Returns ISchema representing the DTD.
 * 
 * To parse a DTD, you must parse an XML document. The <code>parseDTD()</code>
 * method builds a temporary XML document in memory that refers to or includes
 * the DTD.
 * 
 * There is no dependency in this package on any code outside the package except
 * XMLReader.
 * 
 * To hide the underlying parser, XML parser exceptions are wrapped by a
 * ParseError. Unless debugging, the two string constants are sufficient to
 * determine the cause of the error.
 * @author Bob Foster
 */
public class Parser {
	
	/** ParseError message when system parser doesn't do the job */
	public static final String NOT_SUPPORTED = AntDTDMessages.Parser_XML_parser_does_not_support_DeclHandler_1;
	/** ParseError message for a well-formed or validation error in XML or DTD.
	 *  Currently not returned. */
	public static final String PARSE_ERROR = AntDTDMessages.Parser_Error_parsing_XML_document_or_DTD_2;
	
	private static final String INTERNAL = "internal://usereader.objfac.com"; //$NON-NLS-1$
	
	/**
	 * Parse the XML document at the input source and return a document walker
	 * that can be used to validate any document with the same DTD (internal and
	 * external) or provide user assistance for this document.
	 * @param inputSource Source for XML document to start DTD parse. Must
	 * contain a DOCTYPE declaration with internal or external subset, or both.
	 * @param entityResolver EntityResolver or null.
	 * @return schema for document.
	 * @throws ParseError for NOT_SUPPORTED or PARSE_ERROR.
	 * @throws IOException
	 */
	public ISchema parse(InputSource inputSource, EntityResolver entityResolver) throws ParseError, IOException {
		XMLReader parser = null;
		SchemaFactory factory = new SchemaFactory();
		try {
			parser = getXMLReader();
			DeclHandler handler = factory;
			parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler); //$NON-NLS-1$
			if (entityResolver != null) {
				parser.setEntityResolver(entityResolver);
			}
			parser.parse(inputSource);
		} catch (SAXNotRecognizedException e) {
			throw new ParseError(NOT_SUPPORTED);
		} catch (SAXNotSupportedException e) {
			throw new ParseError(NOT_SUPPORTED);
		} catch (SAXException e) {
			// Don't care about errors in XML, so just fall thru.
			// If parse failed in DTD, may have incomplete schema,
			// but this is better than no schema.
			factory.setErrorException(e);
		}

		return factory.getSchema();
	}
	
	private XMLReader getXMLReader() throws ParseError {
		SAXParser parser = null;
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
			return parser.getXMLReader();
		} catch (ParserConfigurationException e) {
			throw new ParseError(e.getMessage());
		} catch (SAXException e) {
			throw new ParseError(e.getMessage());
		}
	}
	
	/**
	 * Parse the XML document at the argument URL and return a document walker
	 * that can be used to validate any document with the same DTD (internal
	 * and external) or provide user assistance for this document.
	 * @param url Of XML document to start DTD parse. Must contain a DOCTYPE
	 * declaration with internal or external subset, or both.
	 * @return IWalker that can be used to traverse document.
	 * @throws ParseError for NOT_SUPPORTED or PARSE_ERROR.
	 * @throws IOException
	 */
	public ISchema parse(String url) throws ParseError, IOException {
		return parse(new InputSource(url), null);
	}
	
	/**
	 * Parse the XML document using the argument reader and return a document
	 * walker that can be used to validate any document with the same DTD
	 * (internal and external) or provide user assistance for this document.
	 * @param reader Reader for XML document to start DTD parse. Must contain a
	 * DOCTYPE declaration with internal or external subset, or both.
	 * @return IWalker that can be used to traverse document.
	 * @throws ParseError for NOT_SUPPORTED or PARSE_ERROR.
	 * @throws IOException
	 */
	public ISchema parse(Reader reader) throws ParseError, IOException {
		return parse(new InputSource(reader), null);
	}
	
	/**
	 * Parse the DTD with the given public and system ids and return a document
	 * walker that can be used to validate or provide user assistance for any
	 * document with the same external DTD and no internal subset.
	 * @param pub PUBLIC id of DTD.
	 * @param sys SYSTEM id of DTD.
	 * @param root Plausible root element qname. Any name will do but a
	 * name that will not cause a validation error is preferred.
	 * @return IWalker that can be used to traverse document.
	 * @throws ParseError for NOT_SUPPORTED or PARSE_ERROR.
	 * @throws IOException
	 */
	public ISchema parseDTD(String pub, String sys, String root) throws ParseError, IOException {
		return parse(new InputSource(new DTDReader(pub, sys, root)), null);
	}
	
	/**
	 * Parse the DTD from the reader and return a document walker that can be
	 * used to validate or provide user assistance for any document with the
	 * same external DTD and no internal subset.
	 * @param reader Reader for external subset DTD
	 * @param root Plausible root element qname. Any name will do but a
	 * name that will not cause a validation error is preferred.
	 * @return ISchema that can be used to traverse document.
	 * @throws ParseError for NOT_SUPPORTED or PARSE_ERROR.
	 * @throws IOException
	 */
	public ISchema parseDTD(Reader reader, String root) throws ParseError, IOException {
		return parse(new InputSource(new DTDReader(INTERNAL, INTERNAL, root)), new DTDEntityResolver(reader));
	}
	
	private static class DTDReader extends Reader {
		private Reader fDelegate;
		
		public DTDReader(String pub, String sys, String root) {
			String document = "<!DOCTYPE "+root+" PUBLIC '"+pub+"' '"+sys+"'><"+root+"/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			fDelegate = new StringReader(document);
		}
		
		/**
		 * @see java.io.Reader#close()
		 */
		public void close() throws IOException {
			fDelegate.close();
		}

		/* (non-Javadoc)
		 * @see java.io.Reader#read(char[], int, int)
		 */
		public int read(char[] cbuf, int off, int len) throws IOException {
			return fDelegate.read(cbuf, off, len);
		}
	}
	
	private static class DTDEntityResolver implements EntityResolver {
		private Reader reader;
		public DTDEntityResolver(Reader reader) {
			this.reader = reader;
		}
		/**
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
		 */
		public InputSource resolveEntity(String publicId, String systemId) {
			if (publicId.equals(INTERNAL) && systemId.equals(INTERNAL))
				return new InputSource(reader);
			return null;
		}
	}	
}