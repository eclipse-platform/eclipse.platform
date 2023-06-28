/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.tests.breakpoint.TestBreakpoint;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;

/**
 * Model presentation to show the text attribute of {@link TestBreakpoint}s in
 * Breakpoint View.
 */
public class DebugTestModelPresentation extends LabelProvider implements IDebugModelPresentation {

	@Override
	public String getText(Object element) {
		if (element instanceof TestBreakpoint) {
			return "TestBreakpoint: " + ((TestBreakpoint) element).getText();
		}
		return super.getText(element);
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		return null;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		return null;
	}

	@Override
	public void setAttribute(String attribute, Object value) {
	}

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		listener.detailComputed(value, null);
	}

}