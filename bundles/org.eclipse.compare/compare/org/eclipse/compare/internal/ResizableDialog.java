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

import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.DialogSettings;


/**
 * Base class for resizable Dialogs with persistent window bounds.
 */
public abstract class ResizableDialog extends Dialog {

	// dialog store id constants
	private final static String DIALOG_BOUNDS_KEY= "ResizableDialogBounds"; //$NON-NLS-1$
	private static final String X= "x"; //$NON-NLS-1$
	private static final String Y= "y"; //$NON-NLS-1$
	private static final String WIDTH= "width"; //$NON-NLS-1$
	private static final String HEIGHT= "height"; //$NON-NLS-1$
	
	protected ResourceBundle fBundle;
	private Rectangle fNewBounds;
	private IDialogSettings fSettings;
	private String fContextId;


	public ResizableDialog(Shell parent, ResourceBundle bundle) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		
		fBundle= bundle;
		
		fSettings= CompareUIPlugin.getDefault().getDialogSettings();
	}
	
	public void setHelpContextId(String contextId) {
		fContextId= contextId;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (fContextId != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, fContextId);
	}
	
	protected Point getInitialSize() {
		
		int width= 0;
		int height= 0;
		
		final Shell s= getShell();
		if (s != null) {
			s.addControlListener(
				new ControlListener() {
					public void controlMoved(ControlEvent arg0) {
						fNewBounds= s.getBounds();
					}
					public void controlResized(ControlEvent arg0) {
						fNewBounds= s.getBounds();
					}
				}
			);
		}
		
		IDialogSettings bounds= fSettings.getSection(DIALOG_BOUNDS_KEY);
		if (bounds == null) {
			if (fBundle != null) {
				width= Utilities.getInteger(fBundle, WIDTH, 0);
				height= Utilities.getInteger(fBundle, HEIGHT, 0);
				Shell shell= getParentShell();
				if (shell != null) {
					Point parentSize= shell.getSize();
					if (width <= 0)
						width= parentSize.x-300;
					if (height <= 0)
						height= parentSize.y-200;
				}
			} else {
				Shell shell= getParentShell();
				if (shell != null) {
					Point parentSize= shell.getSize();
					width= parentSize.x-100;
					height= parentSize.y-100;
				}
			}
			if (width < 700)
				width= 700;
			if (height < 500)
				height= 500;
		} else {
			try {
				width= bounds.getInt(WIDTH);
			} catch (NumberFormatException e) {
				width= 700;
			}
			try {
				height= bounds.getInt(HEIGHT);
			} catch (NumberFormatException e) {
				height= 500;
			}
		}	
	
		return new Point(width, height);
	}
	
	protected Point getInitialLocation(Point initialSize) {
		Point loc= super.getInitialLocation(initialSize);
		
		IDialogSettings bounds= fSettings.getSection(DIALOG_BOUNDS_KEY);
		if (bounds != null) {
			try {
				loc.x= bounds.getInt(X);
			} catch (NumberFormatException e) {
				// silently ignored
			}
			try {
				loc.y= bounds.getInt(Y);
			} catch (NumberFormatException e) {
				// silently ignored
			}
		}
		return loc;
	}
	
	public boolean close() {
		boolean closed= super.close();
		if (closed && fNewBounds != null)
			saveBounds(fNewBounds);
		return closed;
	}

	private void saveBounds(Rectangle bounds) {
		IDialogSettings dialogBounds= fSettings.getSection(DIALOG_BOUNDS_KEY);
		if (dialogBounds == null) {
			dialogBounds= new DialogSettings(DIALOG_BOUNDS_KEY);
			fSettings.addSection(dialogBounds);
		}
		dialogBounds.put(X, bounds.x);
		dialogBounds.put(Y, bounds.y);
		dialogBounds.put(WIDTH, bounds.width);
		dialogBounds.put(HEIGHT, bounds.height);
	}
}
