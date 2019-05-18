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
package org.eclipse.team.internal.ccvs.core.connection;

 
import java.io.InterruptedIOException;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

public class CVSCommunicationException extends CVSException {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new <code>CVSCommunicationException</code> with the
	 * given status.
	 */
	private CVSCommunicationException(IStatus status) {
		super(status);
	}
	/**
	 * Create a new <code>CVSCommunicationException</code> with the
	 * given message.
	 */
	public CVSCommunicationException(String message) {
		super(message);
	}
	
	/**
	 * Create a new <code>CVSCommunicationException</code>.
	 *
	 * @param message a message describing the exception in detail.
	 * @param the CVS server
	 * @param the caught exception that has caused the communication
	 *  exception.
	 */
	public CVSCommunicationException(String message, ICVSRepositoryLocation cvsLocation, Exception e) {
		this(new CVSStatus(IStatus.ERROR, CVSStatus.COMMUNICATION_FAILURE, message, e, cvsLocation));		
	}
	
	/**
	 * Create a new <code>CVSCommunicationException </code>.
	 *
	 * @param the caught exception that has caused the communication
	 *  exception.
	 * @param the location of the CVS server.
	 */
	public CVSCommunicationException(ICVSRepositoryLocation cvsLocation,Exception e) {
		this(getStatusFor(e,cvsLocation));
	}	
	
	private static IStatus getStatusFor(Exception e,ICVSRepositoryLocation cvsLocation) {
		if (e instanceof InterruptedIOException) {
			MultiStatus status = new MultiStatus(CVSProviderPlugin.ID, 0, getMessageFor(e), e);
			status.add(new CVSStatus(IStatus.ERROR, CVSStatus.COMMUNICATION_FAILURE, CVSMessages.CVSCommunicationException_interruptCause, cvsLocation)); 
			status.add(new CVSStatus(IStatus.ERROR, CVSStatus.COMMUNICATION_FAILURE, CVSMessages.CVSCommunicationException_interruptSolution, cvsLocation)); 
			status.add(new CVSStatus(IStatus.ERROR, CVSStatus.COMMUNICATION_FAILURE, CVSMessages.CVSCommunicationException_alternateInterruptCause, cvsLocation)); 
			status.add(new CVSStatus(IStatus.ERROR, CVSStatus.COMMUNICATION_FAILURE, CVSMessages.CVSCommunicationException_alternateInterruptSolution, cvsLocation)); 
			return status;
		}
		return new CVSStatus(IStatus.ERROR,CVSStatus.COMMUNICATION_FAILURE, getMessageFor(e), e, cvsLocation);
	}
	
	public static String getMessageFor(Throwable throwable) {
		String message = Policy.getMessage(getMessageKey(throwable));
		if (message == null) {
			message = NLS.bind(CVSMessages.CVSCommunicationException_io, (new Object[] {throwable.toString()}));
		} else {
			message = NLS.bind(message, (new Object[] {throwable.getMessage()}));
		}
		return message;
	}
	
	private static String getMessageKey(Throwable t) {
		String name = t.getClass().getName();
		name = name.replace('.', '_');
		return name;
	}
}
