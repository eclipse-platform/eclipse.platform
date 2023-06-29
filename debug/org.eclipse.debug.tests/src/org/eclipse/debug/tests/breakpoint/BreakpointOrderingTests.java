/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.breakpoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsComparator;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.junit.Test;

/**
 * Test the ordering used in the breakpoints view.
 *
 * Using a special Comparator which sorts breakpoint texts like file:1, file:2 and file:11 in a numerical ordering.
 */
public class BreakpointOrderingTests extends AbstractDebugTest {

	/**
	 * Test only implementation of ILineBreakpoint.
	 */
	static class TestLineBreakpoint extends TestBreakpoint implements ILineBreakpoint {
		private final int fLineNum;

		TestLineBreakpoint(String text, int lineNum) {
			super(text, IBreakpoint.LINE_BREAKPOINT_MARKER);
			fLineNum = lineNum;
		}

		@Override
		public int getLineNumber() throws CoreException {
			return fLineNum;
		}

		@Override
		public int getCharEnd() throws CoreException {
			fail("not implemented in test"); //$NON-NLS-1$
			return 0;
		}

		@Override
		public int getCharStart() throws CoreException {
			fail("not implemented in test"); //$NON-NLS-1$
			return 0;
		}
	}

	/**
	 * Test only implementation of StructuredViewer.
	 */
	StructuredViewer fTestViewer = new StructuredViewer() {

		@Override
		public IBaseLabelProvider getLabelProvider() {
			return fDebugModelPres;
		}

		@Override
		protected Widget doFindInputItem(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}

		@Override
		protected Widget doFindItem(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}

		@Override
		protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		protected List getSelectionFromWidget() {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}

		@Override
		protected void internalRefresh(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public void reveal(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		protected void setSelectionToWidget(List l, boolean reveal) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public Control getControl() {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}};

	// Test debug model presentation for label text retrieval.
	IDebugModelPresentation fDebugModelPres = new IDebugModelPresentation() {

		@Override
		public void computeDetail(IValue value, IValueDetailListener listener) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public Image getImage(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}

		@Override
		public String getText(Object element) {
			assertTrue("Unexpected element", element instanceof TestBreakpoint); //$NON-NLS-1$
			return ((TestBreakpoint)element).getText();
		}

		@Override
		public void setAttribute(String attribute, Object value) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public void dispose() {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			fail("not implemented in test"); //$NON-NLS-1$
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			fail("not implemented in test"); //$NON-NLS-1$
		}

		@Override
		public String getEditorId(IEditorInput input, Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}

		@Override
		public IEditorInput getEditorInput(Object element) {
			fail("not implemented in test"); //$NON-NLS-1$
			return null;
		}};

	// Test vector with some UNIX paths
	TestBreakpoint[] createTestBreakpoints0() {
		TestBreakpoint[] fTestBps = {
 new TestBreakpoint(""), //$NON-NLS-1$
		new TestBreakpoint("/file/text.c"), //$NON-NLS-1$
		new TestBreakpoint("/file/text.c:1"), //$NON-NLS-1$
		new TestLineBreakpoint("", 0), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c", 0), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c", 1), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:", 0), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:0", 0), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:1", 1), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:0002", 2), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:3xxx", 3), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:10xxx", 10), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_01", 1), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_01a", 1), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_09", 9), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_09a", 9), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_011", 11), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:a_011a", 11), //$NON-NLS-1$
		new TestLineBreakpoint("/file/text.c:y", 0), //$NON-NLS-1$
		};
		return fTestBps;
	}

	// Check plain numbers
	TestBreakpoint[] createTestBreakpoints1() {
		TestBreakpoint[] fTestBps = {
 new TestLineBreakpoint("0", 0), //$NON-NLS-1$
		new TestLineBreakpoint("1", 1), //$NON-NLS-1$
		new TestLineBreakpoint("1_a", 1), //$NON-NLS-1$
		new TestLineBreakpoint("001_b", 1), //$NON-NLS-1$
		new TestLineBreakpoint("01_c", 1), //$NON-NLS-1$
		new TestLineBreakpoint("3", 3), //$NON-NLS-1$
		new TestLineBreakpoint("10", 10), //$NON-NLS-1$
		new TestLineBreakpoint("11", 11), //$NON-NLS-1$
		new TestLineBreakpoint("20", 20), //$NON-NLS-1$
		new TestLineBreakpoint("110", 110), //$NON-NLS-1$
		new TestLineBreakpoint("112", 112), //$NON-NLS-1$
		new TestLineBreakpoint("112a", 112), //$NON-NLS-1$
		new TestLineBreakpoint("112b", 112), //$NON-NLS-1$
		};
		return fTestBps;
	}

	// Test consistent behavior with leading 0's
	TestBreakpoint[] createTestBreakpoints2() {
		TestBreakpoint[] fTestBps = {
 new TestLineBreakpoint("0", 0), //$NON-NLS-1$
		new TestLineBreakpoint("00", 0), //$NON-NLS-1$
		new TestLineBreakpoint("0000", 0), //$NON-NLS-1$
		new TestLineBreakpoint("0001", 1), //$NON-NLS-1$
		new TestLineBreakpoint("0010", 10), //$NON-NLS-1$
		new TestLineBreakpoint("1000", 1000), //$NON-NLS-1$
		new TestLineBreakpoint("10000", 10000), //$NON-NLS-1$
		};
		return fTestBps;
	}

	// Test Win32 paths
	TestBreakpoint[] createTestBreakpoints3() {
		TestBreakpoint[] fTestBps = {
 new TestLineBreakpoint(":a", 0), //$NON-NLS-1$
		new TestLineBreakpoint("c:\\file\\text.c:1", 1), //$NON-NLS-1$
		new TestLineBreakpoint("c:\\file\\text.c:2", 2), //$NON-NLS-1$
		new TestLineBreakpoint("d:\\file\\text.c:3", 3), //$NON-NLS-1$
		};
		return fTestBps;
	}

	@Test
	public void testBreakpointOrdering0() throws CoreException {
		executeTest(createTestBreakpoints0());
	}

	@Test
	public void testBreakpointOrdering1() throws CoreException {
		executeTest(createTestBreakpoints1());
	}

	@Test
	public void testBreakpointOrdering2() throws CoreException {
		executeTest(createTestBreakpoints2());
	}

	@Test
	public void testBreakpointOrdering3() throws CoreException {
		executeTest(createTestBreakpoints3());
	}


	/**
	 * Test expected ordering.
	 * Expecting the same ordering as in which the BP's are returned by createTestBreakpoints.
	 */
	void executeTest(TestBreakpoint[] testBps) throws CoreException {
		BreakpointsComparator bpCompare = new BreakpointsComparator();
		try {
			boolean failed = false;
			for (int inner = 0; inner < testBps.length; inner++) {
				TestBreakpoint testInner = testBps[inner];
				for (int outer = 0; outer < testBps.length; outer++) {
					TestBreakpoint testOuter = testBps[outer];
					int res = bpCompare.compare(fTestViewer, testInner, testOuter);
					boolean equalCheck = (res == 0) == (inner == outer);
					boolean ltCheck = (res < 0) == (inner < outer);
					if (!equalCheck) {
						System.err.println("Equal Comparison in between " + inner + " and " + outer + " (" + testBps[inner].getText() + " and " + testBps[outer].getText() + ") failed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						failed = true;
					}
					if (!ltCheck) {
						System.err.println("Less Comparison in between " + inner + " and " + outer + " (" + testBps[inner].getText() + " and " + testBps[outer].getText() + ") failed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						failed = true;
					}
				}
			}
			assertFalse(failed);
		} finally {
			for (TestBreakpoint testBp : testBps) {
				testBp.delete();
			}
		}
	}
}