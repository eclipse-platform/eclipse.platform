/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class AllUITests extends EclipseTest {

	public AllUITests() {
		super();
	}

	public AllUITests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(CheckoutOperationTests.suite());
		suite.addTest(CompareOperationTests.suite());
		suite.addTest(MiscOperationsTests.suite());
		suite.addTest(ProjectSetImporterTests.suite());

		//TODO Temporary switched off, see Bug 400540
//		suite.addTest(EditorTests.suite());

		suite.addTest(PatchWizardRadioButtonGroupTests.suite());
		suite.addTest(CVSProjectSetImportTest.suite());
		suite.addTest(CreatePatchTest.suite());
		suite.addTest(CVSHisoryTableProviderTest.suite());
		suite.addTest(PatchTreeTest.suite());
		suite.addTest(RepositoriesViewTests.suite());
		return suite;
	}

}
