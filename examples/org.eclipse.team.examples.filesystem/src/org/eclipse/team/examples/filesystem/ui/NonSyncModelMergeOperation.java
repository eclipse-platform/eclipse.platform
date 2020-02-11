/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemMergeContext;
import org.eclipse.team.ui.synchronize.ModelMergeOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This operation shows an example of how a repository tool could
 * perform the Preview (manual merge) phase of the merge operation
 * without using the Synchronize view. It will do so by showing the
 * user a flat list of all model elements that require a manual merge.
 * <p>
 * There are a couple of open issues here:
 * <ol>
 * <li>All the model provider UI is Common Navigator based so there is no model
 * provider contributed actions available to overwrite and mark-as-merged. This will
 * be a problem for models that have multiple resource mappings within a single file.
 * <li>The order in which model elements are merged may matter. There is currently
 * no Team API to determine this order.</li>
 * <li>This operation assumes that a compare input will be available for the
 * model objects that are obtained from the model provider. Although this
 * is a reasonable assumption, it is not enforced by the API specification.</li>
 * </ol>
 */
public class NonSyncModelMergeOperation extends ModelMergeOperation {

	private FileSystemMergeContext context;

	/**
	 * Create the operation
	 * @param part the part from which the operation was launched
	 * @param manager the scope of the operation
	 */
	protected NonSyncModelMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part, manager);
	}

	@Override
	protected void initializeContext(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			// Create the context
			context = new FileSystemMergeContext(getScopeManager());
			// Refresh the context to get the latest remote state
			context.refresh(getScope().getTraversals(),
					RemoteResourceMappingContext.FILE_CONTENTS_REQUIRED, SubMonitor.convert(monitor, 75));
			// What for the context to asynchronously update the diff tree
			try {
				Job.getJobManager().join(context, SubMonitor.convert(monitor, 25));
			} catch (InterruptedException e) {
				// Ignore
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	protected ISynchronizationContext getContext() {
		return context;
	}

	/**
	 * Handle the preview request by opening a dialog that allows the user to manually merge
	 * any changes.
	 * @see org.eclipse.team.ui.synchronize.ModelMergeOperation#handlePreviewRequest()
	 */
	@Override
	protected void handlePreviewRequest() {
		// We perform a syncExec so that the job will dispose of the scope manager
		// after the dialog closes
		Display.getDefault().syncExec(() -> NonSyncMergeDialog.openFor(NonSyncModelMergeOperation.this));
	}

	@Override
	public Shell getShell() {
		// Change method to public
		return super.getShell();
	}

}
