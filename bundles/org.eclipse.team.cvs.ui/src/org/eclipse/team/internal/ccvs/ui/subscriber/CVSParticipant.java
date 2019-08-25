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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.synchronize.*;

/**
 * Superclass for all CVS particpants (workspace, merge and compare)
 */
public class CVSParticipant extends SubscriberParticipant implements IChangeSetProvider {
	
	private CVSChangeSetCapability capability;

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		// The decorator adds itself to the configuration
		ILabelDecorator labelDecorator = new CVSParticipantLabelDecorator(configuration);
		configuration.addLabelDecorator(labelDecorator);
	}
	
	@Override
	public void prepareCompareInput(ISynchronizeModelElement element, CompareConfiguration config, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100);
		deriveBaseContentsFromLocal(element, Policy.subMonitorFor(monitor, 10));
		super.prepareCompareInput(element, config, Policy.subMonitorFor(monitor, 80));
		updateLabelsForCVS(element, config, Policy.subMonitorFor(monitor, 10));
		monitor.done();
	}

	/**
	 * Helper method for updating compare editor labels
	 */
	protected static void updateLabelsForCVS(ISynchronizeModelElement element, CompareConfiguration config, IProgressMonitor monitor) {
		// Add the author to the remote or base
		if (TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SHOW_AUTHOR_IN_COMPARE_EDITOR)) {
			SyncInfo info = getSyncInfo(element);
			if (info != null) {
				final IResourceVariant remote = info.getRemote();
				final IResourceVariant base = info.getBase();
				String remoteAuthor = null;
				if (remote != null && !remote.isContainer()) {
					try {
						ILogEntry entry = ((ICVSRemoteFile)remote).getLogEntry(monitor);
						remoteAuthor = entry.getAuthor();
						config.setRightLabel(NLS.bind(CVSUIMessages.CVSParticipant_0, new String[] { remote.getContentIdentifier(), remoteAuthor })); 
					} catch (TeamException e) {
						CVSUIPlugin.log(e);
					}
				}
				if (base != null && !base.isContainer()) {
					try {
						String baseAuthor;
						if (remoteAuthor != null && remote.getContentIdentifier().equals(base.getContentIdentifier())) {
							baseAuthor = remoteAuthor;
						} else {
							ILogEntry entry = ((ICVSRemoteFile)base).getLogEntry(monitor);
							baseAuthor = entry.getAuthor();
						}
						config.setAncestorLabel(NLS.bind(CVSUIMessages.CVSParticipant_1, new String[] { base.getContentIdentifier(), baseAuthor })); 
					} catch (TeamException e) {
						CVSUIPlugin.log(e);
					}
				}
			}
		}
	}
	
	protected static SyncInfo getSyncInfo(ISynchronizeModelElement element) {
		if (element instanceof IAdaptable) {
			return ((IAdaptable)element).getAdapter(SyncInfo.class);
		}
		return null;
	}

	/**
	 * If the local is not modified and the base matches the local then 
	 * cache the local contents as the contents of the base.
	 * @param element
	 * @throws CoreException
	 * @throws TeamException
	 */
	public static void deriveBaseContentsFromLocal(ISynchronizeModelElement element, IProgressMonitor monitor) throws TeamException {
		SyncInfo info = getSyncInfo(element);
		if (info == null) 
			return;
		
		// We need a base that is a file and a local that is a file
		IResource local = info.getLocal();
		IResourceVariant base = info.getBase();
		if (base == null || base.isContainer() || local.getType() != IResource.FILE || !local.exists())
			return;
		
		// We can only use the local contents for incoming changes.
		// Outgoing or conflicting changes imply that the local has changed
		if ((info.getKind() & SyncInfo.DIRECTION_MASK) != SyncInfo.INCOMING)
			return;
		
		try {
			RemoteFile remoteFile = (RemoteFile)base;
			if (!remoteFile.isContentsCached())
				(remoteFile).setContents((IFile)local, monitor);
		} catch (CoreException e) {
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				// The file must have just been deleted
				return;
			}
			throw CVSException.wrapException(e);
		}
	}
	
	@Override
	public PreferencePage[] getPreferencePages() {
		return addCVSPreferencePages(super.getPreferencePages());
	}

	public static PreferencePage[] addCVSPreferencePages(PreferencePage[] inheritedPages) {
		PreferencePage[] pages = new PreferencePage[inheritedPages.length + 1];
		System.arraycopy(inheritedPages, 0, pages, 0, inheritedPages.length);
		pages[pages.length - 1] = new ComparePreferencePage();
		pages[pages.length - 1].setTitle(CVSUIMessages.CVSParticipant_2); 
		return pages;
	}
	
	@Override
	public ChangeSetCapability getChangeSetCapability() {
		if (capability == null) {
			capability = createChangeSetCapability();
		}
		return capability;
	}

	/**
	 * Create the change set capability for this particpant.
	 * @return the created capability
	 */
	protected CVSChangeSetCapability createChangeSetCapability() {
		return new CVSChangeSetCapability();
	}
	
	@Override
	protected boolean isViewerContributionsSupported() {
		return true;
	}
}
