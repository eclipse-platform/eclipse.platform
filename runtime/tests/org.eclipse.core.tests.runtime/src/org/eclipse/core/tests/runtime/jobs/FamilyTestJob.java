/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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

import org.eclipse.core.tests.harness.TestJob;

/**
 * A test job that belongs to a particular family.
 */
class FamilyTestJob extends TestJob {
	private int familyType = TestJobFamily.TYPE_NONE;

	public FamilyTestJob(String name, int type) {
		super(name);
		familyType = type;
	}

	public FamilyTestJob(String name, int ticks, int tickLength, int type) {
		super(name, ticks, tickLength);
		familyType = type;
	}

	@Override
	public boolean belongsTo(Object family) {
		return ((family instanceof TestJobFamily) && (((TestJobFamily) family).getType() == familyType));
	}

}
