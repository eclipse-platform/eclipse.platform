/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class CreatePatchAction extends CVSParticipantAction {

	protected CreatePatchAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return null;
	}
	
	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING});
	}

	@Override
	public void runOperation() {
		final SyncInfoSet set = getSyncInfoSet();
		GenerateDiffFileWizard.run(getConfiguration().getSite().getPart(), set.getResources(), false);
	}
	
	/*
	 * Return the selected SyncInfo for which this action is enabled.
	 * 
	 * @return the selected SyncInfo for which this action is enabled.
	 */
	private SyncInfoSet getSyncInfoSet() {
		IDiffElement [] elements= getFilteredDiffElements();
		SyncInfoSet filtered = new SyncInfoSet();
		for (IDiffElement e : elements) {
			if (e instanceof SyncInfoModelElement) {
				filtered.add(((SyncInfoModelElement)e).getSyncInfo());
			}
		}
		return filtered;
	}
	
	@Override
	protected String getBundleKeyPrefix() {
		return "GenerateDiffFileAction."; //$NON-NLS-1$
	}
}
