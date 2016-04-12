/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ResourceMergeHandler extends ResourceMergeActionHandler {

	private final boolean overwrite;
	private ResourceModelProviderOperation operation;

	public ResourceMergeHandler(ISynchronizePageConfiguration configuration, boolean overwrite) {
		super(configuration);
		this.overwrite = overwrite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.MergeActionHandler#getOperation()
	 */
	@Override
	protected synchronized SynchronizationOperation getOperation() {
		if (operation == null) {
			operation = new ResourceModelProviderOperation(getConfiguration(), getStructuredSelection()) {
				/* (non-Javadoc)
				 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
				 */
				@Override
				public void execute(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						IMergeContext context = (IMergeContext)getContext();
						IDiff[] diffs = getTargetDiffs();
						if (diffs.length == 0) {
							promptForNoChanges();
						}
						IStatus status = context.merge(diffs, overwrite, monitor);
						if (!status.isOK())
							throw new CoreException(status);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
				/* (non-Javadoc)
				 * @see org.eclipse.team.internal.ui.mapping.ResourceModelProviderOperation#getDiffFilter()
				 */
				@Override
				protected FastDiffFilter getDiffFilter() {
					return new FastDiffFilter() {
						@Override
						public boolean select(IDiff node) {
							if (node instanceof IThreeWayDiff) {
								IThreeWayDiff twd = (IThreeWayDiff) node;
								if ((twd.getDirection() == IThreeWayDiff.OUTGOING && overwrite) || twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
									return true;
								}
								return false;
							}
							// Overwrite should always be available for two-way diffs
							return overwrite;
						}
					};
				}
				@Override
				protected String getJobName() {
					IDiff[] diffs = getTargetDiffs();
					if (overwrite) {
						if (diffs.length == 1)
							return TeamUIMessages.ResourceMergeHandler_0;
						return NLS.bind(TeamUIMessages.ResourceMergeHandler_1, new Integer(diffs.length).toString());

					}
					if (diffs.length == 1)
						return TeamUIMessages.ResourceMergeHandler_2;
					return NLS.bind(TeamUIMessages.ResourceMergeHandler_3, new Integer(diffs.length).toString());
				}
			};
		}
		return operation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.MergeActionHandler#updateEnablement(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void updateEnablement(IStructuredSelection selection) {
		synchronized (this) {
			operation = null;
		}
		super.updateEnablement(selection);
		int mode = getConfiguration().getMode();
		if (mode == ISynchronizePageConfiguration.OUTGOING_MODE && !overwrite) {
			setEnabled(false);
			return;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (saveDirtyEditors() && (!overwrite || promptToConfirm()))
			return super.execute(event);
		return null;
	}

	protected boolean promptToConfirm() {
		if (Display.getCurrent() != null)
			return internalPromptToConfirm();
		final boolean[] confirmed = new boolean[] { false };
		Shell shell = getConfiguration().getSite().getShell();
		if (!shell.isDisposed()) {
			Utils.syncExec(new Runnable() {
				@Override
				public void run() {
					confirmed[0] = promptToConfirm();
				}

			}, shell);
		}
		return confirmed[0];
	}

	private boolean internalPromptToConfirm() {
		return MessageDialog.openQuestion(getConfiguration().getSite().getShell(), TeamUIMessages.ResourceMergeHandler_4, TeamUIMessages.ResourceMergeHandler_5);
	}

	protected void promptForNoChanges() {
		Utils.syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(getConfiguration().getSite().getShell(), TeamUIMessages.ResourceMergeHandler_6, TeamUIMessages.ResourceMergeHandler_7);
			}
		}, (StructuredViewer)getConfiguration().getPage().getViewer());
	}


}
