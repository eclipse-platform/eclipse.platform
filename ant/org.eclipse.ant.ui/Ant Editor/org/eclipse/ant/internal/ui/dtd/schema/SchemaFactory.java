/*******************************************************************************
 * Copyright (c) 2002, 2013 Object Factory Inc.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *      IBM Corporation - Fix for bug 40951
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.ant.internal.ui.dtd.IModel;
import org.eclipse.ant.internal.ui.dtd.ISchema;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;

/**
 * SchemaFactory is a SAX DeclHandler that converts DTD ELEMENT and ATTLIST declarations to schema form on the fly. The only two methods available to
 * external users of SchemaFactory are its constructor and <code>getSchema()</code>. The latter returns the schema built by this process and should
 * not be called until the XML parser to which this handler is attached has finished parsing.
 * 
 * @author Bob Foster
 */
public class SchemaFactory implements DeclHandler {
	// used for parsing models
	private char[] fBuf;
	private int fLen;
	private int fPos;
	private Element fElement;

	private final Schema fSchema;
	private static HashSet<String> fTypes = new HashSet<>();
	private Exception fErrorException;
	static {
		fTypes.add("CDATA"); //$NON-NLS-1$
		fTypes.add("ID"); //$NON-NLS-1$
		fTypes.add("IDREF"); //$NON-NLS-1$
		fTypes.add("IDREFS"); //$NON-NLS-1$
		fTypes.add("NMTOKEN"); //$NON-NLS-1$
		fTypes.add("NMTOKENS"); //$NON-NLS-1$
		fTypes.add("ENTITY"); //$NON-NLS-1$
		fTypes.add("ENTITIES"); //$NON-NLS-1$
	}

	/**
	 * Constructor.
	 */
	public SchemaFactory() {
		fSchema = new Schema();
	}

	/**
	 * @return ISchema produced from the DeclHandler. The schema is always correct, though it may be incomplete if the parse was interrupted due to
	 *         validation or well-formed errors.
	 */
	public ISchema getSchema() {
		fSchema.setErrorException(fErrorException);
		return fSchema;
	}

	@Override
	public void attributeDecl(String eName, String aName, String type, String valueDefault, String value) {
		Element element = getElement(eName);
		Attribute attr = (Attribute) element.getAttributes().get(aName);
		if (attr == null) {
			attr = new Attribute(aName, element);
			element.addAttribute(attr);

			String[] enumeration = null;
			if (fTypes.contains(type))
				attr.setType(type);
			else if (type.startsWith("NOTATION")) //$NON-NLS-1$
				enumeration = parseValues(type.substring("NOTATION".length() + 1), ','); //$NON-NLS-1$
			else {
				type = stripSurroundingParentheses(type);
				enumeration = parseValues(type, '|');
			}
			attr.setEnum(enumeration);

			attr.setRequired(valueDefault == null || !valueDefault.equals("#IMPLIED")); //$NON-NLS-1$
			attr.setFixed(valueDefault != null && valueDefault.equals("#FIXED")); //$NON-NLS-1$
			attr.setDefault(value);
		}
	}

	/**
	 * Strips the surrounding parentheses from <code>aString</code>.
	 * <P>
	 * i.e.: {@code (true|false) -> true|false }
	 */
	private String stripSurroundingParentheses(String aString) {
		if (aString.startsWith("(")) { //$NON-NLS-1$
			aString = aString.substring(1);
		}
		if (aString.endsWith(")")) { //$NON-NLS-1$
			aString = aString.substring(0, aString.length() - 1);
		}
		return aString;
	}

	/**
	 * @param eName
	 *            Element name
	 * @return Element from schema or new element. Either way the element is in the schema upon return.
	 */
	private Element getElement(String eName) {
		Element element = (Element) fSchema.getElement(eName);
		if (element == null) {
			element = new Element(eName);
			fSchema.addElement(element);
		}
		return element;
	}

	private String[] parseValues(String type, char separator) {
		int start = 0, pos, len = type.length();
		LinkedList<String> values = new LinkedList<>();
		while (start < len) {
			pos = type.indexOf(separator, start);
			if (pos < 0)
				pos = len;
			String term = type.substring(start, pos);
			start = pos + 1;
			values.add(term);
		}
		return values.toArray(new String[values.size()]);
	}

	@Override
	public void elementDecl(String name, String model) throws SAXException {
		Element element = getElement(name);
		if (!element.isUndefined()) {
			// if the element has already been defined, this is an error
			throw new SAXException(MessageFormat.format(AntDTDSchemaMessages.SchemaFactory_Doubly_defined, new Object[] { name }));
		}

		fElement = element;
		if (model.equals("ANY")) { //$NON-NLS-1$
			element.setAny(true);
		} else if (model.equals("EMPTY")) { //$NON-NLS-1$
			element.setEmpty(true);
		} else if (model.equals("(#PCDATA)")) {//$NON-NLS-1$
			element.setText(true);
		} else {
			element.setContentModel(parseModel(model));
		}
	}

	/**
	 * Convert model string to IModel. The <code>fElement</code> variable is an implicit argument to this method, and it sets <code>fBuf</code>,
	 * <code>fPos</code> and <code>fLen</code> for use by other parser methods.
	 * 
	 * @param model
	 *            String from DTD, with parameter entities replaced.
	 * @return IModel
	 * @throws SAXException
	 *             if syntax error detected in model. This is a validation error. Since the DTD is usually not read unless the parser is validating,
	 *             we may not ever be handed a bad content model, but we need to check them, just the same.
	 */
	private IModel parseModel(String model) throws SAXException {
		fBuf = model.toCharArray();
		fLen = fBuf.length;
		if (fBuf[0] != '(') {
			throw new SAXException(MessageFormat.format(AntDTDSchemaMessages.SchemaFactory_Start_with_left_parenthesis, new Object[] {
					fElement.getName() }));
		}

		boolean ortext = model.startsWith("(#PCDATA|"); //$NON-NLS-1$
		if (ortext) {
			fPos = 8; // "(#PCDATA".length()
		} else {
			fPos = 0;
		}
		IModel emodel = scanExpr();
		return emodel;
	}

	/**
	 * Scan a parenthesized expression starting from the left parenthesis or leftmost operator.
	 * 
	 * @return IModel
	 */
	private IModel scanExpr() throws SAXException {
		// skip opening ( or |
		fPos++;
		return scanExpr(scanElement());
	}

	/**
	 * Scan a parenthesized expression with the first term in hand.
	 * 
	 * @param term
	 *            The first operand in the expression, pre-scanned.
	 * @return IModel
	 * @throws SAXException
	 *             if errors are detected in the model.
	 */
	private IModel scanExpr(IModel term) throws SAXException {
		checkLen();
		if (fBuf[fPos] != ')') {
			char op = fBuf[fPos];
			if (op != '|' && op != ',') {
				throw new SAXException(MessageFormat.format(AntDTDSchemaMessages.SchemaFactory_Expecting_operator_or_right_parenthesis, new Object[] {
						fElement.getName(), String.valueOf(fBuf) }));
			}
			Model model = new Model(op == '|' ? IModel.CHOICE : IModel.SEQUENCE);
			model.addModel(term);
			term = model;

			while (fBuf[fPos] == op) {
				fPos++;
				IModel next = scanElement();
				model.addModel(next);
			}
			if (fBuf[fPos] != ')') {
				throw new SAXException(MessageFormat.format(AntDTDSchemaMessages.SchemaFactory_Expecting_operator_or_right_parenthesis, new Object[] {
						fElement.getName(), String.valueOf(fBuf) }));
			}
			fPos++;
		}
		return term;
	}

	/**
	 * Scan an element name or a parenthesized sub-expression.
	 * 
	 * @return IModel
	 */
	private IModel scanElement() throws SAXException {
		checkLen();
		if (fBuf[fPos] == '(')
			return scanExpr();
		StringBuilder sb = new StringBuilder();
		while (fBuf[fPos] != '|' && fBuf[fPos] != ',' && fBuf[fPos] != ')' && fBuf[fPos] != '*' && fBuf[fPos] != '+' && fBuf[fPos] != '?') {
			sb.append(fBuf[fPos++]);
			checkLen();
		}
		String name = sb.toString();
		Element element = getElement(name);
		Model model = new Model(IModel.LEAF);
		model.setLeaf(element);
		return model;
	}

	private void checkLen() throws SAXException {
		if (fPos == fLen) {
			throw new SAXException(MessageFormat.format(AntDTDSchemaMessages.SchemaFactory_Unexpected_end, new Object[] { fElement.getName(),
					String.valueOf(fBuf) }));
		}
	}

	@Override
	public void externalEntityDecl(String name, String publicId, String systemId) {
		// do nothing
	}

	@Override
	public void internalEntityDecl(String name, String value) {
		// do nothing
	}

	public void setErrorException(Exception e) {
		fErrorException = e;
	}
}
