/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.swt.widgets.Composite;
import org.junit.Before;
import org.junit.Test;

public class ContentMergeViewerTest  {
	private MyContentMergeViewer myContentMergeViewer;
	/**
	 * result[0]-event occurred or not; result[1]-new state that was set
	 */
	boolean[] result = new boolean[] { false, false };


	private class MyContentMergeViewer extends ContentMergeViewer {

		protected MyContentMergeViewer() {
			super(0, null, new CompareConfiguration());
		}

		public boolean leftDirty = false;
		public boolean rightDirty = false;

		@Override
		protected boolean isLeftDirty() {
			return leftDirty;
		}

		@Override
		protected boolean isRightDirty() {
			return rightDirty;
		}

		@Override
		protected void setLeftDirty(boolean dirty) {
			super.setLeftDirty(dirty);
		}

		@Override
		protected void setRightDirty(boolean dirty) {
			super.setRightDirty(dirty);
		}

		@Override
		protected void copy(boolean leftToRight) {
			// nothing here
		}

		@Override
		protected void createControls(Composite composite) {
			// nothing here
		}

		@Override
		protected byte[] getContents(boolean left) {
			return null;
		}

		@Override
		protected void handleResizeAncestor(int x, int y, int width, int height) {
			// nothing here
		}

		@Override
		protected void handleResizeLeftRight(int x, int y, int leftWidth,
				int centerWidth, int rightWidth, int height) {
			// nothing here
		}

		@Override
		protected void updateContent(Object ancestor, Object left, Object right) {
			// nothing here
		}
	}

	@Before
	public void setUp()  {
		result = new boolean[] { false, false };
		myContentMergeViewer = new MyContentMergeViewer();
		myContentMergeViewer.addPropertyChangeListener(event -> {
			result[0] = true;
			result[1] = ((Boolean) event.getNewValue()).booleanValue();
		});
	}

	// set left to true
	@Test
	public void testFFTX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(true);

		assertEquals(true, result[0]);
		assertEquals(true, result[1]);
	}

	@Test
	public void testFTTX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(true);

		assertEquals(true, result[0]);
		assertEquals(true, result[1]);
	}

	@Test
	public void testTFTX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(true);

		assertEquals(false, result[0]);
	}

	@Test
	public void testTTTX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(true);

		assertEquals(false, result[0]);
	}

	// set left to false
	@Test
	public void testFFFX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(false);

		assertEquals(false, result[0]);
	}

	@Test
	public void testFTFX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(false);

		assertEquals(false, result[0]);
	}

	@Test
	public void testTFFX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(false);

		assertEquals(true, result[0]);
		assertEquals(false, result[1]);
	}

	@Test
	public void testTTFX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(false);

		assertEquals(true, result[0]);
		assertEquals(false, result[1]);
	}

	// set right to true
	@Test
	public void testFFXT() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(true);

		assertEquals(true, result[0]);
		assertEquals(true, result[1]);
	}

	@Test
	public void testFTXT() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(true);

		assertEquals(false, result[0]);
	}

	@Test
	public void testTFXT() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(true);

		assertEquals(true, result[0]);
		assertEquals(true, result[1]);
	}

	@Test
	public void testTTXT() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(true);

		assertEquals(false, result[0]);
	}

	// set right to false
	@Test
	public void testFFXF() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(false);

		assertEquals(false, result[0]);
	}

	@Test
	public void testFTXF() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(false);

		assertEquals(true, result[0]);
		assertEquals(false, result[1]);
	}

	@Test
	public void testTFXF() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(false);

		assertEquals(false, result[0]);
	}

	@Test
	public void testTTXF() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(false);

		assertEquals(true, result[0]);
		assertEquals(false, result[1]);
	}
}
