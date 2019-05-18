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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogEntry;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSFileRevision;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.ShowAnnotationOperation;

public class ShowAnnotationAction extends WorkspaceAction {

	/**
	 * Action to open a CVS Annotate View
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final ICVSResource resource= getSingleSelectedCVSResource();
		if (resource == null)
			return;
		execute(resource);
	}
	
	/**
	 * Fetch the revision number of a CVS resource and perform a ShowAnnotationOperation
	 * in the background.
	 * 
	 * @param cvsResource The CVS resource (must not be null)
	 * 
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public void execute(final ICVSResource cvsResource) throws InvocationTargetException, InterruptedException {
		final String revision= getRevision(cvsResource);
		if (revision == null)
			return;
		boolean binary = isBinary(cvsResource);
		if (binary) {
			final IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
			final String option = store.getString(ICVSUIConstants.PREF_ANNOTATE_PROMPTFORBINARY);
			if (option.equals(MessageDialogWithToggle.PROMPT)) {
				final MessageDialogWithToggle dialog = (MessageDialogWithToggle.openYesNoQuestion(getShell(), CVSUIMessages.ShowAnnotationAction_2, NLS.bind(CVSUIMessages.ShowAnnotationAction_3, new String[] {cvsResource.getName()}), CVSUIMessages.ShowAnnotationOperation_4, false, store, ICVSUIConstants.PREF_ANNOTATE_PROMPTFORBINARY));
				final int result = dialog.getReturnCode();
				switch (result) {
					case IDialogConstants.NO_ID :
						return;
				}
			} else if (option.equals(MessageDialogWithToggle.NEVER))
				return;
		}
		
		new ShowAnnotationOperation(getTargetPart(), cvsResource, revision, binary).run();
	}

	private boolean isBinary(ICVSResource cvsResource) {
		if (cvsResource.isFolder()) return false;
		
		try {
			byte[] syncBytes = ((ICVSFile)cvsResource).getSyncBytes();
				if (syncBytes == null)
					return false;
			return ResourceSyncInfo.isBinary(syncBytes);
		} catch (CVSException e) {
			return false;
		}
	}

	/**
	 * Only enabled for single resource selection
	 */
	public boolean isEnabled() {
		ICVSResource resource = getSingleSelectedCVSResource();
		try {
			return (resource != null && ! resource.isFolder() && resource.isManaged());
		} catch (CVSException e) {
			return isEnabledForException(e);
		}
	}

	/**
	 * This action is called from one of a Resource Navigator a CVS Resource
	 * Navigator or a History Log Viewer. Return the selected resource as an
	 * ICVSResource
	 * 
	 * @return ICVSResource
	 */
	private ICVSResource getSingleSelectedCVSResource() {
		// Selected from a CVS Resource Navigator
		final ICVSResource[] cvsResources = getSelectedCVSResources();
		if (cvsResources.length == 1) {
			return cvsResources[0];
		}

		// Selected from a History Viewer
		final Object[] logEntries = getAdaptedSelection(LogEntry.class);
		if (logEntries.length == 1) {
			final LogEntry aLogEntry = (LogEntry) logEntries[0];
			final ICVSRemoteFile cvsRemoteFile = aLogEntry.getRemoteFile();
			return cvsRemoteFile;
		}
		
		//Selected from the CVS History Page
		final Object[] fileRevisions = getAdaptedSelection(CVSFileRevision.class);
		if (fileRevisions.length == 1) {
			final ICVSRemoteFile cvsRemoteFile =((CVSFileRevision) fileRevisions[0]).getCVSRemoteFile();
			return cvsRemoteFile;
		}
		

		// Selected from a Resource Navigator
		final IResource[] resources = getSelectedResources();
		if (resources.length == 1) {
			return getCVSResourceFor(resources[0]);
		}
		return null;
	}

	
	/**
	 * Get the revision for the CVS resource. Throws an InvocationTargetException
	 * if the revision could not be determined.
	 * 
	 * @param cvsResource The CVS resource
	 * @return The revision of the resource.
	 * @throws InvocationTargetException
	 */
	private String getRevision(ICVSResource cvsResource) throws InvocationTargetException {
		final ResourceSyncInfo info;
		try {
			info= cvsResource.getSyncInfo();
			if (info == null)
				throw new CVSException(NLS.bind(CVSUIMessages.ShowAnnotationAction_noSyncInfo, new String[] { cvsResource.getName() }));
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
		return info.getRevision();
	}
	
	public String getId() {
		return ICVSUIConstants.CMD_ANNOTATE;
	}
}
