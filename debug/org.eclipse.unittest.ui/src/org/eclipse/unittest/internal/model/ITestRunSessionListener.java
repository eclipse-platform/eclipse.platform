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

package org.eclipse.unittest.internal.model;

import org.eclipse.unittest.model.ITestRunSession;

/**
 * An interface to listen to the events from the on added/removed
 * {@link ITestRunSession} instances.
 */
public interface ITestRunSessionListener {
	/**
	 * Notifies on an added {@link ITestRunSession} instance.
	 *
	 * @param testRunSession the new session
	 */
	void sessionAdded(ITestRunSession testRunSession);

	/**
	 * Notifies on a removed {@link ITestRunSession} instance.
	 *
	 * @param testRunSession the new session
	 */
	void sessionRemoved(ITestRunSession testRunSession);
}
