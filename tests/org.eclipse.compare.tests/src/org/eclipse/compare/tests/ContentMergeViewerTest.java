/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.swt.widgets.Composite;
import org.junit.Assert;

import junit.framework.TestCase;

public class ContentMergeViewerTest extends TestCase {
	private MyContentMergeViewer myContentMergeViewer;
	/**
	 * result[0]-event occurred or not; result[1]-new state that was set
	 */
	boolean[] result = new boolean[] { false, false };

	public ContentMergeViewerTest() {
		super();
	}

	public ContentMergeViewerTest(String name) {
		super(name);
	}

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

	@Override
	protected void setUp() throws Exception {
		result = new boolean[] { false, false };
		myContentMergeViewer = new MyContentMergeViewer();
		myContentMergeViewer.addPropertyChangeListener(event -> {
			result[0] = true;
			result[1] = ((Boolean) event.getNewValue()).booleanValue();
		});
	}

	// set left to true

	public void testFFTX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(true);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(true, result[1]);
	}

	public void testFTTX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(true);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(true, result[1]);
	}

	public void testTFTX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(true);

		Assert.assertEquals(false, result[0]);
	}

	public void testTTTX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(true);

		Assert.assertEquals(false, result[0]);
	}

	// set left to false

	public void testFFFX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(false);

		Assert.assertEquals(false, result[0]);
	}

	public void testFTFX() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(false);

		Assert.assertEquals(false, result[0]);
	}

	public void testTFFX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setLeftDirty(false);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(false, result[1]);
	}

	public void testTTFX() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setLeftDirty(false);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(false, result[1]);
	}

	// set right to true

	public void testFFXT() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(true);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(true, result[1]);
	}

	public void testFTXT() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(true);

		Assert.assertEquals(false, result[0]);
	}

	public void testTFXT() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(true);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(true, result[1]);
	}

	public void testTTXT() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(true);

		Assert.assertEquals(false, result[0]);
	}

	// set right to false

	public void testFFXF() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(false);

		Assert.assertEquals(false, result[0]);
	}

	public void testFTXF() {
		myContentMergeViewer.leftDirty = false;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(false);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(false, result[1]);
	}

	public void testTFXF() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = false;
		myContentMergeViewer.setRightDirty(false);

		Assert.assertEquals(false, result[0]);
	}

	public void testTTXF() {
		myContentMergeViewer.leftDirty = true;
		myContentMergeViewer.rightDirty = true;
		myContentMergeViewer.setRightDirty(false);

		Assert.assertEquals(true, result[0]);
		Assert.assertEquals(false, result[1]);
	}
}
