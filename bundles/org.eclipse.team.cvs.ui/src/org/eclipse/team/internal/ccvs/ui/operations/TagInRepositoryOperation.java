/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.RTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.actions.TagAction;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.ui.IWorkbenchPart;

public class TagInRepositoryOperation extends RemoteOperation implements ITagOperation {

	private Set localOptions = new HashSet();
	private CVSTag tag;

	public TagInRepositoryOperation(IWorkbenchPart part, ICVSRemoteResource[] remoteResource) {
		super(part, remoteResource);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		ICVSRemoteResource[] resources = getRemoteResources();
		monitor.beginTask(null, 1000 * resources.length);
		for (ICVSRemoteResource resource : resources) {
			IStatus status = resource.tag(getTag(), getLocalOptions(), SubMonitor.convert(monitor, 1000));
			collectStatus(status);
		}
		if (!errorsOccurred()) {
			try {
				TagAction.broadcastTagChange(getCVSResources(), getTag());
			} catch (InvocationTargetException e) {
				throw CVSException.wrapException(e);
			}
		}
	}

	/**
	 * Override to dislay the number of tag operations that succeeded
	 */
	@Override
	protected String getErrorMessage(IStatus[] problems, int operationCount) {
		if(operationCount == 1) {
			return CVSUIMessages.TagInRepositoryAction_tagProblemsMessage; 
		} else {
			return NLS.bind(CVSUIMessages.TagInRepositoryAction_tagProblemsMessageMultiple, new String[] { Integer.toString(operationCount - problems.length), Integer.toString(problems.length) });
		}
	}

	private LocalOption[] getLocalOptions() {
		return (LocalOption[]) localOptions.toArray(new LocalOption[localOptions.size()]);
	}

	@Override
	public CVSTag getTag() {
		return tag;
	}

	@Override
	public void setTag(CVSTag tag) {
		this.tag = tag;
	}

	public void addLocalOption(LocalOption option)  {
		localOptions.add(option);
	}

	@Override
	public void moveTag() {
		addLocalOption(RTag.FORCE_REASSIGNMENT);
		addLocalOption(RTag.CLEAR_FROM_REMOVED);
		if (tag != null && tag.getType() == CVSTag.BRANCH) {
			addLocalOption(RTag.FORCE_BRANCH_REASSIGNMENT);
		}
	}

	@Override
	public void doNotRecurse() {
		addLocalOption(Command.DO_NOT_RECURSE);
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.TagFromRepository_taskName; 
	}

	@Override
	public TagSource getTagSource() {
		return TagSource.create(getCVSResources());
	}
	
	@Override
	protected boolean isReportableError(IStatus status) {
		return super.isReportableError(status)
			|| status.getCode() == CVSStatus.TAG_ALREADY_EXISTS;
	}

	@Override
	public boolean isEmpty() {
		return getCVSResources().length == 0;
	}
}
