/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.builders;

/**
 * A builder that has the callOnEmptyDelta attribute set to true in the
 * builder extension.
 */
public class EmptyDeltaBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.emptydeltabuilder";

	/**
	 * The most recently created instance
	 */
	protected static EmptyDeltaBuilder singleton;

	/**
	 * Returns the most recently created instance.
	 */
	public static EmptyDeltaBuilder getInstance() {
		return singleton;
	}

	public EmptyDeltaBuilder() {
		singleton = this;
	}
}
