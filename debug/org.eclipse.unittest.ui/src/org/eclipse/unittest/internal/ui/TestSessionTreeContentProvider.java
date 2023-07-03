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

package org.eclipse.unittest.internal.ui;

import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestSuiteElement;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A test session tree content provider
 */
public class TestSessionTreeContentProvider implements ITreeContentProvider {

	private static final Object[] NO_CHILDREN = new Object[0];

	@Override
	public void dispose() {
		// nothing to dispose
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TestSuiteElement) {
			return ((TestSuiteElement) parentElement).getChildren().toArray();
		} else {
			return NO_CHILDREN;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		TestRunnerViewPart part = (TestRunnerViewPart) inputElement;
		TestRunSession session = part.getCurrentTestRunSession();
		return new Object[] { session.getChildren().size() == 1 ? session.getChildren().get(0) : session };
	}

	@Override
	public Object getParent(Object element) {
		return ((TestElement) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TestSuiteElement) {
			return !((TestSuiteElement) element).getChildren().isEmpty();
		} else {
			return false;
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}
}
