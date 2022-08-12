/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;

public class OrderedMatching extends AbstractMatching {

	public OrderedMatching() {
		super();
	}

	protected int orderedMath(XMLNode x, XMLNode y) {
		//assumes x and y have children
		Object[] xc= x.getChildren();
		Object[] yc= y.getChildren();

		ArrayList<XMLNode> xc_elementsAL= new ArrayList<>();
		ArrayList<XMLNode> xc_attrsAL= new ArrayList<>();

		ArrayList<XMLNode> yc_elementsAL= new ArrayList<>();
		ArrayList<XMLNode> yc_attrsAL= new ArrayList<>();

		//find attributes and elements and put them in xc_elementsAL and xc_attrsAL, respectively
		for (Object xc1 : xc) {
			XMLNode x_i = (XMLNode) xc1;
			if (x_i.getXMLType().equals(XMLStructureCreator.TYPE_ELEMENT)) {
				xc_elementsAL.add(x_i);
			} else if (
					x_i.getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE)) {
				xc_attrsAL.add(x_i);
			}
		}
		//do the same for yc
		for (Object yc1 : yc) {
			XMLNode y_i = (XMLNode) yc1;
			if (y_i.getXMLType().equals(XMLStructureCreator.TYPE_ELEMENT)) {
				yc_elementsAL.add(y_i);
			} else if (
					y_i.getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE)) {
				yc_attrsAL.add(y_i);
			}
		}

		Object[] xc_elements= xc_elementsAL.toArray();
		Object[] yc_elements= yc_elementsAL.toArray();

		ArrayList DTMatching= new ArrayList();
		// Matching to be added to Entry in fDT_Matchings
		int distance= 0; //distance to be added to entry in fDT

		// perform unordered matching on attributes
		// this updates fDT and fDT_Matchings
		if (xc_attrsAL.size() > 0 || yc_attrsAL.size() > 0) {
			if (xc_attrsAL.isEmpty())
				distance += yc_attrsAL.size();
			else if (yc_attrsAL.isEmpty())
				distance += xc_attrsAL.size();
			else {
				//unorderedMatch(x, y, xc_attrs, yc_attrs);
				//				distance += fDT[indexOfLN(x)][indexOfRN(y)];
				//				DTMatching= fDT_Matchings[indexOfLN(x)][indexOfRN(y)];
				distance= handleAttributes(xc_attrsAL, yc_attrsAL, DTMatching);
			}
		}

		//perform ordered matching on element children, i.e. number them in order of appearance

		/* start new */
		distance=
			handleRangeDifferencer(
				xc_elements,
				yc_elements,
				DTMatching,
				distance);
		/* end new */

		/* start: Naive ordered compare /*
		//			int minlength= (xc_elements.length > yc_elements.length)?yc_elements.length:xc_elements.length;
		//			for (int i= 0; i < minlength; i++) {
		//				distance += dist((XMLNode) xc_elements[i], (XMLNode) yc_elements[i]);
		//				DTMatching.add(new Match( (XMLNode)xc_elements[i], (XMLNode)yc_elements[i]));
		//			}
		//			if (xc_elements.length > yc_elements.length) {
		//				for (int i= minlength; i < xc_elements.length; i++) {
		//					distance += countNodes((XMLNode) xc_elements[i]);
		//				}
		//			} else if (xc_elements.length < yc_elements.length) {
		//				for (int i= minlength; i < yc_elements.length; i++) {
		//					distance += countNodes((XMLNode) yc_elements[i]);
		//				}
		//			}
		/* end: Naive ordered compare */

		fDT[indexOfLN(x)][indexOfRN(y)]= distance;
		fDT_Matchings[indexOfLN(x)][indexOfRN(y)]= DTMatching;
		return distance;

	}

	/* matches two trees according to paper "X-Diff", p. 16 */
	@Override
	public void match(
		XMLNode LeftTree,
		XMLNode RightTree,
		boolean rightTreeIsAncestor,
		IProgressMonitor monitor) {

		fNLeft= new Vector<XMLNode>();
		//numbering LeftTree: Mapping nodes in LeftTree to numbers to be used as array indexes
		fNRight= new Vector<XMLNode>();
		//numbering RightTree: Mapping nodes in RightTree to numbers to be used as array indexes
		numberNodes(LeftTree, fNLeft);
		numberNodes(RightTree, fNRight);
		fDT= new int[fNLeft.size()][fNRight.size()];
		fDT_Matchings= new ArrayList[fNLeft.size()][fNRight.size()];
		for (int i= 0; i < fDT.length; i++) {
			fDT[i]= new int[fNRight.size()];
			for (int j= 0; j < fDT[0].length; j++) {
				fDT[i][j]= NO_ENTRY;
			}
		}

		dist(LeftTree, RightTree);
		//		/* mark matchings on LeftTree and RightTree */
		fMatches= new Vector();
		if (!LeftTree.getSignature().equals(RightTree.getSignature())) {
			//matching is empty	
		} else {
			fMatches.add(new Match(LeftTree, RightTree));
			for (int i_M= 0; i_M < fMatches.size(); i_M++) {
				Match m= (Match) fMatches.elementAt(i_M);
				if (!isLeaf(m.fx) && !isLeaf(m.fy)) {
					//					if (fDT_Matchings[ indexOfLN(m.fx) ][ indexOfRN(m.fy) ] == null)
					//						System.out.println("Error: ID not unique for " + m.fx.getId());
					//					else
					//						fMatches.addAll(fDT_Matchings[ indexOfLN(m.fx) ][ indexOfRN(m.fy) ]);
					if (fDT_Matchings[indexOfLN(m.fx)][indexOfRN(m.fy)]
						!= null)
						fMatches.addAll(
							fDT_Matchings[indexOfLN(m.fx)][indexOfRN(m.fy)]);
				}
			}
		}
		//end of Step2
		/* Renumber Id of Nodes to follow Matches. Or for ancestor, copy over Id to ancestor */
		if (rightTreeIsAncestor) {
			for (ListIterator it_M= fMatches.listIterator(); it_M.hasNext();) {
				Match m= (Match) it_M.next();
				if (m.fx != null && m.fy != null)
					m.fy.setId(m.fx.getId());
			}
		} else {
			int newId= 0;
			for (ListIterator it_M= fMatches.listIterator();
				it_M.hasNext();
				newId++) {
				Match m= (Match) it_M.next();
				if (m.fx != null)
					m.fx.setId(Integer.toString(newId));
				if (m.fy != null)
					m.fy.setId(Integer.toString(newId));
				//				System.out.println("Matching: "+ ((m.fx != null)?m.fx.getOrigId():"null")+" -> "+((m.fx != null)?m.fx.getId():"null")+" , "+((m.fy != null)?m.fy.getOrigId():"null")+" -> "+((m.fy != null)?m.fy.getId():"null")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		//if (monitor != null) monitor.done();
	}

	public int handleAttributes(
		ArrayList<XMLNode> xc_attrs,
		ArrayList<XMLNode> yc_attrs,
		ArrayList<Match> DTMatching) {
		int distance= 0;
		x_for : for (XMLNode x_attr : xc_attrs) {
			String x_attr_name= x_attr.getName();
			for (XMLNode y_attr : yc_attrs) {
				if (y_attr.getName().equals(x_attr_name)) {
					if (!y_attr.getValue().equals(x_attr.getValue()))
						distance += 1;
					DTMatching.add(new Match(x_attr, y_attr));
					yc_attrs.remove(y_attr);
					continue x_for;
				}
			}
			DTMatching.add(new Match(x_attr, null));
			distance += 1;
		}

		for (Iterator<XMLNode> iter_yc= yc_attrs.iterator(); iter_yc.hasNext();) {
			DTMatching.add(new Match(null, iter_yc.next()));
			distance += 1;
		}

		return distance;
	}

	@Override
	protected int handleXandYnotLeaves(XMLNode x, XMLNode y) {
		/* handle entries as ordered*/
		return orderedMath(x, y);
	}
}
