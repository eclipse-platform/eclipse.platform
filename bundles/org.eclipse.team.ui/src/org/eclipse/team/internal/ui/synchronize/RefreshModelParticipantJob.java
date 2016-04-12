/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Eugene Kuleshov (eu@md.pp.ru) - Bug 138152 Improve sync job status reporting
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.core.mapping.GroupProgressMonitor;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

public class RefreshModelParticipantJob extends RefreshParticipantJob {

	private final ResourceMapping[] mappings;
	private IProgressMonitor group;
	private int groupTicks;

	public class ChangeDescription implements IChangeDescription, IDiffChangeListener {
		Map changes = new HashMap();

		@Override
		public int getChangeCount() {
			return changes.size();
		}

		@Override
		public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
			IDiff[] additions = event.getAdditions();
			for (int i = 0; i < additions.length; i++) {
				IDiff node = additions[i];
				changes.put(node.getPath(), node);
			}
			IDiff[] changed = event.getChanges();
			for (int i = 0; i < changed.length; i++) {
				IDiff node = changed[i];
				changes.put(node.getPath(), node);
			}
		}

		@Override
		public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
			// Do nothing
		}
	}

	public RefreshModelParticipantJob(ISynchronizeParticipant participant, String jobName, String taskName, ResourceMapping[] mappings, IRefreshSubscriberListener listener) {
		super(participant, jobName, taskName, listener);
		this.mappings = mappings;
	}

	@Override
	protected void doRefresh(IChangeDescription changeListener,
			IProgressMonitor monitor) throws CoreException {
		ISynchronizationContext context = ((ModelSynchronizeParticipant)getParticipant()).getContext();
		try {
			context.getDiffTree().addDiffChangeListener((ChangeDescription)changeListener);
			// TODO: finer grained refresh
			context.refresh(mappings, monitor);
			// Wait for any asynchronous updating to complete
			try {
				Job.getJobManager().join(context, monitor);
			} catch (InterruptedException e) {
				// Ignore
			}
		} finally {
			context.getDiffTree().removeDiffChangeListener((ChangeDescription)changeListener);
		}
	}

	@Override
	protected int getChangeCount() {
		return ((ModelSynchronizeParticipant)getParticipant()).getContext().getDiffTree().size();
	}

    @Override
	protected int getIncomingChangeCount() {
      IResourceDiffTree diffTree = ((ModelSynchronizeParticipant)getParticipant()).getContext().getDiffTree();
      return (int) diffTree.countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK);
    }

    @Override
	protected int getOutgoingChangeCount() {
      IResourceDiffTree diffTree = ((ModelSynchronizeParticipant)getParticipant()).getContext().getDiffTree();
      return (int) diffTree.countFor(IThreeWayDiff.OUTGOING, IThreeWayDiff.DIRECTION_MASK);
    }

	@Override
	protected void handleProgressGroupSet(IProgressMonitor group, int ticks) {
		this.group = group;
		this.groupTicks = ticks;
	}

	@Override
	protected IChangeDescription createChangeDescription() {
		return new ChangeDescription();
	}

	@Override
	public boolean belongsTo(Object family) {
		if (family instanceof RefreshModelParticipantJob) {
			RefreshModelParticipantJob rmpj = (RefreshModelParticipantJob) family;
			return rmpj.getParticipant() == getParticipant();
		}
		if (family == getParticipant())
			return true;
		return super.belongsTo(family);
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		if (group != null)
			monitor = wrapMonitorWithGroup(monitor);
		return super.run(monitor);
	}

	private IProgressMonitor wrapMonitorWithGroup(IProgressMonitor monitor) {
		return new GroupProgressMonitor(monitor, group, groupTicks);
	}

}
