/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import org.eclipse.core.runtime.CoreException;

/**
 */
public class SimpleNature extends TestNature {
	private static SimpleNature instance;
	public boolean wasConfigured;
	public boolean wasDeconfigured;

	/**
	 * Returns the instance of this nature.
	 */
	public static SimpleNature getInstance() {
		if (instance == null) {
			new SimpleNature();
		}
		return instance;
	}

	/**
	 * Constructor for SimpleNature.
	 */
	public SimpleNature() {
		super();
		instance = this;
	}

	@Override
	public void configure() throws CoreException {
		super.configure();
		wasConfigured = true;
	}

	@Override
	public void deconfigure() throws CoreException {
		super.deconfigure();
		wasDeconfigured = true;
	}

	/**
	 * Resets validation flags
	 */
	public void reset() {
		wasConfigured = false;
		wasDeconfigured = false;
	}
}
