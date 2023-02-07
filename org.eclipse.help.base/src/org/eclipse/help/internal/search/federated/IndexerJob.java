/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.search.federated;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.*;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;

public class IndexerJob extends Job {
	public static final String FAMILY = "org.eclipse.help.base.indexer"; //$NON-NLS-1$
	public IndexerJob() {
		super(HelpBaseResources.IndexerJob_name);
	}
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SearchIndexWithIndexingProgress index = BaseHelpSystem.getLocalSearchManager().getIndex(Platform.getNL());
		try {
			long start = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_SEARCH) {
				System.out.println("Start to update search index"); //$NON-NLS-1$
			}
			BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(monitor, index);
			long stop = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_SEARCH) {
				System.out.println("Milliseconds to update search index =  = " + (stop - start)); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		}
		catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		catch (Exception e) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.OK, HelpBaseResources.IndexerJob_error, e);
		}
	}
	@Override
	public boolean belongsTo(Object family) {
		return FAMILY.equals(family);
	}
}
