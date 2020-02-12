/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.junit.Test;

public class CompareFileRevisionEditorInputTest {
	@Test
	public void testPrepareCompareInputWithNonLocalResourceTypedElements()
			throws InvocationTargetException, InterruptedException {
		TestFriendlyCompareFileRevisionEditorInput input = new TestFriendlyCompareFileRevisionEditorInput(
				new DummyTypedElement(), new DummyTypedElement(), null);
		input.prepareCompareInput(null);
	}

	private static class DummyTypedElement implements ITypedElement {

		@Override
		public String getName() {
			return null;
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getType() {
			return null;
		}

	}

	private static class TestFriendlyCompareFileRevisionEditorInput extends CompareFileRevisionEditorInput {
		public TestFriendlyCompareFileRevisionEditorInput(ITypedElement left, ITypedElement right,
				IWorkbenchPage page) {
			super(left, right, page);
		}

		@Override
		public ICompareInput prepareCompareInput(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			return super.prepareCompareInput(monitor);
		}
	}

}
