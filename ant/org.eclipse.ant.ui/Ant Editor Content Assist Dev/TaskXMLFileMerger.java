/*******************************************************************************
 * Copyright (c) 2002, 2006 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug 24108
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.tools;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class can be used to merge the tasks.xml and XDOCtasks.xml. These files can be found in the Ant Editor Content Assist Dev folder. Move the
 * files to the Ant Editor folder to use this class.
 * 
 * The xml automatically generated from the proposed xdoclet in the Apache Ant's project currently has no information on required attributes. In the
 * future the task writers hopefully will include this information in the source code comments. Our template currently inserts an attribute
 * required="NOTDEFINED" and this class replaces that field if its defined in the xml file we have generated from the information based on the html
 * files in the Apache Ant's manual directory.
 */
public class TaskXMLFileMerger {

	// Definitions for the HTML Generated XML File
	public static String HTML_TASKS_DESCRIPTION_XML_FILE_NAME = "/tasks.xml"; //$NON-NLS-1$
	public static String HTML_XML_TAG_TASKS = "TASKS"; //$NON-NLS-1$
	public static String HTML_XML_TAG_TASK = "TASK"; //$NON-NLS-1$
	public static String HTML_XML_TAG_ATTRIBUTE = "ATTRIBUTE"; //$NON-NLS-1$
	// public static String HTML_XML_TAG_DESCRIPTION = "DESCRIPTION";
	public static String HTML_XML_ATTRIBUTE_NAME = "NAME"; //$NON-NLS-1$
	public static String HTML_XML_ATTRIBUTE_REQUIRED = "REQUIRED"; //$NON-NLS-1$

	// Definitions for the XDoclet Genereated XML File
	public static String XDOC_TASKS_DESCRIPTION_XML_FILE_NAME = "/XDOCtasks.xml"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_TASKS = "tasks"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_TASK = "task"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_NAME = "name"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_STRUCTURE = "structure"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_ATTRIBUTES = "attributes"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_ATTRIBUTE = "attribute"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_ELEMENTS = "elements"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_ELEMENT = "element"; //$NON-NLS-1$
	public static String XDOC_XML_TAG_REQUIRED = "required"; //$NON-NLS-1$

	protected NodeList taskNodes_HTML = null;
	protected NodeList taskNodes_XDOC = null;
	public Document xdocXMLDocument = null;

	/**
	 * Creates an initialized instance.
	 */
	public TaskXMLFileMerger() {
		initialize();
	}

	/**
	 * Parses the task description xml files and stores the information.
	 */
	private void initialize() {

		Document tmpDocument = null;

		// Get All the Tasks in the HTML XML Generated file and store in the taskNodes_HTML
		tmpDocument = parseFile(HTML_TASKS_DESCRIPTION_XML_FILE_NAME);
		taskNodes_HTML = tmpDocument.getFirstChild().getChildNodes();

		// Do the same for the XDOC XML Generated file
		tmpDocument = parseFile(XDOC_TASKS_DESCRIPTION_XML_FILE_NAME);
		taskNodes_XDOC = tmpDocument.getFirstChild().getChildNodes();
		xdocXMLDocument = tmpDocument;
		/*
		 * Document tempDocument = parseFile(aFileName); Node tempRootNode = tempDocument.getDocumentElement(); NodeList tempChildNodes =
		 * tempRootNode.getChildNodes(); for(int i=0; i<tempChildNodes.getLength(); i++) { Node tempNode = tempChildNodes.item(i);
		 * if(tempNode.getNodeType() == Node.ELEMENT_NODE) { String tempTagName = tempNode.getNodeName(); if(tempTagName.equals(anXML_TAG_TASK)) {
		 * NamedNodeMap tempAttributes = tempNode.getAttributes(); Node tempAttributeNode = tempAttributes.getNamedItem(anXML_ATTRIBUTE_NAME);
		 * if(tempAttributeNode != null) { String tempTaskName = tempAttributeNode.getNodeValue(); if(tempTaskName != null) {
		 * aHashMap.put(tempTaskName, tempNode); } } } } }
		 */
	}

	/**
	 * This is the function that does all the work. Calling this will cause all Required fields in all Attributes in the XMLDoc file to be replaced
	 * with the value in the corresponding Attributes Required field in the HTML Xml file.
	 */
	public void runReplaceAttributeRequiredProcess() {

		// Iterate over all the tasks. If task is found in sourceList,
		// then iterate over all the attributes, try to find out if the
		// the attribute is required.
		for (int i = 0; i < taskNodes_XDOC.getLength(); ++i) {
			Node tmpTargetNode = taskNodes_XDOC.item(i);

			if (tmpTargetNode.getNodeType() == Node.ELEMENT_NODE) {
				replaceAttributeRequiredInTaskNode(tmpTargetNode);
			}
		}
	}

	private void replaceAttributeRequiredInTaskNode(Node aTargetTaskNode) {

		String tmpTaskName = aTargetTaskNode.getAttributes().getNamedItem(XDOC_XML_TAG_NAME).getNodeValue();

		if (tmpTaskName != null) {
			Node tmpSourceNode = getTaskInHTMLGeneratedTaskListNamed(tmpTaskName);

			if (tmpSourceNode != null) {
				replaceAttributeRequiredInXMLTaskNodeWithAttributeRequiredInHTMLNode(aTargetTaskNode, tmpSourceNode);
			} else {
				System.out.println(MessageFormat.format("Did not find Task \"{0}\" in HTML XML file.", new String[] { tmpTaskName })); //$NON-NLS-1$
			}
		} else {
			System.out.println(MessageFormat.format("Did not find TaskName in TargetTaskNode: {0}", new String[] { aTargetTaskNode.toString() })); //$NON-NLS-1$
		}
	}

	private Node getTaskInHTMLGeneratedTaskListNamed(String aTaskName) {

		for (int i = 0; i < taskNodes_HTML.getLength(); ++i) {

			Node tmpTaskNode = taskNodes_HTML.item(i);
			if (tmpTaskNode.getNodeType() == Node.ELEMENT_NODE) {
				String tmpTagName = tmpTaskNode.getNodeName();
				if (tmpTagName.equals(HTML_XML_TAG_TASK)) {
					NamedNodeMap tmpMap = tmpTaskNode.getAttributes();
					Node tmpNameNode = tmpMap.getNamedItem(HTML_XML_ATTRIBUTE_NAME);
					if (aTaskName.equals(tmpNameNode.getNodeValue())) {
						return tmpTaskNode;
					}
				}
			}
		}
		// Not found
		return null;
	}

	private void replaceAttributeRequiredInXMLTaskNodeWithAttributeRequiredInHTMLNode(Node aTargetTaskNode, Node aSourceTaskNode) {

		Node tmpStructureNode = getChildNodeNamedWithTypeFromNode(XDOC_XML_TAG_STRUCTURE, Node.ELEMENT_NODE, aTargetTaskNode);

		if (tmpStructureNode != null) {
			Node tmpTargetAttributesNode = getChildNodeNamedWithTypeFromNode(XDOC_XML_TAG_ATTRIBUTES, Node.ELEMENT_NODE, tmpStructureNode);
			if (tmpTargetAttributesNode != null) {
				Vector tmpTargetAttributesVector = getAttributeNodesFromXMLAttributesNode(tmpTargetAttributesNode);
				Vector tmpSourceAttributesVector = getAttributeNodesFromHTMLTaskNode(aSourceTaskNode);

				// Iterate over all the attributes in the targetTaskNode
				for (int i = 0; i < tmpTargetAttributesVector.size(); ++i) {
					Node tmpAttributeNode = (Node) tmpTargetAttributesVector.get(i);
					replaceAttributeRequiredInAttributeNodeWithValueFoundInNodeVector(tmpAttributeNode, tmpSourceAttributesVector);
				}
			}
		}
	}

	private void replaceAttributeRequiredInAttributeNodeWithValueFoundInNodeVector(Node aTargetAttributeNode, Vector aSourceAttributeVector) {

		NamedNodeMap tmpTargetNamedNodeMap = aTargetAttributeNode.getAttributes();
		String tmpTargetAttributeName = tmpTargetNamedNodeMap.getNamedItem(XDOC_XML_TAG_NAME).getNodeValue();

		String tmpSourceAttributeName = null;
		String tmpSourceRequiredValue = null;

		for (int i = 0; i < aSourceAttributeVector.size(); ++i) {
			Node tmpSourceAttributeNode = (Node) aSourceAttributeVector.get(i);
			NamedNodeMap tmpSourceAttributeNamedNodeMap = tmpSourceAttributeNode.getAttributes();
			tmpSourceAttributeName = tmpSourceAttributeNamedNodeMap.getNamedItem(HTML_XML_ATTRIBUTE_NAME).getNodeValue();
			// If the Attribute Name is the same we replace the REQUIRED Value
			if (tmpTargetAttributeName.equals(tmpSourceAttributeName)) {
				tmpSourceRequiredValue = tmpSourceAttributeNamedNodeMap.getNamedItem(HTML_XML_ATTRIBUTE_REQUIRED).getNodeValue();
				// Set the Vaule to the on we just got.
				tmpTargetNamedNodeMap.getNamedItem(XDOC_XML_TAG_REQUIRED).setNodeValue(tmpSourceRequiredValue);
			}
		}
	}

	private Vector getAttributeNodesFromXMLAttributesNode(Node anXMLAttributesNode) {

		Vector allAttributes = new Vector();
		NodeList tmpList = anXMLAttributesNode.getChildNodes();

		for (int i = 0; i < tmpList.getLength(); ++i) {
			Node tmpNode = tmpList.item(i);
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE && XDOC_XML_TAG_ATTRIBUTE.equals(tmpNode.getNodeName())) {
				allAttributes.add(tmpNode);
			}
		}
		return allAttributes;
	}

	private Vector getAttributeNodesFromHTMLTaskNode(Node anHTTP_XML_TaskNode) {

		Vector tmpVector = new Vector();
		NodeList tmpList = anHTTP_XML_TaskNode.getChildNodes();

		for (int i = 0; i < tmpList.getLength(); ++i) {
			Node tmpNode = tmpList.item(i);
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE && HTML_XML_TAG_ATTRIBUTE.equals(tmpNode.getNodeName())) {
				tmpVector.add(tmpNode);
			}
		}

		return tmpVector;
	}

	/**
	 * Returns the ChildNode of the node defined by the Arguments. The first child found matching the criterias is returned.
	 * 
	 * @param aNodeName
	 *            The Name of the Node to return.
	 * @param aType
	 *            The Type of the node @see Node
	 * @param aParentNode
	 *            The Node to get the child from
	 * 
	 * @return The First Child Node found matching the criterias, or null if none is found.
	 */
	private Node getChildNodeNamedWithTypeFromNode(String aName, short aNodeType, Node aNode) {

		NodeList tmpNodeList = aNode.getChildNodes();
		for (int i = 0; i < tmpNodeList.getLength(); ++i) {
			Node tmpNode = tmpNodeList.item(i);
			if ((tmpNode.getNodeType() == aNodeType) && aName.equals(tmpNode.getNodeName())) {
				return tmpNode;
			}
		}
		// Not found
		return null;
	}

	/**
	 * Returns the (DOM) document as a result of parsing the file with the specified file name.
	 * <P>
	 * The file will be loaded as resource, thus must begin with '/' and must be relative to the classpath.
	 */
	private Document parseFile(String aFileName) {
		Document tempDocument = null;

		DocumentBuilderFactory tempFactory = XmlProcessorFactory.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		tempFactory.setIgnoringComments(true);
		tempFactory.setIgnoringElementContentWhitespace(true);
		tempFactory.setCoalescing(true);

		try {
			DocumentBuilder tempDocBuilder = tempFactory.newDocumentBuilder();
			URL tempURL = getClass().getResource(aFileName);
			InputSource tempInputSource = new InputSource(tempURL.toExternalForm());
			tempDocument = tempDocBuilder.parse(tempInputSource);
		}
		catch (ParserConfigurationException e) {
			AntUIPlugin.log(e);
		}
		catch (IOException ioException) {
			AntUIPlugin.log(ioException);
		}
		catch (SAXException saxException) {
			AntUIPlugin.log(saxException);
		}

		return tempDocument;
	}

	/**
	 * This function writes the XMLDocument to the specified file.
	 * 
	 * @param aFileName
	 *            The filename to which the XMLDocument should be written.
	 */
	public void writeXMLDocumentToFile(String aFileName) {

		// try {
		// XmlDocument xmlDocument = (XmlDocument)xdocXMLDocument;
		// xmlDocument.write(new FileWriter(aFileName), "UTF-8"); //$NON-NLS-1$
		// }
		// catch(IOException ioe) {
		// System.out.println(MessageFormat.format(AntEditorToolsMessages.getString("TaskXMLFileMerger.Could_not_print"), new
		// String[]{ioe.toString()})); //$NON-NLS-1$
		// }
	}

	public static void main(String[] args) {

		TaskXMLFileMerger tmpTaskXMLFileMerger = new TaskXMLFileMerger();
		tmpTaskXMLFileMerger.runReplaceAttributeRequiredProcess();
		tmpTaskXMLFileMerger.writeXMLDocumentToFile("src\\anttasks_1.5b.xml"); //$NON-NLS-1$
	}
}
