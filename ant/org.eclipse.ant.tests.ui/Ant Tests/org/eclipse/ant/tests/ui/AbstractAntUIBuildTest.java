/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.junit.Rule;

public abstract class AbstractAntUIBuildTest extends AbstractAntUITest {

	@Rule
	public RunInSeparateThreadRule runInSeparateThread = new RunInSeparateThreadRule();

}
