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

import java.util.Collections;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

public class AntProjectNodeProxy extends AntProjectNode {

	private final String fBuildFileName;
	private String fDefaultTargetName;
	private boolean fParsed = false;

	/**
	 * Creates a new project node with the given name and the given build file name.
	 * 
	 * @param name
	 *            the project's name or <code>null</code> if the project's name is not known. If this value is <code>null</code>, the file will be
	 *            parsed the first time a value is requested that requires it.
	 */
	public AntProjectNodeProxy(String name, String buildFileName) {
		super(null, null);
		fName = name;
		fBuildFileName = buildFileName;
	}

	/**
	 * Creates a new project node on the given build file.
	 */
	public AntProjectNodeProxy(String buildFileName) {
		this(null, buildFileName);
	}

	public void parseBuildFile(boolean force) {
		if (fParsed && !force) {
			return;
		}
		fChildNodes = null;
		fParsed = true;
		AntTargetNode[] nodes = null;
		IPath buildFilePath = AntUtil.getFile(getBuildFileName()).getLocation();
		if (buildFilePath == null) {
			setProblemSeverity(AntModelProblem.SEVERITY_ERROR);
			setProblemMessage(AntModelMessages.AntProjectNodeProxy_0);
			return;
		}
		nodes = AntUtil.getTargets(buildFilePath.toString());

		if (nodes == null || nodes.length < 1) {
			setProblemSeverity(AntModelProblem.SEVERITY_ERROR);
			setProblemMessage(AntModelMessages.AntProjectNodeProxy_1);
			return;
		}

		AntProjectNode projectNode = nodes[0].getProjectNode();
		if (nodes[0].getTargetName().length() != 0) {
			// not just the implicit target
			for (AntTargetNode node : nodes) {
				addChildNode(node);
			}
		}

		fModel = projectNode.getAntModel();
		fProject = (AntModelProject) projectNode.getProject();
		fLabel = null;
		fName = null;
	}

	public void parseBuildFile() {
		parseBuildFile(false);
	}

	@Override
	public String getDescription() {
		if (fProject == null) {
			parseBuildFile();
		}
		return super.getDescription();
	}

	@Override
	public String getLabel() {
		if (fProject == null) {
			parseBuildFile();
		}
		fName = super.getLabel();
		return fName;
	}

	@Override
	public List<IAntElement> getChildNodes() {
		if (fProject == null) {
			parseBuildFile();
		}
		List<IAntElement> children = super.getChildNodes();
		if (children == null) {
			return Collections.EMPTY_LIST;
		}
		return children;
	}

	@Override
	public String getBuildFileName() {
		return fBuildFileName;
	}

	public void setDefaultTargetName(String defaultTarget) {
		fDefaultTargetName = defaultTarget;
	}

	@Override
	public String getDefaultTargetName() {
		if (fProject == null) {
			return fDefaultTargetName;
		}
		return super.getDefaultTargetName();
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public void dispose() {
		if (fProject != null) {
			super.dispose();
		}
	}

	@Override
	public boolean isErrorNode() {
		if (fProject == null) {
			return super.isErrorNode();
		}
		return getRealNode().isErrorNode();
	}

	@Override
	public String getProblemMessage() {
		if (isErrorNode()) {
			return getBuildFileName();
		}
		return null;
	}

	@Override
	public boolean isWarningNode() {
		if (fProject == null) {
			return super.isWarningNode();
		}
		return getRealNode().isWarningNode();
	}

	private AntProjectNode getRealNode() {
		if (fModel != null) {
			return fModel.getProjectNode();
		}
		return null;
	}

	@Override
	protected IAntModel getAntModel() {
		if (fProject == null) {
			parseBuildFile();
		}
		return super.getAntModel();
	}

	@Override
	public int getLength() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode = getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getLength();
	}

	@Override
	public int getOffset() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode = getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getOffset();
	}

	@Override
	public int getSelectionLength() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode = getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getSelectionLength();
	}

	@Override
	public IFile getBuildFileResource() {
		if (fProject == null) {
			if (fBuildFileName != null) {
				return AntUtil.getFile(fBuildFileName);
			}
		}
		return super.getBuildFileResource();
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
