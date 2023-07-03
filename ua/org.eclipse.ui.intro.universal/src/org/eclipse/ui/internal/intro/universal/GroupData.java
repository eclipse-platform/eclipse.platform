/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.intro.config.IntroElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupData {
	boolean fDefault=false;
	private String path;
	private ArrayList<BaseData> children = new ArrayList<>();

	public GroupData(String path, boolean defaultGroup) {
		fDefault = defaultGroup;
		this.path = path;
	}

	public GroupData(Element element) {
		if (element.getNodeName().equals("hidden")) //$NON-NLS-1$
			path = IUniversalIntroConstants.HIDDEN;
		else
			path = element.getAttribute("path"); //$NON-NLS-1$
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("extension")) { //$NON-NLS-1$
					loadExtension((Element) child);
				}
				else if (child.getNodeName().equals("separator")) {//$NON-NLS-1$"
					loadSeparator((Element)child);
				}
			}
		}
		String df = element.getAttribute("default"); //$NON-NLS-1$
		if (df!=null && df.equalsIgnoreCase("true")) //$NON-NLS-1$
			fDefault = true;
	}

	public boolean isHidden() {
		return (path.equals(IUniversalIntroConstants.HIDDEN));
	}

	public boolean isDefault() {
		return fDefault;
	}

	public void addAnchors(List<IntroElement> result) {
		for (int i = 0; i < children.size(); i++) {
			BaseData edata = children.get(i);
			String id = edata.getId();
			IntroElement element = null;
			String tagName="anchor"; //$NON-NLS-1$
			if (edata instanceof SeparatorData)
				tagName = "hr"; //$NON-NLS-1$
			element = new IntroElement(tagName);
			element.setAttribute("id", id); //$NON-NLS-1$
			result.add(element);
		}
	}

	public void add(BaseData ed) {
		children.add(ed);
		ed.setParent(this);
	}
	public void add(int index, BaseData ed) {
		children.add(index, ed);
		ed.setParent(this);
	}
	public void remove(BaseData ed) {
		children.remove(ed);
		ed.setParent(null);
	}

	public void addImplicitExtension(String id, String name) {
		ExtensionData ed = new ExtensionData(id, name, IUniversalIntroConstants.LOW, true);
		add(ed);
	}

	private void loadSeparator(Element element) {
		String id = element.getAttribute("id"); //$NON-NLS-1$
		SeparatorData sd = new SeparatorData(id);
		add(sd);
	}

	private void loadExtension(Element element) {
		String id = element.getAttribute("id"); //$NON-NLS-1$
		String name = element.getAttribute("name"); //$NON-NLS-1$
		String importance = element.getAttribute("importance"); //$NON-NLS-1$
		ExtensionData ed = new ExtensionData(id, name, importance, false);
		add(ed);
	}

	public BaseData[] getChildren() {
		return children.toArray(new BaseData[children.size()]);
	}

	public int getExtensionCount() {
		int count=0;
		for (int i=0; i<children.size(); i++) {
			BaseData data = children.get(i);
			if (data instanceof ExtensionData)
				count++;
		}
		return count;
	}
/*
	public ExtensionData[] getExtensions() {
		ArrayList result = new ArrayList();
		for (int i=0; i<children.size(); i++) {
			BaseData data = (BaseData)children.get(i);
			if (data instanceof ExtensionData)
				result.add(data);
		}
		return (ExtensionData[]) result.toArray(new ExtensionData[result.size()]);
	}
	*/

	public String getPath() {
		return path;
	}

	public boolean contains(String id) {
		return find(id)!=null;
	}

	BaseData find(String extensionId) {
		for (int i = 0; i < children.size(); i++) {
			BaseData data = children.get(i);
			if (data.getId().equals(extensionId))
				return data;
		}
		return null;
	}

	public int getIndexOf(BaseData ed) {
		return children.indexOf(ed);
	}

	public int getIndexOf(String baseId) {
		for (int i = 0; i < children.size(); i++) {
			BaseData bd = children.get(i);
			if (bd.getId().equals(baseId))
				return i;
		}
		return -1;
	}

	public boolean canMoveUp(BaseData ed) {
		int index = children.indexOf(ed);
		return (index>0);
	}

	public boolean canMoveDown(BaseData ed) {
		int index = children.indexOf(ed);
		return (index!= -1 && index < children.size()-1);
	}

	public void moveUp(BaseData ed) {
		int index = children.indexOf(ed);
		BaseData swapped = children.get(index-1);
		children.set(index, swapped);
		children.set(index-1, ed);
	}

	public void moveDown(BaseData ed) {
		int index = children.indexOf(ed);
		BaseData swapped = children.get(index+1);
		children.set(index, swapped);
		children.set(index+1, ed);
	}

	public void addSeparator(BaseData after) {
		SeparatorData sd = new SeparatorData();
		sd.id = ""+sd.hashCode(); //$NON-NLS-1$
		if (after!=null) {
			int index = children.indexOf(after);
			if (index!= -1) {
				children.add(index+1, sd);
				return;
			}
		}
		children.add(sd);
	}

	public void write(PrintWriter writer, String indent) {
		writer.print(indent);
		if (isHidden())
			writer.print("<hidden>"); //$NON-NLS-1$
		else {
			writer.print("<group path=\""+path+"\"");  //$NON-NLS-1$ //$NON-NLS-2$
			if (fDefault)
				writer.println(" default=\"true\">"); //$NON-NLS-1$
			else
				writer.println(">"); //$NON-NLS-1$
		}
		for (int i=0; i<children.size(); i++) {
			BaseData ed = children.get(i);
			ed.write(writer, indent+"   "); //$NON-NLS-1$
		}
		writer.print(indent);
		if (isHidden())
			writer.println("</hidden>"); //$NON-NLS-1$
		else
			writer.println("</group>"); //$NON-NLS-1$
	}
}