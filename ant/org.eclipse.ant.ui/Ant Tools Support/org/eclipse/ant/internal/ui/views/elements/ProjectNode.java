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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.eclipse.ant.core.ProjectInfo;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Representation of an ant build project.
 */
public class ProjectNode extends AntNode {

	private List targets= null;
	private TargetNode defaultTarget= null;
	private String buildFileName;
	private boolean isErrorNode= false;
	
	private String defaultTargetName;
	/**
	 * Creates a new project node with the given name and the given build file
	 * name.
	 * 
	 * @param name the project's name or <code>null</code> if the project's
	 * name is not known. If this value is <code>null</code>, the file will be
	 * parsed the first time a value is requested that requires it.
	 * @param buildFileName
	 */
	public ProjectNode(String name, String buildFileName) {
		super(name);
		this.buildFileName= buildFileName;
	}
	
	/**
	 * Creates a new project node on the given build file.
	 */
	public ProjectNode(String buildFileName) {
		this(null, buildFileName);
	}

	/**
	 * Returns the targets in this project
	 * 
	 * @return TargetNode[] the targets in this project
	 */
	public TargetNode[] getTargets() {
		if (targets == null) {
			// Lazily parse the file to populate the targets
			parseBuildFile();
		}
		return (TargetNode[])targets.toArray(new TargetNode[targets.size()]);
	}
	
	/**
	 * Adds the given target to this project
	 * 
	 * @param target the target to add
	 */
	private void addTarget(TargetNode target) {
		targets.add(target);
		target.setParent(this);
	}
	
	/**
	 * Sets this project's default target to the given target
	 * 
	 * @param target this project's default target
	 */
	public void setDefaultTarget(TargetNode target) {
		defaultTarget= target;
		if (target != null) {
			defaultTargetName= target.getName();
		}
	}
	
	/**
	 * Returns the name of the build file containing this project
	 * 
	 * @return String the name of this project's build file
	 */
	public String getBuildFileName() {
		return buildFileName;
	}
	
	/**
	 * Returns the default target in this project or <code>null</code> if none
	 * has been set
	 * 
	 * @return TargetNode the default target or <code>null</code> if none has
	 * been set
	 */
	public TargetNode getDefaultTarget() {
		if (targets == null) {
			// Lazily parse the file to populate the targets
			parseBuildFile();
		}
		return defaultTarget;
	}
	
	/**
	 * Returns the default target name. If this project has not been parsed yet, this
	 * method will return the default target name specified via setDefaultTargetName(String)
	 * or <code>null</code> if no default target name has been specified.
	 * 
	 * This method is intended to be used by clients who want to access the name of this
	 * project's default target without forcing the build file to be parsed.
	 *   
	 * @return String the name of the default target in this project.
	 */
	public String getDefaultTargetName() {
		return defaultTargetName;
	}
	
	/**
	 * Sets the name of this project node's default target.
	 * 
	 * @param name the name of this project node's default target
	 * @see ProjectNode#getDefaultTargetName()
	 */
	public void setDefaultTargetName(String name) {
		defaultTargetName= name;
	}
	
	/**
	 * Sets this project's error node state
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
	
	/**
	 * Parses the given build file and populates the targets contained in the
	 * build file. If an error occurs while parsing the file, the error
	 * state will be set and a target error node will be added
	 */
	public void parseBuildFile() {
		clear();
		TargetInfo[] infos = null;
		IPath buildFilePath= AntUtil.getFile(getBuildFileName()).getLocation();
		if (buildFilePath == null) {
			setErrorMessage(AntViewElementsMessages.getString("ProjectNode.Build_file_not_found_1")); //$NON-NLS-1$
			return;
		}
		try {
			infos = AntUtil.getTargets(buildFilePath.toString());
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return;
		}
		if (infos.length < 1) {
			setErrorMessage(AntViewElementsMessages.getString("ProjectNode.No_targets")); //$NON-NLS-1$
			return;
		}
		ProjectInfo projectInfo= infos[0].getProject();
		// Create Apache Ant objects
		Project project = new Project();
		if (projectInfo.getName() != null) {
			project.setName(projectInfo.getName());
		}
		
		for (int i = 0; i < infos.length; i++) {
			TargetInfo info = infos[i];
			if (info.isDefault()) {
				project.setDefault(info.getName());
			}
			Target target = new Target();
			target.setName(info.getName());
			String[] dependencies = info.getDependencies();
			StringBuffer depends = new StringBuffer();
			int numDependencies = dependencies.length;
			if (numDependencies > 0) {
				// Onroll the loop to avoid trailing comma
				depends.append(dependencies[0]);
			}
			for (int j = 1; j < numDependencies; j++) {
				depends.append(',').append(dependencies[j]);
			}
			target.setDepends(depends.toString());
			target.setDescription(info.getDescription());
			project.addTarget(target);
		}
		if (project.getDefaultTarget() == null) {
			setErrorMessage(AntViewElementsMessages.getString("ProjectNode.No_default")); //$NON-NLS-1$
			return;
		}
		// Set the project node data based on the Apache Ant data
		String projectName = project.getName();
		if (projectName == null) {
			projectName = AntViewElementsMessages.getString("ProjectNode.<name_unspecified>_1"); //$NON-NLS-1$
		}
		// Update the project name
		setName(projectName);
		setDescription(projectInfo.getDescription());
		Enumeration projTargets = project.getTargets().elements();
		while (projTargets.hasMoreElements()) {
			Target target = (Target) projTargets.nextElement();
			// Target Node -----------------
			Enumeration targetDependencies = target.getDependencies();
			TargetNode targetNode = new TargetNode(target.getName(), target.getDescription());
			while (targetDependencies.hasMoreElements()) {
				targetNode.addDependency((String) targetDependencies.nextElement());
			}
			addTarget(targetNode);
			if (target.getName().equals(project.getDefaultTarget())) {
				setDefaultTarget(targetNode);
			}
		}
		Collections.sort(targets, new Comparator() {
			/**
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			public int compare(Object o1, Object o2) {
				String name1=""; //$NON-NLS-1$
				String name2= ""; //$NON-NLS-1$
				if (o1 instanceof TargetNode) {
					name1= ((TargetNode)o1).getName();
				}
				if (o2 instanceof TargetNode) {
					name2= ((TargetNode)o2).getName();
				}
				return name1.compareToIgnoreCase(name2);
			}
		});
	}
	
	/**
	 * Clear's this node's internally stored data
	 */
	private void clear() {
		targets= new ArrayList();
		setIsErrorNode(false);
		setDefaultTarget(null);
		setDefaultTargetName(null);
	}
	
	/**
	 * Sets the error message of this project and creates a new target child
	 * node with the message
	 *
	 * @param errorMessage the error message generated while parsing this
	 * project
	 */
	private void setErrorMessage(String errorMessage) {
		setName(getBuildFileName());
		setIsErrorNode(true);
		TargetNode target= new TargetNode(errorMessage, errorMessage);
		target.setIsErrorNode(true);
		addTarget(target);
	}

	/**
	 * Returns the name of this project, parsing the build file first if
	 * necessary.
	 * 
	 * @return String this project's name
	 */
	public String getName() {
		if (super.getName() == null) {
			parseBuildFile();
		}
		String name= super.getName();
		if (name == null || name.length() == 0) {
			name= AntViewElementsMessages.getString("ProjectNode.<name_unspecified>_1"); //$NON-NLS-1$
		}
		return name;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
}
