/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.core.tools.metadata;

public class PartialDumpException extends DumpException {

	private static final long serialVersionUID = 1L;
	/**
	 * Data read when the error happened. May be
	 * null.
	 */
	private Object partialContents;

	public PartialDumpException(String msg, Object partialContents) {
		super(msg);
		this.partialContents = partialContents;
	}

	/**
	 * Returns the contents partially read when this exception occurred. May
	 * return <code>null</code>.
	 *
	 * @return the contents partially read, or <code>null</code>
	 */
	public Object getPartialContents() {
		return partialContents;
	}
}
