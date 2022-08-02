/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob.IChangeDescription;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * A refresh event generated to notify clients of the refresh lifecycle.
 */
public class RefreshEvent implements IRefreshEvent {
	int type;
	SyncInfo[] changes;
	long startTime = 0;
	long stopTime = 0;
	IStatus status;
	private final ISynchronizeParticipant participant;
	private final IChangeDescription description;
	private boolean isLink;

	public RefreshEvent(int type, ISynchronizeParticipant participant, IChangeDescription description) {
		this.type = type;
		this.participant = participant;
		this.description = description;
	}

	@Override
	public int getRefreshType() {
		return type;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	@Override
	public long getStopTime() {
		return stopTime;
	}

	public void setStopTime(long stopTime) {
		this.stopTime = stopTime;
	}

	@Override
	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus status) {
		this.status = status;
	}

	@Override
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}

	@Override
	public IChangeDescription getChangeDescription() {
		return description;
	}

	@Override
	public boolean isLink() {
		return isLink;
	}

	@Override
	public void setIsLink(boolean isLink) {
		this.isLink = isLink;
	}
}
