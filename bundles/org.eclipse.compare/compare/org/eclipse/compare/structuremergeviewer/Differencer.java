/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.ibm.icu.text.MessageFormat;

/**
 * A generic two-way or three-way differencing engine.
 * <p>
 * The engine is used by calling one of the <code>findDifferences</code> methods and passing
 * in the objects to compare.
 * The engine calls the following methods on the input objects to perform the compare:
 * <UL>
 * <LI><code>getChildren</code>: for enumerating the children of an object (if any),
 * <LI><code>contentsEqual</code>: for comparing the content of leaf objects, that is, objects without children,
 * <LI><code>visit</code>: for every pair of compared object the compare result is passed in.
 * </UL>
 * Clients may use as is, or subclass to provide a custom implementation for the three hooks. 
 * However the default implementation already deals with the typical case:
 * <UL>
 * <LI><code>getChildren</code>: tries to apply the <code>IStructureComparator</code>
 * 	interface to enumerate the children,
 * <LI><code>contentsEqual</code>: tries to apply the <code>IStreamContentAccessor</code> interface
 *	to perform a byte-wise content comparison,
 * <LI><code>visit</code>: creates a <code>DiffNode</code> for any detected difference between the compared objects and
 *	links it under a parent node effectively creating a tree of differences.
 * </UL>
 * The different kind of changes detected by the engine are decoded as follows:
 * In the two-way case only NO_CHANGE, ADDITION, DELETION, and CHANGE are used.
 * In the three-way case these constants are bitwise ORed with one of directional constants
 * LEFT, RIGHT, and CONFLICTING.
 */
public class Differencer {
	
	// The kind of differences.
	/**
	 * Difference constant (value 0) indicating no difference.
	 */
	public static final int NO_CHANGE= 0;
	/**
	 * Difference constant (value 1) indicating one side was added.
	 */
	public static final int ADDITION= 1;
	/**
	 * Difference constant (value 2) indicating one side was removed.
	 */
	public static final int DELETION= 2;
	/**
	 * Difference constant (value 3) indicating side changed.
	 */
	public static final int CHANGE= 3;

	/**
	 * Bit mask (value 3) for extracting the kind of difference.
	 */
	public static final int CHANGE_TYPE_MASK= 3;

	// The direction of a three-way change.
	/**
	 * Three-way change constant (value 4) indicating a change on left side.
	 */
	public static final int LEFT= 4;
	
	/**
	 * Three-way change constant (value 8) indicating a change on right side.
	 */
	public static final int RIGHT= 8;
	
	/**
	 * Three-way change constant (value 12) indicating a change on left and
	 * right sides.
	 */
	public static final int CONFLICTING= 12;

	/**
	 * Bit mask (value 12) for extracting the direction of a three-way change.
	 */
	public static final int DIRECTION_MASK= 12;

	/**
	 * Constant (value 16) indicating a change on left and 
	 * right side (with respect to ancestor) but left and right are identical.
	 */
	public static final int PSEUDO_CONFLICT= 16;

	
	static class Node {
		List fChildren;
		int fCode;
		Object fAncestor;
		Object fLeft;
		Object fRight;
		
		Node() {
			// nothing to do
		}
		Node(Node parent, Object ancestor, Object left, Object right) {
			parent.add(this);
			fAncestor= ancestor;
			fLeft= left;
			fRight= right;
		}
		void add(Node child) {
			if (fChildren == null)
				fChildren= new ArrayList();
			fChildren.add(child);
		}
		Object visit(Differencer d, Object parent, int level) {
			if (fCode == NO_CHANGE)
				return null;
			//dump(level);
			Object data= d.visit(parent, fCode, fAncestor, fLeft, fRight);
			if (fChildren != null) {
				Iterator i= fChildren.iterator();
				while (i.hasNext()) {
					Node n= (Node) i.next();
					n.visit(d, data, level+1);
				}
			}
			return data;
		}
//		private void dump(int level) {
//			String name= null;
//			if (fAncestor instanceof ITypedElement)
//				name= ((ITypedElement)fAncestor).getName();
//			if (name == null && fLeft instanceof ITypedElement)
//				name= ((ITypedElement)fLeft).getName();
//			if (name == null && fRight instanceof ITypedElement)
//				name= ((ITypedElement)fRight).getName();
//			if (name == null)
//				name= "???"; //$NON-NLS-1$
//			
//			for (int i= 0; i < level; i++)
//				System.out.print("  "); //$NON-NLS-1$
//			
//			System.out.println(getDiffType(fCode) + name);
//		}

//		private String getDiffType(int code) {
//			String dir= " "; //$NON-NLS-1$
//			switch (code & DIRECTION_MASK) {
//			case LEFT:
//				dir= ">"; //$NON-NLS-1$
//				break;
//			case RIGHT:
//				dir= "<"; //$NON-NLS-1$
//				break;
//			case CONFLICTING:
//				dir= "!"; //$NON-NLS-1$
//				break;
//			}
//			String change= "="; //$NON-NLS-1$
//			switch (code & CHANGE_TYPE_MASK) {
//			case ADDITION:
//				change= "+"; //$NON-NLS-1$
//				break;
//			case DELETION:
//				change= "-"; //$NON-NLS-1$
//				break;
//			case CHANGE:
//				change= "#"; //$NON-NLS-1$
//				break;
//			}
//			return dir + change + " "; //$NON-NLS-1$
//		}
	} 
	
	/**
	 * Creates a new differencing engine.
	 */
	public Differencer() {
		// nothing to do
	}
	
	/**
	 * Starts the differencing engine on the three input objects. If threeWay is <code>true</code> a 
	 * three-way comparison is performed, otherwise a two-way compare (in the latter case the ancestor argument is ignored).
	 * The progress monitor is passed to the method <code>updateProgress</code> which is called for every node or
	 * leaf compare. The method returns the object that was returned from the top-most call to method <code>visit</code>.
	 * At most two of the ancestor, left, and right parameters are allowed to be <code>null</code>.
	 *
	 * @param threeWay if <code>true</code> a three-way comparison is performed, otherwise a two-way compare
	 * @param pm a progress monitor which is passed to method <code>updateProgress</code>
	 * @param data a client data that is passed to the top-level call to <code>visit</code>
	 * @param ancestor the ancestor object of the compare (may be <code>null</code>)
	 * @param left the left object of the compare 
	 * @param right the right object of the compare
	 * @return the object returned from the top most call to method <code>visit</code>,
	 *   possibly <code>null</code>
	 */
	public Object findDifferences(boolean threeWay, IProgressMonitor pm, Object data, Object ancestor, Object left, Object right) {
		
		Node root= new Node();
		
		int code= traverse(threeWay, root, pm, threeWay ? ancestor : null, left, right);
				
		if (code != NO_CHANGE) {
			List l= root.fChildren;
			if (l.size() > 0) {
				Node first= (Node)l.get(0);
				return first.visit(this, data, 0);
			}
		}
		return null;
	}
	
	/*
	 * Traverse tree in postorder.
	 */
	private int traverse(boolean threeWay, Node parent, IProgressMonitor pm, Object ancestor, Object left, Object right) {
				
		Object[] ancestorChildren= getChildren(ancestor);
		Object[] rightChildren= getChildren(right);
		Object[] leftChildren= getChildren(left);
		
		int code= NO_CHANGE;
		
		Node node= new Node(parent, ancestor, left, right);
			
		boolean content= true;	// we reset this if we have at least one child
		
		if (((threeWay && ancestorChildren != null) || !threeWay)
					 && rightChildren != null && leftChildren != null) {
			// we only recurse down if no leg is null
			// a node
			
			Set allSet= new HashSet(20);
			Map ancestorSet= null;
			Map rightSet= null;
			Map leftSet= null;
						
			if (ancestorChildren != null) {
				ancestorSet= new HashMap(10);
				for (int i= 0; i < ancestorChildren.length; i++) {
					Object ancestorChild= ancestorChildren[i];
					ancestorSet.put(ancestorChild, ancestorChild);
					allSet.add(ancestorChild);
				}
			}

			if (rightChildren != null) {
				rightSet= new HashMap(10);
				for (int i= 0; i < rightChildren.length; i++) {
					Object rightChild= rightChildren[i];
					rightSet.put(rightChild, rightChild);
					allSet.add(rightChild);
				}
			}

			if (leftChildren != null) {
				leftSet= new HashMap(10);
				for (int i= 0; i < leftChildren.length; i++) {
					Object leftChild= leftChildren[i];
					leftSet.put(leftChild, leftChild);
					allSet.add(leftChild);
				}
			}
						
			Iterator e= allSet.iterator();
			while (e.hasNext()) {
				Object keyChild= e.next();
				
				if (pm != null) {
					
					if (pm.isCanceled())
						throw new OperationCanceledException();
						
					updateProgress(pm, keyChild);
				}
				
				Object ancestorChild= ancestorSet != null ? ancestorSet.get(keyChild) : null;
				Object leftChild= leftSet != null ? leftSet.get(keyChild) : null;
				Object rightChild= rightSet != null ? rightSet.get(keyChild) : null;
				
				int c= traverse(threeWay, node, pm, ancestorChild, leftChild, rightChild);
			
				if ((c & CHANGE_TYPE_MASK) != NO_CHANGE) {
					code|= CHANGE;	// deletions and additions of child result in a change of the container
					code|= (c & DIRECTION_MASK);	// incoming & outgoing are just ored
					content= false;
				}
			}
		}

		if (content)			// a leaf
			code= compare(threeWay, ancestor, left, right);
								
		node.fCode= code;
							
		return code;
	}
	
	/**
	 * Called for every node or leaf comparison.
	 * The differencing engine passes in the input objects of the compare and the result of the compare.
	 * The data object is the value returned from a call to the <code>visit</code> method on the parent input.
	 * It can be considered the "parent" reference and is useful when building a tree.
	 * <p>
	 * The <code>Differencer</code> implementation returns a new
	 * <code>DiffNode</code> which is initialized with the corresponding values.
	 * Subclasses may override.
	 *
	 * @param data object returned from parent call to <code>visit</code>,
	 *   possibly <code>null</code>
	 * @param result the result of the compare operation performed on the three inputs
	 * @param ancestor the compare ancestor of the left and right inputs
	 * @param left the left input to the compare
	 * @param right the right input to the compare
	 * @return the result, possibly <code>null</code>
	 */
	protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
		return new DiffNode((IDiffContainer) data, result, (ITypedElement)ancestor, (ITypedElement)left, (ITypedElement)right);
	}
	
	/*
	 * Performs a 2-way or 3-way compare of the given leaf elements and returns an integer
	 * describing the kind of difference.
	 */
	private int compare(boolean threeway, Object ancestor, Object left, Object right) {
		
		int description= NO_CHANGE;
		
		if (threeway) {
			if (ancestor == null) {
				if (left == null) {
					if (right == null) {
						Assert.isTrue(false);
						// shouldn't happen
					} else {
						description= RIGHT | ADDITION;
					}
				} else {
					if (right == null) {
						description= LEFT | ADDITION;
					} else {
						description= CONFLICTING | ADDITION;
						if (contentsEqual(left, MergeViewerContentProvider.LEFT_CONTRIBUTOR, right, MergeViewerContentProvider.RIGHT_CONTRIBUTOR))
							description|= PSEUDO_CONFLICT;
					}
				}
			} else {
				if (left == null) {
					if (right == null) {
						description= CONFLICTING | DELETION | PSEUDO_CONFLICT;
					} else {
						if (contentsEqual(ancestor,	MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR, right, MergeViewerContentProvider.RIGHT_CONTRIBUTOR))
							description= LEFT | DELETION;
						else
							description= CONFLICTING | CHANGE;	
					}
				} else {
					if (right == null) {
						if (contentsEqual(ancestor, MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR, left, MergeViewerContentProvider.LEFT_CONTRIBUTOR))
							description= RIGHT | DELETION;
						else
							description= CONFLICTING | CHANGE;	
					} else {
						boolean ay= contentsEqual(ancestor, MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR, left, MergeViewerContentProvider.LEFT_CONTRIBUTOR);
						boolean am= contentsEqual(ancestor, MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR, right, MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
						
						if (ay && am) {
							// empty
						} else if (ay && !am) {
							description= RIGHT | CHANGE;
						} else if (!ay && am) {
							description= LEFT | CHANGE;
						} else {
							description= CONFLICTING | CHANGE;
							if (contentsEqual(left, MergeViewerContentProvider.LEFT_CONTRIBUTOR, right, MergeViewerContentProvider.RIGHT_CONTRIBUTOR))
								description|= PSEUDO_CONFLICT;
						}
					}
				}
			}
		} else {	// two way compare ignores ancestor
			if (left == null) {
				if (right == null) {
					Assert.isTrue(false);
					// shouldn't happen
				} else {
					description= ADDITION;
				}
			} else {
				if (right == null) {
					description= DELETION;
				} else {
					if (! contentsEqual(left, MergeViewerContentProvider.LEFT_CONTRIBUTOR, right, MergeViewerContentProvider.RIGHT_CONTRIBUTOR))
						description= CHANGE;
				}
			}
		}
							
		return description;
	}

	/**
	 * Performs a content compare on the two given inputs.
	 * <p>
	 * The <code>Differencer</code> implementation returns
	 * <code>contentsEqual(Object input1, Object input2)</code>. Subclasses may
	 * override to implement a different content compare on the given inputs.
	 * </p>
	 * 
	 * @param input1
	 *            first input to contents compare
	 * @param contributor1
	 *            the contributor of input1
	 * @param input2
	 *            second input to contents compare
	 * @param contributor2
	 *            the contributor of input2
	 * @return <code>true</code> if content is equal
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 3.6
	 */
	protected boolean contentsEqual(Object input1, char contributor1,
			Object input2, char contributor2) {
		return contentsEqual(input1, input2);
	}

	/**
	 * Performs a content compare on the two given inputs.
	 * <p>
	 * The <code>Differencer</code> implementation
	 * returns <code>true</code> if both inputs implement <code>IStreamContentAccessor</code>
	 * and their byte contents is identical. Subclasses may override to implement 
	 * a different content compare on the given inputs.
	 * </p>
	 *
	 * @param input1 first input to contents compare
	 * @param input2 second input to contents compare
	 * @return <code>true</code> if content is equal
	 */
	protected boolean contentsEqual(Object input1, Object input2) {
		
		if (input1 == input2)
			return true;
			
		InputStream is1= getStream(input1);
		InputStream is2= getStream(input2);
		
		if (is1 == null && is2 == null)	// no byte contents
			return true;
		
		try {
			if (is1 == null || is2 == null)	// only one has contents
				return false;
			
			while (true) {
				int c1= is1.read();
				int c2= is2.read();
				if (c1 == -1 && c2 == -1)
					return true;
				if (c1 != c2)
					break;
				
			}
		} catch (IOException ex) {
			// NeedWork
		} finally {
			if (is1 != null) {
				try {
					is1.close();
				} catch(IOException ex) {
					// silently ignored
				}
			}
			if (is2 != null) {
				try {
					is2.close();
				} catch(IOException ex) {
					// silently ignored
				}
			}
		}
		return false;
	}
	
	/*
	 * Tries to return an InputStream for the given object.
	 * Returns <code>null</code> if the object not an IStreamContentAccessor
	 * or an error occurred.
	 */
	private InputStream getStream(Object o) {
		if (o instanceof IStreamContentAccessor) {
			try {
				return ((IStreamContentAccessor)o).getContents();
			} catch(CoreException ex) {
				// NeedWork
			}
		}
		return null;
	}
	
	/**
	 * Returns the children of the given input or <code>null</code> if there are no children.
	 * <p>
	 * The <code>Differencer</code> implementation checks whether the input 
	 * implements the <code>IStructureComparator</code> interface. If yes it is used
	 * to return an array containing all children. Otherwise <code>null</code> is returned.
	 * Subclasses may override to implement a different strategy to enumerate children.
	 * </p>
	 *
	 * @param input the object for which to return children
	 * @return the children of the given input or <code>null</code> if there are no children.
	 */
	protected Object[] getChildren(Object input) {
		if (input instanceof IStructureComparator)
			return ((IStructureComparator)input).getChildren();
		return null;
	}
	
	/**
	 * Called for every leaf or node compare to update progress information.
	 * <p>
	 * The <code>Differencer</code> implementation shows the name of the input object
	 * as a subtask. Subclasses may override.
	 * </p>
	 *
	 * @param progressMonitor the progress monitor for reporting progress
	 * @param node the currently processed non-<code>null</code> node
	 */
	protected void updateProgress(IProgressMonitor progressMonitor, Object node) {
		if (node instanceof ITypedElement) {
			String name= ((ITypedElement)node).getName();
			String fmt= Utilities.getString("Differencer.progressFormat"); //$NON-NLS-1$
			String msg= MessageFormat.format(fmt, new String[] { name });
			progressMonitor.subTask(msg);
			//progressMonitor.worked(1);
		}
	}
}

