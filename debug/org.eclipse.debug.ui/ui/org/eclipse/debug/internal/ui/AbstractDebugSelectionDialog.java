/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;

import java.util.List;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.osgi.framework.FrameworkUtil;

/**
 * This class provides the framework for a general selection dialog class.
 *
 * @see AbstractDebugListSelectionDialog
 * @see AbstractDebugCheckboxSelectionDialog
 *
 * @since 3.3
 */
public abstract class AbstractDebugSelectionDialog extends SelectionDialog {

	protected StructuredViewer fViewer = null;

	/**
	 * Constructor
	 * @param parentShell the parent shell
	 */
	public AbstractDebugSelectionDialog(Shell parentShell) {
		super(parentShell);
		setDialogBoundsSettings(getDialogBoundsSettings(), Dialog.DIALOG_PERSISTSIZE);
	}

	/**
	 * returns the dialog settings area id
	 * @return the id of the dialog settings area
	 */
	protected abstract String getDialogSettingsId();

	/**
	 * Returns the object to use as input for the viewer
	 * @return the object to use as input for the viewer
	 */
	protected abstract Object getViewerInput();

	/**
	 * Create and return a viewer to use in this dialog.
	 *
	 * @param parent the composite the viewer should be created in
	 * @return the viewer to use in the dialog
	 */
	protected abstract StructuredViewer createViewer(Composite parent);

	/**
	 * Returns if the dialog and/or current selection is/are valid.
	 * This method is polled when selection changes are made to update the enablement
	 * of the OK button by default
	 * @return true if the dialog is in a valid state, false otherwise
	 *
	 * @since 3.4
	 */
	protected abstract boolean isValid();

	/**
	 * Returns the content provider for the viewer
	 * @return the content provider for the viewer
	 */
	protected IContentProvider getContentProvider() {
		//by default return a simple array content provider
		return ArrayContentProvider.getInstance();
	}

	/**
	 * Returns the label provider used by the viewer
	 * @return the label provider used in the viewer
	 */
	protected IBaseLabelProvider getLabelProvider() {
		return new DefaultLabelProvider();
	}

	/**
	 * Returns the help context id for this dialog
	 * @return the help context id for this dialog
	 */
	abstract protected String getHelpContextId();

	/**
	 * This method allows listeners to be added to the viewer after it
	 * is created.
	 */
	/**
	 * This method allows listeners to be added to the viewer.  Called
	 * after the viewer has been created and its input set.
	 *
	 * @param viewer the viewer returned by createViewer()
	 */
	protected void addViewerListeners(StructuredViewer viewer){
		//do nothing by default
	}

	/**
	 * This method allows custom controls to be added before the viewer
	 * @param parent the parent composite to add these custom controls to
	 */
	protected void addCustomHeaderControls(Composite parent) {
		//do nothing by default
	}

	/**
	 * This method allows custom controls to be added after the viewer
	 * @param parent the parent composite to add these controls to
	 */
	protected void addCustomFooterControls(Composite parent) {
		//do nothing by default
	}

	/**
	 * This method allows the newly created controls to be initialized.
	 * This method is called only once all controls have been created from the
	 * <code>createContents</code> method.
	 *
	 * By default this method initializes the OK button control.
	 */
	protected void initializeControls() {
		getButton(IDialogConstants.OK_ID).setEnabled(isValid());
	}

	/**
	 * Returns the viewer used to display information in this dialog.
	 * Can be <code>null</code> if the viewer has not been created.
	 * @return viewer used in this dialog
	 */
	protected Viewer getViewer(){
		return fViewer;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = (Composite) super.createContents(parent);
		initializeControls();
		return comp;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
		Composite comp = (Composite) super.createDialogArea(parent);
		addCustomHeaderControls(comp);
		String label = getMessage();
		if(label != null && !IInternalDebugCoreConstants.EMPTY_STRING.equals(label)) {
			SWTFactory.createWrapLabel(comp, label, 1);
		}
		label = getViewerLabel();
		if(label != null && !IInternalDebugCoreConstants.EMPTY_STRING.equals(label)) {
			SWTFactory.createLabel(comp, label, 1);
		}
		fViewer = createViewer(comp);
		fViewer.setLabelProvider(getLabelProvider());
		fViewer.setContentProvider(getContentProvider());
		fViewer.setInput(getViewerInput());
		List<?> selectedElements = getInitialElementSelections();
		if (selectedElements != null && !selectedElements.isEmpty()){
			fViewer.setSelection(new StructuredSelection(selectedElements));
		}
		addViewerListeners(fViewer);
		addCustomFooterControls(comp);
		Dialog.applyDialogFont(comp);
		String help = getHelpContextId();
		if(help != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, help);
		}
		return comp;
	}

	/**
	 * This method returns the label describing what to do with the viewer. Typically this label
	 * will include the key accelerator to get to the viewer via the keyboard
	 * @return the label for the viewer
	 */
	abstract protected String getViewerLabel();

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(AbstractDebugSelectionDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsId());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsId());
		}
		return section;
	}
}
