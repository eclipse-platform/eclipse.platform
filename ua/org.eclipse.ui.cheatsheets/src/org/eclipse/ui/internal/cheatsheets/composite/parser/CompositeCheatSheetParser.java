/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.composite.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParserException;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.data.ParserStatusUtility;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CompositeCheatSheetParser implements IStatusContainer {

	private DocumentBuilder documentBuilder;

	private IStatus status;

	private int nextTaskId = 0;


	/**
	 *  Gets the status of the last call to parseGuide
	 */

	public IStatus getStatus() {
		return status;
	}

	/**
	 * Returns the DocumentBuilder to be used by composite cheat sheets.
	 */
	public DocumentBuilder getDocumentBuilder() {
		if(documentBuilder == null) {
			try {
				documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				documentBuilder.setEntityResolver(new LocalEntityResolver());
			} catch (Exception e) {
				addStatus(IStatus.ERROR, Messages.ERROR_CREATING_DOCUMENT_BUILDER, e);
			}
		}
		return documentBuilder;
	}

	@Override
	public void addStatus(int severity, String message, Throwable exception) {
		status = ParserStatusUtility.addStatus(status, severity, message, exception);
	}

	/**
	 * Parse a composite cheat sheet from a url. The parser status will be set as a result
	 * of this operation, if the status is IStatus.ERROR the parser returns null
	 * @param url The url of the input
	 * @return A valid composite cheat sheet or null if there was an error
	 */
	public CompositeCheatSheetModel parseGuide(URL url) {
		status = Status.OK_STATUS;
		if(url == null) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {""})); //$NON-NLS-1$
			addStatus(IStatus.ERROR,  message,  null);
			return null;
		}

		InputStream is = null;

		try {
			is = url.openStream();

			if (is == null) {
				String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {url.getFile()}));
				addStatus(IStatus.ERROR,  message,  null);
				return null;
			}
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {url.getFile()}));
			addStatus(IStatus.ERROR,  message,  e);
			return null;
		}

		Document document;
		String filename = url.getFile();
		try {
			InputSource inputSource = new InputSource(is);
			document = getDocumentBuilder().parse(inputSource);
		} catch (IOException e) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {filename}));
			addStatus(IStatus.ERROR,  message,  e);
			return null;
		} catch (SAXParseException spe) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING_WITH_LOCATION, (new Object[] { filename,
					Integer.valueOf(spe.getLineNumber()), Integer.valueOf(spe.getColumnNumber()) }));
			addStatus(IStatus.ERROR,  message,  spe);
			return null;
		} catch (SAXException se) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING, (new Object[] {filename}));
			addStatus(IStatus.ERROR,  message, se);
			return null;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}

		CompositeCheatSheetModel result = parseCompositeCheatSheet(document, url);
		return result;
	}

	/**
	 * Parse a composite cheatsheet. The parser status will be set as a result
	 * of this operation, if the status is IStatus.ERROR the parser returns null
	 * @param url The url of the input. This is only used so the model can record
	 * its input location
	 * @param document the document to be parse
	 * @return A valid composite cheat sheet or null if there was an error
	 * @return
	 */
	public CompositeCheatSheetModel parseCompositeCheatSheet(Document document, URL url)   {
		status = Status.OK_STATUS;
		try {
			// If the document passed is null return a null tree and update the status
			if (document != null) {
				Node rootnode = document.getDocumentElement();

				// Is the root node correct?
				if( !rootnode.getNodeName().equals(ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET )) {
					String message = NLS.bind(Messages.ERROR_PARSING_ROOT_NODE_TYPE, (
							new Object[] {ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET}));
					throw new CheatSheetParserException(message);
				}

				String name = "";  //$NON-NLS-1$
				boolean nameFound = false;
				String explorerId = ICompositeCheatsheetTags.TREE;

				NamedNodeMap attributes = rootnode.getAttributes();
				if (attributes != null) {
					for (int x = 0; x < attributes.getLength(); x++) {
						Node attribute = attributes.item(x);
						String attributeName = attribute.getNodeName();
						if ( attributeName != null && attributeName.equals(ICompositeCheatsheetTags.NAME)) {
							nameFound = true;
							name= attribute.getNodeValue();
						}
						if (attributeName.equals(ICompositeCheatsheetTags.EXPLORER)) {
							explorerId= attribute.getNodeValue();
						}
					}
				}
				CompositeCheatSheetModel compositeCS = new CompositeCheatSheetModel(name, name, explorerId);

				parseCompositeCheatSheetChildren(rootnode, compositeCS);

				compositeCS.getDependencies().resolveDependencies(this);

				if (compositeCS.getRootTask() == null) {
					addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_ROOT, null);
				}
				if (!nameFound) {
					addStatus(IStatus.ERROR, Messages.ERROR_PARSING_CCS_NO_NAME, null);
				}
				if (status.getSeverity() != IStatus.ERROR) {
					compositeCS.setContentUrl(url);
					return compositeCS;
				}
			}
			return null;
		} catch(CheatSheetParserException e) {
			addStatus(IStatus.ERROR, e.getMessage(), null);
			return null;
		}
	}

	private void parseCompositeCheatSheetChildren(Node compositeCSNode, CompositeCheatSheetModel model) {
		nextTaskId = 0;
		NodeList childNodes = compositeCSNode.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index++) {
			Node nextNode = childNodes.item(index);
			if (isAbstractTask(nextNode.getNodeName()) ) {
				AbstractTask task = parseAbstractTask(nextNode, model);
				if (model.getRootTask() == null ) {
					model.setRootTask(task);
					parseTaskChildren(nextNode, task, model);
				} else {
					addStatus(IStatus.ERROR, Messages.ERROR_PARSING_MULTIPLE_ROOT, null);
				}
			}
		}
	}

	public static boolean isAbstractTask(String nodeName) {
		return nodeName == ICompositeCheatsheetTags.TASK ||
			nodeName == ICompositeCheatsheetTags.TASK_GROUP;
	}

	private void parseTaskChildren(Node parentNode, AbstractTask parentTask, CompositeCheatSheetModel model) {
		NodeList childNodes = parentNode.getChildNodes();
		ITaskParseStrategy strategy = parentTask.getParserStrategy();
		strategy.init();
		for (int index = 0; index < childNodes.getLength(); index++) {
			Node childNode = childNodes.item(index);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = childNode.getNodeName();
				if (nodeName == IParserTags.PARAM) {
					addParameter(parentTask, childNode.getAttributes());
				} else if (nodeName == IParserTags.INTRO) {
					parentTask.setDescription(MarkupParser.parseAndTrimTextMarkup(childNode));
				} else if (nodeName == ICompositeCheatsheetTags.ON_COMPLETION) {
					parentTask.setCompletionMessage(MarkupParser.parseAndTrimTextMarkup(childNode));
				} else if (nodeName == ICompositeCheatsheetTags.DEPENDS_ON) {
					parseDependency(childNode, parentTask, model);
				} else if (CompositeCheatSheetParser.isAbstractTask(nodeName)) {
					if (parentTask instanceof TaskGroup) {
						AbstractTask task = parseAbstractTask(childNode, model);
						((TaskGroup)parentTask).addSubtask(task);
						parseTaskChildren(childNode, task, model);
					}
				} else {
					if (!strategy.parseElementNode(childNode, parentNode, parentTask, this)) {
						String message = NLS
						.bind(
								Messages.WARNING_PARSING_UNKNOWN_ELEMENT,
								(new Object[] { nodeName,
										parentNode.getNodeName() }));
						addStatus(IStatus.WARNING, message, null);
					}
				}
			}
		}
		// Check for missing attributes and add dependencies if this was a sequence
		strategy.parsingComplete(parentTask, this);
	}

	private void parseDependency(Node taskNode, AbstractTask task, CompositeCheatSheetModel model) {
		NamedNodeMap attributes = taskNode.getAttributes();
		if (attributes != null) {
			Node taskAttribute = attributes.getNamedItem(ICompositeCheatsheetTags.TASK);
			if (taskAttribute != null) {
				String requiredTaskId = taskAttribute.getNodeValue();
				model.getDependencies().addDependency(task, requiredTaskId);
			} else {
				addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_ID, null);
			}
		}
	}

	private void addParameter(AbstractTask parentTask, NamedNodeMap attributes) {
		String name = null;
		String value = null;

		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;
				if (attributeName.equals(ICompositeCheatsheetTags.NAME)) {
					name = attribute.getNodeValue();
				}
				if (attributeName.equals(ICompositeCheatsheetTags.VALUE)) {
					value= attribute.getNodeValue();
				}
			}
		}
		if (name == null) {
			addStatus(IStatus.WARNING, Messages.ERROR_PARSING_NO_NAME, null);
			return;
		} else if (value == null) {
			addStatus(IStatus.WARNING, Messages.ERROR_PARSING_NO_VALUE, null);
			return;
		} else {
			parentTask.getParameters().put(name, value);
		}

	}

	private AbstractTask parseAbstractTask(Node taskNode, CompositeCheatSheetModel model) {
		AbstractTask task;
		NamedNodeMap attributes = taskNode.getAttributes();
		String kind = null;
		String name = null;
		String id = null;
		boolean skippable = false;
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;
				if (attributeName.equals(ICompositeCheatsheetTags.KIND)) {
					kind = attribute.getNodeValue();
				}
				if (attributeName.equals(ICompositeCheatsheetTags.NAME)) {
					name= attribute.getNodeValue();
				}
				if (attributeName.equals(IParserTags.ID)) {
					id = attribute.getNodeValue();
				}
				if (attributeName.equals(IParserTags.SKIP)) {
					skippable = "true".equalsIgnoreCase(attribute.getNodeValue()); //$NON-NLS-1$
				}
			}
		}

		String nodeName = taskNode.getNodeName();
		if (id == null) {
			id = autoGenerateId();
		}
		if (name == null) {
			String message = NLS.bind(Messages.ERROR_PARSING_TASK_NO_NAME, (new Object[] {nodeName}));
			addStatus(IStatus.ERROR, message, null);
		}
		task = createTask(nodeName, model, kind, id, name);
		task.setSkippable(skippable);

		if (model.getDependencies().getTask(id) != null) {
			String message = NLS.bind(Messages.ERROR_PARSING_DUPLICATE_TASK_ID, (new Object[] {id, }));
			addStatus(IStatus.ERROR, message, null);
		} else {
			model.getDependencies().saveId(task);
		}

		return task;
	}

	private AbstractTask createTask(String nodeKind, CompositeCheatSheetModel model, String kind, String id, String name) {
		AbstractTask task;
		if (ICompositeCheatsheetTags.TASK_GROUP.equals(nodeKind)) {
			task = new TaskGroup(model, id, name, kind);
		} else {
			task = new EditableTask(model, id, name, kind);
		}
		task.setCompletionMessage(Messages.COMPLETED_TASK);
		return task;
	}

	private String autoGenerateId() {
		return "TaskId_" + nextTaskId++; //$NON-NLS-1$
	}

}
