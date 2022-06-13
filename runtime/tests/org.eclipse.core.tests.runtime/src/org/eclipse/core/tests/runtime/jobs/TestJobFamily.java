/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.jobs;

public class TestJobFamily {
	public static final int TYPE_NONE = 0;
	public static final int TYPE_ONE = 1;
	public static final int TYPE_TWO = 2;
	public static final int TYPE_THREE = 3;
	public static final int TYPE_FOUR = 4;
	public static final int TYPE_FIVE = 5;

	private int type = TYPE_NONE;

	public TestJobFamily() {
		this(TYPE_NONE);
	}

	public TestJobFamily(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
