/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.webapp;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * Filter for filtering out content of help documents delivered to the client
 * @since 3.4
 */
public interface IFilter {
	/**
	 * Filters OutputStream out
	 *
	 * @param req
	 *            HTTPServletRequest for resource being filtered; filter's logic
	 *            might differ depending on the request
	 * @param out
	 *            original OutputStream
	 * @return filtered OutputStream
	 */
	OutputStream filter(HttpServletRequest req, OutputStream out);
}
