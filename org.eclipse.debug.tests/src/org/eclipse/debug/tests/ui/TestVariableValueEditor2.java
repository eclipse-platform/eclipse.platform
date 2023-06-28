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

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.actions.IVariableValueEditor;
import org.eclipse.swt.widgets.Shell;

public class TestVariableValueEditor2 implements IVariableValueEditor {

	@Override
	public boolean editVariable(IVariable variable, Shell shell) {
		return false;
	}

	@Override
	public boolean saveVariable(IVariable variable, String expression, Shell shell) {
		return false;
	}

}
