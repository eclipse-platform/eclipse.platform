/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Broadcom Corporation - build configurations
 *******************************************************************************/
package org.eclipse.core.internal.events;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IProject;

public class BuilderPersistentInfo {
	protected String builderName;
	/**
	 * Index of this builder in the build spec. A value of -1 indicates
	 * that this index is unknown (it was not serialized in older workspace versions).
	 */
	private int buildSpecIndex = -1;
	protected IProject[] interestingProjects = ICoreConstants.EMPTY_PROJECT_ARRAY;
	protected ElementTree lastBuildTree;
	protected String projectName;
	protected String configName;

	public BuilderPersistentInfo(String projectName, String builderName, int buildSpecIndex) {
		this(projectName, null, builderName, buildSpecIndex);
	}

	public BuilderPersistentInfo(String projectName, String configName, String builderName, int buildSpecIndex) {
		this.projectName = projectName;
		this.configName = configName;
		this.builderName = builderName;
		this.buildSpecIndex = buildSpecIndex;
	}

	public String getBuilderName() {
		return builderName;
	}

	public int getBuildSpecIndex() {
		return buildSpecIndex;
	}

	/**
	 * @return the name of the configuration for which this information refers.
	 * Will return null if the build command doesn't support configurations, or the
	 * build persistent info has been loaded from a workspace without configurations.
	 */
	public String getConfigName() {
		return configName;
	}

	public IProject[] getInterestingProjects() {
		return interestingProjects;
	}

	public ElementTree getLastBuiltTree() {
		return lastBuildTree;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public void setInterestingProjects(IProject[] projects) {
		interestingProjects = projects;
	}

	public void setLastBuildTree(ElementTree tree) {
		lastBuildTree = tree;
	}
}
