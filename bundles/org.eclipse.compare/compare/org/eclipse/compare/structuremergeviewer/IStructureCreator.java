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

/**
 * Interface used to create a hierarchical structure of
 * <code>IStructureComparator</code>s for a given input object.
 * In addition, it provides methods for locating a path in the hierarchical structure
 * and to map a node of this structure back to the corresponding input object.
 * <p>
 * Structure creators are used in the following contexts:
 * <ul>
 * <li>
 * the <code>StructureDiffViewer</code> uses an <code>IStructureCreator</code> to
 * build two (or three) tree structures of its input elements (method <code>getStructure</code>).
 * These trees are then compared with each other by means of the differencing engine and displayed
 * with the <code>DiffTreeViewer</code>,
 * </li>
 * <li>
 * the <code>ReplaceWithEditionDialog</code> uses an <code>IStructureCreator</code>
 * to map a path back to a range of characters in the textual representation.
 * </li>
 * </ul>
 * A <code>IStructureCreator</code> provides methods for rewriting the tree produced by the differencing
 * engine to support "smart" structural differencing. E.g. certain patterns of pairs of "addition"
 * and "deletion" nodes can be detected as renames and merged into a single node.
 * </p>
 * <p>
 * Clients may implement this interface; there is no standard implementation.
 * </p>
 *
 * @see StructureDiffViewer
 * @see org.eclipse.compare.EditionSelectionDialog
 * @see Differencer
 */
public interface IStructureCreator {

	/**
	 * Returns a descriptive name which can be used in the UI of the <code>StructureDiffViewer</code>.
	 *
	 * @return a descriptive name for this <code>IStructureCreator</code>
	 */
	String getName();

	/**
	 * Creates a tree structure consisting of <code>IStructureComparator</code>s
	 * from the given object and returns its root object.
	 * Implementing this method typically involves parsing the input object.
	 * In case of an error (e.g. a parsing error) the value <code>null</code> is returned.
	 *
	 * @param input the object from which to create the tree of <code>IStructureComparator</code>
	 * @return the root node of the structure or <code>null</code> in case of error
	 */
	IStructureComparator getStructure(Object input);

	/**
	 * Creates the single node specified by path from the given input object.
	 * In case of an error (e.g. a parsing error) the value <code>null</code> is returned.
	 * This method is similar to <code>getStructure</code> but in
	 * contrast to <code>getStructure</code> only a single node without any children must be returned.
	 * This method is used in the <code>ReplaceWithEditionDialog</code> to locate a sub element
	 * (e.g. a method) within an input object (e.g. a file containing source code).
	 * <p>
	 * One (not optimized) approach to implement this method is calling <code>getStructure(input)</code>
	 * to build the full tree, and then finding that node within the tree that is specified
	 * by <code>path</code>.
	 * <p>
	 * The syntax of <code>path</code> is not specified, because it is treated by the compare subsystem
	 * as an opaque entity and is not further interpreted. Clients using this functionality
	 * will pass a value of <code>path</code> to the <code>selectEdition</code>
	 * method of <code>ReplaceWithEditionDialog</code> and will receive this value unchanged
	 * as an argument to <code>locate</code>.
	 *
	 * @param path specifies a sub object within the input object
	 * @param input the object from which to create the <code>IStructureComparator</code>
	 * @return the single node specified by <code>path</code> or <code>null</code>
	 *
	 */
	IStructureComparator locate(Object path, Object input);

	/**
	 * Returns the contents of the given node as a string for the purpose
	 * of performing a content comparison only (that is the string will not be visible in the UI).
	 * If <code>ignoreWhitespace</code> is <code>true</code> all character sequences considered
	 * whitespace should be removed from the returned string.
	 *
	 * @param node the node for which to return a string representation
	 * @param ignoreWhitespace if <code>true</code> the returned string should not contain whitespace
	 * @return the string contents of the given node
	 */
	String getContents(Object node, boolean ignoreWhitespace);

	/**
	 * Called whenever a copy operation has been performed on a tree node.
	 *
	 * @param node the node for which to save the new content
	 * @param input the object from which the structure tree was created in <code>getStructure</code>
	 */
	void save(IStructureComparator node, Object input);
}

