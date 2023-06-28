/*******************************************************************************
 * Copyright (c) 2021 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov (loskutov@gmx.de) - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.ui;

import static org.junit.Assert.assertEquals;

import org.eclipse.debug.internal.ui.VariableValueEditorManager;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.ui.actions.IVariableValueEditor;
import org.junit.Test;

/**
 * Tests status handlers
 */
public class VariableValueEditorManagerTests extends AbstractDebugTest {

	@Test
	public void testHighestPriorityEditorUsed() {
		IVariableValueEditor editor = VariableValueEditorManager.getDefault().getVariableValueEditor("testModel");
		assertEquals("Not the editor with highest priority used by VariableValueEditorManager", TestVariableValueEditor2.class, editor.getClass());
	}



}
