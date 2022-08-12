/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class RestoreRemovedItemsAction extends ResourceModelParticipantAction {

	public RestoreRemovedItemsAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		Utils.initAction(this, "action.restoreRemovedFromView."); //$NON-NLS-1$
	}

	@Override
	public void run() {
		ISynchronizationContext context = getSynchronizationContext();
		if(context instanceof SubscriberMergeContext){
			SubscriberMergeContext smc = (SubscriberMergeContext) context;
			SubscriberDiffTreeEventHandler handler  = smc.getAdapter(SubscriberDiffTreeEventHandler.class);
			handler.reset();
		}
		super.run();
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return true;
	}


}
