/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     Valentin Ciocoi - Bug 393700 - [Export breakpoints] Wrong file appears in overwrite dialog and wrong file can be overwritten
 *******************************************************************************/

package org.eclipse.debug.internal.ui.importexport.breakpoints;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsViewer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.ExportBreakpointsOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;

/**
 * <p>
 * This class provides an internal implementation of a WizardPage, which is used
 * in the Export Breakpoints wizard.
 * </p>
 * <p>
 * The implementation presents the breakpoints to the user as they are shown in
 * their current breakpoint view.
 * </p>
 * <p>
 * Possible extensions would include:
 * <ul>
 * <li> Able to change the views as in the breakpoints view itself
 * <li> Able to reorder groups from within the wizard - easier in the viewer itself though
 * </ul>
 * </p>
 * This class is used by <code>WizardExportBreakpoints</code>
 *
 * @since 3.2
 */
public class WizardExportBreakpointsPage extends WizardPage implements Listener {

	// widgets
	private Button fOverwriteExistingFilesCheckbox = null;
	private Text fDestinationNameField = null;
	private Button fDestinationBrowseButton = null;
	private EmbeddedBreakpointsViewer fTView = null;
	private IStructuredSelection fSelection = null;
	private Button fSelectAll = null;
	private Button fDeselectAll = null;

	//state constants
	private static final String OVERWRITE_ALL_STATE = "overwrite"; //$NON-NLS-1$
	private static final String DESTINATION_FILE_NAME = "filename"; //$NON-NLS-1$

	/**
	 * This is the default constructor. It accepts the name for the tab as a
	 * parameter and an existing selection
	 *
	 * @param pageName the name of the page
	 */
	public WizardExportBreakpointsPage(String pageName, IStructuredSelection selection) {
		super(pageName, ImportExportMessages.WizardExportBreakpoints_0, null);
		fSelection = selection;
	}

	@Override
	public void handleEvent(Event event) {
		Widget source = event.widget;
		if (source == fDestinationBrowseButton) {
			handleDestinationBrowseButtonPressed();
		}
		else if (source == fDestinationNameField) {
			handlePathTextModifiedEvent();
		}
		else if(source == fSelectAll) {
			handleSelectAllPressed();
		}
		else if(source == fDeselectAll) {
			handleDeselectAllPressed();
		}
	}

	/**
	 * Handles the select all button pressed
	 *
	 */
	private void handleSelectAllPressed() {
		BreakpointsViewer viewer = fTView.getViewer();
		viewer.getTree().selectAll();
		viewer.setCheckedElements(viewer.getStructuredSelection().toArray());
		viewer.setGrayedElements(new Object[] {});
		viewer.getTree().deselectAll();
		setPageComplete(detectPageComplete());
	}

	/**
	 * Handles the de-select all button pressed
	 *
	 */
	private void handleDeselectAllPressed() {
		BreakpointsViewer viewer = fTView.getViewer();
		viewer.setCheckedElements(new Object[] {});
		viewer.setGrayedElements(new Object[] {});
		setPageComplete(detectPageComplete());
	}

	/**
	 * This method handles the modified event from the path combo box.
	 */
	protected void handlePathTextModifiedEvent() {
		setPageComplete(detectPageComplete());
	}

	/**
	 * Open the SaveAsDialog so the user can save the listing of selected breakpoints
	 */
	protected void handleDestinationBrowseButtonPressed() {
		FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE | SWT.SHEET);
		dialog.setFilterExtensions(new String[]{"*."+IImportExportConstants.EXTENSION});  //$NON-NLS-1$
		dialog.setText(ImportExportMessages.WizardExportBreakpoints_0);
		String file = dialog.open();
		if(file != null) {
			IPath path = IPath.fromOSString(file);
			if (path != null) {
				setErrorMessage(null);
				if(path.getFileExtension() == null) {
					path = path.addFileExtension(IImportExportConstants.EXTENSION);
				}
				else if(!path.getFileExtension().equals(IImportExportConstants.EXTENSION)) {
					path = path.addFileExtension(IImportExportConstants.EXTENSION);
				}
				fDestinationNameField.setText(path.toString());
			}
		}
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(composite, ImportExportMessages.WizardExportBreakpointsPage_2, 1);
		fTView = new EmbeddedBreakpointsViewer(composite, DebugPlugin.getDefault().getBreakpointManager(), fSelection);
		fTView.getViewer().addCheckStateListener(event -> setPageComplete(detectPageComplete()));
		fTView.getViewer().setSelection(fSelection);
		//ensure we can see the beginning check-boxes etc. (bug 180971)
		//this will not work in Windows Vista as there is no way to over-ride the default viewer item showing policy
		//by setting the horizontal bar selection index. I.e. the following line of code is ignored in Vista
		fTView.getViewer().getTree().getHorizontalBar().setSelection(0);
		createButtonsGroup(composite);
		createDestinationGroup(composite);
		fOverwriteExistingFilesCheckbox = SWTFactory.createCheckButton(composite, ImportExportMessages.WizardBreakpointsPage_6, null, false, 1);
		setControl(composite);
		setPageComplete(false);
		setMessage(ImportExportMessages.WizardBreakpointsPage_4);
		restoreWidgetState();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.EXPORT_BREAKPOINTS_WIZARD_PAGE);

		Dialog.applyDialogFont(parent);
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_WIZBAN_EXPORT_BREAKPOINTS);
	}

	/**
	 * Creates the buttons for selecting all or none of the elements.
	 *
	 * @param parent the parent control
	 */
	private void createButtonsGroup(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, parent.getFont(), 3, 1, GridData.FILL_HORIZONTAL, 0, 0);
		fSelectAll = SWTFactory.createPushButton(composite, ImportExportMessages.WizardBreakpointsPage_1, null);
		fSelectAll.addListener(SWT.Selection, this);
		fDeselectAll = SWTFactory.createPushButton(composite, ImportExportMessages.WizardBreakpointsPage_2, null);
		fDeselectAll.addListener(SWT.Selection, this);
	}

	/**
	 * This method is used to determine if the page can be "finished".
	 *
	 * To be determined "finishable" there must be a save path and there must be
	 * a selection in the tree.
	 *
	 * @return if the prerequisites of the wizard are met to allow the wizard to complete.
	 */
	private boolean detectPageComplete() {
		String filepath = fDestinationNameField.getText().trim();
		if (filepath.equals(IInternalDebugCoreConstants.EMPTY_STRING)) {
			setErrorMessage(ImportExportMessages.WizardExportBreakpointsPage_0);
			return false;
		}
		IPath path = IPath.fromOSString(filepath);
		if(!path.removeLastSegments(1).toFile().exists()) {
			setErrorMessage(ImportExportMessages.WizardExportBreakpointsPage_3);
			return false;
		}
		int size = fTView.getCheckedElements().size();
		if (size == 0) {
			setErrorMessage(ImportExportMessages.WizardExportBreakpointsPage_1);
			return false;
		}
		setErrorMessage(null);
		setMessage(ImportExportMessages.WizardBreakpointsPage_4);
		return true;
	}

	/**
	 * Create the export destination specification widgets
	 *
	 * @param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createDestinationGroup(Composite parent) {
		// destination specification group
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 3, 1, GridData.FILL_HORIZONTAL, 0, 10);
		SWTFactory.createLabel(comp, ImportExportMessages.WizardBreakpointsPage_7, 1);

		fDestinationNameField = SWTFactory.createText(comp, SWT.SINGLE | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		fDestinationNameField.addListener(SWT.Modify, this);
		fDestinationBrowseButton = SWTFactory.createPushButton(comp, ImportExportMessages.WizardBreakpointsPage_8, null);
		fDestinationBrowseButton.addListener(SWT.Selection, this);
	}

	/**
	 * Save the state of the widgets select, for successive invocations of the wizard
	 */
	private void saveWidgetState() {
		IDialogSettings settings = getDialogSettings();
		if(settings != null) {
			settings.put(OVERWRITE_ALL_STATE, fOverwriteExistingFilesCheckbox.getSelection());
			settings.put(DESTINATION_FILE_NAME, fDestinationNameField.getText().trim());
		}
	}

	/**
	 * Restores the state of the wizard from previous invocations
	 */
	private void restoreWidgetState() {
		IDialogSettings settings = getDialogSettings();
		if(settings != null) {
			fOverwriteExistingFilesCheckbox.setSelection(Boolean.parseBoolean(settings.get(OVERWRITE_ALL_STATE)));
			String filename = settings.get(DESTINATION_FILE_NAME);
			if (filename != null) {
				fDestinationNameField.setText(filename);
			}
		}
	}

	/**
	 * The Finish button is clicked on the main wizard
	 * dialog to export the breakpoints, we write them out with all persistent
	 * information to a simple XML file via the use of XMLMemento.
	 *
	 * @return if the save operation was successful or not
	 */
	public boolean finish() {
		try {
			IPath path = IPath.fromOSString(fDestinationNameField.getText().trim());
			if(path.getFileExtension() == null) {
				path = path.addFileExtension(IImportExportConstants.EXTENSION);
			}
			else if(!path.getFileExtension().equals(IImportExportConstants.EXTENSION)) {
				path = path.addFileExtension(IImportExportConstants.EXTENSION);
			}
			saveWidgetState();
			if(path.toFile().exists() && !fOverwriteExistingFilesCheckbox.getSelection()) {
				if (!MessageDialog.openQuestion(null, ImportExportMessages.WizardBreakpointsPage_12, MessageFormat.format(ImportExportMessages.ImportExportOperations_0, new Object[] { path.toPortableString() }))) {
					return false;
				}
			}
			// collect breakpoints
			Object[] elements = fTView.getCheckedElements().toArray();
			List<IBreakpoint> breakpoints = new ArrayList<>();
			for (Object object : elements) {
				if (object instanceof IBreakpoint) {
					breakpoints.add((IBreakpoint) object);
				}
			}
			getContainer().run(false,
					true,
					new ExportBreakpointsOperation(
							breakpoints.toArray(new IBreakpoint[breakpoints.size()]),
							path.toOSString()));
		}
		catch (InterruptedException e) {
			DebugPlugin.log(e);
			return false;
		}
		catch (InvocationTargetException e) {
			DebugPlugin.log(e);
			return false;
		}
		return true;
	}
}
