/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.INavigatable;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.ui.progress.UIJob;

/**
 * A {@link SyncInfo} editor input used as input to a two-way or three-way
 * compare viewer. It defines methods for accessing the three sides for the
 * compare, and a name and image which is used when displaying the three way input
 * in an editor. This input can alternately be used to show compare results in
 * a dialog by calling {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}.
 * <p>
 * The editor will not update when the elements in the sync info are changed.
 * </p>
 * <p>
 * Supports saving the local resource that is changed in the editor and will be updated
 * when the local resources is changed.
 * </p>
 * @see SyncInfo
 * @since 3.0
 */
public final class SyncInfoCompareInput extends SaveableCompareEditorInput implements IResourceChangeListener {

	private MyDiffNode node;
	private String description;
	private IResource resource;
	private ISynchronizeParticipant participant;
	private ISynchronizePageConfiguration synchronizeConfiguration;

	/*
	 * This class exists so that we can force the text merge viewers to update by
	 * calling #fireChange when we save the compare input to disk. The side
	 * effect is that the compare viewers will be updated to reflect the new changes
	 * that have been made. Compare doesn't do this by default.
	 */
	private static class MyDiffNode extends SyncInfoModelElement {
		public MyDiffNode(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}
		@Override
		public void fireChange() {
			super.fireChange();
		}
	}

	/**
	 * Creates a compare editor input based on an existing <code>SyncInfo</code>.
	 *
	 * @param description a description of the context of this sync info. This
	 * is displayed to the user.
	 * @param sync the <code>SyncInfo</code> used as the base for the compare input.
	 */
	public SyncInfoCompareInput(String description, SyncInfo sync) {
		super(getDefaultCompareConfiguration(), null);
		Assert.isNotNull(sync);
		Assert.isNotNull(description);
		this.description = description;
		this.resource = sync.getLocal();
		this.node = new MyDiffNode(null, sync);
		setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { sync.getLocal().getName() }));
	}

	/**
	 * Creates a compare editor input based on an existing <code>SyncInfo</code>
	 * from the given participant.
	 *
	 * @param participant the participant from which the sync info was obtained. The
	 * name of the participant is used as the description which is displayed to the user.
	 * @param sync the <code>SyncInfo</code> used as the base for the compare input.
	 *
	 * @since 3.1
	 */
	public SyncInfoCompareInput(ISynchronizeParticipant participant, SyncInfo sync) {
		this(participant.getName(), sync);
		this.participant = participant;
	}

	public SyncInfoCompareInput(ISynchronizePageConfiguration configuration,
			SyncInfo info) {
		this(configuration.getParticipant(), info);
		this.synchronizeConfiguration = configuration;
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		if (synchronizeConfiguration != null) {
			ICompareNavigator navigator = (ICompareNavigator)synchronizeConfiguration.getProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR);
			if (navigator != null && navigator == super.getNavigator()) {
				synchronizeConfiguration.setProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR, new CompareNavigator() {
					@Override
					protected INavigatable[] getNavigatables() {
						return new INavigatable[0];
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IFile.class.equals(adapter) && resource.getType() == IResource.FILE) {
			return (T) resource;
		}
		return super.getAdapter(adapter);
	}

	private static CompareConfiguration getDefaultCompareConfiguration() {
		CompareConfiguration cc = new CompareConfiguration();
		//cc.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, true);
		return cc;
	}

	/**
	 * Note that until the compare editor inputs can be part of the compare editors lifecycle we
	 * can't register as a listener because there is no dispose() method to remove the listener.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta resourceDelta = delta.findMember(resource.getFullPath());
			if (resourceDelta != null) {
				UIJob job = new UIJob("") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (!isSaveNeeded()) {
							//updateNode();
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}
	}

	@Override
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// update the title now that the remote revision number as been fetched
		// from the server
		setTitle(getTitle());
		monitor.beginTask(TeamUIMessages.SyncInfoCompareInput_3, 100);
		monitor.setTaskName(TeamUIMessages.SyncInfoCompareInput_3);
		try {
			if (participant != null) {
				participant.prepareCompareInput(node, getCompareConfiguration(), Policy.subMonitorFor(monitor, 100));
			} else {
				Utils.updateLabels(node.getSyncInfo(), getCompareConfiguration(), monitor);
				node.cacheContents(Policy.subMonitorFor(monitor, 100));
			}
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
		return node;
	}

	@Override
	public String getToolTipText() {
		return NLS.bind(TeamUIMessages.SyncInfoCompareInput_tooltip, new String[] { Utils.shortenText(30, description), node.getResource().getFullPath().toString() });
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof SyncInfoCompareInput) {
			SyncInfo otherSyncInfo = ((SyncInfoCompareInput) other).getSyncInfo();
			SyncInfo thisSyncInfo = getSyncInfo();
			// Consider the inputs equal if the sync info are equal and the
			// left nodes are equal (i.e they have the same timestamp)
			return thisSyncInfo.equals(otherSyncInfo)
				&& node.getLeft().equals(((SyncInfoCompareInput) other).node.getLeft());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getSyncInfo().hashCode();
	}

	public SyncInfo getSyncInfo() {
		return node.getSyncInfo();
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	@Override
	public synchronized ICompareNavigator getNavigator() {
		if (synchronizeConfiguration != null && isSelectedInSynchronizeView()) {
			ICompareNavigator nav = (ICompareNavigator)synchronizeConfiguration.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
			synchronizeConfiguration.setProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR, super.getNavigator());
			return nav;
		}
		return super.getNavigator();
	}

	private boolean isSelectedInSynchronizeView() {
		if (synchronizeConfiguration != null) {
			ISelection s = synchronizeConfiguration.getSite().getSelectionProvider().getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) s;
				Object element = ss.getFirstElement();
				if (element instanceof SyncInfoModelElement) {
					SyncInfoModelElement sime = (SyncInfoModelElement) element;
					return sime.getSyncInfo().getLocal().equals(resource);
				}
			}
		}
		return false;
	}

	@Override
	protected void fireInputChange() {
		node.fireChange();
	}
}
