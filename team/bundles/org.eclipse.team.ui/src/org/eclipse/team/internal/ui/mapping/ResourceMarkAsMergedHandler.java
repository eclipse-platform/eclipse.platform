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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ResourceMarkAsMergedHandler extends ResourceMergeActionHandler {

	private ResourceModelProviderOperation operation;


	public ResourceMarkAsMergedHandler(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected synchronized SynchronizationOperation getOperation() {
		if (operation == null) {
			operation = new ResourceModelProviderOperation(getConfiguration(),
						getStructuredSelection()) {
				@Override
				public void execute(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						final IMergeContext context = (IMergeContext) getContext();
						final IDiff[] deltas = getTargetDiffs();
						ISchedulingRule rule = getMergeRule(context, deltas);
						context.run(monitor1 -> markAsMerged(deltas, context, monitor1), rule, IResource.NONE, monitor);

					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}

				private ISchedulingRule getMergeRule(IMergeContext context,
						IDiff[] deltas) {
					ISchedulingRule result = null;
					for (IDiff node : deltas) {
						ISchedulingRule rule = context.getMergeRule(node);
						if (result == null) {
							result = rule;
						} else {
							result = MultiRule.combine(result, rule);
						}
					}
					return result;
				}

				private void markAsMerged(IDiff[] deltas,
						final IMergeContext context, IProgressMonitor monitor)
						throws CoreException {
					context.markAsMerged(deltas, false, monitor);
				}

				@Override
				protected FastDiffFilter getDiffFilter() {
					return new FastDiffFilter() {
						@Override
						public boolean select(IDiff node) {
							if (node instanceof IThreeWayDiff) {
								IThreeWayDiff twd = (IThreeWayDiff) node;
								if (twd.getDirection() == IThreeWayDiff.CONFLICTING
										|| twd.getDirection() == IThreeWayDiff.INCOMING) {
									return true;
								}
							}
							return false;
						}
					};
				}
				@Override
				protected String getJobName() {
					IDiff[] diffs = getTargetDiffs();
					if (diffs.length == 1)
						return TeamUIMessages.ResourceMarkAsMergedHandler_0;
					return NLS.bind(TeamUIMessages.ResourceMarkAsMergedHandler_1, Integer.toString(diffs.length));
				}
			};
		}
		return operation;
	}


	@Override
	public void updateEnablement(IStructuredSelection selection) {
		synchronized (this) {
			operation = null;
		}
		super.updateEnablement(selection);
		int mode = getConfiguration().getMode();
		if ((mode == ISynchronizePageConfiguration.OUTGOING_MODE
				&& getSynchronizationContext().getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) == 0)
				|| (getSynchronizationContext().getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) == 0
						&& getSynchronizationContext().getDiffTree().countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) == 0)) {
			setEnabled(false);
			return;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (saveDirtyEditors())
			return super.execute(event);
		return null;
	}

}
