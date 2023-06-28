/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;



/**
 * Describes a change within a model. A delta is a hierarchical description of changes
 * within a model. It consists of a tree of nodes. Each node references an element
 * from a model describing how that element changed. A model proxy fires model deltas
 * as its model changes in order to update views displaying that model.
 * <p>
 * Each node in a model delta describes the following:
 * <ul>
 * <li>the type of change - for example, whether an element was added, removed,
 *  or whether its content or state changed</li>
 * <li>action to consider - for example, select or reveal the element</li>
 * </ul>
 * </p>
 * <p>
 * @noimplement Clients are not intended to implement this interface directly. Instead, clients
 * creating and firing model deltas should create instances of {@link ModelDelta}.
 * </p>
 * @since 3.2
 */
public interface IModelDelta {

	// types of changes

	/**
	 * Indicates an element has not changed, but has children that have
	 * changed in some way.
	 */
	int NO_CHANGE = 0;
	/**
	 * Indicates an element has been added to the model, as described by
	 * its path.
	 */
	int ADDED = 1;
	/**
	 * Indicates an element has been removed from the model, as described by
	 * its path.
	 */
	int REMOVED = 1 << 1;
	/**
	 * Indicates an element has been replaced in the model, as described by
	 * its path. In this case a replacement element is also specified in the
	 * model delta node.
	 */
	int REPLACED = 1 << 3;
	/**
	 * Indicates an element has been inserted into the model, as described
	 * by its path and index.
	 */
	int INSERTED = 1 << 4;

	// how an element changed

	/**
	 * Indicates an elements content has changed (i.e. its children).
	 */
	int CONTENT = 1 << 10;
	/**
	 * Indicates an elements state has changed (i.e. label)
	 */
	int STATE = 1 << 11;

	// Suggested actions

	/**
	 * Suggests that the element should be expanded, as described by its path.
	 */
	int EXPAND = 1 << 20;
	/**
	 * Suggests that the element should be selected, as described by its path.
	 */
	int SELECT = 1 << 21;

	/**
	 * Suggests that the element should be revealed, as described by its path.
	 * @since 3.3
	 */
	int REVEAL = 1 << 24;

	/**
	 * Indicates a model proxy should be installed for the given element
	 * @since 3.3
	 */
	int INSTALL = 1 << 22;

	/**
	 * Indicates a model proxy should be uninstalled for the given element
	 * @since 3.3
	 */
	int UNINSTALL = 1 << 23;

	/**
	 * Suggests that the element should be collapsed, as described by its path.
	 * @since 3.3
	 */
	int COLLAPSE = 1 << 25;

	/**
	 * Flag indicating that the view layout deltas should override the
	 * model selection policy.  This flag can be used in conjunction with
	 * SELECT and REVEAL flags.
	 *
	 * @see IModelSelectionPolicy
=	 * @since 3.5
	 */
	int FORCE = 1 << 26;

	/**
	 * Returns the parent of this node, or <code>null</code> if this is
	 * a root node.
	 *
	 * @return parent node or <code>null</code> if this is a root node
	 */
	IModelDelta getParentDelta();

	/**
	 * Returns the model element this node describes.
	 *
	 * @return associated model element
	 */
	Object getElement();

	/**
	 * Returns flags describing how this element changed. A bit mask of the
	 * change type constants described in {@link IModelDelta}.
	 *
	 * @return change flags
	 */
	int getFlags();

	/**
	 * Returns nodes describing changed children, possibly an empty collection.
	 *
	 * @return changed children, possibly empty
	 */
	IModelDelta[] getChildDeltas();

	/**
	 * When a node indicates the <code>IModelDelta.REPLACED</code> flag, this method
	 * returns the replacement element, otherwise <code>null</code>.
	 *
	 * @return replacement element or <code>null</code>
	 */
	Object getReplacementElement();

	/**
	 * Returns this node's index in its parents child collection or -1 if unknown.
	 * This attribute is required when expanding or selecting an element.
	 * <p>
	 * When a node indicates the <code>IModelDelta.INSERTED</code> flag, this method
	 * returns the index that the new element should be inserted at relative to its
	 * parents children, otherwise -1.
	 * </p>
	 * @return insertion index or -1
	 */
	int getIndex();

	/**
	 * Returns the total number of children this element has, or -1 if unknown. Note
	 * that this number may be greater than the number of child delta nodes for this
	 * node, since not all children may be reporting deltas.
	 * <p>
	 * This attribute is required when expanding or selecting an element.
	 * </p>
	 *
	 * @return total number of child elements this element has
	 */
	int getChildCount();

	/**
	 * Accepts the given visitor.
	 *
	 * @param visitor delta visitor to accept
	 * @since 3.3
	 */
	void accept(IModelDeltaVisitor visitor);

}
