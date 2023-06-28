/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christian Georgi - Bug 388321 Perspectives are not sorted in debug's view management preference page
 *******************************************************************************/
package org.eclipse.debug.internal.ui.preferences;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.views.ViewContextService;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.PerspectiveLabelProvider;

/**
 * Preference page for configuring the debugger's automatic
 * view management.
 */
public class ViewManagementPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private CheckboxTableViewer fPerspectiveViewer;
	private Button fTrackViewsButton;
	private Button fResetViewsButton;
	// This flag indicates whether or not the user has pressed the reset button
	private boolean fResetPressed= false;
	private PerspectiveLabelProvider fLabelProvider= null;
	private SelectionListener fSelectionListener= new SelectionAdapter() {

		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fResetViewsButton) {
				handleResetPressed();
			} else if (source == fTrackViewsButton) {
				handleTrackViewsToggled();
			}
		}

	};

	public ViewManagementPreferencePage() {
		super();
		setTitle(DebugPreferencesMessages.ViewManagementPreferencePage_1);
		setDescription(DebugPreferencesMessages.ViewManagementPreferencePage_0);
		setPreferenceStore(DebugUITools.getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.VIEW_MANAGEMENT_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		createPerspectiveViewer(composite);

		createViewTrackingOptions(composite);

		Dialog.applyDialogFont(composite);

		return composite;
	}

	/**
	 * @param composite
	 */
	private void createViewTrackingOptions(Composite composite) {
		fTrackViewsButton= new Button(composite, SWT.CHECK);
		fTrackViewsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTrackViewsButton.setText(DebugPreferencesMessages.ViewManagementPreferencePage_3);
		fTrackViewsButton.setSelection(DebugUITools.getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_TRACK_VIEWS));
		fTrackViewsButton.addSelectionListener(fSelectionListener);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(DebugPreferencesMessages.ViewManagementPreferencePage_4);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fResetViewsButton= SWTFactory.createPushButton(composite, DebugPreferencesMessages.ViewManagementPreferencePage_5, null);
		((GridData) fResetViewsButton.getLayoutData()).horizontalAlignment= GridData.BEGINNING;
		fResetViewsButton.addSelectionListener(fSelectionListener);
		updateResetButton();
	}

	private void handleResetPressed() {
		fResetPressed= true;
		fResetViewsButton.setEnabled(false);
	}

	protected void handleTrackViewsToggled() {
		if (fTrackViewsButton.getSelection()) {
			// When toggled on, possibly re-enable the reset button
			updateResetButton();
		} else {
			// When toggled off, disable the reset button
			fResetViewsButton.setEnabled(false);
		}
	}

	/**
	 * @param parent
	 */
	private void createPerspectiveViewer(Composite parent) {
		Label label= new Label(parent, SWT.WRAP);
		label.setText(DebugPreferencesMessages.ViewManagementPreferencePage_2);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Table table= new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPerspectiveViewer= new CheckboxTableViewer(table);
		PerspectiveProvider provider= new PerspectiveProvider();
		fPerspectiveViewer.setContentProvider(provider);
		fLabelProvider= new PerspectiveLabelProvider();
		fPerspectiveViewer.setLabelProvider(fLabelProvider);
		fPerspectiveViewer.setComparator(new PerspectiveComparator());
		fPerspectiveViewer.setInput(this);

		Set<String> perspectives;
		String preference = DebugUIPlugin.getDefault().getPreferenceStore().getString(
			IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES);
		if (IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES_DEFAULT.equals(preference)) {
			perspectives = ViewContextService.getDefaultEnabledPerspectives();
		} else {
			perspectives = ViewContextService.parseList(preference);
		}
		checkPerspectives(perspectives);
	}

	private void checkPerspectives(Set<String> perspectives) {
		fPerspectiveViewer.setAllChecked(false);
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		for (String id : perspectives) {
			IPerspectiveDescriptor descriptor = registry.findPerspectiveWithId(id);
			if (descriptor != null) {
				fPerspectiveViewer.setChecked(descriptor, true);
			}
		}
	}

	@Override
	public boolean performOk() {
		Set<String> perspectives = new HashSet<>();
		for (Object descriptor : fPerspectiveViewer.getCheckedElements()) {
			perspectives.add( ((IPerspectiveDescriptor)descriptor).getId() );
		}
		if (perspectives.equals(ViewContextService.getDefaultEnabledPerspectives())) {
			getPreferenceStore().setValue(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES,
										  IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES_DEFAULT);
		} else {
			StringBuilder buffer= new StringBuilder();
			for (String id : perspectives) {
				buffer.append(id).append(',');
			}
			getPreferenceStore().setValue(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES, buffer.toString());
		}

		boolean trackViews = fTrackViewsButton.getSelection();
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_TRACK_VIEWS, trackViews);
		if (fResetPressed || !trackViews) {
			// Reset if the user has pressed reset or chosen to no longer track views
			getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_USER_VIEW_BINDINGS, IInternalDebugCoreConstants.EMPTY_STRING);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		checkPerspectives( ViewContextService.getDefaultEnabledPerspectives() );
		fTrackViewsButton.setSelection(getPreferenceStore().getDefaultBoolean(IInternalDebugUIConstants.PREF_TRACK_VIEWS));
		fResetPressed= false;
		updateResetButton();
		super.performDefaults();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * Updates enablement of the reset button.
	 * Enable if either persisted view collection is not empty.
	 * Always disable if "track views" is turned off.
	 */
	private void updateResetButton() {
		boolean enableReset= !IInternalDebugCoreConstants.EMPTY_STRING.equals(getPreferenceStore().getString(IInternalDebugUIConstants.PREF_USER_VIEW_BINDINGS));
		// Only enable the button if there are views to clear, the user hasn't pressed the reset
		// button, and the option to "track views" is turned on.
		fResetViewsButton.setEnabled(enableReset && !fResetPressed && fTrackViewsButton.getSelection());
	}

	private static class PerspectiveProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return PlatformUI.getWorkbench().getPerspectiveRegistry().getPerspectives();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

	}

	private static class PerspectiveComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IPerspectiveDescriptor && e2 instanceof IPerspectiveDescriptor) {
				return ((IPerspectiveDescriptor) e1).getLabel().compareToIgnoreCase(((IPerspectiveDescriptor) e2).getLabel());
			}
			return super.compare(viewer, e1, e2);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fLabelProvider != null) {
			fLabelProvider.dispose();
		}
	}
}
