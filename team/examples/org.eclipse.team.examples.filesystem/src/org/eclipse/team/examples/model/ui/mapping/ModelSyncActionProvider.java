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
package org.eclipse.team.examples.model.ui.mapping;

import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * The action provider that is used for synchronizations.
 */
public class ModelSyncActionProvider extends SynchronizationActionProvider {

	public ModelSyncActionProvider() {
		super();
	}

	@Override
	protected void initialize() {
		super.initialize();
		final ISynchronizePageConfiguration configuration= getSynchronizePageConfiguration();
		// We provide custom handlers that ensure that the MOD files get updated properly
		// when MOE files are merged.
		registerHandler(MERGE_ACTION_ID, new ModelMergeActionHandler(configuration, false));
		registerHandler(OVERWRITE_ACTION_ID, new ModelMergeActionHandler(configuration, true));
		// We can just use the default mark as merged handler
	}
}
