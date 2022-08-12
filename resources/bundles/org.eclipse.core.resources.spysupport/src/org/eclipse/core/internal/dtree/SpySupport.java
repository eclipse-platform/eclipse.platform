/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.dtree;


/**
 * Provides special internal access to the workspace resource implementation.
 * This class is to be used for testing purposes only.
 */
public class SpySupport {

	/*
	 * Class cannot be instantiated.
	 */
	private SpySupport() {
		// not allowed
	}

	public static AbstractDataTreeNode getRootNode(DeltaDataTree node) {
		return node.getRootNode();
	}
}
