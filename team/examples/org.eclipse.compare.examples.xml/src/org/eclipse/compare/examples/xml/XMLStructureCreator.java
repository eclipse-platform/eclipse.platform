/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.examples.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;

/**
 * This structure analyzer builds a parse tree of an XML document found in a
 * <code>IByteContentAccessor</code> input by calling getStructure(Object)
 */
public class XMLStructureCreator implements IStructureCreator {

	protected static final boolean DEBUG_MODE= false;
	
	public static final String DEFAULT_NAME= XMLCompareMessages.XMLStructureCreator_pluginname; 

	public static final String USE_UNORDERED= XMLCompareMessages.XMLStructureCreator_unordered; 
	public static final String USE_ORDERED= XMLCompareMessages.XMLStructureCreator_ordered; 
	public static final String DEFAULT_IDMAP= USE_ORDERED;

	public static final String TYPE_ELEMENT= "element"; //$NON-NLS-1$
	public static final String TYPE_TEXT= "text"; //$NON-NLS-1$
	public static final String TYPE_ATTRIBUTE= "attribute"; //$NON-NLS-1$

	// for signatures
	public static final String ROOT_ID= "root"; //$NON-NLS-1$
	public static final char SIGN_SEPARATOR= '>';//'.'
	public static final char SIGN_ENCLOSING= '$';
	public static final String SIGN_ELEMENT= SIGN_ENCLOSING + TYPE_ELEMENT + SIGN_ENCLOSING;
	public static final String SIGN_TEXT= SIGN_ENCLOSING + TYPE_TEXT + SIGN_ENCLOSING;
	public static final String SIGN_ATTRIBUTE= SIGN_ENCLOSING + TYPE_ATTRIBUTE + SIGN_ENCLOSING;
	
	public static final String IDMAP_UNORDERED= XMLCompareMessages.XMLStructureCreator_idmap_unordered; 
	public static final char ID_SEPARATOR= '<';
	public static final char ID_TYPE_BODY= '<';

	private XMLNode fcurrentParent;
	private String fsignature;
	private Document fdoc;
	private boolean ignoreBodies= false;
	private HashMap fIdMapsInternal;
	private HashMap fIdMaps;
	private HashMap fIdExtensionToName;
	private HashMap fOrderedElementsInternal;
	private HashMap fOrderedElements;
	private HashMap idMap;
	private ArrayList fOrdered;
	private String fIdMapToUse;
	private boolean fUseIdMap;
	private String fFileExt;
	private boolean fFirstCall= true;
	private boolean fRemoveWhiteSpace;

	protected class XMLHandler extends DefaultHandler {

		protected Locator prevlocator; //previous locator
		protected Locator locator; //current locator

		@Override
		public void setDocumentLocator(Locator locator0) {
			this.locator= locator0;
		}

		// DocumentHandler methods
		
		/* Processing instruction. */
		@Override
		public void processingInstruction(String target, String data) {

			//    	System.out.println("target: " + target);
			//    	System.out.println("data: " + data);
			//        System.out.print("<?");
			//        System.out.print(target);
			//        if (data != null && data.length() > 0) {
			//            System.out.print(' ');
			//            System.out.print(data);
			//        }
			//        System.out.print("?>");
			//        System.out.flush();
			prevlocator= new LocatorImpl(locator);
		}

		/** Start document. */
		@Override
		public void startDocument() {
			prevlocator= new LocatorImpl(locator);
		}

		/* Start element. */
		@Override
		public void startElement(String uri, String local, String raw, Attributes attrs) {
			XMLNode currentElement;

			/* add root node for this element */

			if (XMLStructureCreator.DEBUG_MODE) {
				if (locator != null && prevlocator != null) {
					System.out.println("prevlocator: line " + prevlocator.getLineNumber() + "  column " + prevlocator.getColumnNumber() + "  id " + prevlocator.getPublicId()); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
					System.out.println("locator: line " + locator.getLineNumber() + "  column " + locator.getColumnNumber() + "  id " + locator.getPublicId()); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
				}
			}

			try {
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("Node where children field accessed: " + fcurrentParent.getId()); //$NON-NLS-1$
				XMLChildren currentParent= (XMLChildren) fcurrentParent;
				currentParent.children++;
				String elementId;
				String elementName;
				IRegion r= fdoc.getLineInformation(prevlocator.getLineNumber() - 1);

				String parentSig= fsignature;
				fsignature= fsignature + raw + SIGN_SEPARATOR;

				if (isUseIdMap() && idMap.containsKey(fsignature)) {
					String attrName= (String) idMap.get(fsignature);
					elementId= raw + Character.valueOf(ID_SEPARATOR) + attrs.getValue(attrName);
					elementName= raw + " [" + attrName + "=" + attrs.getValue(attrName) + "]"; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
				} else {
					if (!currentParent.childElements.containsKey(raw)) {
						currentParent.childElements.put(raw, Integer.valueOf(1));
					} else {
						currentParent.childElements.put(raw, Integer.valueOf(currentParent.childElements.get(raw).intValue() + 1));
					}
					elementId= raw + Character.valueOf(ID_SEPARATOR) + "[" + currentParent.childElements.get(raw) + "]"; //$NON-NLS-2$ //$NON-NLS-1$
					elementName= MessageFormat.format("{0} [{1}]", raw, currentParent.childElements.get(raw).toString()); //$NON-NLS-1$
				}
				int start= r.getOffset() + prevlocator.getColumnNumber() - 1;
				if (start < 0)
					start= 0;
				currentElement= new XMLChildren(TYPE_ELEMENT, elementId, elementId, (fsignature + SIGN_ELEMENT), fdoc, start, 0);
				currentElement.setName(elementName);
				if (isUseIdMap() && idMap.containsKey(fsignature))
					currentElement.setUsesIDMAP(true);
				if (fOrdered != null && fOrdered.contains(parentSig))
					currentElement.setIsOrderedChild(true);

				fcurrentParent.addChild(currentElement);
				currentElement.setParent(fcurrentParent);
				fcurrentParent= currentElement;
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("\nAdded Element " + raw + "  with offset " + r.getOffset()); //$NON-NLS-2$ //$NON-NLS-1$
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("fcurrentParent1: " + fcurrentParent.getId()); //$NON-NLS-1$

				if (attrs != null) {
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("attrs != null, fcurrentParent is " + fcurrentParent.getId()); //$NON-NLS-1$
					//attrs = sortAttributes(attrs);
					int len= attrs.getLength();
					int element_lines_length_size;
					int[] element_lines_length;
					int column_offset;
					String element_string;
					if (fcurrentParent.getParent().getId().equals(ROOT_ID)) {
						element_lines_length_size= locator.getLineNumber() - prevlocator.getLineNumber();
						element_lines_length= new int[element_lines_length_size];
						column_offset= 0;
						element_string= ""; //$NON-NLS-1$
						for (int i_ell= 0; i_ell < element_lines_length.length; i_ell++) {
							IRegion attr_r= fdoc.getLineInformation(i_ell + prevlocator.getLineNumber());
							element_lines_length[i_ell]= fdoc.get(attr_r.getOffset(), attr_r.getLength()).length() + 1;
							element_string= element_string + fdoc.get(attr_r.getOffset(), attr_r.getLength()) + " "; //$NON-NLS-1$
						}
					} else {
						element_lines_length_size= locator.getLineNumber() - prevlocator.getLineNumber() + 1;
						//if (element_lines_length_size < 1)
						// element_lines_length_size = 1;
						element_lines_length= new int[element_lines_length_size];
						IRegion first_line= fdoc.getLineInformation(prevlocator.getLineNumber() - 1);
						column_offset= prevlocator.getColumnNumber() - 1;
						int first_line_relevant_offset= first_line.getOffset() + column_offset;
						int first_line_relevant_length= first_line.getLength() - column_offset;
						element_string= fdoc.get(first_line_relevant_offset, first_line_relevant_length) + " "; //$NON-NLS-1$
						element_lines_length[0]= element_string.length();
						for (int i_ell= 1; i_ell < element_lines_length.length; i_ell++) {
							IRegion attr_r= fdoc.getLineInformation(i_ell + prevlocator.getLineNumber() - 1);
							element_lines_length[i_ell]= fdoc.get(attr_r.getOffset(), attr_r.getLength()).length() + 1;
							element_string= element_string + fdoc.get(attr_r.getOffset(), attr_r.getLength()) + " "; //$NON-NLS-1$
						}
					}

					for (int i_attr= 0; i_attr < len; i_attr++) {
						String attr_name= attrs.getQName(i_attr);
						String attr_value= attrs.getValue(i_attr);

						/*
						 * find range of attribute in doc; manually parses the
						 * line
						 */
						boolean found= false;
						int first_quotes= -1;
						int second_quotes= -1;
						int id_index= -1;
						while (!found) {
							first_quotes= element_string.indexOf("\"", second_quotes + 1); //$NON-NLS-1$
							second_quotes= element_string.indexOf("\"", first_quotes + 1); //$NON-NLS-1$
							String value;
							try {
								value= element_string.substring(first_quotes + 1, second_quotes);
							} catch (Exception e) {
								value= ""; //$NON-NLS-1$
							}
							if (value.isEmpty())
								found= true;
							else if (value.equals(attr_value)) {
								id_index= element_string.lastIndexOf(attr_name, first_quotes - 1);
								boolean wrong= false;
								boolean found_equal= false;
								for (int i_char= id_index + attr_name.length(); i_char < first_quotes && !wrong; i_char++) {
									if (element_string.charAt(i_char) == '=')
										if (!found_equal)
											found_equal= true;
										else
											wrong= true;
									else if (!Character.isWhitespace(element_string.charAt(i_char)))
										wrong= true;
								}
								if (!wrong)
									found= true;
							}
						}
						//id_index has one char missing for every line (the
						// final cr)
						int line_of_index= 0;
						for (line_of_index= 0; id_index > element_lines_length[line_of_index] - 1; line_of_index++)
							id_index-= (element_lines_length[line_of_index]);
						if (line_of_index == 0)
							id_index+= column_offset;
						if (fcurrentParent.getParent().getId().equals(ROOT_ID))
							line_of_index+= prevlocator.getLineNumber();
						else
							line_of_index+= prevlocator.getLineNumber() - 1;
						//index at line line_of_index, line offset id_index
						int line_of_end_of_value= 0;
						int end_of_value_index= second_quotes;
						for (line_of_end_of_value= 0; end_of_value_index > element_lines_length[line_of_end_of_value] - 1; line_of_end_of_value++)
							end_of_value_index-= (element_lines_length[line_of_end_of_value]);
						if (line_of_end_of_value == 0)
							end_of_value_index+= column_offset;
						if (fcurrentParent.getParent().getId().equals(ROOT_ID))
							line_of_end_of_value+= prevlocator.getLineNumber();
						else
							line_of_end_of_value+= prevlocator.getLineNumber() - 1;
						//end of value at line line_of_end_of_value, line
						// offset end_of_value_index

						int attr_start_doc_offset= fdoc.getLineInformation(line_of_index).getOffset() + id_index;
						//int attr_length_doc_offset =
						// fdoc.getLineInformation(line_of_value).getOffset()+value_index+attr_value.length()+1+(line_of_end_of_value-line_of_index)
						// - attr_start_doc_offset;
						int attr_length_doc_offset= fdoc.getLineInformation(line_of_end_of_value).getOffset() + end_of_value_index + 1 - attr_start_doc_offset;
						currentElement= new XMLNode(TYPE_ATTRIBUTE, attr_name, attr_value, (fsignature + attr_name + SIGN_SEPARATOR + SIGN_ATTRIBUTE), fdoc, attr_start_doc_offset, attr_length_doc_offset);
						currentElement.setName(attr_name);
						fcurrentParent.addChild(currentElement);
						currentElement.setParent(fcurrentParent);
						if (XMLStructureCreator.DEBUG_MODE)
							System.out.println("added attribute " + currentElement.getId() + " with value >" + currentElement.getValue() + "<" + " to element " + fcurrentParent.getId() + " which has parent " + fcurrentParent.getParent().getId()); //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
					}
				}
			} catch (BadLocationException ex) {
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("BadLocationException in startElement(...) " + ex); //$NON-NLS-1$
				currentElement= new XMLChildren(TYPE_ELEMENT, raw + "_(" + ((XMLChildren) fcurrentParent).children + ")", raw + "_(" + ((XMLChildren) fcurrentParent).children + ")", (fsignature + SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
			}
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("At the end of startElement(...), fcurrentParent is " + fcurrentParent.getId()); //$NON-NLS-1$
			prevlocator= new LocatorImpl(locator);
		}

		/* Characters. */
		@Override
		public void characters(char ch[], int start, int length) {
			if (!ignoreBodies) {
				//			String chars = (new String(ch, start, length)).trim();
				String chars= new String(ch, start, length);
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("characters: >" + chars + "<"); //$NON-NLS-2$ //$NON-NLS-1$
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("Body Location: line " + locator.getLineNumber() + "  column " + locator.getColumnNumber()); //$NON-NLS-2$ //$NON-NLS-1$

				//if text contains only white space, it will be ignored.
				if (!trimWhiteSpace(chars).isEmpty()) {
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("Adding body"); //$NON-NLS-1$
					try {
						IRegion r= fdoc.getLineInformation(locator.getLineNumber() - 1);
						//location returns the END of the characters
						//offset of BEGINNING of characters:
						int offset= r.getOffset() + locator.getColumnNumber() - 1 - length;
						fcurrentParent.bodies++;
						String body_value= new String(ch, start, length);
						if (fRemoveWhiteSpace) {
							body_value= removeWhiteSpace(body_value);
						}
						XMLNode bodynode= new XMLNode(TYPE_TEXT, "body_(" + fcurrentParent.bodies + ")", body_value, (fsignature + SIGN_TEXT), fdoc, offset, length); //$NON-NLS-2$ //$NON-NLS-1$
						bodynode.setName(MessageFormat.format("{0} ({1})", XMLCompareMessages.XMLStructureCreator_body, Integer.toString(fcurrentParent.bodies)));  //$NON-NLS-1$
						fcurrentParent.addChild(bodynode);
						bodynode.setParent(fcurrentParent);
						if (XMLStructureCreator.DEBUG_MODE)
							System.out.println("Created body " + fcurrentParent.bodies //$NON-NLS-1$
									+ " with offset " + offset + " and length " + length //$NON-NLS-2$ //$NON-NLS-1$
									+ " with parent " + bodynode.getParent().getId()); //$NON-NLS-1$
						//bodies as id attributes
						String popsig= fcurrentParent.getParent().getSignature(); //signature of parent of
						// parent
						popsig= popsig.substring(0, popsig.lastIndexOf(SIGN_ELEMENT));
						if (isUseIdMap() && fcurrentParent.bodies == 1 && idMap.containsKey(popsig)) {
							String pid= fcurrentParent.getId();//id of parent
							String pelementname= pid.substring(0, pid.indexOf("<")); //name of parent element //$NON-NLS-1$
							if (((String) idMap.get(popsig)).equals(ID_TYPE_BODY + pelementname)) {
								XMLNode pop= fcurrentParent.getParent();
								String popid= pop.getId();
								String popelementname= popid.substring(0, popid.indexOf("<")); //$NON-NLS-1$
								pop.setId(popelementname + "<" + body_value); //$NON-NLS-1$
								pop.setOrigId(popelementname + "<" + body_value); //$NON-NLS-1$
								pop.setName(MessageFormat.format("{0} [{1}={2}]", popelementname, pelementname, body_value)); //$NON-NLS-1$
								pop.setUsesIDMAP(true);
							}
						}
					} catch (BadLocationException ex) {
						if (XMLStructureCreator.DEBUG_MODE)
							System.out.println("BadLocationException in characters(...) " + ex); //$NON-NLS-1$
						fcurrentParent.addChild(new XMLNode(TYPE_TEXT, "body_(" + fcurrentParent.bodies + ")", new String(ch, start, length), (fsignature + SIGN_TEXT), fdoc, 0, 0)); //$NON-NLS-2$ //$NON-NLS-1$
					}
				}
			}
			prevlocator= new LocatorImpl(locator);
		}

		/* Ignorable whitespace. */
		@Override
		public void ignorableWhitespace(char ch[], int start, int length) {
			//
			//// characters(ch, start, length);
			//// System.out.flush();
			//
			prevlocator= new LocatorImpl(locator);
		}

		/* End element. */
		@Override
		public void endElement(String uri, String local, String raw) {
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("\nExiting element " + fcurrentParent.getId()); //$NON-NLS-1$

			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("prevlocator: line " + prevlocator.getLineNumber() + "  column " + prevlocator.getColumnNumber() + "  id " + prevlocator.getPublicId()); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("locator: line " + locator.getLineNumber() + "  column " + locator.getColumnNumber() + "  id " + locator.getPublicId()); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

			if (fcurrentParent.getParent() != null) {
				try {
					IRegion r2= fdoc.getLineInformation(locator.getLineNumber() - 1);
					Position pos= fcurrentParent.getRange();

					int elem_length= r2.getOffset() + locator.getColumnNumber() - 1 - pos.getOffset();//length of element from
					// start tag to end tag
					fcurrentParent.setLength(elem_length);
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("pos.getOffset: " + pos.getOffset() + "  elem_length: " + elem_length); //$NON-NLS-2$ //$NON-NLS-1$
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("fdoc.get(pos.getOffset()+elem_length-5,4): >" + fdoc.get(pos.getOffset() + elem_length - 5, 4) + "<"); //$NON-NLS-2$ //$NON-NLS-1$
					//if (fdoc.get(pos.getOffset()+elem_length-2,1) != ">")
					// elem_length-=1;
					try {
						fcurrentParent.setValue(fdoc.get(pos.getOffset(), elem_length));
					} catch (BadLocationException ex) {
						try {
							fcurrentParent.setValue(fdoc.get(pos.getOffset(), elem_length - 1));
						} catch (BadLocationException ex2) {
							if (XMLStructureCreator.DEBUG_MODE) {
								System.out.println("BadLocationException in endElement(...) while attempting fcurrentParent.setValue(...): " + ex); //$NON-NLS-1$
								System.out.println("Attempt to correct BadLocationException failed: " + ex2); //$NON-NLS-1$
							}
						}
					}
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("Value of " + fcurrentParent.getId() + "  is >" + fcurrentParent.getValue() + "<"); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

					//going from ending element to parent element
					fcurrentParent= fcurrentParent.getParent();
					if (XMLStructureCreator.DEBUG_MODE)
						System.out.println("fcurrentParent = fcurrentParent.getParent();"); //$NON-NLS-1$
				} catch (BadLocationException ex) {
					if (XMLStructureCreator.DEBUG_MODE) {
						System.out.println("BadLocationException in endElement(...): " + ex); //$NON-NLS-1$
						System.out.println("fcurrentParent.getId(): " + fcurrentParent.getId()); //$NON-NLS-1$
					}
				}
			} else {
				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("Error: Cannot reach Parent of Parent"); //$NON-NLS-1$
			}
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("fcurrentParent is now " + fcurrentParent.getId()); //$NON-NLS-1$

			prevlocator= new LocatorImpl(locator);
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("Signature before cutting: " + fsignature); //$NON-NLS-1$
			int ssi= fsignature.lastIndexOf(SIGN_SEPARATOR);//fsignature
			// separator index
			ssi= fsignature.lastIndexOf(SIGN_SEPARATOR, ssi - 1);//second-last
			// ".", e.g. in
			// root.a.b. to
			// obtain
			// root.a.
			fsignature= fsignature.substring(0, ssi + 1);
			if (XMLStructureCreator.DEBUG_MODE)
				System.out.println("Signature after cutting: " + fsignature); //$NON-NLS-1$
		}

		//
		// ErrorHandler methods
		//

		/* Warning. */
		@Override
		public void warning(SAXParseException ex) {
			System.err.println("[Warning] " + //$NON-NLS-1$
					getLocationString(ex) + ": " + //$NON-NLS-1$
					ex.getMessage());
		}

		/* Error. */
		@Override
		public void error(SAXParseException ex) {
			System.err.println("[Error] " + //$NON-NLS-1$
					getLocationString(ex) + ": " + //$NON-NLS-1$
					ex.getMessage());
		}

		/* Fatal error. */
		@Override
		public void fatalError(SAXParseException ex) throws SAXException {
			System.err.println("[Fatal Error] " + //$NON-NLS-1$
					getLocationString(ex) + ": " + //$NON-NLS-1$
					ex.getMessage());
			//System.out.println(ex);
			//throw ex;
		}

		/* Returns a string of the location. */
		private String getLocationString(SAXParseException ex) {
			StringBuilder str= new StringBuilder();

			String systemId= ex.getSystemId();
			if (systemId != null) {
				int index= systemId.lastIndexOf('/');
				if (index != -1)
					systemId= systemId.substring(index + 1);
				str.append(systemId);
			}
			str.append(':');
			str.append(ex.getLineNumber());
			str.append(':');
			str.append(ex.getColumnNumber());

			return str.toString();

		}
	}

	public XMLStructureCreator() {
		//set default idmap
		fIdMapToUse= DEFAULT_IDMAP;
		fUseIdMap= false;
		XMLPlugin plugin= XMLPlugin.getDefault();
		//if statement required for tests
		if (plugin != null) {
			fIdMaps= plugin.getIdMaps();
			fIdMapsInternal= plugin.getIdMapsInternal();
			fIdExtensionToName= plugin.getIdExtensionToName();
			fOrderedElements= plugin.getOrderedElements();
			fOrderedElementsInternal= plugin.getOrderedElementsInternal();
		}
		fRemoveWhiteSpace= false;
	}

	/*
	 * This title will be shown in the title bar of the structure compare pane.
	 */
	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	/*
	 * Set File extension of the parsed file. This extension will be used to choose an Id Map scheme.
	 */
	public void setFileExtension(String ext) {
		fFileExt= ext;
	}

	/**
	 * Initialize the Id Mappings for the Id Mapping Scheme and the Ordered Elements
	 * This method must be called before getStructure(Object) is called on the two/three inputs of the compare
	 */
	public void initIdMaps() {
		if (fFirstCall && fFileExt != null) {
			fFirstCall= false;
			String fileExtLower= fFileExt.toLowerCase();
			if (fIdExtensionToName.containsKey(fileExtLower))
				setIdMap((String) fIdExtensionToName.get(fileExtLower));
		}

		setUseIdMap();
		fOrdered= null;
		if (!isUseIdMap())
			idMap= null;
		else if (fIdMaps.containsKey(fIdMapToUse)) {
			idMap= (HashMap) fIdMaps.get(fIdMapToUse);
		} else if (fIdMapsInternal.containsKey(fIdMapToUse)) {
			idMap= (HashMap) fIdMapsInternal.get(fIdMapToUse);
		}

		if (fOrderedElements != null)
			fOrdered= (ArrayList) fOrderedElements.get(fIdMapToUse);
		if (fOrdered == null && fOrderedElementsInternal != null)
			fOrdered= (ArrayList) fOrderedElementsInternal.get(fIdMapToUse);
	}

	/*
	 * Returns the XML parse tree of the input.
	 */
	@Override
	public IStructureComparator getStructure(Object input) {
		if (XMLStructureCreator.DEBUG_MODE)
			System.out.println("Starting parse"); //$NON-NLS-1$

		if (!(input instanceof IStreamContentAccessor))
			return null;

		IStreamContentAccessor sca= (IStreamContentAccessor) input;

		try {
			// Input parsed with parser.parse(new InputSource(sca.getContents));	

			String contents= readString(sca);
			if (contents == null)
				contents= ""; //$NON-NLS-1$
			fdoc= new Document(contents);

			fsignature= ROOT_ID + SIGN_SEPARATOR;
			XMLChildren root= new XMLChildren(TYPE_ELEMENT, ROOT_ID, "", (fsignature + SIGN_ELEMENT), fdoc, 0, fdoc.getLength()); //$NON-NLS-1$
			fcurrentParent= root;

			XMLHandler handler= new XMLHandler();

			try {
				//            	/* original xerces code
				//            	SAXParser parser = (SAXParser)Class.forName(parserName).newInstance();
				//            	*/
				//				XMLReader parser = XMLReaderFactory.createXMLReader(parserName);
				//				
				//	            parser.setFeature( "http://xml.org/sax/features/validation", setValidation); //$NON-NLS-1$
				//    	        parser.setFeature( "http://xml.org/sax/features/namespaces", setNameSpaces ); //$NON-NLS-1$
				//    	        /*
				//    	        parser.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
				//        	    parser.setFeature( "http://apache.org/xml/features/validation/schema", setSchemaSupport ); //$NON-NLS-1$
				//	            parser.setFeature( "http://apache.org/xml/features/validation/schema-full-checking", setSchemaFullSupport); //$NON-NLS-1$
				//	           	*/
				//	            parser.setContentHandler(handler);
				//	            parser.setErrorHandler(handler);
				//	            
				//	            parser.parse(new InputSource(sca.getContents()));

				SAXParserFactory factory= SAXParserFactory.newInstance();
				factory.setNamespaceAware(true);
				SAXParser parser= factory.newSAXParser();
				parser.parse(new InputSource(new StringReader(contents)), handler);

				if (XMLStructureCreator.DEBUG_MODE)
					System.out.println("End of parse"); //$NON-NLS-1$
			} catch (SAXParseException e) {
				XMLPlugin.log(e);
				return null;
			} catch (Exception e) {
				//				MessageDialog.openError(XMLPlugin.getActiveWorkbenchShell(),"Error in XML parser","An error occured in the XML parser.\nNo structured compare can be shown");
				XMLPlugin.log(e);
				return null;
			}
			return root;
		} catch (CoreException ex) {
			XMLPlugin.log(ex);
		}
		return null;
	}

	public boolean canSave() {
		return true;
	}

	public boolean canRewriteTree() {
		return false;
	}

	public void rewriteTree(Differencer differencer, IDiffContainer root) {
			// nothing to do
	}

	@Override
	public void save(IStructureComparator structure, Object input) {
		if (input instanceof IEditableContent && structure instanceof XMLNode) {
			IDocument document= ((XMLNode) structure).getDocument();
			IEditableContent bca= (IEditableContent) input;
			String contents= document.get();
			String encoding= null;
			if (input instanceof IEncodedStreamContentAccessor) {
				try {
					encoding= ((IEncodedStreamContentAccessor)input).getCharset();
				} catch (CoreException e1) {
					// ignore
				}
			}
			if (encoding == null)
				encoding= "UTF-8"; //$NON-NLS-1$
			try {
				bca.setContent(contents.getBytes(encoding));
			} catch (UnsupportedEncodingException e) {
				bca.setContent(contents.getBytes());	
			}
		}
	}

	@Override
	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof XMLNode) {
			String s= ((XMLNode) node).getValue();
			if (ignoreWhitespace)
				s= s.trim();
			return s;
		}
		return null;
	}

	@Override
	public IStructureComparator locate(Object path, Object source) {
		return null;
	}

	static String readString(IStreamContentAccessor sa) throws CoreException {
		String encoding= null;
		if (sa instanceof IEncodedStreamContentAccessor)
			encoding= ((IEncodedStreamContentAccessor) sa).getCharset();
		if (encoding == null)
			encoding= "UTF-8"; //$NON-NLS-1$
		try (InputStream is= sa.getContents()) {
			if (is == null) {
				return null;
			}
			return new String(is.readAllBytes(), encoding);
		} catch (IOException e) {
			// ignored
			return null;
		}
	}

	/* Returns a sorted list of attributes.
	 */
	protected Attributes sortAttributes(Attributes attrs) {

		AttributesImpl attributes= new AttributesImpl();
		int len= (attrs != null) ? attrs.getLength() : 0;
		for (int i= 0; i < len; i++) {
			String name= attrs.getQName(i);
			int count= attributes.getLength();
			int j= 0;
			while (j < count) {
				if (name.compareTo(attributes.getQName(j)) < 0)
					break;
				j++;
			}
			attributes.insertAttributeAt(j, name, attrs.getType(i), attrs.getValue(i));
		}

		return attributes;

	}

	public void setIdMap(String idmap_name) {
		fIdMapToUse= idmap_name;
	}

	/*
	 * Returns the name of the IdMap Scheme that will be used to set ids.
	 */
	public String getIdMap() {
		return fIdMapToUse;
	}

	public void setUseIdMap() {
		if (fIdMaps != null && fIdMapsInternal != null)
			fUseIdMap= fIdMaps.containsKey(fIdMapToUse) || fIdMapsInternal.containsKey(fIdMapToUse);
	}

	public boolean isUseIdMap() {
		return fUseIdMap;
	}

	public void updateIdMaps() {
		fIdMaps= XMLPlugin.getDefault().getIdMaps();
		fOrderedElements= XMLPlugin.getDefault().getOrderedElements();
	}

	protected boolean isWhiteSpace(char c) {
		return c == '\t' || c == '\n' || c == '\r' || c == ' ';
	}

	protected String removeWhiteSpace(String str) {
		str= trimWhiteSpace(str);
		StringBuilder retStr= new StringBuilder();
		int start= 0, end= 0;
		outer_while: while (true) {
			while (end < str.length() && !isWhiteSpace(str.charAt(end))) {
				end++;
			}
			if (end > str.length())
				break outer_while;
			if (start != 0)
				retStr.append(' ');
			retStr.append(str.substring(start, end));
			end++;
			while (end < str.length() && isWhiteSpace(str.charAt(end))) {
				end++;
			}
			start= end;
		}
		return retStr.toString();
	}

	protected String trimWhiteSpace(String str) {
		int start= 0, end= str.length() - 1;
		while (start < str.length() && isWhiteSpace(str.charAt(start))) {
			start++;
		}
		if (start == str.length())
			return ""; //$NON-NLS-1$
		while (end >= 0 && isWhiteSpace(str.charAt(end))) {
			end--;
		}
		return str.substring(start, end + 1);
	}

	public void setRemoveWhiteSpace(boolean removeWhiteSpace) {
		fRemoveWhiteSpace= removeWhiteSpace;
	}

	public boolean getRemoveWhiteSpace() {
		return fRemoveWhiteSpace;
	}
}
