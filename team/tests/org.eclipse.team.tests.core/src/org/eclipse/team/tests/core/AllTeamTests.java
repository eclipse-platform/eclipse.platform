/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.tests.core;

import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.team.tests.core.regression.AllTeamRegressionTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ //
		AllTeamRegressionTests.class, //
		RepositoryProviderTests.class, //
		StorageMergerTests.class, //
		StreamTests.class, //
		UserMappingTest.class, //
})
public class AllTeamTests extends ResourceTest {
}
