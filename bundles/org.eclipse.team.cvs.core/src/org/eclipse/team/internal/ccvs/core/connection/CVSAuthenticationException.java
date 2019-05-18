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


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

public class CVSAuthenticationException extends CVSException {

	private static final long serialVersionUID = 1L;
	
	private int retryStatus = 0;

	/**
	 * Code indicating that authentication can be retried after 
	 * prompting the user for corrected authentication information
	 */
	public static final int RETRY = 1;
	
	/**
	 * Code indicating that authentication should not be reattempted.
	 */
	public static final int NO_RETRY = 2;
	
	/**
	 * Creates a new <code>CVSAuthenticationException</code>
	 * 
	 * @param status the status result describing this exception.
	 */
	private CVSAuthenticationException(IStatus status) {
		super(status);
	}	
	
	/**
	 * Creates a new <code>CVSAuthenticationException</code>
	 * 
	 * @param detail  a message that describes the exception in detail.
	 * @param code indicates whether authentication can be retried or not
	 */
	public CVSAuthenticationException(String detail, int retryStatus) {
		this(new CVSStatus(IStatus.ERROR, CVSStatus.AUTHENTICATION_FAILURE,NLS.bind(CVSMessages.CVSAuthenticationException_detail, (new Object[] { detail })), (IResource) null)); //
		this.retryStatus = retryStatus;
	}
	
	/**
	 * Creates a new <code>CVSAuthenticationException</code>
	 * 
	 * @param detail  a message that describes the exception in detail.
	 * @param code indicates whether authentication can be retried or not
	 * @param the location of the CVS server
	 */
	public CVSAuthenticationException(String detail, int retryStatus, ICVSRepositoryLocation cvsLocation) {
		this(new CVSStatus(IStatus.ERROR, CVSStatus.AUTHENTICATION_FAILURE,NLS.bind(CVSMessages.CVSAuthenticationException_detail, (new Object[] { detail })),cvsLocation)); //
		this.retryStatus = retryStatus;
	}	
	
	/**
	 * Creates a new <code>CVSAuthenticationException</code>
	 * 
	 * @param detail  a message that describes the exception in detail.
	 * @param code indicates whether authentication can be retried or not
	 * @param the location of the CVS server
	 * @param the exception 
	 */	
	public CVSAuthenticationException(String detail, int retryStatus,ICVSRepositoryLocation cvsLocation, Exception e) {
		this(new CVSStatus(IStatus.ERROR, CVSStatus.AUTHENTICATION_FAILURE , NLS.bind(CVSMessages.CVSAuthenticationException_detail, (new Object[] { detail })), e, cvsLocation)); //
		this.retryStatus = retryStatus;
	}

	public int getRetryStatus() {
		return retryStatus;
	}
}
