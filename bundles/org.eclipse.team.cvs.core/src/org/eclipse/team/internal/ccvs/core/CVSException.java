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
package org.eclipse.team.internal.ccvs.core;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;

/**
 * A checked expection representing a failure in the CVS plugin.
 * <p>
 * CVS exceptions contain a status object describing the cause of 
 * the exception.
 * </p>
 *
 * @see IStatus
 */
public class CVSException extends TeamException {

	private static final long serialVersionUID = 1L;

	public CVSException(CoreException e) {
		super(e);
	}

	public CVSException(String message) {
		this(new CVSStatus(IStatus.ERROR, message));
	}

	public CVSException(IStatus status) {
		super(status);
	}

	/*
	 * Static helper methods for creating exceptions
	 */
	public static CVSException wrapException(IResource resource, String message, IOException e) {
		return new CVSException(new CVSStatus(IStatus.ERROR, IO_FAILED, message, e, resource));
	}

	/*
	 * Static helper methods for creating exceptions
	 */
	public static CVSException wrapException(IResource resource, String message, CoreException e) {
		return new CVSException(new CVSStatus(IStatus.ERROR, e.getStatus().getCode(), message, e, resource));
	}

	/*
	 * Static helper methods for creating exceptions
	 */
	public static CVSException wrapException(Exception e) {
		Throwable t = e;
		if (e instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException) e).getTargetException();
			if (target instanceof CVSException) {
				return (CVSException) target;
			}
			t = target;
		}
		//TODO: fix the caller to include a resource
		//TODO: fix the caller to setup the error code
		return new CVSException(new CVSStatus(IStatus.ERROR, UNABLE, t.getMessage() != null ? t.getMessage() : "", t, (IResource)null)); //$NON-NLS-1$
	}

	public static CVSException wrapException(CoreException e) {
		if (e instanceof CVSException) { 
			return (CVSException)e;
		}
		return new CVSException(e);
	}
}
