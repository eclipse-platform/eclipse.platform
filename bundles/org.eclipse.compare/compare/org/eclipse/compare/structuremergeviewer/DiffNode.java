/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.graphics.Image;

import com.ibm.icu.text.MessageFormat;

/**
 * Diff node are used as the compare result of the differencing engine.
 * Since it implements the <code>ITypedElement</code> and <code>ICompareInput</code>
 * interfaces it can be used directly to display the
 * compare result in a <code>DiffTreeViewer</code> and as the input to any other
 * compare/merge viewer.
 * <p>
 * <code>DiffNode</code>s are typically created as the result of performing
 * a compare with the <code>Differencer</code>.
 * <p>
 * Clients typically use this class as is, but may subclass if required.
 * 
 * @see DiffTreeViewer
 * @see Differencer
 */
public class DiffNode extends DiffContainer implements ICompareInput {

	private ITypedElement fAncestor;
	private ITypedElement fLeft;
	private ITypedElement fRight;
	private boolean fDontExpand;
	private ListenerList fListener;
	private boolean fSwapSides;

	
	/**
	 * Creates a new <code>DiffNode</code> and initializes with the given values.
	 *
	 * @param parent under which the new container is added as a child or <code>null</code>
	 * @param kind of difference (defined in <code>Differencer</code>)
	 * @param ancestor the common ancestor input to a compare
	 * @param left the left input to a compare
	 * @param right the right input to a compare
	 */
	public DiffNode(IDiffContainer parent, int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		this(parent, kind);
		fAncestor= ancestor;
		fLeft= left;
		fRight= right;
	}

	/**
	 * Creates a new <code>DiffNode</code> with diff kind <code>Differencer.CHANGE</code>
	 * and initializes with the given values.
	 *
	 * @param left the left input to a compare
	 * @param right the right input to a compare
	 */
	public DiffNode(ITypedElement left, ITypedElement right) {
		this(null, Differencer.CHANGE, null, left, right);
	}

	/**
	 * Creates a new <code>DiffNode</code> and initializes with the given values.
	 *
	 * @param kind of difference (defined in <code>Differencer</code>)
	 * @param ancestor the common ancestor input to a compare
	 * @param left the left input to a compare
	 * @param right the right input to a compare
	 */
	public DiffNode(int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		this(null, kind, ancestor, left, right);
	}

	/**
	 * Creates a new <code>DiffNode</code> with the given diff kind.
	 *
	 * @param kind of difference (defined in <code>Differencer</code>)
	 */
	public DiffNode(int kind) {
		super(null, kind);
	}

	/**
	 * Creates a new <code>DiffNode</code> and initializes with the given values.
	 *
	 * @param parent under which the new container is added as a child or <code>null</code>
	 * @param kind of difference (defined in <code>Differencer</code>)
	 */
	public DiffNode(IDiffContainer parent, int kind) {
		super(parent, kind);
	}

	/**
	 * Registers a listener for changes of this <code>ICompareInput</code>.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 */
	public void addCompareInputChangeListener(ICompareInputChangeListener listener) {
		if (fListener == null)
			fListener= new ListenerList();
		fListener.add(listener);
	}
	
	/**
	 * Unregisters a <code>ICompareInput</code> listener.
	 * Has no effect if listener is not registered.
	 *
	 * @param listener the listener to remove
	 */
	public void removeCompareInputChangeListener(ICompareInputChangeListener listener) {
		if (fListener != null) {
			fListener.remove(listener);
			if (fListener.isEmpty())
				fListener= null;
		}
	}
	
	/**
	 * Sends out notification that a change has occurred on the <code>ICompareInput</code>.
	 */
	protected void fireChange() {
		if (fListener != null) {
			Object[] listeners= fListener.getListeners();
			for (int i= 0; i < listeners.length; i++)
				((ICompareInputChangeListener) listeners[i]).compareInputChanged(this);
		}
	}

	//---- getters & setters

	/**
	 * Returns <code>true</code> if this node shouldn't automatically be expanded in
	 * a </code>DiffTreeViewer</code>.
	 *
	 * @return <code>true</code> if node shouldn't automatically be expanded
	 */
	public boolean dontExpand() {
		return fDontExpand;
	}

	/**
	 * Controls whether this node is not automatically expanded when displayed in
	 * a </code>DiffTreeViewer</code>.
	 *
	 * @param dontExpand if <code>true</code> this node is not automatically expanded in </code>DiffTreeViewer</code>
	 */
	public void setDontExpand(boolean dontExpand) {
		fDontExpand= dontExpand;
	}

	/**
	 * Returns the first not-<code>null</code> input of this node.
	 * Method checks the three inputs in the order: ancestor, right, left.
	 *
	 * @return the first not-<code>null</code> input of this node
	 */
	public ITypedElement getId() {
		if (fAncestor != null)
			return fAncestor;
		if (fRight != null)
			return fRight;
		return fLeft;
	}

	/**
	 * Returns the (non-<code>null</code>) name of the left or right side if they are identical.
	 * Otherwise both names are concatenated (separated with a slash ('/')).
	 * <p>
	 * Subclasses may re-implement to provide a different name for this node.
	 * @return the name of this node.
	 */
	public String getName() {
		String right= null;
		if (fRight != null)
			right= fRight.getName();

		String left= null;
		if (fLeft != null)
			left= fLeft.getName();

		if (right == null && left == null) {
			if (fAncestor != null)
				return fAncestor.getName();
			return Utilities.getString("DiffNode.noName"); //$NON-NLS-1$
		}

		if (right == null)
			return left;
		if (left == null)
			return right;

		if (right.equals(left))
			return right;
			
		String s1;
		String s2;
		
		if (fSwapSides) {
			s1= left;
			s2= right;
		} else {
			s1= right;
			s2= left;
		}
		
		String fmt= Utilities.getString("DiffNode.nameFormat"); //$NON-NLS-1$
		return MessageFormat.format(fmt, new String[] { s1, s2 });
	}
	
	void swapSides(boolean swap) {
		fSwapSides= swap;
	}
		
	/* (non Javadoc)
	 * see ITypedElement.getImage
	 */
	public Image getImage() {
		ITypedElement id= getId();
		if (id != null)
			return id.getImage();
		return null;
	}

	/* (non Javadoc)
	 * see ITypedElement.getType
	 */
	public String getType() {
		ITypedElement id= getId();
		if (id != null)
			return id.getType();
		return ITypedElement.UNKNOWN_TYPE;
	}

	/**
	 * Sets the ancestor input to the given value.
	 *
	 * @param ancestor the new value for the ancestor input
	 * @since 3.0
	 */
	public void setAncestor(ITypedElement ancestor) {
	    fAncestor= ancestor;
	}
	
	/* (non Javadoc)
	 * see ICompareInput.getAncestor
	 */
	public ITypedElement getAncestor() {
		return fAncestor;
	}
	
	/**
	 * Sets the left input to the given value.
	 *
	 * @param left the new value for the left input
	 */
	public void setLeft(ITypedElement left) {
		fLeft= left;
	}
	
	/* (non Javadoc)
	 * see ICompareInput.getLeft
	 */
	public ITypedElement getLeft() {
		return fLeft;
	}

	/**
	 * Sets the right input to the given value.
	 *
	 * @param right the new value for the right input
	 */
	public void setRight(ITypedElement right) {
		fRight= right;
	}
	
	/* (non Javadoc)
	 * see ICompareInput.getRight
	 */
	public ITypedElement getRight() {
		return fRight;
	}

	/* (non Javadoc)
	 * see ICompareInput.copy
	 */
	public void copy(boolean leftToRight) {
		//System.out.println("DiffNode.copy: " + leftToRight);
		
		IDiffContainer pa= getParent();
		if (pa instanceof ICompareInput) {
			ICompareInput parent= (ICompareInput) pa;
			Object dstParent= leftToRight ? parent.getRight() : parent.getLeft();
			
			if (dstParent instanceof IEditableContent) {
				ITypedElement dst= leftToRight ? getRight() : getLeft();
				ITypedElement src= leftToRight ? getLeft() : getRight();
				dst= ((IEditableContent)dstParent).replace(dst, src);
				if (leftToRight)
					setRight(dst);
				else
					setLeft(dst);
				
				//setKind(Differencer.NO_CHANGE);
				
				fireChange();
			}
		}
	}
	
	/* (non Javadoc)
	 * see Object.hashCode
	 */
	public int hashCode() {
		String[] path= getPath(this, 0);
		int hashCode= 1;
		for (int i= 0; i < path.length; i++) {
	    	String s= path[i];
	   	 	hashCode= (31*hashCode) + (s != null ? s.hashCode() : 0);
		}
		return hashCode;
	}
	
	/* (non Javadoc)
	 * see Object.equals
	 */
	public boolean equals(Object other) {
		if (other != null && getClass() == other.getClass()) {
			String[] path1= getPath(this, 0);
			String[] path2= getPath((DiffNode) other, 0);
			if (path1.length != path2.length)
				return false;
			for (int i= 0; i < path1.length; i++)
				if (! path1[i].equals(path2[i]))
					return false;
			return true;
		}
		return super.equals(other);
	}
	
	private static String[] getPath(ITypedElement el, int level) {
		String[] path= null;
		if (el instanceof IDiffContainer) {
			IDiffContainer parent= ((IDiffContainer)el).getParent();
			if (parent != null)
				path= getPath(parent, level+1);
		}
		if (path == null)
			path= new String[level+1];
		path[(path.length-1)-level]= el.getName();
		return path;
	}
}
