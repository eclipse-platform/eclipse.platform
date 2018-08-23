/*******************************************************************************
 * Copyright (c) 2016 Conrad Groth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Conrad Groth - Bug 213780 - Compare With direction should be configurable
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.graphics.Image;

/**
 * This class just swaps the left and right input for display.
 * The model values for left and right are not changed.
 * We must extend from the class and not the interface, because some implementations expect the class.
 */
public class MirroredMergeViewerContentProvider extends MergeViewerContentProvider {
	private MergeViewerContentProvider delegate;

	public MirroredMergeViewerContentProvider(CompareConfiguration cc, MergeViewerContentProvider delegate) {
		super(cc);
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

	@Override
	public void setAncestorError(String errorMessage) {
		delegate.setAncestorError(errorMessage);
	}

	@Override
	public void setLeftError(String errorMessage) {
		delegate.setLeftError(errorMessage);
	}

	@Override
	public void setRightError(String errorMessage) {
		delegate.setRightError(errorMessage);
	}
}
