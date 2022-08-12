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

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

/**
 * Objects that make up the parse tree.
 */
public class XMLNode extends DocumentRangeNode implements ITypedElement {

	private String fValue;
	private String fName;
	private String fSignature;
	private String fOrigId;
	private XMLNode parent;
	private String fXMLType;
	private boolean fUsesIDMAP;
	private boolean fOrderedChild;

	public int bodies; // counts the number of bodies

	public XMLNode(String XMLType, String id, String value, String signature, IDocument doc, int start, int length) {
		super(0, id, doc, start, length);
		fXMLType= XMLType;
		fValue= value;
		fSignature= signature;
		fOrigId= id;
		if (XMLStructureCreator.DEBUG_MODE)
			System.out.println("Created XMLNode with XMLType: " + XMLType + ", id: " + id + ", value: " + value + ", signature: " + fSignature); //$NON-NLS-1$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$
		bodies= 0;
		fUsesIDMAP= false;
		fOrderedChild= false;
	}

	void setValue(String value) {
		fValue= value;
	}

	String getValue() {
		return fValue;
	}

	@Override
	public String getName() {
		if (fName != null)
			return fName;
		return this.getId();
	}

	public void setName(String name) {
		fName= name;
	}

	/*
	 * Every xml node is of type "txt" so that the builtin TextMergeViewer is used automatically.
	 * @see ITypedElement#getType
	 */
	@Override
	public String getType() {
		return "txt"; //$NON-NLS-1$
	}

	public void setIsOrderedChild(boolean isOrderedChild) {
		fOrderedChild= isOrderedChild;
	}
	
	@Override
	public Image getImage() {
		if (fOrderedChild)
			return CompareUI.getImage(XMLPlugin.IMAGE_TYPE_PREFIX + XMLStructureCreator.TYPE_ELEMENT + XMLPlugin.IMAGE_TYPE_ORDERED_SUFFIX);
		return CompareUI.getImage(XMLPlugin.IMAGE_TYPE_PREFIX + getXMLType());
	}

	public void setParent(XMLNode parent0) {
		this.parent= parent0;
	}

	public XMLNode getParent() {
		return this.parent;
	}

	String getXMLType() {
		return fXMLType;
	}

	String getSignature() {
		return fSignature;
	}

	void setOrigId(String id) {
		fOrigId= id;
	}

	public String getOrigId() {
		return fOrigId;
	}

	public void setUsesIDMAP(boolean b) {
		fUsesIDMAP= b;
	}

	public boolean usesIDMAP() {
		return fUsesIDMAP;
	}

	//for tests
	public boolean testEquals(Object obj) {
		if (obj instanceof XMLNode) {
			XMLNode n= (XMLNode) obj;
			return fValue.equals(n.getValue())
				&& fSignature.equals(n.getSignature())
				&& fXMLType.equals(n.getXMLType())
				&& fUsesIDMAP == n.usesIDMAP();
		}
		return false;
	}

	/*
	 * Returns true if the subtree rooted at this node is equals to the subtree rooted at <code>obj</code>
	 */
	public boolean subtreeEquals(Object obj) {
		if (!testEquals(obj))
			return false;
		if (obj instanceof XMLNode) {
			XMLNode n= (XMLNode) obj;
			if (getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE)
				&& n.getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE))
				return true;
			Object[] children= getChildren();
			Object[] n_children= n.getChildren();
			//if both nodes have no children, return true;
			if ((children == null || children.length <= 0)
				&& (n_children == null || n_children.length <= 0))
				return true;
			//now at least one of the two nodes has children;
			/* so if one of the two nodes has no children, or they don't have the same number of children,
			 * return false;
			 */
			if ((children == null || children.length <= 0)
				|| (n_children == null || n_children.length <= 0)
				|| (children.length != n_children.length))
				return false;
			//now both have children and the same number of children
			for (int i= 0; i < children.length; i++) {
				/* if the subtree rooted at children[i] is not equal to the subtree rooted at n_children[i],
				 * return false
				 */
				if (!((XMLNode) children[i]).subtreeEquals(n_children[i]))
					return false;
			}
		}
		return true;
	}
}
