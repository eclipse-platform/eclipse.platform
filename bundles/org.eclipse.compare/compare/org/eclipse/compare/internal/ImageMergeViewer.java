/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.InputStream;
import java.io.IOException;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;

/**
 */
public class ImageMergeViewer extends ContentMergeViewer {
	
	private static final String BUNDLE_NAME= "org.eclipse.compare.internal.ImageMergeViewerResources"; //$NON-NLS-1$
		
	private Object fLeftImage;
	private Object fRightImage;

	private ImageCanvas fAncestor;
	private ImageCanvas fLeft;
	private ImageCanvas fRight;
	
			
	public ImageMergeViewer(Composite parent, int styles, CompareConfiguration mp) {
		super(styles, ResourceBundle.getBundle(BUNDLE_NAME), mp);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ICompareContextIds.IMAGE_COMPARE_VIEW);

		buildControl(parent);
		String title= Utilities.getString(getResourceBundle(), "title"); //$NON-NLS-1$
		getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, title);
	}

	protected void updateContent(Object ancestor, Object left, Object right) {
		
		setInput(fAncestor, ancestor);
		
		fLeftImage= left;
		setInput(fLeft, left);
		
		fRightImage= right;
		setInput(fRight, right);
	}
	
	/*
	 * We can't modify the contents of either side we just return null.
	 */
	protected byte[] getContents(boolean left) {
		return null;
	}
	
	public void createControls(Composite composite) {
		fAncestor= new ImageCanvas(composite, SWT.NO_FOCUS);
		fLeft= new ImageCanvas(composite, SWT.NO_FOCUS);
		fRight= new ImageCanvas(composite, SWT.NO_FOCUS);
	}

	private static void setInput(ImageCanvas canvas, Object input) {
		if (canvas != null) {

			InputStream stream= null;
			if (input instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) input;
				if (sca != null) {
					try {
						stream= sca.getContents();
					} catch (CoreException ex) {
						// NeedWork
					}
				}
			}
			
			Image image= null;			
			Display display= canvas.getDisplay();
			if (stream != null) {
				try {
					image= new Image(display, stream);
				} catch (SWTException ex) {
					// silently ignored
				}
			}

			canvas.setImage(image);
			if (image != null) {
				canvas.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			} else {
				canvas.setBackground(null);
			}
			
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
	}
	
	protected void handleResizeAncestor(int x, int y, int width, int height) {
		if (width > 0) {
			fAncestor.setVisible(true);
			fAncestor.setBounds(x, y, width, height);
		} else {
			fAncestor.setVisible(false);
		}
	}

	protected void handleResizeLeftRight(int x, int y, int width1, int centerWidth, int width2, int height) {
		fLeft.setBounds(x, y, width1, height);
		fRight.setBounds(x+width1+centerWidth, y, width2, height);
	}
	
	protected void copy(boolean leftToRight) {
		if (leftToRight) {
			fRightImage= fLeftImage;
			setInput(fRight, fRightImage);
			setRightDirty(true);
		} else {
			fLeftImage= fRightImage;
			setInput(fLeft, fLeftImage);
			setLeftDirty(true);
		}
	}
}

