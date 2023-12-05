/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.model;

import java.util.List;

/**
 * Interface to tag all Ant elements for adapter factory - currently empty.
 */
public interface IAntElement {

	/**
	 * Returns whether this element has been generated as part of an element hierarchy that has error(s) associated with it
	 */
	public boolean isErrorNode();

	/**
	 * Returns whether this element has been generated as part of an element hierarchy that has warning(s) associated with it
	 */
	public boolean isWarningNode();

	/**
	 * Sets whether this element has been generated as part of an element hierarchy that has problems. The severity of the problem is provided.
	 * 
	 * @param severity
	 *            the new severity to set
	 * 
	 * @see AntModelProblem
	 */
	public void setProblemSeverity(int severity);

	/**
	 * Sets the problem associated with this element
	 * 
	 * @param problem
	 *            The problem associated with this element.
	 */
	public void setProblem(IProblem problem);

	/**
	 * Returns the name of the element. May return <code>null</code>
	 * 
	 * @return the name or <code>null</code>
	 */
	public String getName();

	/**
	 * Returns the display label for the element. This should not return <code>null</code> and should be formatted for user consumption
	 * 
	 * @return the label to display for the element, not <code>null</code>
	 */
	public String getLabel();

	/**
	 * Returns whether this XML element is defined in an external entity.
	 * 
	 * @return boolean
	 */
	public boolean isExternal();

	/**
	 * Returns the 0-based index of the first character of the source code for this element, relative to the source buffer in which this element is
	 * contained.
	 * 
	 * @return the 0-based index of the first character of the source code for this element, relative to the source buffer in which this element is
	 *         contained
	 */
	public int getOffset();

	/**
	 * Sets the offset.
	 * 
	 * @see #getOffset()
	 */
	public void setOffset(int anOffset);

	/**
	 * Returns the complete live list of offsets for the given identifier
	 * 
	 * @return the list of offsets for the given identifier
	 */
	public List<Integer> computeIdentifierOffsets(String identifier);

	/**
	 * Returns the number of characters of the source code for this element, relative to the source buffer in which this element is contained.
	 * 
	 * @return the number of characters of the source code for this element, relative to the source buffer in which this element is contained
	 */
	public int getLength();

	/**
	 * Sets the length.
	 * 
	 * @see #getLength()
	 */
	public void setLength(int aLength);

	/**
	 * Returns the length of source to select for this node.
	 * 
	 * @return the length of source to select
	 */
	public int getSelectionLength();

	/**
	 * Returns whether this node contains a reference to the supplied identifier
	 * 
	 * @return whether this node contains a reference to the supplied identifier
	 */
	public boolean containsOccurrence(String identifier);

	/**
	 * Return the complete live collection of {@link IAntElement} child nodes of the this node. If the node has no children an empty list must be
	 * returned, never <code>null</code>
	 * 
	 * @return the live list f child nodes or an empty list
	 */
	public List<IAntElement> getChildNodes();

	/**
	 * Returns if this element has child elements.
	 * 
	 * @return <code>true</code> is this node has child elements, <code>false</code> otherwise
	 */
	public boolean hasChildren();

	/**
	 * Returns the node with the narrowest source range that contains the offset. It may be this node or one of its children or <code>null</code> if
	 * the offset is not in the source range of this node.
	 * 
	 * @param sourceOffset
	 *            The source offset
	 * @return the node that includes the offset in its source range or <code>null</code>
	 */
	public AntElementNode getNode(int sourceOffset);

	/**
	 * Returns a unique string representation of this element. The format of the string is not specified.
	 * 
	 * @return the string representation
	 */
	public String getElementPath();

	/**
	 * Returns the parent <code>AntElementNode</code>.
	 * 
	 * @return the parent or <code>null</code> if this element has no parent.
	 */
	public IAntElement getParentNode();

	/**
	 * Returns the {@link AntProjectNode} this node belongs to
	 * 
	 * @return the {@link AntProjectNode} or <code>null</code>
	 */
	public AntProjectNode getProjectNode();

	/**
	 * Returns the node set as the import node
	 * 
	 * @return the import node or <code>null</code>
	 */
	public IAntElement getImportNode();

	/**
	 * Returns the problem message for this node or <code>null</code> if one has not been set
	 * 
	 * @return the problem message or <code>null</code>
	 */
	public String getProblemMessage();

	/**
	 * Allows the problem message to be set for this node. Specifying <code>null</code> as a problem message will remove any existing message.
	 * 
	 * @param problemMessage
	 *            the new message or <code>null</code>
	 */
	public void setProblemMessage(String problemMessage);

	/**
	 * Returns whether to collapse the code folding projection (region) represented by this node.
	 * 
	 * @return whether the user preference is set to collapse the code folding projection (region) represented by this node
	 */
	public boolean collapseProjection();
}
