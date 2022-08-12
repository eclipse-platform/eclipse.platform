/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.Vector;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @version 	1.0
 * @author
 */
public abstract class AbstractMatching {

	protected static final int NO_ENTRY = -1;//value with which fDT elements are initialized
	protected static final String SIGN_ELEMENT= XMLStructureCreator.SIGN_ELEMENT;
	int[][] fDT;//Distance Table; 1st index from fNLeft, 2nd index from fNRight
	ArrayList[][] fDT_Matchings;//Mathing entries of children for a match. 1st index from fNLeft, 2nd index from fNRight
	Vector fNLeft;
	Vector fNRight;
	Vector fMatches;
	
	/* methods used for match */

	/* finds all the leaves of a tree and puts them in a vector */
	protected void findLeaves(XMLNode root, ArrayList<XMLNode> leaves) {
		if (isLeaf(root)) {
			leaves.add(root);			
		} else {
			Object[] children = root.getChildren();
			for (Object child : children) {
				findLeaves((XMLNode) child, leaves);
			}
		}
	}

	/* true if x is a leaf */
	protected boolean isLeaf(XMLNode x) {
		if (x == null) return true;
		return x.getChildren() == null || x.getChildren().length <= 0;
	}

	/* Numbers all nodes of tree. The number of x is its index in the vector numbering */
	protected void numberNodes(XMLNode root, Vector<XMLNode> numbering) {
		if (root != null) {
			numbering.add(root);
			Object[] children = root.getChildren();
			if (children != null) {
				for (Object child : children) {
					numberNodes((XMLNode) child, numbering);
				}
			}
		}
	}
	
	/* counts # of nodes in tree including root */
	protected int countNodes(XMLNode root) {
		if (root == null) return 0;
		int count = 1;
		if (isLeaf(root)) return count;
		Object[] children = root.getChildren();
		for (Object child : children) {
			count += countNodes((XMLNode) child);
		}
		return count;
	}

	/* returns index of node x in fNLeft */
	protected int indexOfLN (XMLNode x) {
		int i= 0;
		while ((i<fNLeft.size()) && (fNLeft.elementAt(i) != x))
			i++;
		return i;
	}
	
	/* returns index of node y in fNRight */
	protected int indexOfRN (XMLNode y) {
		int j= 0;
		while ((j<fNRight.size()) && (fNRight.elementAt(j) != y))
			j++;
		return j;
	}

/* for testing */
	public Vector getMatches() {
		return fMatches;
	}

	protected class XMLComparator implements IRangeComparator {
	
		private Object[] fXML_elements;
	
		public XMLComparator(Object[] xml_elements) {
			fXML_elements= xml_elements;
		}
	
		@Override
		public int getRangeCount() {
			return fXML_elements.length;
		}
	
		@Override
		public boolean rangesEqual(
			int thisIndex,
			IRangeComparator other_irc,
			int otherIndex) {
			
			if (other_irc instanceof XMLComparator) {
				XMLComparator other= (XMLComparator) other_irc;
				//return ((XMLNode)fXML_elements[thisIndex]).subtreeEquals(other.getXML_elements()[otherIndex]);
				
				//ordered compare of subtrees
				//boolean result= ((XMLNode)fXML_elements[thisIndex]).subtreeEquals(other.getXML_elements()[otherIndex]);
				
				//taking ids into account
				boolean sameId= false;
				XMLNode thisNode= (XMLNode)fXML_elements[thisIndex];
				XMLNode otherNode= (XMLNode)other.getXML_elements()[otherIndex]; 
				if ( thisNode.usesIDMAP() && otherNode.usesIDMAP() ) {
					if ( otherNode.getOrigId().equals(thisNode.getOrigId()) ) {
						sameId= true;
					}
				}
				
				//unordered compare of subtrees
				// TODO The dist method is order dependent but should not be
				int distance= dist(thisNode, otherNode);
				return sameId || distance == 0;
			}
			return false;
		}
	
		@Override
		public boolean skipRangeComparison(
			int length,
			int maxLength,
			IRangeComparator other) {
			return false;
		}
	
		public Object[] getXML_elements() {
			return fXML_elements;
		}
	
	}

	/* represents a matching between a node in the Left tree and a node in the Right tree */
	static class Match {
		public XMLNode fx;
		public XMLNode fy;
		
		Match(XMLNode x, XMLNode y) {
			fx = x;
			fy = y;	
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Match) {
				Match m = (Match) obj;
				if (m != null)
					return fx == m.fx && fy == m.fy;
			}
			return false;
		}
	}
	
	protected int handleRangeDifferencer(Object[] xc_elements, Object[] yc_elements, ArrayList<Match> DTMatching, int distance) {
		RangeDifference[] differences= RangeDifferencer.findDifferences(new XMLComparator(xc_elements), new XMLComparator(yc_elements));
		
		int cur_pos_left= 0;
		int cur_pos_right= 0;
		for (RangeDifference rd : differences) {
			int equal_length= rd.leftStart();
			//handle elements before current range which are unchanged
			while (cur_pos_left < equal_length) {
				//assuming XMLComparator has already filled fDT and fDT_Matchings for subtrees
				//rooted at xc_elements[cur_pos_left] and yc_elements[cur_pos_right]
//				if ( fDT[indexOfLN( (XMLNode)xc_elements[cur_pos_left])][indexOfRN( (XMLNode)yc_elements[cur_pos_right])] != 0)
//					System.out.println("distance not 0");
//				distance += fDT[indexOfLN( (XMLNode)xc_elements[cur_pos_left])][indexOfRN( (XMLNode)yc_elements[cur_pos_right])];
				//DTMatching.addAll(fDT_Matchings[index_left][index_right]);
				DTMatching.add(new Match( (XMLNode)xc_elements[cur_pos_left], (XMLNode)yc_elements[cur_pos_right]));
				cur_pos_left++;
				cur_pos_right++;
			}
			//now handle RangeDifference rd
			int smaller_length, greater_length;
			boolean leftGreater= rd.leftLength() > rd.rightLength();
			if (leftGreater) {
				smaller_length= rd.rightLength();
				greater_length= rd.leftLength();
			} else {
				smaller_length= rd.leftLength();
				greater_length= rd.rightLength();
			}
			//handle elements elements in range
			for (int j=0; j < smaller_length; j++) {
				distance += dist((XMLNode) xc_elements[cur_pos_left], (XMLNode) yc_elements[cur_pos_right]);
				DTMatching.add(new Match( (XMLNode)xc_elements[cur_pos_left], (XMLNode)yc_elements[cur_pos_right]));
				cur_pos_left++;
				cur_pos_right++;
			}
			//int cur_pos_greater= (leftGreater)?cur_pos_left:cur_pos_right;
			if (leftGreater) {
				for (int j=smaller_length; j < greater_length; j++) {
					distance += countNodes((XMLNode) xc_elements[cur_pos_left]);
					DTMatching.add(new Match( (XMLNode)xc_elements[cur_pos_left], null));
					cur_pos_left++;
				}
			} else {
				for (int j=smaller_length; j < greater_length; j++) {
					distance += countNodes((XMLNode) yc_elements[cur_pos_right]);
					DTMatching.add(new Match( null, (XMLNode)yc_elements[cur_pos_right]));
					cur_pos_right++;
				}
			}
//			for (int j=smaller_length; j < greater_length; j++) {
//				distance += countNodes((XMLNode) xc_elements[cur_pos_greater]);
//				cur_pos_greater++;
//			}
//			if (leftGreater)
//				cur_pos_left= cur_pos_greater;
//			else
//				cur_pos_right= cur_pos_greater;
		}
		
		for (int i= cur_pos_left; i < xc_elements.length; i++) {
			//distance += fDT[indexOfLN( (XMLNode)xc_elements[cur_pos_left])][indexOfRN( (XMLNode)yc_elements[cur_pos_right])];
			//DTMatching.addAll(fDT_Matchings[index_left][index_right]);
			DTMatching.add(new Match( (XMLNode)xc_elements[cur_pos_left], (XMLNode)yc_elements[cur_pos_right]));
			cur_pos_left++;
			cur_pos_right++;
		}
		
		return distance;
	}

	abstract public void match(XMLNode LeftTree, XMLNode RightTree, boolean rightTreeIsAncestor, IProgressMonitor monitor);

	protected int dist(XMLNode x, XMLNode y) {
		//System.out.println("dist( "+x.getSignature()+" , "+y.getSignature()+")");
		int ret= NO_ENTRY;

		int index_x= indexOfLN(x);
		int index_y= indexOfRN(y);
		if (fDT[index_x][index_y] != NO_ENTRY) return fDT[index_x][index_y];
		
		if (isLeaf(x) && isLeaf(y)) {
			if (x.getXMLType() == XMLStructureCreator.TYPE_ELEMENT) {
				if ( x.getSignature().equals(y.getSignature()) ) {
					ret= 0;
					fDT[index_x][index_y] = ret;
				} else {
					ret= 2;
					fDT[index_x][index_y] = ret;
				}
				return ret;
			} else if (x.getXMLType() == XMLStructureCreator.TYPE_ATTRIBUTE || x.getXMLType() == XMLStructureCreator.TYPE_TEXT) {
				if ( x.getSignature().equals(y.getSignature()) ) {
					if (x.getValue().equals(y.getValue())) {
						ret= 0;
						fDT[index_x][index_y] = ret;
					} else {
						ret= 1;
						fDT[index_x][index_y] = ret;
					}
				} else {
					ret= 2;
					fDT[index_x][index_y] = ret;
				}
				return ret;
			}
		} else {//x or y are not leaves
			if ( !x.getSignature().equals(y.getSignature()) ) {
				ret= countNodes(x) + countNodes(y);
				fDT[index_x][index_y] = ret;
				return ret;
			}
			//x.getSignature().equals(y.getSignature())
			if (isLeaf(x)) {
				ret= countNodes(y)-1;
				fDT[index_x][index_y] = ret;
				return ret;
			}
			if (isLeaf(y)) {
				ret= countNodes(x)-1;
				fDT[index_x][index_y] = ret;
				return ret;
			}
			//both x and y have children
			return handleXandYnotLeaves(x,y);
		}
		return ret;
	}
	
	abstract int handleXandYnotLeaves(XMLNode x, XMLNode y);
}
