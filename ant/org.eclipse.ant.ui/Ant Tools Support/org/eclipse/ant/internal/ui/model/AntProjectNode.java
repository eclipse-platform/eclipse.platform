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

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;

public class AntProjectNode extends AntElementNode {

	protected AntModelProject fProject;
	protected IAntModel fModel;
	protected String fLabel;

	public AntProjectNode(AntModelProject project, IAntModel antModel) {
		super("project"); //$NON-NLS-1$
		fProject = project;
		fModel = antModel;
	}

	@Override
	public String getLabel() {
		if (fLabel == null) {
			if (fProject != null) {
				fLabel = fProject.getName();
			} else {
				fLabel = AntModelMessages.AntProjectNode_0;
			}
			if (fLabel == null || fLabel.length() == 0) {
				fLabel = "project"; //$NON-NLS-1$
			}
		}
		return fLabel;
	}

	@Override
	protected ImageDescriptor getBaseImageDescriptor() {
		return AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_PROJECT);
	}

	/**
	 * Returns the Ant project associated with this project node.
	 *
	 * @return the Ant project
	 */
	public Project getProject() {
		return fProject;
	}

	@Override
	protected IAntModel getAntModel() {
		return fModel;
	}

	@Override
	public void reset() {
		super.reset();
		fProject.reset();
		setProblemSeverity(AntModelProblem.NO_PROBLEM);
		setProblemMessage(null);
		fOffset = -1;
		fLength = -1;
	}

	public String getDescription() {
		if (fProject == null) {
			return AntModelMessages.AntProjectNode_0;
		}
		return fProject.getDescription();
	}

	public String getBuildFileName() {
		LocationProvider locationProvider = getAntModel().getLocationProvider();
		if (locationProvider != null) {
			IFile file = locationProvider.getFile();
			if (file != null) {
				return file.getFullPath().toOSString();
			}
		}
		return null;
	}

	public String getDefaultTargetName() {
		return fProject.getDefaultTarget();
	}

	/**
	 * @param node
	 *            the property node that is currently being configured
	 */
	public void setCurrentConfiguringProperty(AntPropertyNode node) {
		AntModelProject project = (AntModelProject) getProject();
		project.setCurrentConfiguringProperty(node);
	}

	@Override
	public AntProjectNode getProjectNode() {
		return this;
	}

	@Override
	public boolean containsOccurrence(String identifier) {
		return identifier.equals(getDefaultTargetName());
	}

	@Override
	public List<Integer> computeIdentifierOffsets(String identifier) {
		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null || textToSearch.length() == 0 || identifier.length() == 0) {
			return null;
		}
		List<Integer> results = new ArrayList<>(1);
		String newidentifier = new StringBuffer("\"").append(identifier).append('"').toString(); //$NON-NLS-1$
		int defaultTargetNameOffset = textToSearch.indexOf(IAntCoreConstants.DEFAULT);
		defaultTargetNameOffset = textToSearch.indexOf(newidentifier, defaultTargetNameOffset);
		results.add(Integer.valueOf(getOffset() + defaultTargetNameOffset + 1));
		return results;
	}

	@Override
	public boolean isRegionPotentialReference(IRegion region) {
		if (!super.isRegionPotentialReference(region)) {
			return false;
		}

		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null) {
			return false;
		}

		return checkReferenceRegion(region, textToSearch, "default"); //$NON-NLS-1$
	}
}