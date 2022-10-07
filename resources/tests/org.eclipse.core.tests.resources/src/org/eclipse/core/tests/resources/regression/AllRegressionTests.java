/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * A suite that runs all regression tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ Bug_006708.class, Bug_025457.class, Bug_026294.class, Bug_027271.class, Bug_028981.class,
		Bug_029116.class, Bug_029671.class, Bug_029851.class, Bug_032076.class, Bug_044106.class, Bug_092108.class,
		Bug_097608.class, Bug_098740.class, Bug_126104.class, Bug_127562.class, Bug_132510.class, Bug_134364.class,
		Bug_147232.class, Bug_160251.class, Bug_165892.class, Bug_192631.class, Bug_226264.class, Bug_231301.class,
		Bug_233939.class, Bug_265810.class, Bug_264182.class, Bug_288315.class, Bug_303517.class, Bug_329836.class,
		Bug_331445.class, Bug_332543.class, Bug_378156.class, IFileTest.class, IFolderTest.class, IProjectTest.class,
		IResourceTest.class, IWorkspaceTest.class, LocalStoreRegressionTests.class, NLTest.class,
		PR_1GEAB3C_Test.class,
		PR_1GH2B0N_Test.class, PR_1GHOM0N_Test.class, Bug_530868.class, Bug_185247_recursiveLinks.class,
		Bug_185247_LinuxTests.class })
public class AllRegressionTests {
}
