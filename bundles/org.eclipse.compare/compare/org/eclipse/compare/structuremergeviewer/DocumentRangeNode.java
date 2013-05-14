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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.swt.widgets.Shell;


/**
 * A document range node represents a structural element
 * when performing a structure compare of documents.
 * <code>DocumentRangeNodes</code> are created while parsing the document and represent
 * a semantic entity (e.g. a Java class or method).
 * As a consequence of the parsing a <code>DocumentRangeNode</code> maps to a range
 * of characters in the document.
 * <p>
 * Since a <code>DocumentRangeNode</code> implements the <code>IStructureComparator</code>
 * and <code>IStreamContentAccessor</code> interfaces it can be used as input to the
 * differencing engine. This makes it possible to perform
 * a structural diff on a document and have the nodes and leaves of the compare easily map
 * to character ranges within the document.
 * <p>
 * Clients need to be aware that this node registers position updaters with the document
 * using {@link IDocument#addPosition(String, Position)} with the category set to 
 * {@link IDocumentRange#RANGE_CATEGORY}. The {@link StructureDiffViewer} will 
 * remove the category when the nodes are no longer being used. Other clients
 * must do the same.
 * <p>
 * Subclasses may add additional state collected while parsing the document.
 * </p> 
 * @see Differencer
 */
public class DocumentRangeNode
		implements IDocumentRange, IStructureComparator, IEditableContent, IEncodedStreamContentAccessor, IAdaptable, IEditableContentExtension {

	private static final String UTF_16= "UTF-16"; //$NON-NLS-1$
		
	private IDocument fBaseDocument;
	private Position fRange; // the range in the base document
	private int fTypeCode;
	private String fID;
	private Position fAppendPosition; // a position where to insert a child textually
	private ArrayList fChildren;
	private final DocumentRangeNode fParent;

	/**
	 * Creates a new <code>DocumentRangeNode</code> for the given range within the specified
	 * document. The <code>typeCode</code> is uninterpreted client data. The ID is used when comparing
	 * two nodes with each other: i.e. the differencing engine performs a content compare 
	 * on two nodes if their IDs are equal.
	 *
	 * @param typeCode a type code for this node
	 * @param id an identifier for this node
	 * @param document document on which this node is based on
	 * @param start start position of range within document
	 * @param length length of range
	 */
	public DocumentRangeNode(int typeCode, String id, IDocument document, int start, int length) {
		this(null, typeCode, id, document, start, length);
	}
	
	/**
	 * Creates a new <code>DocumentRangeNode</code> for the given range within the specified
	 * document. The <code>typeCode</code> is uninterpreted client data. The ID is used when comparing
	 * two nodes with each other: i.e. the differencing engine performs a content compare 
	 * on two nodes if their IDs are equal.
	 * 
	 * @param parent the parent node
	 * @param typeCode a type code for this node
	 * @param id an identifier for this node
	 * @param document document on which this node is based on
	 * @param start start position of range within document
	 * @param length length of range
	 * @since 3.3
	 */
	public DocumentRangeNode(DocumentRangeNode parent, int typeCode, String id, IDocument document, int start, int length) {
		fParent = parent;
		fTypeCode= typeCode;
		fID= id;
		fBaseDocument= document;
		registerPositionUpdater(start, length);
	}

	private void registerPositionUpdater(int start, int length) {
		fBaseDocument.addPositionCategory(RANGE_CATEGORY);
		fRange= new Position(start, length);
		try {
			fBaseDocument.addPosition(RANGE_CATEGORY, fRange);
		} catch (BadPositionCategoryException ex) {
			CompareUIPlugin.log(ex);
		} catch (BadLocationException ex) {
			CompareUIPlugin.log(ex);
		}
	}

	/* (non Javadoc)
	 * see IDocumentRange.getDocument
	 */
	public IDocument getDocument() {
		return fBaseDocument;
	}
	
	/* (non Javadoc)
	 * see IDocumentRange.getRange
	 */
	public Position getRange() {
		return fRange;
	}
	
	/**
	 * Returns the type code of this node.
	 * The type code is uninterpreted client data which can be set in the constructor.
	 *
	 * @return the type code of this node
	 */
	public int getTypeCode() {
		return fTypeCode;
	}
	
	/**
	 * Returns this node's id.
	 * It is used in <code>equals</code> and <code>hashcode</code>.
	 *
	 * @return the node's id
	 */
	public String getId() {
		return fID;
	}

	/**
	 * Sets this node's id.
	 * It is used in <code>equals</code> and <code>hashcode</code>.
	 *
	 * @param id the new id for this node
	 */
	public void setId(String id) {
		fID= id;
	}

	/**
	 * Adds the given node as a child.
	 *
	 * @param node the node to add as a child
	 */
	public void addChild(DocumentRangeNode node) {
		if (fChildren == null)
			fChildren= new ArrayList();
		fChildren.add(node);
	}

	/* (non Javadoc)
	 * see IStructureComparator.getChildren
	 */
	public Object[] getChildren() {
		if (fChildren != null)
			return fChildren.toArray(); 
		return new Object[0];
	}

	/**
	 * Sets the length of the range of this node.
	 *
	 * @param length the length of the range
	 */
	public void setLength(int length) {
		getRange().setLength(length);
	}

	/**
	 * Sets a position within the document range that can be used to (legally) insert
	 * text without breaking the syntax of the document.
	 * <p>
	 * E.g. when parsing a Java document the "append position" of a <code>DocumentRangeNode</code>
	 * representing a Java class could be the character position just before the closing bracket.
	 * Inserting the text of a new method there would not disturb the syntax of the class.
	 *
	 * @param pos the character position within the underlying document where text can be legally inserted
	 */
	public void setAppendPosition(int pos) {
		if (fAppendPosition != null)
			try {
				fBaseDocument.removePosition(RANGE_CATEGORY, fAppendPosition);
			} catch (BadPositionCategoryException e) {
				// Ignore
			}
		try {
			// TODO: Avoid an exception for a position that is past the end of the document
			if (pos <= getDocument().getLength()) {
				Position p= new Position(pos);
				fBaseDocument.addPosition(RANGE_CATEGORY, p);
				fAppendPosition= p;
			}
		} catch (BadPositionCategoryException ex) {
			// silently ignored
		} catch (BadLocationException ex) {
			// silently ignored
		}
	}

	/**
	 * Returns the position that has been set with <code>setAppendPosition</code>.
	 * If <code>setAppendPosition</code> hasn't been called, the position after the last character
	 * of this range is returned. This method will return <code>null</code> if the position
	 * could not be registered with the document.
	 *
	 * @return a position where text can be legally inserted
	 */
	public Position getAppendPosition() {
		if (fAppendPosition == null) {
			try {
				Position p= new Position(fBaseDocument.getLength());
				fBaseDocument.addPosition(RANGE_CATEGORY, p);
				fAppendPosition= p;
				return fAppendPosition;
			} catch (BadPositionCategoryException ex) {
				// silently ignored
			} catch (BadLocationException ex) {
				// silently ignored
			}
		}
		return new Position(fBaseDocument.getLength());
	}

	/**
	 * Implementation based on <code>getID</code>.
     * @param other the object to compare this <code>DocumentRangeNode</code> against.
     * @return <code>true</code> if the <code>DocumentRangeNodes</code>are equal; <code>false</code> otherwise.
	 */
	public boolean equals(Object other) {
		if (other != null && other.getClass() == getClass()) {
			DocumentRangeNode tn= (DocumentRangeNode) other;
			return fTypeCode == tn.fTypeCode && fID.equals(tn.fID);
		}
		return super.equals(other);
	}

	/**
	 * Implementation based on <code>getID</code>.
	 * @return a hash code for this object.
	 */
	public int hashCode() {
		return fID.hashCode();
	}

	/*
	 * Find corresponding position
	 */
	private Position findCorrespondingPosition(DocumentRangeNode otherParent, DocumentRangeNode child) {

		// we try to find a predecessor of left Node which exists on the right side

		if (child != null && fChildren != null) {
			int ix= otherParent.fChildren.indexOf(child);
			if (ix >= 0) {

				for (int i= ix - 1; i >= 0; i--) {
					DocumentRangeNode c1= (DocumentRangeNode) otherParent.fChildren.get(i);
					int i2= fChildren.indexOf(c1);
					if (i2 >= 0) {
						DocumentRangeNode c= (DocumentRangeNode) fChildren.get(i2);
						//System.out.println("  found corresponding: " + i2 + " " + c);
						Position p= c.fRange;

						//try {
						Position po= new Position(p.getOffset() + p.getLength() + 1, 0);
						//c.fBaseDocument.addPosition(RANGE_CATEGORY, po);
						return po;
						//} catch (BadLocationException ex) {
						//}
						//break;
					}
				}

				for (int i= ix; i < otherParent.fChildren.size(); i++) {
					DocumentRangeNode c1= (DocumentRangeNode) otherParent.fChildren.get(i);
					int i2= fChildren.indexOf(c1);
					if (i2 >= 0) {
						DocumentRangeNode c= (DocumentRangeNode) fChildren.get(i2);
						//System.out.println("  found corresponding: " + i2 + " " + c);
						Position p= c.fRange;
						//try {
						Position po= new Position(p.getOffset(), 0);
						//c.fBaseDocument.addPosition(RANGE_CATEGORY, po);
						return po;
						//} catch (BadLocationException ex) {
						//}
						//break;
					}
				}

			}
		}
		return getAppendPosition();
	}

	private void add(String s, DocumentRangeNode parent, DocumentRangeNode child) {

		Position p= findCorrespondingPosition(parent, child);
		if (p != null) {
			try {
				fBaseDocument.replace(p.getOffset(), p.getLength(), s);
			} catch (BadLocationException ex) {
				CompareUIPlugin.log(ex);
			}
		}
	}
	
	/* (non Javadoc)
	 * see IStreamContentAccessor.getContents
	 */
	public InputStream getContents() {
		String s;
		try {
			s= fBaseDocument.get(fRange.getOffset(), fRange.getLength());
		} catch (BadLocationException ex) {
			s= ""; //$NON-NLS-1$
		}		
		return new ByteArrayInputStream(Utilities.getBytes(s, UTF_16));
	}


	/**
	 * If this node has a parent, return the editability of the parent.
	 * Otherwise return <code>true</code>. Subclasses may override.
	 * @see org.eclipse.compare.IEditableContent#isEditable()
	 */
	public boolean isEditable() {
		if (fParent != null)
			return fParent.isEditable();
		return true;
	}
		
	/* (non Javadoc)
	 * see IEditableContent.replace
	 */
	public ITypedElement replace(ITypedElement child, ITypedElement other) {

		if (fParent == null) {
			// TODO: I don't believe this code does anything useful but just in case
			// I'm leaving it in but disabling it for the shared document case
			// since all the subclasses that have been converted overrode the method anyway
			DocumentRangeNode src= null;
			String srcContents= ""; //$NON-NLS-1$
			
			if (other != null) {
				src= (DocumentRangeNode) child;
				
				if (other instanceof IStreamContentAccessor) {
					try {
						srcContents= Utilities.readString((IStreamContentAccessor)other);
					} catch(CoreException ex) {
						// NeedWork
						CompareUIPlugin.log(ex);
					}
				}
			}
	
			if (child == null) // no destination: we have to add the contents into the parent
				add(srcContents, null, src);
		}
		nodeChanged(this);
		return child;
	}
	
	/**
	 * Default implementation that calls {@link #internalSetContents(byte[])}
	 * and then {@link #nodeChanged(DocumentRangeNode)}. Subclasses
	 * may override but should then call {@link #nodeChanged(DocumentRangeNode)}
	 * after the contents have been set.
	 * @see org.eclipse.compare.IEditableContent#setContent(byte[])
	 */
	public void setContent(byte[] content) {
		internalSetContents(content);
		nodeChanged(this);
	}

	/**
	 * Method that is invoked from {@link #setContent(byte[])}. By default,
	 * this method does nothing. Subclasses may override. 
	 * @param content the new content
	 * @since 3.3
	 */
	protected void internalSetContents(byte[] content) {
		// By default, do nothing
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IStreamContentAccessor#getEncoding()
	 */
	public String getCharset() {
		return UTF_16;
	}
	
	/**
	 * Method that should be invoked whenever the contents of this node are
	 * changed. the change is propagated to the parent if there is one.
	 * @param node the node that has changed.
	 * @since 3.3
	 */
	protected void nodeChanged(DocumentRangeNode node) {
		if (fParent != null)
			fParent.nodeChanged(node);
	}
	
	/**
	 * Implement {@link IAdaptable#getAdapter(Class)} in order to provide
	 * an {@link ISharedDocumentAdapter} that provides the proper look up key based
	 * on the input from which this structure node was created. The proper
	 * shared document adapter is obtained by calling {@link #getAdapter(Class)}
	 * on this node's parent if there is one.
	 * @param adapter the adapter class to look up
	 * @return the object adapted to the given class or <code>null</code>
	 * @see IAdaptable#getAdapter(Class)
	 * @since 3.3
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class && fParent != null)
			return fParent.getAdapter(adapter);
		
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContentExtension#isReadOnly()
	 */
	public boolean isReadOnly() {
		if (fParent != null)
			return fParent.isReadOnly();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContentExtension#validateEdit(org.eclipse.swt.widgets.Shell)
	 */
	public IStatus validateEdit(Shell shell) {
		if (fParent != null)
			return fParent.validateEdit(shell);
		return Status.OK_STATUS;
	}

	/**
	 * Return the parent of this node or <code>null</code>
	 * if the node doesn't have a parent or the parent is not known.
	 * @return the parent of this node or <code>null</code>
	 */
	public Object getParentNode() {
		return fParent;
	}
}

