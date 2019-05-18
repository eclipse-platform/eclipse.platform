/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;

/**
 * Prompts the user for a multi-line comment for releasing to CVS.
 */
public class ReleaseCommentDialog extends DetailsDialog {
	
	CommitCommentArea commitCommentArea;
	//	dialogs settings that are persistent between workbench sessions
	private IDialogSettings settings;
	
	private static final int DEFAULT_WIDTH_IN_CHARS= 80;

	private static final String HEIGHT_KEY = "width-key"; //$NON-NLS-1$
	private static final String WIDTH_KEY = "height-key"; //$NON-NLS-1$
	
	/**
	 * ReleaseCommentDialog constructor.
	 * 
	 * @param parentShell  the parent of this dialog
	 * @param proposedComment
	 */
	public ReleaseCommentDialog(Shell parentShell, IResource[] resourcesToCommit, String proposedComment, int depth) {
		super(parentShell, CVSUIMessages.ReleaseCommentDialog_title); 
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE | SWT.MAX);
		commitCommentArea = new CommitCommentArea();
		// Get a project from which the commit template can be obtained
		if (resourcesToCommit.length > 0) 
		commitCommentArea.setProject(resourcesToCommit[0].getProject());
		commitCommentArea.setProposedComment(proposedComment);
		
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("ReleaseCommentDialog");//$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("ReleaseCommentDialog");//$NON-NLS-1$
		}		
	}
	
	@Override
	protected boolean includeDetailsButton() {
		return false;
	}
	
	@Override
	protected boolean includeErrorMessage() {
		return false;
	}
	
	@Override
	protected void createMainDialogArea(Composite parent) {
		getShell().setText(CVSUIMessages.ReleaseCommentDialog_title); 
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		commitCommentArea.createArea(composite);
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED)
					okPressed();
			}
		});
		
		Dialog.applyDialogFont(parent);
	}

	@Override
	protected String getHelpContextId() {
		return IHelpContextIds.RELEASE_COMMENT_DIALOG;
	}
	
	@Override
	protected Point getInitialSize() {
		try {
			return new Point(settings.getInt(WIDTH_KEY), settings.getInt(HEIGHT_KEY));
		} catch(NumberFormatException e) {
			final Point size= super.getInitialSize();
			size.x= convertWidthInCharsToPixels(DEFAULT_WIDTH_IN_CHARS);
			size.y += convertHeightInCharsToPixels(8);
			return size;
		}
	}
	
	public String getComment() {
		return commitCommentArea.getComment(true);
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		return null;
	}

	@Override
	protected void updateEnablements() {
	}
	
	@Override
	public boolean close() {
		Rectangle bounds = getShell().getBounds();
		settings.put(HEIGHT_KEY, bounds.height);
		settings.put(WIDTH_KEY, bounds.width);
		return super.close();
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control c =  super.createContents(parent);
		commitCommentArea.setFocus();
		return c;
	}
}
