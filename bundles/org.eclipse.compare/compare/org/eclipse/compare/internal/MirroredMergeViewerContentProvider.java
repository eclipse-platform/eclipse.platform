/*******************************************************************************
 * Copyright (c) 2016 Conrad Groth and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Conrad Groth - Bug 213780 - Compare With direction should be configurable
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;
import org.eclipse.swt.graphics.Image;

/**
 * This class just swaps the left and right input for display.
 * The model values for left and right are not changed.
 */
public class MirroredMergeViewerContentProvider implements IMergeViewerContentProvider {

	private IMergeViewerContentProvider delegate;

	public MirroredMergeViewerContentProvider(IMergeViewerContentProvider delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getLeftLabel(Object input) {
		return delegate.getRightLabel(input);
	}

	@Override
	public Image getLeftImage(Object input) {
		return delegate.getRightImage(input);
	}

	@Override
	public Object getLeftContent(Object input) {
		return delegate.getRightContent(input);
	}

	@Override
	public boolean isLeftEditable(Object input) {
		return delegate.isRightEditable(input);
	}

	@Override
	public void saveLeftContent(Object input, byte[] bytes) {
		delegate.saveRightContent(input, bytes);
	}

	@Override
	public String getRightLabel(Object input) {
		return delegate.getLeftLabel(input);
	}

	@Override
	public Image getRightImage(Object input) {
		return delegate.getLeftImage(input);
	}

	@Override
	public Object getRightContent(Object input) {
		return delegate.getLeftContent(input);
	}

	@Override
	public boolean isRightEditable(Object input) {
		return delegate.isLeftEditable(input);
	}

	@Override
	public void saveRightContent(Object input, byte[] bytes) {
		delegate.saveLeftContent(input, bytes);
	}

	@Override
	public String getAncestorLabel(Object input) {
		return delegate.getAncestorLabel(input);
	}

	@Override
	public Image getAncestorImage(Object input) {
		return delegate.getAncestorImage(input);
	}

	@Override
	public Object getAncestorContent(Object input) {
		return delegate.getAncestorContent(input);
	}

	@Override
	public boolean showAncestor(Object input) {
		return delegate.showAncestor(input);
	}
}
