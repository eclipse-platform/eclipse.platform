/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.views.elements;

import java.util.ArrayList;
import java.util.List;


/**
 * Representation of an ant target
 */
public class TargetNode extends AntNode {
	private List dependencies= new ArrayList();
	private boolean isErrorNode= false;
	
	/**
	 * Creates a new target node with the given name, and description
	 * 
	 * @param name the new node's name
	 * @param description the target description or <code>null</code> if the
	 * target has no description
	 */
	public TargetNode(String name, String description) {
		super(name);
		setDescription(description);
	}
	
	/**
	 * Adds the given dependency to the list of this target's dependencies
	 * 
	 * @param dependency the dependency to add
	 */
	public void addDependency(String dependency) {
		dependencies.add(dependency);
	}

	/**
	 * Returns the dependency node containing the names of the targets on which
	 * this target depends
	 * 
	 * @return DependencyNode the node containing the names of this target's
	 * dependencies
	 */
	public String[] getDependencies() {
		return (String[]) dependencies.toArray(new String[dependencies.size()]);
	}
	
	/**
	 * Returns the ProjectNode containing this target. This method is equivalent
	 * to calling getParent() and casting the result to a ProjectNode.
	 * 
	 * @return ProjectNode the project containing this target
	 */
	public ProjectNode getProject() {
		return (ProjectNode) getParent();
	}
	
	/**
	 * Sets this target's error node state
	 *
	 * @param isErrorNode whether or not an error occurred while parsing this node
	 */
	public void setIsErrorNode(boolean isErrorNode) {
		this.isErrorNode= isErrorNode;
	}

	/**
	 * Returns whether an error occurred while parsing this Ant node
	 *
	 * @return whether an error occurred while parsing this Ant node
	 */
	public boolean isErrorNode() {
		return isErrorNode;
	}
}
