/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

import java.io.File;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.xml.sax.Attributes;

public interface IAntModel {

	/**
	 * Returns the project node for this Ant model
	 * 
	 * @return the project node for this Ant model
	 */
	AntProjectNode getProjectNode();

	/**
	 * Returns the name of the external entity defined by the specified path
	 * 
	 * @param path
	 *            the path to the entity
	 * @return the name or the entity or <code>null</code> if no entity could be resolved
	 */
	String getEntityName(String path);

	/**
	 * Returns the location provider for this Ant model
	 * 
	 * @return the location provider for this Ant model
	 */
	LocationProvider getLocationProvider();

	/**
	 * Disposes this Ant model
	 */
	void dispose();

	/**
	 * Reconciles this Ant model with the buildfile it represents
	 */
	void reconcile();

	/**
	 * Sets the classloader for this Ant model
	 * 
	 * @param newClassLoader
	 *            the classloader to use when resolving this Ant model
	 */
	void setClassLoader(URLClassLoader newClassLoader);

	/**
	 * Sets the extra properties to be used for parsing If global settings as defined in the Ant UI preferences are to be used this method does not
	 * need to be called.
	 * 
	 * @param properties
	 *            the properties to ensure are set for parsing.
	 */
	void setProperties(Map<String, String> properties);

	/**
	 * Sets the property files to be used for parsing If global settings as defined in the Ant UI preferences are to be used this method does not need
	 * to be called.
	 * 
	 * @param propertyFiles
	 *            the file names of the property files to use for parsing
	 */
	void setPropertyFiles(String[] propertyFiles);

	/**
	 * Returns the Eclipse resource for the buildfile this Ant model represents
	 * 
	 * @return the Eclipse resource or <code>null</code> if the buildfile is not in the workspace
	 */
	IFile getFile();

	/**
	 * Returns the encoding from the backing {@link IAntModel}. If the model is <code>null</code> or the encoding cannot be computed from the location
	 * backing the model, <code>UTF-8</code> is returned
	 * 
	 * @return the encoding
	 * @since 3.7
	 */
	String getEncoding();

	/**
	 * Handles a <code>BuildException</code> that occurred during parsing.
	 * 
	 * @param be
	 *            the build exception that occurred
	 * @param node
	 *            the node associated with the problem
	 * @param severity
	 *            the severity of the problem
	 */
	void handleBuildException(BuildException be, AntElementNode node, int severity);

	/**
	 * Returns the project node for this Ant model
	 * 
	 * @param reconcile
	 *            whether or not to ensure the Ant model is reconciled before retrieving the project node
	 * @return the project node for this Ant model
	 */
	AntProjectNode getProjectNode(boolean reconcile);

	/**
	 * Adds the new target to this Ant model
	 * 
	 * @param newTarget
	 *            the new Apache Ant target
	 * @param lineNumber
	 *            the line number of the new target
	 * @param columnNumber
	 *            the column number of the new target
	 */
	void addTarget(Target newTarget, int lineNumber, int columnNumber);

	/**
	 * Adds the new project to this Ant model
	 * 
	 * @param project
	 *            the new Apache Ant project
	 * @param lineNumber
	 *            the line number of the new target
	 * @param columnNumber
	 *            the column number of the new target
	 */
	void addProject(Project project, int lineNumber, int columnNumber);

	/**
	 * Return the <code>java.io.File</code> that is the buildfile that this Ant model represents
	 * 
	 * @return the <code>java.io.File</code> that is the buildfile
	 */
	// TODO Could this just be getPath().toLocation();
	File getEditedFile();

	/**
	 * Returns whether this model contains task information
	 * 
	 * @return whether task information is included in this model
	 */
	boolean canGetTaskInfo();

	/**
	 * Returns whether this model contains lexical information
	 * 
	 * @return whether lexical information is included in this model
	 */
	boolean canGetLexicalInfo();

	/**
	 * Returns whether this model contains position information for the elements
	 * 
	 * @return whether position information is included in this model
	 */
	boolean canGetPositionInfo();

	/**
	 * Adds a comment to the Ant model Only called if <code>canGetLexicalInfo()</code> is <code>true</code>
	 * 
	 * @param lineNumber
	 *            the line number of the comment
	 * @param columnNumber
	 *            the column number of the comment
	 * @param length
	 *            the length of the comment
	 */
	void addComment(int lineNumber, int columnNumber, int length);

	/**
	 * Adds a DTD element to the Ant model Only called if <code>canGetLexicalInfo()</code> is <code>true</code>
	 * 
	 * @param name
	 *            the name of the DTD element
	 * @param lineNumber
	 *            the line number of the comment
	 * @param columnNumber
	 *            the column number of the comment
	 */
	void addDTD(String name, int lineNumber, int columnNumber);

	/**
	 * Adds the external entity to the Ant model
	 * 
	 * @param name
	 *            the name of the external entity
	 * @param currentEntityPath
	 *            the path of the entity
	 */
	void addEntity(String name, String currentEntityPath);

	/**
	 * Adds a task element to the Ant model Only called if <code>canGetTaskInfo()</code>() is <code>true</code>
	 * 
	 * @param newTask
	 *            the new Apache Ant task
	 * @param parentTask
	 *            the parent Apache Ant task or <code>null</code>
	 * @param attributes
	 *            the attributes of the new task
	 * @param lineNumber
	 *            the line number of the task
	 * @param columnNumber
	 *            the column number of the task
	 */
	void addTask(Task newTask, Task parentTask, Attributes attributes, int lineNumber, int columnNumber);

	/**
	 * Sets the length of the current element that was just finished being parsed
	 * 
	 * @param lineNumber
	 *            the current line number of parsing
	 * @param columnNumber
	 *            the current column number of parsing
	 */
	void setCurrentElementLength(int lineNumber, int columnNumber);

	/**
	 * Returns the offset in the document associated with this Ant model for the given line number and position in the line
	 * 
	 * @param lineNumber
	 *            the line number in the doc
	 * @param column
	 *            the column number in that line
	 * @return the offset in the document
	 */
	int getOffset(int lineNumber, int column) throws BadLocationException;

	/**
	 * Handles a fatal error from an exception during parsing.
	 * 
	 * @param e
	 *            the exception that occurred
	 */
	void error(Exception e);

	/**
	 * Handles a fatal error from an exception during parsing.
	 * 
	 * @param e
	 *            the exception that occurred
	 */
	void fatalError(Exception e);

	/**
	 * Handles a warning from an exception during parsing.
	 * 
	 * @param e
	 *            the exception that occurred
	 */
	void warning(Exception e);

	void errorFromElement(Exception e, AntElementNode element, int lineNumber, int columnNumber);

	void errorFromElementText(Exception e, int offset, int columnNumber);

	/**
	 * Returns the text in the document of this Ant model for the given offset and length
	 * 
	 * @param offset
	 *            the offset within the document
	 * @param length
	 *            the length of text to retrieve
	 * @return the text at the given offset of <code>null</code> if not contained within the document range
	 */
	String getText(int offset, int length);

	/**
	 * Caches the text from the provided defining node so that the node definitions are only updated if the text changes on reconciliation
	 * 
	 * @param node
	 *            the defining task node to cache the associated text
	 */
	void setDefiningTaskNodeText(AntDefiningTaskNode node);

	/**
	 * Record the prefix-URI Namespace mapping.
	 * 
	 * @param prefix
	 *            The Namespace prefix being declared.
	 * @param uri
	 *            The Namespace URI the prefix is mapped to.
	 */
	void addPrefixMapping(String prefix, String uri);
}
