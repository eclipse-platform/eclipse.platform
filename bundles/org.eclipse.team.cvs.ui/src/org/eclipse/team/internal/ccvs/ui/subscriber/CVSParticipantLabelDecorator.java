/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetModelProvider;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelCompareParticipant;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.team.ui.synchronize.*;


public class CVSParticipantLabelDecorator extends LabelProvider implements IPropertyChangeListener, ILabelDecorator {
	private ISynchronizePageConfiguration configuration;
	private SynchronizationStateTester tester = new SynchronizationStateTester() {
		public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
			// Disable state decoration
			return IDiff.NO_CHANGE;
		}
	};
	
	public CVSParticipantLabelDecorator(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		// Listen for decorator changed to refresh the viewer's labels.
		CVSUIPlugin.addPropertyChangeListener(this);
		TeamUI.addPropertyChangeListener(this);
	}
	
	public String decorateText(String input, Object elementOrPath) {
		if (!isEnabledFor(elementOrPath))
			return input;
		try {
			String text = input;
			IResource resource = getResource(elementOrPath);
			if (resource != null && resource.getType() != IResource.ROOT) {
				// Prepare the decoration but substitute revision and hide dirty indicator
				CVSDecoration decoration = getDecoration(resource);
				decoration.setRevision(getRevisionNumber(elementOrPath));
				decoration.compute();
				// Update label
				StringBuilder output = new StringBuilder(25);
				if (decoration.getPrefix() != null) {
					output.append(decoration.getPrefix());
				}
				output.append(text);
				if (decoration.getSuffix() != null) {
					output.append(decoration.getSuffix());
				}
				return output.toString();
			}
			return text;
		} catch (CoreException e) {
			return input;
		}
	}
	
	protected IResource getResource(Object element) {
		if (element instanceof ISynchronizeModelElement)
			return ((ISynchronizeModelElement) element).getResource();
		return Utils.getResource(internalGetElement(element));
	}

	protected CVSDecoration getDecoration(IResource resource) throws CoreException {
		return CVSLightweightDecorator.decorate(resource, tester);
	}

	public Image decorateImage(Image base, Object element) {
		return base;
	}
	public void propertyChange(PropertyChangeEvent event) {
		if (needsRefresh(event)) {
			Viewer viewer = configuration.getPage().getViewer();
			if(viewer instanceof StructuredViewer && !viewer.getControl().isDisposed()) {
				((StructuredViewer)viewer).refresh(true);
			}
		}
	}
	
	protected boolean needsRefresh(PropertyChangeEvent event) {
		final String property= event.getProperty();
		return property.equals(CVSUIPlugin.P_DECORATORS_CHANGED) || property.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED);
	}
	public void dispose() {
		CVSUIPlugin.removePropertyChangeListener(this);
		TeamUI.removePropertyChangeListener(this);
	}
	
	protected String getRevisionNumber(Object elementOrPath) {
		IResource resource = getResource(elementOrPath);
		if (resource != null && resource.getType() == IResource.FILE) {
			IResourceVariant local;
			try {
				local = (IResourceVariant) CVSWorkspaceRoot.getRemoteResourceFor(resource);
			} catch (CVSException e) {
				local = null;
			}
			if(local == null) {
				local = getBase(elementOrPath);
			}
			String localRevision = getRevisionString(local);
			StringBuilder revisionString = new StringBuilder();
			IResourceVariant remote = getRemote(elementOrPath);
			String remoteRevision = getRevisionString(remote);
			if(localRevision != null) {
				revisionString.append(localRevision);
			}
			if(remoteRevision != null) {
				revisionString.append( (localRevision != null ? " - " : "") + remoteRevision); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return revisionString.toString();
		}
		return null;
	}

	private boolean isEnabledFor(Object elementOrPath) {
		return !isCompareWithChangeSets() || elementOrPath instanceof TreePath;
	}

	private boolean isCompareWithChangeSets() {
		String id = (String)configuration.getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
		return configuration.getParticipant() instanceof ModelCompareParticipant && id.equals(ChangeSetModelProvider.ID);
	}

	private SyncInfo getSyncInfo(Object element) {
		if (element instanceof SyncInfoModelElement) {
			SyncInfoModelElement sime = (SyncInfoModelElement) element;
			return sime.getSyncInfo();
		}
		IResource resource = getResource(element);
		if (resource != null) {
			ISynchronizeParticipant participant = configuration.getParticipant();
			if (participant instanceof ModelSynchronizeParticipant) {
				ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
				ISynchronizationContext context = msp.getContext();
				if (context instanceof SubscriberMergeContext) {
					SubscriberMergeContext smc = (SubscriberMergeContext) context;
					Subscriber subscriber = smc.getSubscriber();
					try {
						return subscriber.getSyncInfo(resource);
					} catch (TeamException e) {
						CVSUIPlugin.log(e);
					}
				}
			}
		}
		return null;
	}
	
	private IResourceVariant getBase(Object element) {
		if (element instanceof TreePath) {
			TreePath tp = (TreePath) element;
			IDiff diff = getDiff(tp);
			IFileRevision revision = Utils.getBase(diff);
			return SyncInfoToDiffConverter.asResourceVariant(revision);
		}
		SyncInfo info = getSyncInfo(element);
		if (info != null)
			return info.getBase();
		return null;
	}
	
	private IResourceVariant getRemote(Object element) {
		if (element instanceof TreePath) {
			TreePath tp = (TreePath) element;
			IDiff diff = getDiff(tp);
			IFileRevision revision = Utils.getRemote(diff);
			return SyncInfoToDiffConverter.asResourceVariant(revision);
		}
		SyncInfo info = getSyncInfo(element);
		if (info != null)
			return info.getRemote();
		return null;
	}

	private IDiff getDiff(TreePath tp) {
		Object first = tp.getFirstSegment();
		Object last = tp.getLastSegment();
		IResource resource = Utils.getResource(last);
		if (first instanceof DiffChangeSet && resource != null) {
			DiffChangeSet dcs = (DiffChangeSet) first;
			return dcs.getDiffTree().getDiff(resource);
		}
		return null;
	}

	private String getRevisionString(IResourceVariant remoteFile) {
		if(remoteFile instanceof RemoteFile) {
			return ((RemoteFile)remoteFile).getRevision();
		}
		return null;
	}
	
	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}
}
