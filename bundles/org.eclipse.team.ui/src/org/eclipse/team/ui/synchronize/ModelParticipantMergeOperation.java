/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A model merge operation that uses a participant to preview the changes
 * in either a dialog or the Synchronize view.
 *
 * @since 3.2
 */
public abstract class ModelParticipantMergeOperation extends ModelMergeOperation {

	/**
	 * Status code that can be returned from the {@link #performMerge(IProgressMonitor)}
	 * method to indicate that a subclass would like to force a preview of the merge.
	 * The message of such a status should be ignored.
	 */
	public static final int REQUEST_PREVIEW = 1024;

	private ModelSynchronizeParticipant participant;
	private boolean ownsParticipant = true;

	private boolean sentToSyncView;

	private static final Object PARTICIPANT_MERGE_FAMILY = new Object();

	/**
	 * Create a merge participant operation for the scope of the given manager.
	 * @param part the workbench part from which the merge was launched or <code>null</code>
	 * @param manager the scope manager
	 */
	protected ModelParticipantMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part, manager);
	}

	@Override
	protected void initializeContext(IProgressMonitor monitor) throws CoreException {
		if (participant == null) {
			participant = createParticipant();
			if (isPreviewRequested() && !isPreviewInDialog()) {
				// Put the participant into the sync view right away since a preview is requested
				handlePreviewRequest();
				sentToSyncView = true;
			}
			participant.getContext().refresh(getScope().getTraversals(),
					RemoteResourceMappingContext.FILE_CONTENTS_REQUIRED, monitor);
			// Only wait if we are not going to preview or we are previewing in a dialog
			if (!sentToSyncView)
				try {
					Job.getJobManager().join(participant.getContext(), monitor);
				} catch (InterruptedException e) {
					// Ignore
				}
		}
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			super.execute(monitor);
		} finally {
			if (ownsParticipant && participant != null)
				participant.dispose();
		}
	}

	@Override
	protected void executeMerge(IProgressMonitor monitor) throws CoreException {
		if (!sentToSyncView)
			super.executeMerge(monitor);
	}

	@Override
	protected void handlePreviewRequest() {
		Job job = new WorkbenchJob(getJobName()) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (isPreviewInDialog()) {
					CompareConfiguration cc = new CompareConfiguration();
					ISynchronizePageConfiguration pageConfiguration = participant.createPageConfiguration();
					// Restrict preview page to only support incoming and conflict modes
					if (pageConfiguration.getComparisonType() == ISynchronizePageConfiguration.THREE_WAY) {
						pageConfiguration.setSupportedModes(ISynchronizePageConfiguration.INCOMING_MODE | ISynchronizePageConfiguration.CONFLICTING_MODE);
						pageConfiguration.setMode(ISynchronizePageConfiguration.INCOMING_MODE);
					}
					ParticipantPageCompareEditorInput input = new ParticipantPageCompareEditorInput(cc, pageConfiguration, participant);
					CompareUI.openCompareDialog(input);
				} else {
					ISynchronizeManager mgr = TeamUI.getSynchronizeManager();
					ISynchronizeView view = mgr.showSynchronizeViewInActivePage();
					mgr.addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
					view.display(participant);
					Object adapted = view.getSite().getAdapter(IWorkbenchSiteProgressService.class);
					if (adapted instanceof IWorkbenchSiteProgressService) {
						IWorkbenchSiteProgressService siteProgress = (IWorkbenchSiteProgressService) adapted;
						siteProgress.showBusyForFamily(PARTICIPANT_MERGE_FAMILY);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				// Ensure that the participant is disposed it it didn't go to the sync view
				if (TeamUI.getSynchronizeManager().get(participant.getId(), participant.getSecondaryId()) == null)
					participant.dispose();
			}

		});
		ownsParticipant = false;
		job.schedule();
	}

	@Override
	public boolean belongsTo(Object family) {
		if (family == PARTICIPANT_MERGE_FAMILY) {
			return true;
		}
		if (participant != null && participant == family)
			return true;
		return super.belongsTo(family);
	}

	/**
	 * Return whether previews should occur in a dialog or in the synchronize view.
	 * @return whether previews should occur in a dialog or in the synchronize view
	 */
	protected boolean isPreviewInDialog() {
		return true;
	}

	@Override
	protected ISynchronizationContext getContext() {
		if (participant != null)
			return participant.getContext();
		return null;
	}

	@Override
	protected String getPreviewRequestMessage() {
		if (!isPreviewRequested()) {
			return TeamUIMessages.ResourceMappingMergeOperation_4;
		}
		return super.getPreviewRequestMessage();
	}

	/**
	 * Create the synchronize participant to be used by this operation
	 * to preview changes. By default, a {@link ModelSynchronizeParticipant}
	 * is created using the scope manager ({@link #getScopeManager()}) context
	 * from ({@link #createMergeContext()}) and job name ({@link #getJobName()})
	 * of this operation. Subclasses may override this method.
	 * <p>
	 * Once created, it is the responsibility of the participant to dispose of the
	 * synchronization context when it is no longer needed.
	 * @return a newly created synchronize participant to be used by this operation
	 */
	protected ModelSynchronizeParticipant createParticipant() {
		return ModelSynchronizeParticipant.createParticipant(createMergeContext(), getJobName());
	}

	/**
	 * Create a merge context for use by this operation. This method
	 * is not long running so the operation should not refresh the
	 * context or perform other long running operations in this thread.
	 * However the context may start initializing in another thread as long
	 * as the job used to perform the initialization belongs to the
	 * family that matches the context.
	 * @return a merge context for use by this operation
	 */
	protected abstract SynchronizationContext createMergeContext();

}
