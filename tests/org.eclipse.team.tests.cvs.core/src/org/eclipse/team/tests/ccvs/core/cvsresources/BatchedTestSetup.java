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
package org.eclipse.team.tests.ccvs.core.cvsresources;


import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;

public class BatchedTestSetup extends TestSetup {
	private ISchedulingRule rule;

	public BatchedTestSetup(Test test) {
		super(test);
	}

	@Override
	public void setUp() throws CVSException {
		rule = EclipseSynchronizer.getInstance().beginBatching(ResourcesPlugin.getWorkspace().getRoot(), null);
	}
	
	@Override
	public void tearDown() throws CVSException {
		EclipseSynchronizer.getInstance().endBatching(rule, null);
	}
}
