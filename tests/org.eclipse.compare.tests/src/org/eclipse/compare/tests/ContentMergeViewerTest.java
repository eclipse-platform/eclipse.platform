/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

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

		protected boolean isLeftDirty() {
			return leftDirty;
		}

		protected boolean isRightDirty() {
			return rightDirty;
		}

		protected void setLeftDirty(boolean dirty) {
			super.setLeftDirty(dirty);
		}

		protected void setRightDirty(boolean dirty) {
			super.setRightDirty(dirty);
		}

		protected void copy(boolean leftToRight) {
			// nothing here
		}

		protected void createControls(Composite composite) {
			// nothing here
		}

		protected byte[] getContents(boolean left) {
			return null;
		}

		protected void handleResizeAncestor(int x, int y, int width, int height) {
			// nothing here
		}

		protected void handleResizeLeftRight(int x, int y, int leftWidth,
				int centerWidth, int rightWidth, int height) {
			// nothing here
		}

		protected void updateContent(Object ancestor, Object left, Object right) {
			// nothing here
		}
	}

	protected void setUp() throws Exception {
		result = new boolean[] { false, false };
		myContentMergeViewer = new MyContentMergeViewer();
		myContentMergeViewer.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				result[0] = true;
				result[1] = ((Boolean) event.getNewValue()).booleanValue();
			}
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
