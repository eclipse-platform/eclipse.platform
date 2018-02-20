/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		ICVSRemoteResource[] resources = getRemoteResources();
		monitor.beginTask(null, 1000 * resources.length);
		for (int i = 0; i < resources.length; i++) {
			IStatus status = resources[i].tag(getTag(), getLocalOptions(), SubMonitor.convert(monitor, 1000));
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#getTag()
	 */
	public CVSTag getTag() {
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#setTag(org.eclipse.team.internal.ccvs.core.CVSTag)
	 */
	public void setTag(CVSTag tag) {
		this.tag = tag;
	}

	public void addLocalOption(LocalOption option)  {
		localOptions.add(option);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#moveTag()
	 */
	public void moveTag() {
		addLocalOption(RTag.FORCE_REASSIGNMENT);
		addLocalOption(RTag.CLEAR_FROM_REMOVED);
		if (tag != null && tag.getType() == CVSTag.BRANCH) {
			addLocalOption(RTag.FORCE_BRANCH_REASSIGNMENT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#recurse()
	 */
	public void doNotRecurse() {
		addLocalOption(Command.DO_NOT_RECURSE);
	}

	protected String getTaskName() {
		return CVSUIMessages.TagFromRepository_taskName; 
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#getTagSource()
     */
    public TagSource getTagSource() {
        return TagSource.create(getCVSResources());
    }
    
    protected boolean isReportableError(IStatus status) {
        return super.isReportableError(status)
        	|| status.getCode() == CVSStatus.TAG_ALREADY_EXISTS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#isEmpty()
     */
    public boolean isEmpty() {
        return getCVSResources().length == 0;
    }
}
