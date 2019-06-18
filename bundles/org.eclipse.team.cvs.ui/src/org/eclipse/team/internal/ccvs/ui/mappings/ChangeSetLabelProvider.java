/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetModelProvider;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantLabelDecorator;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

public class ChangeSetLabelProvider extends ResourceModelLabelProvider {

	private CVSParticipantLabelDecorator decorator;

	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (isCompare(configuration)) {
			decorator = new CVSParticipantLabelDecorator(configuration);
		}
	}

	private ISynchronizePageConfiguration getConfiguration() {
		return (ISynchronizePageConfiguration)getExtensionSite().getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
	}
	
	private boolean isCompare(ISynchronizePageConfiguration configuration) {
		return configuration.getParticipant() instanceof ModelCompareParticipant;
	}
	
	public String getText(Object element) {
		String text = super.getText(element);
		if (decorator != null && isChangeSetsEnabled())
			text = decorator.decorateText(text, element);
		return text;
	}
	
	private boolean isChangeSetsEnabled() {
		String id = (String)getConfiguration().getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
		return id.equals(ChangeSetModelProvider.ID);
	}

	protected String getDelegateText(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet set = (DiffChangeSet) element;
			return set.getName();
		}
		return super.getDelegateText(elementOrPath);
	}
	
	protected Image getDelegateImage(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			return getChangeSetImage();
		}
		return super.getDelegateImage(elementOrPath);
	}

	private Image getChangeSetImage() {
		return getImageManager().getImage(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_CHANGELOG));
	}
	
	public void dispose() {
		if (decorator != null)
			decorator.dispose();
		super.dispose();
	}
	
	protected boolean isBusy(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			IResource[] resources = dcs.getResources();
			for (IResource resource : resources) {
				if (getContext().getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_BUSY_HINT))
					return true;
			}
			return false;
		}
		return super.isBusy(elementOrPath);
	}
	
	protected boolean hasDecendantConflicts(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			IResource[] resources = dcs.getResources();
			for (IResource resource : resources) {
				if (getContext().getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS))
					return true;
			}
			return false;
		}
		if (elementOrPath instanceof TreePath && element instanceof IResource) {
			DiffChangeSet set = internalGetChangeSet(elementOrPath);
			if (set != null) {
				ResourceTraversal[] traversals = getTraversalCalculator().getTraversals(set, (TreePath)elementOrPath);
				return (getContext().getDiffTree().hasMatchingDiffs(traversals, CONFLICT_FILTER));
			}
		}
		return super.hasDecendantConflicts(elementOrPath);
	}
	
	private DiffChangeSet internalGetChangeSet(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			Object o = tp.getFirstSegment();
			if (o instanceof DiffChangeSet) {
				return (DiffChangeSet) o;
			}
		}
		return null;
	}

	protected int getMarkerSeverity(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			Set projects = new HashSet();
			IResource[] resources = dcs.getResources();
			int severity = -1;
			for (IResource resource : resources) {
				IProject project = resource.getProject();
				if (!projects.contains(project)) {
					projects.add(project);
					int next = super.getMarkerSeverity(project);
					if (next == IMarker.SEVERITY_ERROR)
						return IMarker.SEVERITY_ERROR;
					if (next == IMarker.SEVERITY_WARNING)
						severity = next;
				}
			}
			return severity;
		}
		return super.getMarkerSeverity(elementOrPath);
	}
	
	protected void updateLabels(Object[] elements) {
		super.updateLabels(addSetsContainingElements(elements));
	}

	private Object[] addSetsContainingElements(Object[] elements) {
		Set result = new HashSet();
		for (Object object : elements) {
			result.add(object);
			if (object instanceof IProject) {
				IProject project = (IProject) object;
				ChangeSet[] sets = getSetsContaing(project);
				Collections.addAll(result, sets);
			}
		}
		return result.toArray();
	}

	private ChangeSet[] getSetsContaing(IProject project) {
		return getContentProvider().getSetsShowingPropogatedStateFrom(project.getFullPath());
	}

	private ChangeSetContentProvider getContentProvider() {
		return (ChangeSetContentProvider)getExtensionSite().getExtension().getContentProvider();
	}
	
	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}
	
	public Font getFont(Object element) {
		element = internalGetElement(element);
		if (element instanceof ActiveChangeSet && isDefaultActiveSet((ActiveChangeSet)element)) {
			return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}
		return super.getFont(element);
	}

	private boolean isDefaultActiveSet(ActiveChangeSet set) {
		ChangeSetCapability changeSetCapability = getContentProvider().getChangeSetCapability();
		if (changeSetCapability != null) {
			ActiveChangeSetManager activeChangeSetManager = changeSetCapability.getActiveChangeSetManager();
			if (activeChangeSetManager != null)
				return activeChangeSetManager.isDefault(set);
		}
		return false;
	}

}
