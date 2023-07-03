/*******************************************************************************
 * Copyright (c) 2012, 2020 Sebastian Schmidt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sebastian Schmidt - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.importexport.breakpoints;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.BreakpointManager;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsViewer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.ImportBreakpointsOperation;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

/**
 * @since 3.8
 * @noextend This class is not intended to be subclassed by clients.
 */
public class WizardImportBreakpointsSelectionPage extends WizardPage implements Listener {

	private EmbeddedBreakpointsViewer fTView;
	private boolean fIsVisible;
	private Button fSelectAll = null;
	private Button fDeselectAll = null;

	private ICheckStateListener fImportCheckListener = event -> updateCheckedState(event.getChecked());
	protected WizardImportBreakpointsSelectionPage(String pageName) {
		super(pageName, ImportExportMessages.WizardImportBreakpointsSelectionPage_2, null);
	}

	@Override
	public void createControl(Composite parent) {
		setDescription(ImportExportMessages.WizardImportBreakpointsSelectionPage_1);
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(composite, ImportExportMessages.WizardExportBreakpointsPage_2, 1);
		BreakpointManager breakpointManager = new BreakpointManager();
		fTView = new EmbeddedBreakpointsViewer(composite, breakpointManager, null);
		createButtonsGroup(composite);
		BreakpointsViewer viewer = fTView.getViewer();
		viewer.setLabelProvider(new DecoratingLabelProvider((ILabelProvider) viewer.getLabelProvider(), new BreakpointsPathDecorator()));
		viewer.addCheckStateListener(fImportCheckListener);
		setControl(composite);
	}

	/**
	 * Creates the buttons for selecting all or none of the elements.
	 *
	 * @param parent the parent control
	 */
	private void createButtonsGroup(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, parent.getFont(), 3, 1, GridData.FILL_HORIZONTAL, 0,
				0);
		fSelectAll = SWTFactory.createPushButton(composite, ImportExportMessages.WizardBreakpointsPage_1, null);
		fSelectAll.addListener(SWT.Selection, this);
		fDeselectAll = SWTFactory.createPushButton(composite, ImportExportMessages.WizardBreakpointsPage_2, null);
		fDeselectAll.addListener(SWT.Selection, this);
	}

	public List<IMarker> getSelectedMarkers() {
		if(!fIsVisible) {
			return null;
		}
		List<IMarker> markers = new ArrayList<>();
		List<IBreakpoint> breakpoints = fTView.getCheckedElements().toList();
		for (IBreakpoint breakpoint : breakpoints) {
			markers.add(breakpoint.getMarker());
		}
		return markers;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			fIsVisible = true;
			try {
				updateBreakpointsPreviewList(fTView);
			} catch (Exception e) {
				setErrorMessage(e.getMessage());
			}
		} else {
			fIsVisible = false;
		}

		super.setVisible(visible);
	}

	private void updateBreakpointsPreviewList(final EmbeddedBreakpointsViewer currentTView) throws InvocationTargetException, InterruptedException {
		getContainer().run(false, true, monitor -> {
			WizardImportBreakpointsPage mainPage = (WizardImportBreakpointsPage) getWizard()
					.getPage(ImportExportMessages.WizardImportBreakpoints_0);
			ImportBreakpointsOperation operation = new ImportBreakpointsOperation(
					mainPage.getFileNameField().getText().trim(), mainPage.getAutoRemoveDuplicates(), false, false);
			operation.run(monitor);
			BreakpointContainer breakpointManager = new BreakpointContainer(null, null);
			IBreakpoint[] importedBreakpoints = operation.getImportedBreakpoints();
			for (IBreakpoint importedBreakpoint : importedBreakpoints) {
				breakpointManager.addBreakpoint(importedBreakpoint, new ModelDelta(null, IModelDelta.ADDED));
			}
			currentTView.getViewer().setInput(breakpointManager);
			currentTView.getViewer().refresh();
		});
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_WIZBAN_IMPORT_BREAKPOINTS);
	}

	@Override
	public void handleEvent(Event event) {
		Widget source = event.widget;
		if (source == fSelectAll) {
			handleSelectAllPressed();
		} else if (source == fDeselectAll) {
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
		setErrorMessage(null);
		setPageComplete(true);
	}

	/**
	 * Handles the de-select all button pressed
	 *
	 */
	private void handleDeselectAllPressed() {
		BreakpointsViewer viewer = fTView.getViewer();
		viewer.setCheckedElements(new Object[] {});
		viewer.setGrayedElements(new Object[] {});
		setErrorMessage(ImportExportMessages.WizardImportBreakpointsSelectionPage_1);
		setPageComplete(false);
	}

	/**
	 * Update the checked state of the given element and all of its children.
	 *
	 * @param enable the checked status of the obj
	 */
	private void updateCheckedState(boolean enable) {
		if (!enable) {
			int size = fTView.getCheckedElements().size();
			if (size == 0) {
				setErrorMessage(ImportExportMessages.WizardImportBreakpointsSelectionPage_1);
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				setPageComplete(true);
			}
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}

	}
}
