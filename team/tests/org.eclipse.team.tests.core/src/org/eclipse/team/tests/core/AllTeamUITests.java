/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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

import org.eclipse.team.tests.core.mapping.AllTeamMappingTests;
import org.eclipse.team.tests.ui.SaveableCompareEditorInputTest;
import org.eclipse.team.tests.ui.synchronize.AllTeamSynchronizeTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ //
		AllTeamMappingTests.class, //
		AllTeamSynchronizeTests.class, //
		SaveableCompareEditorInputTest.class, //
})
public class AllTeamUITests {
}
