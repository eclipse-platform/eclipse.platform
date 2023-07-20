/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 559885
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.FilterHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.cheatsheets.AbstractItemExtensionElement;
import org.eclipse.ui.internal.cheatsheets.CheatSheetEvaluationContext;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.parser.CompositeCheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.composite.parser.IStatusContainer;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetItemExtensionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parser for the cheatsheet content files.
 *
 * Construct an intance of the CheatSheetDomParser.
 * Call <code>parse()</code>.
 * Then get the content items by calling
 * <code>getIntroItem()</code> and <code>getItemsList()</code>.
 * The title of the cheatsheet can be retrieved by calling
 * <code>getTitle()</code>.
 *
 */
public class CheatSheetParser implements IStatusContainer {

	private static final String TRUE_STRING = "true"; //$NON-NLS-1$

	private DocumentProcessor processor;
	private ArrayList<CheatSheetItemExtensionElement> itemExtensionContainerList;

	// Cheatsheet kinds that can be parsed
	public static final int COMPOSITE_ONLY = 1;
	public static final int SIMPLE_ONLY = 2;
	public static final int ANY = 3;

	private IStatus status;

	private int commandCount;

	private int actionCount;

	/**
	 *  Gets the status of the last call to parse()
	 */
	public IStatus getStatus() {
		return status;
	}

	@Override
	public void addStatus(int severity, String message, Throwable exception) {
		status = ParserStatusUtility.addStatus(status, severity, message, exception);
	}

	/**
	 * Converts any characters required to escaped by an XML parser to their
	 * escaped counterpart.
	 *
	 * <pre>
	 * {@code
	 * Characters           XML escaped counterpart
	 * <            ->      &lt;
	 * >            ->      &gt;
	 * &            ->      &amp;
	 * '            ->      &apos;
	 * "            ->      &quot;
	 * }
	 * </pre>
	 *
	 * Tags that will be ignored {@literal <b>, </b>, <a>, </a> and <br/>
	 * }.
	 *
	 * @param text
	 *            the string buffer to have its characters escaped
	 * @return string buffer with any of the characters requiring XML escaping
	 *         escaped
	 */
	private StringBuilder escapeXMLCharacters(StringBuilder text) {
		// Set the maximum length of the tags to ignore
		final int MAXIMUM_TAG_LENGTH = 5;

		// Keep a local variable for the orignal string's length
		int length = text.length();

		// Create the buffer to store the resulting string
		StringBuilder result = new StringBuilder(length);

		boolean hyperlinkContext = false;

		// Loop for the characters of the original string
		for(int i=0; i<length; i++) {
			// Grab the next character and determine how to handle it
			char c = text.charAt(i);
			switch (c) {
				case '<': {
					// We have a less than, grab the maximum tag length of characters
					// or the remaining characters which follow and determine if it
					// is the start of a tag to ignore.
					String tmp = ICheatSheetResource.EMPTY_STRING;
					if(i+MAXIMUM_TAG_LENGTH < length)
						tmp = text.substring(i, i+MAXIMUM_TAG_LENGTH).toLowerCase();
					else {
						tmp = text.substring(i, length).toLowerCase();
					}
					if (hyperlinkContext || tmp.startsWith(IParserTags.HYPERLINK_START_TAG)) {
						result.append(c);
						hyperlinkContext = true;
					} else if (tmp.startsWith(IParserTags.BOLD_START_TAG) || tmp.startsWith(IParserTags.BOLD_END_TAG)
							|| tmp.startsWith(IParserTags.BREAK_TAG)) {
						// We have a tag to ignore so just emit the character
						result.append(c);
					} else {
						// We have detemined that it is just a less than
						// so emit the XML escaped counterpart
						result.append(IParserTags.LESS_THAN);
					}
					break; }
				case '>': {
					// We have a greater than, grab the maximum tag length of characters
					// or the starting characters which come before and determine if it
					// is the end of a tag to ignore.
					String tmp = ICheatSheetResource.EMPTY_STRING;
					if(i>=MAXIMUM_TAG_LENGTH) {
						tmp = text.substring(i-MAXIMUM_TAG_LENGTH, i+1).toLowerCase();
					} else {
						tmp = text.substring(0, i+1).toLowerCase();
					}
					if (tmp.endsWith(IParserTags.HYPERLINK_END_TAG)) {
						result.append(c);
						hyperlinkContext = false;
						break;
					}
					if (hyperlinkContext) {
						result.append(c);
						break;
					}
					if(tmp.endsWith(IParserTags.BOLD_START_TAG) || tmp.endsWith(IParserTags.BOLD_END_TAG) || tmp.endsWith(IParserTags.BREAK_TAG)) {
						// We have a tag to ignore so just emit the character
						result.append(c);
					} else {
						// We have detemined that it is just a greater than
						// so emit the XML escaped counterpart
						result.append(IParserTags.GREATER_THAN);
					}
					break; }
				case '&':
					// We have an ampersand so emit the XML escaped counterpart
					result.append(IParserTags.AMPERSAND);
					break;
				case '\'':
					// We have an apostrophe so emit the XML escaped counterpart
					result.append(IParserTags.APOSTROPHE);
					break;
				case '"':
					// We have a quote so emit the XML escaped counterpart
					result.append(hyperlinkContext ? c : IParserTags.QUOTE);
					break;
				case '\t':
					// We have a tab, replace with a space
					result.append(' ');
					break;
				default:
					// We have a character that does not require escaping
					result.append(c);
					break;
			}
		}
		return result;
	}

	private Node findNode(Node startNode, String nodeName) {
		if(startNode == null) {
			return null;
		}

		if(startNode.getNodeName().equals(nodeName)) {
			return startNode;
		}

		NodeList nodes = startNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals(nodeName)) {
				return node;
			}
		}

		return null;
	}

	private void handleExecutable(IExecutableItem item, Node executableNode, AbstractExecutable executable) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(executableNode);

		String[] params = null;

		if (executable instanceof CheatSheetCommand) {
			commandCount++;
		}
		if (executable instanceof Action) {
			actionCount++;
		}

		NamedNodeMap attributes = executableNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;
				if (attributeName.equals(IParserTags.CONFIRM)) {
					executable.setConfirm(attribute.getNodeValue().equals(TRUE_STRING));}
				else if (attributeName.equals(IParserTags.WHEN)) {
					executable.setWhen(attribute.getNodeValue());
				} else if (attributeName.equals(IParserTags.REQUIRED)) {
					executable.setRequired(attribute.getNodeValue().equals(TRUE_STRING));
				} else if (attributeName.equals(IParserTags.TRANSLATE)) {
					// Translation hint, no semantic effect
				} else if (executable.hasParams() && attributeName.startsWith(IParserTags.PARAM)) {
					try {
						if(params == null) {
							params = new String[9];
						}
						String paramNum = attributeName.substring(IParserTags.PARAM.length());
						int num = Integer.parseInt(paramNum)-1;

						if(num>-1 && num<9){
							params[num] = attribute.getNodeValue();
						} else {
							String message = NLS.bind(Messages.ERROR_PARSING_PARAM_INVALIDRANGE, (new Object[] {attributeName, paramNum}));
							addStatus(IStatus.ERROR, message, null);
						}
					} catch(NumberFormatException e) {
						String message = Messages.ERROR_PARSING_PARAM_INVALIDNUMBER;
						addStatus(IStatus.ERROR, message, e);
					}
				} else if (!executable.handleAttribute(attribute)) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, executableNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
			String errorMessage = executable.checkAttributes(executableNode);
			if (errorMessage != null) {
				throw new CheatSheetParserException(errorMessage);
			}
		}
		checkForNoChildren(executableNode);
		executable.setParams(params);
		item.setExecutable(executable);
	}

	private void checkForNoChildren(Node parentNode) {
		NodeList nodes = parentNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
				String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), parentNode.getNodeName()}));
				addStatus(IStatus.WARNING, message, null);
			}
		}
	}

	private void handleCheatSheetAttributes(CheatSheet cheatSheet, Node cheatSheetNode) throws CheatSheetParserException {
		Assert.isNotNull(cheatSheet);
		Assert.isNotNull(cheatSheetNode);
		Assert.isTrue(cheatSheetNode.getNodeName().equals(IParserTags.CHEATSHEET));

		boolean title = false;

		NamedNodeMap attributes = cheatSheetNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				if (attributeName.equals(IParserTags.TITLE)) {
					title = true;
					cheatSheet.setTitle(attribute.getNodeValue());
				} else {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, cheatSheetNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!title) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_TITLE, (new Object[] {cheatSheetNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}
	}

	private void handleConditionalSubItem(Item item, Node conditionalSubItemNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(conditionalSubItemNode);
		Assert.isTrue(conditionalSubItemNode.getNodeName().equals(IParserTags.CONDITIONALSUBITEM));

		ConditionalSubItem conditionalSubItem = new ConditionalSubItem();

		boolean condition = false;

		// Handle attributes
		NamedNodeMap attributes = conditionalSubItemNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				if (attributeName.equals(IParserTags.CONDITION)) {
					condition = true;
					conditionalSubItem.setCondition(attribute.getNodeValue());
				} else {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, conditionalSubItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!condition) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_CONDITION, (new Object[] {conditionalSubItemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		boolean subitem = false;

		// Handle nodes
		NodeList nodes = conditionalSubItemNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);

			if(node.getNodeName().equals(IParserTags.SUBITEM)) {
				subitem = true;
				handleSubItem(conditionalSubItem, node);
			} else {
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), conditionalSubItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!subitem) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_SUBITEM, (new Object[] {conditionalSubItemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		item.addSubItem(conditionalSubItem);
	}

	private void handleDescription(Item item, Node startNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(startNode);

		Node descriptionNode = findNode(startNode, IParserTags.DESCRIPTION);

		if(descriptionNode != null) {
			String text = handleMarkedUpText(descriptionNode, startNode, IParserTags.DESCRIPTION);
			item.setDescription(text);
		} else {
			Node parentNode = startNode;
			if( startNode.getNodeName().equals(IParserTags.DESCRIPTION) ) {
				parentNode = startNode.getParentNode();
			}
			String message = NLS.bind(Messages.ERROR_PARSING_NO_DESCRIPTION, (new Object[] {parentNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}
	}

	private void handleSubItemDescription(SubItem subItem, Node startNode) throws CheatSheetParserException {
		Assert.isNotNull(subItem);
		Assert.isNotNull(startNode);

		Node descriptionNode = findNode(startNode, IParserTags.DESCRIPTION);

		if(descriptionNode != null) {
			String text = handleMarkedUpText(descriptionNode, startNode, IParserTags.DESCRIPTION);
			subItem.setLabel(text);
			subItem.setFormatted(true);
		}
	}

	private String handleMarkedUpText(Node nodeContainingText, Node startNode, String nodeName ) {
		NodeList nodes = nodeContainingText.getChildNodes();
		StringBuilder text = new StringBuilder();

		boolean containsMarkup = false;

		// The documentation for the content file specifies
		// that leading whitespace should be ignored at the
		// beginning of a description or after a <br/>. This
		// applies also to <onCompletion> elements.
		// See Bug 129208 and Bug 131185
		boolean isLeadingTrimRequired = true;

		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeType() == Node.TEXT_NODE) {
				String nodeValue = node.getNodeValue();
				if (isLeadingTrimRequired) {
					nodeValue = trimLeadingWhitespace(nodeValue);
				}
				text.append(nodeValue);
				isLeadingTrimRequired = false;
			} else if(node.getNodeType() == Node.ELEMENT_NODE) {
				// handle <b></b>, <br/> and <a></a>
				switch (node.getNodeName()) {
				case IParserTags.BOLD:
					containsMarkup = true;
					text.append(IParserTags.BOLD_START_TAG);
					text.append(node.getFirstChild().getNodeValue());
					text.append(IParserTags.BOLD_END_TAG);
					isLeadingTrimRequired = false;
					break;
				case IParserTags.BREAK:
					containsMarkup = true;
					text.append(IParserTags.BREAK_TAG);
					isLeadingTrimRequired = true;
					break;
				case IParserTags.HYPERLINK:
					containsMarkup = true;
					if (node.getAttributes().getNamedItem(IParserTags.HREF) == null) {
						text.append(NLS.bind(IParserTags.HYPERLINK_PLACEHOLDER, "")); //$NON-NLS-1$
					} else {
						String href = node.getAttributes().getNamedItem(IParserTags.HREF).getNodeValue();
						String link = NLS.bind(IParserTags.HYPERLINK_PLACEHOLDER, href);
						text.append(link);
					}
					text.append(node.getTextContent());
					text.append(IParserTags.HYPERLINK_END_TAG);
					isLeadingTrimRequired = false;
					break;
				default:
					warnUnknownMarkupElement(startNode, nodeName, node);
					break;
				}
			}
		}

		if(containsMarkup) {
			text = escapeXMLCharacters(text);
			text.insert(0, IParserTags.FORM_START_TAG);
			text.append(IParserTags.FORM_END_TAG);
		} else {
			deTab(text);
		}

		// Remove the new line, form feed and tab chars
		return text.toString().trim();
	}

	// Replace any tabs with spaces

	private void deTab(StringBuilder text) {
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\t') {
				text.setCharAt(i, ' ');
			}
		}
	}

	private String trimLeadingWhitespace(String nodeValue) {
		int firstNonWhitespaceIndex = 0;
		while (firstNonWhitespaceIndex < nodeValue.length() &&
				Character.isWhitespace(nodeValue.charAt(firstNonWhitespaceIndex))) {
			firstNonWhitespaceIndex++;
		}
		if (firstNonWhitespaceIndex > 0) {
			return nodeValue.substring(firstNonWhitespaceIndex);
		}
		return nodeValue;
	}

	/*
	 * Write a warning to the log
	 */
	private void warnUnknownMarkupElement(Node startNode, String nodeName, Node node) {
		Node parentNode = startNode;
		if( startNode.getNodeName().equals(nodeName) ) {
			parentNode = startNode.getParentNode();
		}
		String message;
		if (IParserTags.DESCRIPTION.equals(nodeName)) {
			message = NLS.bind(Messages.WARNING_PARSING_DESCRIPTION_UNKNOWN_ELEMENT, (new Object[] {parentNode.getNodeName(), node.getNodeName()}));
		} else {
			message = NLS.bind(Messages.WARNING_PARSING_ON_COMPLETION_UNKNOWN_ELEMENT, (new Object[] {parentNode.getNodeName(), node.getNodeName()}));
		}
		addStatus(IStatus.WARNING, message, null);

	}

	private void handleOnCompletion(Item item, Node onCompletionNode) {
		String text = handleMarkedUpText(onCompletionNode, onCompletionNode, IParserTags.ON_COMPLETION);
		item.setCompletionMessage(text);
	}

	private void handleIntroNode(CheatSheet cheatSheet, Node introNode)
			throws CheatSheetParserException {
		Item introItem = new Item();
		introItem.setTitle(Messages.CHEAT_SHEET_INTRO_TITLE);

		handleIntroAttributes(introItem, introNode);

		boolean hasDescription = false;

		NodeList nodes = introNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);

			if(node.getNodeName().equals(IParserTags.DESCRIPTION)) {
				if (hasDescription) {
					String message = NLS.bind(Messages.ERROR_PARSING_MULTIPLE_DESCRIPTION, (new Object[] {introNode.getNodeName()}));
					addStatus(IStatus.ERROR, message, null);
				} else {
					hasDescription = true;
					handleDescription(introItem, node);
				}
			} else {
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), introNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!hasDescription) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_DESCRIPTION, (new Object[] {introNode.getNodeName()}));
			addStatus(IStatus.ERROR, message, null);
		}

		cheatSheet.setIntroItem(introItem);
	}

	private void handleIntroAttributes(Item item, Node introNode) {
		Assert.isNotNull(item);
		Assert.isNotNull(introNode);

		NamedNodeMap attributes = introNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				switch (attributeName) {
				case IParserTags.CONTEXTID:
					item.setContextId(attribute.getNodeValue());
					break;
				case IParserTags.HREF:
					item.setHref(attribute.getNodeValue());
					break;
				default:
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, introNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
					break;
				}
			}
		}
	}

	private Item handleItem(Node itemNode) throws CheatSheetParserException {
		Assert.isNotNull(itemNode);
		Assert.isTrue(itemNode.getNodeName().equals(IParserTags.ITEM));

		Item item = new Item();

		handleItemAttributes(item, itemNode);

		boolean hasDescription = false;

		NodeList nodes = itemNode.getChildNodes();

		IncompatibleSiblingChecker checker = new IncompatibleSiblingChecker(this, itemNode);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			checker.checkElement(node.getNodeName());
			switch (node.getNodeName()) {
			case IParserTags.ACTION:
				handleExecutable(item, node, new Action());
				break;
			case IParserTags.COMMAND:
				handleExecutable(item, node, new CheatSheetCommand());
				break;
			case IParserTags.DESCRIPTION:
				if (hasDescription) {
					String message = NLS.bind(Messages.ERROR_PARSING_MULTIPLE_DESCRIPTION, (new Object[] {itemNode.getNodeName()}));
					addStatus(IStatus.ERROR, message, null);
				} else {
					hasDescription = true;
					handleDescription(item, node);
				}
				break;
			case IParserTags.ON_COMPLETION:
				handleOnCompletion(item, node);
				break;
			case IParserTags.SUBITEM:
				handleSubItem(item, node);
				break;
			case IParserTags.CONDITIONALSUBITEM:
				handleConditionalSubItem(item, node);
				break;
			case IParserTags.REPEATEDSUBITM:
				handleRepeatedSubItem(item, node);
				break;
			case IParserTags.PERFORMWHEN:
				handlePerformWhen(item, node);
				break;
			default:
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), itemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
				break;
			}
		}

		if(!hasDescription) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_DESCRIPTION, (new Object[] {itemNode.getNodeName()}));
			addStatus(IStatus.ERROR, message, null);
		}

		return item;
	}

	private void handleItemAttributes(Item item, Node itemNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(itemNode);

		ArrayList<AbstractItemExtensionElement[]> itemExtensionElements = new ArrayList<>();

		boolean title = false;

		NamedNodeMap attributes = itemNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				switch (attributeName) {
				case IParserTags.TITLE:
					title = true;
					item.setTitle(attribute.getNodeValue());
					break;
				case IParserTags.CONTEXTID:
					item.setContextId(attribute.getNodeValue());
					break;
				case IParserTags.HREF:
					item.setHref(attribute.getNodeValue());
					break;
				case IParserTags.SKIP:
					item.setSkip(attribute.getNodeValue().equals(TRUE_STRING));
					break;
				case IParserTags.DIALOG:
					item.setDialog(attribute.getNodeValue().equals(TRUE_STRING));
					break;
				default:
					AbstractItemExtensionElement[] ie = handleUnknownItemAttribute(attribute, itemNode);
					if (ie != null) {
						itemExtensionElements.add(ie);
					} else {
						String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, itemNode.getNodeName()}));
						addStatus(IStatus.WARNING, message, null);
					}
					break;
				}
			}
		}

		if(!title) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_TITLE, (new Object[] {itemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		if (itemExtensionElements != null)
			item.setItemExtensions(itemExtensionElements);
	}

	private void handlePerformWhen(IPerformWhenItem item, Node performWhenNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(performWhenNode);
		Assert.isTrue(performWhenNode.getNodeName().equals(IParserTags.PERFORMWHEN));

		PerformWhen performWhen = new PerformWhen();

		 boolean condition = false;

		// Handle attributes
		NamedNodeMap attributes = performWhenNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				if (attributeName.equals(IParserTags.CONDITION)) {
					condition = true;
					performWhen.setCondition(attribute.getNodeValue());
				} else {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, performWhenNode.getNodeName()}));
					addStatus(IStatus.WARNING,message, null);
				}
			}
		}

		if(!condition) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_CONDITION, (new Object[] {performWhenNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		boolean exeutable = false;

		// Handle nodes
		NodeList nodes = performWhenNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			switch (node.getNodeName()) {
			case IParserTags.ACTION:
				exeutable = true;
				handleExecutable(performWhen, node, new Action());
				break;
			case IParserTags.COMMAND:
				exeutable = true;
				handleExecutable(performWhen, node, new CheatSheetCommand());
				break;
			default:
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), performWhenNode .getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
				break;
			}
		}

		if(!exeutable) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_ACTION, (new Object[] {performWhenNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		item.setPerformWhen(performWhen);
	}

	private void handleRepeatedSubItem(Item item, Node repeatedSubItemNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(repeatedSubItemNode);
		Assert.isTrue(repeatedSubItemNode.getNodeName().equals(IParserTags.REPEATEDSUBITM));

		RepeatedSubItem repeatedSubItem = new RepeatedSubItem();

		boolean values = false;

		// Handle attributes
		NamedNodeMap attributes = repeatedSubItemNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				if (attributeName.equals(IParserTags.VALUES)) {
					values = true;
					repeatedSubItem.setValues(attribute.getNodeValue());
				} else {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, repeatedSubItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!values) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_VALUES, (new Object[] {repeatedSubItemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		boolean subitem = false;

		// Handle nodes
		NodeList nodes = repeatedSubItemNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);

			if(node.getNodeName().equals(IParserTags.SUBITEM)) {
				subitem = true;
				handleSubItem(repeatedSubItem, node);
			} else {
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), repeatedSubItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		if(!subitem) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_SUBITEM, (new Object[] {repeatedSubItemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}

		item.addSubItem(repeatedSubItem);
	}

	private void handleSubItem(ISubItemItem item, Node subItemNode) throws CheatSheetParserException {
		Assert.isNotNull(item);
		Assert.isNotNull(subItemNode);
		Assert.isTrue(subItemNode.getNodeName().equals(IParserTags.SUBITEM));

		SubItem subItem = new SubItem();

		IncompatibleSiblingChecker checker = new IncompatibleSiblingChecker(this, subItemNode);

		NodeList nodes = subItemNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			checker.checkElement(node.getNodeName());

			switch (node.getNodeName()) {
			case IParserTags.ACTION:
				handleExecutable(subItem, node, new Action());
				break;
			case IParserTags.COMMAND:
				handleExecutable(subItem, node, new CheatSheetCommand());
				break;
			case IParserTags.PERFORMWHEN:
				handlePerformWhen(subItem, node);
				break;
			case IParserTags.DESCRIPTION:
				if (subItem.isFormatted()) {
					String message = NLS.bind(
							Messages.ERROR_PARSING_MULTIPLE_DESCRIPTION,
							(new Object[] { node.getNodeName() }));
					addStatus(IStatus.ERROR, message, null);
				} else {
					handleSubItemDescription(subItem, node);
				}
				break;
			default:
				if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), subItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
				}
				break;
			}
		}
		handleSubItemAttributes(subItem, subItemNode);
		item.addSubItem(subItem);
	}

	private void handleSubItemAttributes(SubItem subItem, Node subItemNode) throws CheatSheetParserException {
		Assert.isNotNull(subItem);
		Assert.isNotNull(subItemNode);

		NamedNodeMap attributes = subItemNode.getAttributes();
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;

				switch (attributeName) {
				case IParserTags.LABEL:
					subItem.setLabel(attribute.getNodeValue());
					break;
				case IParserTags.SKIP:
					subItem.setSkip(attribute.getNodeValue().equals(TRUE_STRING));
					break;
				case IParserTags.WHEN:
					subItem.setWhen(attribute.getNodeValue());
					break;
				default:
					String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {attributeName, subItemNode.getNodeName()}));
					addStatus(IStatus.WARNING, message, null);
					break;
				}
			}
		}

		if(subItem.getLabel() == null) {
			String message = NLS.bind(Messages.ERROR_PARSING_NO_LABEL, (new Object[] {subItemNode.getNodeName()}));
			throw new CheatSheetParserException(message);
		}
	}

	private AbstractItemExtensionElement[] handleUnknownItemAttribute(Node item, Node node) {
		ArrayList<AbstractItemExtensionElement> al = new ArrayList<>();
		if (itemExtensionContainerList == null)
			return null;

		for (int i = 0; i < itemExtensionContainerList.size(); i++) {
			CheatSheetItemExtensionElement itemExtensionElement = itemExtensionContainerList.get(i);

			if (itemExtensionElement.getItemAttribute().equals(item.getNodeName())) {
				AbstractItemExtensionElement itemElement = itemExtensionElement.createInstance();
				if(itemElement != null) {
					itemElement.handleAttribute(item.getNodeValue());
					al.add(itemElement);
				}
			}
		}

		if(al.isEmpty()) {
			String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ATTRIBUTE, (new Object[] {item.getNodeName(), node.getNodeName()}));
			addStatus(IStatus.WARNING, message, null);
		}
		return al.toArray(new AbstractItemExtensionElement[al.size()]);
	}

	public ICheatSheet parse(URL url, String pluginId, int cheatSheetKind) {
		return parse(new ParserInput(url, pluginId, null), cheatSheetKind);
	}

	public ICheatSheet parse(ParserInput input, int cheatSheetKind) {
		status = Status.OK_STATUS;
		commandCount = 0;
		actionCount = 0;
		if(input == null) {
			return null;
		}
		if (input.getErrorMessage() != null) {
			addStatus(IStatus.ERROR, input.getErrorMessage(), null);
		}

		InputStream is = null;
		InputSource inputSource = null;
		String filename = ""; //$NON-NLS-1$
		URL url = input.getUrl();

		if (input.getXml() != null) {
			StringReader reader = new StringReader(input.getXml());
			inputSource = new InputSource(reader);
		} else if (input.getUrl() != null){
			try {
				is = url.openStream();

				if (is != null) {
					inputSource = new InputSource(is);
				}
			} catch (Exception e) {
				String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {url.getFile()}));
				addStatus(IStatus.ERROR, message, e);
				return null;
			}
		} else {
			return null;
		}

		if (input.getUrl() != null){
			filename = url.getFile();
		}

		Document document;
		try {
			document = LocalEntityResolver.parse(inputSource);
		} catch (ParserConfigurationException e) {
			addStatus(IStatus.ERROR, Messages.ERROR_DOCUMENT_BUILDER_NOT_INIT, null);
			return null;
		} catch (IOException e) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {filename}));
			addStatus(IStatus.ERROR, message, e);
			return null;
		} catch (SAXParseException spe) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING_WITH_LOCATION, (new Object[] { filename,
					Integer.valueOf(spe.getLineNumber()), Integer.valueOf(spe.getColumnNumber()) }));
			addStatus(IStatus.ERROR, message, spe);
			return null;
		} catch (SAXException se) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING, (new Object[] {filename}));
			addStatus(IStatus.ERROR, message, se);
			return null;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}

		// process dynamic content, normalize paths
		if (processor == null) {
			DocumentReader reader = new DocumentReader();
			processor = new DocumentProcessor(new ProcessorHandler[] {
				new FilterHandler(CheatSheetEvaluationContext.getContext()),
				new NormalizeHandler(),
				new IncludeHandler(reader, Platform.getNL()),
				new ExtensionHandler(reader, Platform.getNL())
			});
		}
		String documentPath = null;
		if (input.getPluginId() != null) {
			documentPath = '/' + input.getPluginId() + input.getUrl().getPath();
		}
		processor.process(UAElementFactory.newElement(document.getDocumentElement()), documentPath);

		if ( cheatSheetKind == COMPOSITE_ONLY  ||  (cheatSheetKind == ANY && isComposite(document))) {
			CompositeCheatSheetParser compositeParser = new CompositeCheatSheetParser();
			CompositeCheatSheetModel result = compositeParser.parseCompositeCheatSheet(document, input.getUrl());
			status = compositeParser.getStatus();
			return result;
		}
		try {
			return parseCheatSheet(document);
		} catch(CheatSheetParserException e) {
			addStatus(IStatus.ERROR, e.getMessage(), e);
		}
		return null;
	}

	private boolean isComposite(Document document) {
		if (document != null) {
			Node rootnode = document.getDocumentElement();
			// Is the root node compositeCheatsheet?
			return rootnode.getNodeName().equals(ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET) ;
		}
		return false;
	}

	private CheatSheet parseCheatSheet(Document document) throws CheatSheetParserException {
		// If the document passed is null return a null tree and update the status
		if (document != null) {
			Node rootnode = document.getDocumentElement();

			// Is the root node really <cheatsheet>?
			if( !rootnode.getNodeName().equals(IParserTags.CHEATSHEET) ) {
				throw new CheatSheetParserException(Messages.ERROR_PARSING_CHEATSHEET_ELEMENT);
			}

			// Create the cheat sheet model object
			CheatSheet cheatSheet = new CheatSheet();

			handleCheatSheetAttributes(cheatSheet, rootnode);

			boolean hasItem = false;
			boolean hasIntro = false;

			CheatSheetRegistryReader reader = CheatSheetRegistryReader.getInstance();
			itemExtensionContainerList = reader.readItemExtensions();

			NodeList nodes = rootnode.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);

				switch (node.getNodeName()) {
				case IParserTags.ITEM:
					hasItem = true;
					Item item = handleItem(node);
					cheatSheet.addItem(item);
					break;
				case IParserTags.INTRO:
					if (hasIntro) {
						addStatus(IStatus.ERROR, Messages.ERROR_PARSING_MORE_THAN_ONE_INTRO, null);
					} else {
						hasIntro = true;
						handleIntroNode(cheatSheet, node);
					}
					break;
				default:
					if(node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE ) {
						String message = NLS.bind(Messages.WARNING_PARSING_UNKNOWN_ELEMENT, (new Object[] {node.getNodeName(), rootnode.getNodeName()}));
						addStatus(IStatus.WARNING, message, null);
					}
					break;
				}
			}

			if(!hasIntro) {
				addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_INTRO, null);
			}
			if(!hasItem) {
				addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_ITEM, null);
			}

			//handleIntro(cheatSheet, document);

			//handleItems(cheatSheet, document);

			if (status.getSeverity() == IStatus.ERROR) {
				return null;
			}

			cheatSheet.setContainsCommandOrAction(actionCount != 0 || commandCount != 0);
			return cheatSheet;
		}
		throw new CheatSheetParserException(Messages.ERROR_PARSING_CHEATSHEET_CONTENTS);
	}

	/*
	 * Normalizes composite cheat sheet-relative paths to simple cheat sheets into fully
	 * qualified paths, e.g. for the path "tasks/mySimpleCheatSheet.xml" in composite cheat
	 * sheet "/my.plugin/cheatsheets/myCompositeCheatSheet.xml", this normalizes to
	 * "/my.plugin/cheatsheets/tasks/mySimpleCheatSheet.xml".
	 *
	 * This is necessary because with dynamic content we are pulling in tasks from other
	 * plug-ins and those tasks have relative paths. It also only applies for cheat sheets
	 * located in running plug-ins.
	 */
	private static class NormalizeHandler extends ProcessorHandler {

		private static final String ELEMENT_PARAM = "param"; //$NON-NLS-1$
		private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
		private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
		private static final String NAME_PATH = "path"; //$NON-NLS-1$

		@Override
		public short handle(UAElement element, String id) {
			if (id != null && ELEMENT_PARAM.equals(element.getElementName())) {
				String name = element.getAttribute(ATTRIBUTE_NAME);
				if (NAME_PATH.equals(name)) {
					String value = element.getAttribute(ATTRIBUTE_VALUE);
					if (value != null) {
						int index = id.lastIndexOf('/');
						element.setAttribute(ATTRIBUTE_VALUE, id.substring(0, index + 1) + value);
					}
				}
				return HANDLED_CONTINUE;
			}
			return UNHANDLED;
		}
	}
}
