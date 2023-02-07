/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Observable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EngineDescriptorManager extends Observable implements IHelpUIConstants {
	private ArrayList<EngineDescriptor> descriptors;

	private EngineTypeDescriptor[] engineTypes;

	private static final String USER_FILE = "userSearches.xml"; //$NON-NLS-1$
	private static final String ATT_ENGINE_TYPE_ID = "engineTypeId"; //$NON-NLS-1$

	public static class DescriptorEvent {
		private EngineDescriptor desc;
		private int kind;
		public DescriptorEvent(EngineDescriptor desc, int kind) {
			this.desc = desc;
			this.kind = kind;
		}
		public EngineDescriptor getDescriptor() {
			return desc;
		}
		public int getKind() {
			return kind;
		}
	}

	public EngineDescriptorManager() {
		descriptors = new ArrayList<>();
		load();
	}

	public void add(EngineDescriptor desc) {
		descriptors.add(desc);
		this.setChanged();
		this.notifyObservers(new DescriptorEvent(desc, ADD));
	}

	public void remove(EngineDescriptor desc) {
		descriptors.remove(desc);
		this.setChanged();
		this.notifyObservers(new DescriptorEvent(desc, REMOVE));
	}

	public void notifyPropertyChange(EngineDescriptor desc) {
		this.setChanged();
		this.notifyObservers(new DescriptorEvent(desc, CHANGE));
	}

	public EngineDescriptor[] getDescriptors() {
		return descriptors.toArray(new EngineDescriptor[descriptors.size()]);
	}

	public EngineDescriptor findEngine(String engineId) {
		for (int i=0; i<descriptors.size(); i++) {
			EngineDescriptor desc = descriptors.get(i);
			if (desc.getId().equals(engineId))
				return desc;
		}
		return null;
	}

	public EngineTypeDescriptor[] getEngineTypes() {
		return engineTypes;
	}

	public void save() {
		IPath stateLoc = HelpUIPlugin.getDefault().getStateLocation();
		String fileName = stateLoc.append(USER_FILE).toOSString();
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			fos = new FileOutputStream(fileName);
			osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			PrintWriter writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<engines>"); //$NON-NLS-1$
			for (int i = 0; i < descriptors.size(); i++) {
				EngineDescriptor desc = descriptors.get(i);
				if (desc.isUserDefined()) {
					save(writer, desc);
				}
			}
			writer.println("</engines>"); //$NON-NLS-1$
			writer.flush();
		}
		catch (IOException e) {
			Platform.getLog(getClass()).error(Messages.EngineDescriptorManager_errorSaving, e);
		}
		finally {
			if (osw!=null) {
				try {
					osw.close();
				}
				catch (IOException e) {
				}
			}
			if (fos!=null) {
				try {
					fos.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	public void load() {
		loadFromExtensionRegistry();
		IPath stateLoc = HelpUIPlugin.getDefault().getStateLocation();
		String fileName = stateLoc.append(USER_FILE).toOSString();
		try {
			load(fileName);
		}
		catch (IOException e) {
			Platform.getLog(getClass()).error(Messages.EngineDescriptorManager_errorLoading, e);
		}
	}

	private void loadFromExtensionRegistry() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(ENGINE_EXP_ID);
		Hashtable<String, EngineTypeDescriptor> engineTypes = loadEngineTypes(elements);
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals(TAG_ENGINE)) {
				EngineDescriptor desc = new EngineDescriptor(element);
				String engineId = desc.getEngineTypeId();
				if (engineId != null) {
					EngineTypeDescriptor etdesc = engineTypes.get(engineId);
					if (etdesc != null) {
						desc.setEngineType(etdesc);
						descriptors.add(desc);
					}
				}
			}
		}
	}

	private Hashtable<String, EngineTypeDescriptor> loadEngineTypes(IConfigurationElement[] elements) {
		Hashtable<String, EngineTypeDescriptor> result = new Hashtable<>();
		ArrayList<EngineTypeDescriptor> list = new ArrayList<>();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("engineType")) { //$NON-NLS-1$
				EngineTypeDescriptor etdesc = new EngineTypeDescriptor(element);
				String id = etdesc.getId();
				if (id != null) {
					list.add(etdesc);
					result.put(etdesc.getId(), etdesc);
				}
			}
		}
		engineTypes = list.toArray(new EngineTypeDescriptor[list.size()]);
		return result;
	}

	public void load(Reader r) {
		Document document = null;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			// parser.setProcessNamespace(true);
			document = parser.parse(new InputSource(r));

			// Strip out any comments first
			Node root = document.getFirstChild();
			while (root.getNodeType() == Node.COMMENT_NODE) {
				document.removeChild(root);
				root = document.getFirstChild();
			}
			load(document, (Element) root);
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}
	}

	/*
	 * (non-Javadoc) Method declared on IDialogSettings.
	 */
	public void load(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) return;
		FileInputStream stream = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				stream, StandardCharsets.UTF_8));
		load(reader);
		reader.close();
	}

	private void load(Document doc, Element root) {
		NodeList engines = root.getElementsByTagName(TAG_ENGINE);
		for (int i=0; i<engines.getLength(); i++) {
			Node node = engines.item(i);
			loadUserEntry(node);
		}
	}

	private void loadUserEntry(Node node) {
		EngineDescriptor edesc = new EngineDescriptor(this);
		String id = getAttribute(node, ATT_ID);
		String engineTypeId = getAttribute(node, ATT_ENGINE_TYPE_ID);
		EngineTypeDescriptor etdesc = findEngineType(engineTypeId);
		String label = getAttribute(node, ATT_LABEL);
		String desc = getDescription(node);
		if (etdesc == null)
			return;
		edesc.setEngineType(etdesc);
		edesc.setUserDefined(true);
		edesc.setId(id);
		edesc.setLabel(label);
		edesc.setDescription(desc);
		descriptors.add(edesc);
	}

	public String computeNewId(String typeId) {
		ArrayList<Integer> used = new ArrayList<>();
		for (int i=0; i<descriptors.size(); i++) {
			EngineDescriptor ed = descriptors.get(i);
			if (!ed.isUserDefined()) continue;
			String edTypeId = ed.getEngineTypeId();
			if (typeId.equals(edTypeId)) {
				String edId = ed.getId();
				int loc = edId.lastIndexOf('.');
				if (loc!= -1) {
					String cvalue = edId.substring(loc+1);
					int ivalue = Integer.parseInt(cvalue);
					used.add(Integer.valueOf(ivalue));
				}
			}
		}
		for (int i=1; i<Integer.MAX_VALUE; i++) {
			if (!isUsed(i, used)) {
				return typeId+"."+"user."+i; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return typeId;
	}

	private boolean isUsed(int value, ArrayList<Integer> used) {
		for (int i=0; i<used.size(); i++) {
			Integer iv = used.get(i);
			if (iv.intValue()==value)
				return true;
		}
		return false;
	}

	private String getDescription(Node node) {
		NodeList list = ((Element)node).getElementsByTagName(TAG_DESC);
		if (list.getLength()==1) {
			Node desc = list.item(0);
			NodeList children = desc.getChildNodes();
			for (int i=0; i<children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType()==Node.TEXT_NODE) {
					String text = child.getNodeValue();
					return text.trim();
				}
			}
		}
		return null;
	}

	private void save(PrintWriter writer, EngineDescriptor desc) {
		String indent = "   "; //$NON-NLS-1$
		String attIndent = indent + indent;
		writer.print(indent);
		writer.println("<engine "); //$NON-NLS-1$
		saveAttribute(writer, attIndent, "id", desc.getId()); //$NON-NLS-1$
		saveAttribute(writer, attIndent, ATT_ENGINE_TYPE_ID, desc.getEngineTypeId());
		saveAttribute(writer, attIndent, ATT_LABEL, desc.getLabel());
		writer.println(">"); //$NON-NLS-1$
		saveDescription(writer, indent+indent, desc.getDescription());
		writer.print(indent);
		writer.println("</engine>"); //$NON-NLS-1$
	}

	private String getAttribute(Node node, String name) {
		Node att = node.getAttributes().getNamedItem(name);
		if (att!=null)
			return att.getNodeValue();
		return null;
	}

	private void saveAttribute(PrintWriter writer, String indent, String name, String value) {
		if (value==null)
			return;
		writer.print(indent);
		writer.print(name);
		writer.print("=\""); //$NON-NLS-1$
		writer.print(value);
		writer.println("\""); //$NON-NLS-1$
	}
	private void saveDescription(PrintWriter writer, String indent, String desc) {
		if (desc==null)
			return;
		writer.print(indent);
		writer.println("<description>"); //$NON-NLS-1$
		writer.println(desc);
		writer.print(indent);
		writer.println("</description>"); //$NON-NLS-1$
	}

	private EngineTypeDescriptor findEngineType(String id) {
		if (id == null)
			return null;
		for (int i = 0; i < engineTypes.length; i++) {
			EngineTypeDescriptor etd = engineTypes[i];
			if (etd.getId().equals(id))
				return etd;
		}
		return null;
	}
}
