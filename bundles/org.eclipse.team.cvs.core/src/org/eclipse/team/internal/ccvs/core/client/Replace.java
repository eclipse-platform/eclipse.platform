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
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 179174 CVS client sets timestamps back when replacing
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.util.Date;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;

/**
 * A specialized update that will ignore unmanaged local content like
 * CheckoutWithOverwrite and avoid setting back the timestamps of files
 * recreated after being deleted by PrepareForReplaceVisitor.
 */
public class Replace extends Update {

	private Set/*<ICVSFile>*/ prepDeletedFiles = null;

	public Replace() {}
	public Replace(Set/*<ICVSFile>*/ prepDeletedFiles) {
		this.prepDeletedFiles = prepDeletedFiles;
	}
	
	/**
	 * This class overrides the "Created" handler but uses the "Updated"
	 * behavior which will overwrite existing files.
	 */
	public class CreatedResponseHandler extends UpdatedHandler {
		public CreatedResponseHandler() {
			super(UpdatedHandler.HANDLE_UPDATED);
		}
		public String getResponseID() {
			return "Created"; //$NON-NLS-1$
		}
		protected void receiveTargetFile(Session session, ICVSFile file, String entryLine, Date modTime,
			boolean binary, boolean readOnly, boolean executable, IProgressMonitor monitor) throws CVSException {
			// Discard any timestamp for files being recreated after being
			// deleted by PrepareForReplaceVisitor.
			if (prepDeletedFiles != null && prepDeletedFiles.contains(file))
				modTime = null;
			super.receiveTargetFile(session, file, entryLine, modTime, binary, readOnly, executable, monitor);
		}
	}

	@Override
	protected IStatus doExecute(
			Session session,
			GlobalOption[] globalOptions,
			LocalOption[] localOptions,
			String[] arguments,
			ICommandOutputListener listener,
			IProgressMonitor monitor)
	throws CVSException {
		
		ResponseHandler newCreated = new CreatedResponseHandler();
		ResponseHandler oldCreated = session.getResponseHandler(newCreated.getResponseID());
		session.registerResponseHandler(newCreated);
		try {
			return super.doExecute(
					session,
					globalOptions,
					localOptions,
					arguments,
					listener,
					monitor);
		} finally {
			session.registerResponseHandler(oldCreated);
		}
	}
}
