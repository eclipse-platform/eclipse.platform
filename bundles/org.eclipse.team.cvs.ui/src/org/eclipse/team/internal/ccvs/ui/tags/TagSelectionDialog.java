/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;

/**
 * Dialog to prompt the user to choose a tag for a selected resource
 */
public class TagSelectionDialog extends TrayDialog implements IPropertyChangeListener {
	
	private TagSelectionArea tagSelectionArea;
	
	public static final int INCLUDE_HEAD_TAG = TagSourceWorkbenchAdapter.INCLUDE_HEAD_TAG;
	public static final int INCLUDE_BASE_TAG = TagSourceWorkbenchAdapter.INCLUDE_BASE_TAG;
	public static final int INCLUDE_BRANCHES = TagSourceWorkbenchAdapter.INCLUDE_BRANCHES;
	public static final int INCLUDE_VERSIONS = TagSourceWorkbenchAdapter.INCLUDE_VERSIONS;
	public static final int INCLUDE_DATES = TagSourceWorkbenchAdapter.INCLUDE_DATES;
	public static final int INCLUDE_ALL_TAGS = TagSourceWorkbenchAdapter.INCLUDE_ALL_TAGS;
	
	private Button okButton;
	
	// dialog title, should indicate the action in which the tag selection
	// dialog is being shown
	private String title;
	
	private boolean recurse = true;
	
	// constants
	private static final int SIZING_DIALOG_WIDTH = 90;
	private static final int SIZING_DIALOG_HEIGHT = 25;

	private CVSTag selection;

	private TagSource tagSource;

	private String message;

	private int includeFlags;

	private String helpContext;

	private boolean showRecurse;
		
	public static CVSTag getTagToCompareWith(Shell shell, TagSource tagSource, int includeFlags) {
		TagSelectionDialog dialog = new TagSelectionDialog(shell, tagSource, 
			CVSUIMessages.CompareWithTagAction_message,  
			CVSUIMessages.TagSelectionDialog_Select_a_Tag_1, 
			includeFlags, 
			false, /* show recurse*/
			IHelpContextIds.COMPARE_TAG_SELECTION_DIALOG);
		dialog.setBlockOnOpen(true);
		int result = dialog.open();
		if (result == Window.CANCEL) {
			return null;
		}
		return dialog.getResult();
	}
	
	/**
	 * Creates a new TagSelectionDialog.
	 * @param resource The resource to select a version for.
	 */
	public TagSelectionDialog(Shell parentShell, TagSource tagSource, String title, String message, int includeFlags, final boolean showRecurse, String helpContext) {
		super(parentShell);
		
		// Create a tag selection area with a custom recurse option
		this.tagSource = tagSource;
		this.message = message;
		this.includeFlags = includeFlags;
		this.helpContext = helpContext;
		this.showRecurse = showRecurse;
		this.title = title;
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	protected Point getInitialSize() {
		final Point size= super.getInitialSize();
		size.x= convertWidthInCharsToPixels(SIZING_DIALOG_WIDTH);
		size.y= convertHeightInCharsToPixels(SIZING_DIALOG_HEIGHT);
		return size;
	}
	
	/**
	 * Creates this window's widgetry.
	 * <p>
	 * The default implementation of this framework method
	 * creates this window's shell (by calling <code>createShell</code>),
	 * its control (by calling <code>createContents</code>),
	 * and initializes this window's shell bounds 
	 * (by calling <code>initializeBounds</code>).
	 * This framework method may be overridden; however,
	 * <code>super.create</code> must be called.
	 * </p>
	 */
	public void create() {
		super.create();
		initialize();
	}
	
	/**
	 * Add buttons to the dialog's button bar.
	 *
	 * @param parent the button bar composite
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/**
	 * Creates and returns the contents of the upper part 
	 * of this dialog (above the button bar).
	 * <p>
	 * The default implementation of this framework method
	 * creates and returns a new <code>Composite</code> with
	 * standard margins and spacing.
	 * Subclasses should override.
	 * </p>
	 *
	 * @param parent the parent composite to contain the dialog area
	 * @return the dialog area control
	 */
	protected Control createDialogArea(Composite parent) {
		
		applyDialogFont(parent);
		initializeDialogUnits(parent);
		
		final Composite top = (Composite)super.createDialogArea(parent);
		
		// Delegate most of the dialog to the tag selection area
		tagSelectionArea = new TagSelectionArea(getShell(), tagSource, includeFlags, helpContext) {
			protected void createCustomArea(Composite parent) {
				if(showRecurse) {
					final Button recurseCheck = new Button(parent, SWT.CHECK);
					recurseCheck.setText(CVSUIMessages.TagSelectionDialog_recurseOption); 
					recurseCheck.addListener(SWT.Selection, event -> recurse = recurseCheck.getSelection());
					recurseCheck.setSelection(true);
				}
			}
		};
		if (message != null)
			tagSelectionArea.setTagAreaLabel(message);
		tagSelectionArea.addPropertyChangeListener(this);
		tagSelectionArea.createArea(top);
		tagSelectionArea.setRunnableContext(getRunnableContext());
		
		// Create a separator between the tag area and the button area
		final Label seperator = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		final GridData data = new GridData (GridData.FILL_HORIZONTAL);		
		data.horizontalSpan = 2;
		seperator.setLayoutData(data);
		
		updateEnablement();
		applyDialogFont(parent);
		
		return top;
	}
	
	
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	protected Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	
	/**
	 * Returns the selected tag.
	 */
	public CVSTag getResult() {
		return selection;
	}
	
	public boolean getRecursive() {
		return recurse;
	}

	/**
	 * Initializes the dialog contents.
	 */
	protected void initialize() {
		okButton.setEnabled(false);
		if (CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_AUTO_REFRESH_TAGS_IN_TAG_SELECTION_DIALOG))
			tagSelectionArea.refreshTagList();
	}

	
	/**
	 * Updates the dialog enablement.
	 */
	protected void updateEnablement() {
		if(okButton!=null) {
			okButton.setEnabled(selection != null);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(TagSelectionArea.SELECTED_TAG)) {
			selection = (CVSTag)event.getNewValue();
			updateEnablement();
		} else if (property.equals(TagSelectionArea.OPEN_SELECTED_TAG)) {
			okPressed();
		}
	}
	
	/**
	 * Creates a runnable context that allows refreshing the tags in the background. 
	 * 
	 * @since 3.1
	 */
	private IRunnableContext getRunnableContext() {
		return (fork, cancelable, runnable) -> {
			final Job refreshJob = new Job(CVSUIMessages.TagSelectionDialog_7) { 
				protected IStatus run(IProgressMonitor monitor) {
						if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					try {
						setBusy(true);
						runnable.run(monitor);
					} catch (InvocationTargetException e) {
						return new CVSStatus(IStatus.ERROR, CVSUIMessages.TagSelectionDialog_8, e); 
					} catch (InterruptedException e) {
						return new CVSStatus(IStatus.ERROR, CVSUIMessages.TagSelectionDialog_8, e); 
					} finally {
						setBusy(false);
					}
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					else
						return Status.OK_STATUS;
				}
			};
			refreshJob.setUser(false);
			refreshJob.setPriority(Job.DECORATE);
			getShell().addDisposeListener(e -> refreshJob.cancel());
			refreshJob.schedule();
		};
	}
	
	private void setBusy(final boolean busy) {
		final Shell shell = getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(() -> {
				if (!shell.isDisposed()) {
					Cursor cursor = null;
					if (busy) {
						cursor = shell.getDisplay().getSystemCursor(SWT.CURSOR_APPSTARTING);
					}
					shell.setCursor(cursor);
				}
			});
		}
	}
}
