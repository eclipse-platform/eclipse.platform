/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui.editor.support;

import org.eclipse.ant.internal.ui.model.IProblem;
import org.eclipse.ant.internal.ui.model.IProblemRequestor;

public class TestProblemRequestor implements IProblemRequestor {

	@Override
	public void acceptProblem(IProblem problem) {
		// do nothing

	}

	@Override
	public void beginReporting() {
		// do nothing

	}

	@Override
	public void endReporting() {
		// do nothing
	}
}
