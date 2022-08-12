/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.ui.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelResource;
import org.eclipse.team.examples.model.ui.ModelNavigatorLabelProvider;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

/**
 * The label provider that is used for synchronizations.
 * It provides a diff for each model element and also provides
 * overlay hints for isBusy, conflict propagation and markers.
 */
public class ModelSyncLabelProvider extends SynchronizationLabelProvider {

	private ModelNavigatorLabelProvider delegate;

	public ModelSyncLabelProvider() {
		super();
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new ModelNavigatorLabelProvider();
		delegate.init(site);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	@Override
	protected ILabelProvider getDelegateLabelProvider() {
		return delegate;
	}

	@Override
	protected IDiff getDiff(Object element) {
		if (element instanceof ModelResource) {
			ModelResource mr = (ModelResource) element;
			return getContext().getDiffTree().getDiff(mr.getResource());
		}
		return super.getDiff(element);
	}

	@Override
	protected boolean isIncludeOverlays() {
		return true;
	}

	@Override
	protected boolean isBusy(Object element) {
		if (element instanceof ModelResource) {
			ModelResource mr = (ModelResource) element;
			boolean busy = getContext().getDiffTree().getProperty(mr.getResource().getFullPath(), IDiffTree.P_BUSY_HINT);
			if (!busy && mr instanceof ModelObjectDefinitionFile) {
				ModelObjectDefinitionFile modFile = (ModelObjectDefinitionFile) mr;
				try {
					ModelObjectElementFile[] children = modFile.getModelObjectElementFiles();
					for (ModelObjectElementFile file : children) {
						busy = getContext().getDiffTree().getProperty(file.getResource().getFullPath(), IDiffTree.P_BUSY_HINT);
						if (busy)
							break;
					}
				} catch (CoreException e) {
					FileSystemPlugin.log(e);
				}
			}
			return busy;
		}
		return super.isBusy(element);
	}

	@Override
	protected boolean hasDecendantConflicts(Object element) {
		if (element instanceof ModelResource) {
			ModelResource mr = (ModelResource) element;
			boolean conflict = getContext().getDiffTree().getProperty(mr.getResource().getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS);
			if (!conflict && mr instanceof ModelObjectDefinitionFile) {
				ModelObjectDefinitionFile modFile = (ModelObjectDefinitionFile) mr;
				try {
					ModelObjectElementFile[] children = modFile.getModelObjectElementFiles();
					for (ModelObjectElementFile file : children) {
						conflict = getContext().getDiffTree().getProperty(file.getResource().getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS);
						if (conflict)
							break;
					}
				} catch (CoreException e) {
					FileSystemPlugin.log(e);
				}
			}
			return conflict;
		}
		return super.hasDecendantConflicts(element);
	}

}
