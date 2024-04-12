/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelMergeOperation;

public final class MergeAllOperation extends SynchronizationOperation {

	private final IMergeContext context;
	private final String jobName;

	public MergeAllOperation(String jobName, ISynchronizePageConfiguration configuration, Object[] elements, IMergeContext context) {
		super(configuration, elements);
		this.jobName = jobName;
		this.context = context;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		new ModelMergeOperation(getPart(), ((SynchronizationContext)context).getScopeManager()) {
			@Override
			public boolean isPreviewRequested() {
				return false;
			}
			@Override
			protected void initializeContext(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask(null, 10);
				monitor.done();
			}
			@Override
			protected ISynchronizationContext getContext() {
				return context;
			}
			@Override
			protected void executeMerge(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask(null, 100);
				if (!hasChangesOfInterest()) {
					handleNoChanges();
				} else if (isPreviewRequested()) {
					handlePreviewRequest();
				} else {
					IStatus status = ModelMergeOperation.validateMerge(getMergeContext(), Policy.subMonitorFor(monitor, 10));
					if (!status.isOK()) {
						if (!promptToContinue(status))
							return;
					}
					status = performMerge(Policy.subMonitorFor(monitor, 90));
					if (!status.isOK()) {
						handleMergeFailure(status);
					}
				}
				monitor.done();
			}
			private IMergeContext getMergeContext() {
				return (IMergeContext)getContext();
			}
			private boolean promptToContinue(final IStatus status) {
				final boolean[] result = new boolean[] { false };
				Runnable runnable = () -> {
					ErrorDialog dialog = new ErrorDialog(getShell(), TeamUIMessages.ModelMergeOperation_0, TeamUIMessages.ModelMergeOperation_1, status, IStatus.ERROR | IStatus.WARNING | IStatus.INFO) {
						@Override
						protected void createButtonsForButtonBar(Composite parent) {
							createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL,
									false);
							createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL,
									true);
							createDetailsButton(parent);
						}

						@Override
						protected void buttonPressed(int id) {
							if (id == IDialogConstants.YES_ID)
								super.buttonPressed(IDialogConstants.OK_ID);
							else if (id == IDialogConstants.NO_ID)
								super.buttonPressed(IDialogConstants.CANCEL_ID);
							super.buttonPressed(id);
						}
					};
					int code = dialog.open();
					result[0] = code == 0;
				};
				getShell().getDisplay().syncExec(runnable);
				return (result[0]);
			}
		}.run(monitor);
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	@Override
	protected String getJobName() {
		return jobName;
	}
}