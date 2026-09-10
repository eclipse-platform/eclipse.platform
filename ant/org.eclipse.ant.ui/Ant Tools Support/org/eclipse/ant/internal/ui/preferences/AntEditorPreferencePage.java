/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Robert Roth <robert.roth.off@gmail.com> - bug 184656
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.TabFolderLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.PreferencesUtil;

/*
 * The page for setting the editor options.
 */
public class AntEditorPreferencePage extends AbstractAntEditorPreferencePage {

	protected static class ControlData {
		private final String fKey;
		private final String[] fValues;

		public ControlData(String key, String[] values) {
			fKey = key;
			fValues = values;
		}

		public String getKey() {
			return fKey;
		}

		public String getValue(boolean selection) {
			int index = selection ? 0 : 1;
			return fValues[index];
		}

		public String getValue(int index) {
			return fValues[index];
		}

		public int getSelection(String value) {
			if (value != null) {
				for (int i = 0; i < fValues.length; i++) {
					if (value.equals(fValues[i])) {
						return i;
					}
				}
			}
			return fValues.length - 1; // assume the last option is the least severe
		}
	}

	private final String[] fProblemPreferenceKeys = new String[] { AntEditorPreferenceConstants.PROBLEM_CLASSPATH,
			AntEditorPreferenceConstants.PROBLEM_PROPERTIES, AntEditorPreferenceConstants.PROBLEM_IMPORTS, AntEditorPreferenceConstants.PROBLEM_TASKS,
			AntEditorPreferenceConstants.PROBLEM_SECURITY };

	private SelectionListener fSelectionListener;
	protected Map<String, String> fWorkingValues;
	protected List<Combo> fComboBoxes;
	private List<Label> fProblemLabels;

	private Button fIgnoreAllProblems;

	private Text fBuildFilesToIgnoreProblems;
	private Label fBuildFilesToIgnoreProblemsLabel;

	private Label fSeverityLabel;

	private Label fBuildFilesToIgnoreProblemsDescription;

	public AntEditorPreferencePage() {
		super();
		setDescription(AntPreferencesMessages.AntEditorPreferencePage_description);
	}

	@Override
	protected OverlayPreferenceStore createOverlayStore() {
		ArrayList<OverlayPreferenceStore.OverlayKey> overlayKeys = new ArrayList<>();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.CODEASSIST_AUTOINSERT));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_COMMENTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_DTD));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_DEFINING));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_TARGETS));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_STICKY_OCCURRENCES));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.BUILDFILE_IGNORE_ALL));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AntEditorPreferenceConstants.BUILDFILE_NAMES_TO_IGNORE));

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return new OverlayPreferenceStore(getPreferenceStore(), keys);
	}

	private Control createAppearancePage(Composite parent) {
		Font font = parent.getFont();

		Composite appearanceComposite = new Composite(parent, SWT.NONE);
		appearanceComposite.setFont(font);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		appearanceComposite.setLayout(layout);

		String labelText = AntPreferencesMessages.AntEditorPreferencePage_2;
		addCheckBox(appearanceComposite, labelText, AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES, 0);

		labelText = AntPreferencesMessages.AntEditorPreferencePage_4;
		addCheckBox(appearanceComposite, labelText, AntEditorPreferenceConstants.EDITOR_STICKY_OCCURRENCES, 0);

		return appearanceComposite;
	}

	@Override
	protected Control createContents(Composite parent) {
		getOverlayStore().load();
		getOverlayStore().start();

		createHeader(parent);

		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_general);
		item.setControl(createAppearancePage(folder));

		item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_10);
		item.setControl(createProblemsTabContent(folder));

		item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_19);
		item.setControl(createFoldingTabContent(folder));

		initializeFields();

		applyDialogFont(parent);
		return folder;
	}

	private Control createFoldingTabContent(TabFolder folder) {
		Composite composite = new Composite(folder, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		addCheckBox(composite, AntPreferencesMessages.AntEditorPreferencePage_20, AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, 0);

		Label label = new Label(composite, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntEditorPreferencePage_21);

		addCheckBox(composite, AntPreferencesMessages.AntEditorPreferencePage_22, AntEditorPreferenceConstants.EDITOR_FOLDING_DTD, 0);
		addCheckBox(composite, AntPreferencesMessages.AntEditorPreferencePage_23, AntEditorPreferenceConstants.EDITOR_FOLDING_COMMENTS, 0);
		addCheckBox(composite, AntPreferencesMessages.AntEditorPreferencePage_24, AntEditorPreferenceConstants.EDITOR_FOLDING_DEFINING, 0);
		addCheckBox(composite, AntPreferencesMessages.AntEditorPreferencePage_25, AntEditorPreferenceConstants.EDITOR_FOLDING_TARGETS, 0);
		return composite;
	}

	@Override
	protected void handleDefaults() {
		restoreWorkingValuesToDefaults();
		updateControlsForProblemReporting(!AntUIPlugin.getDefault().getCombinedPreferenceStore().getBoolean(AntEditorPreferenceConstants.BUILDFILE_IGNORE_ALL));
	}

	private Composite createProblemsTabContent(TabFolder folder) {
		fComboBoxes = new ArrayList<>();
		fProblemLabels = new ArrayList<>();
		initializeWorkingValues();

		String[] errorWarningIgnoreLabels = new String[] { AntPreferencesMessages.AntEditorPreferencePage_11,
				AntPreferencesMessages.AntEditorPreferencePage_12, AntPreferencesMessages.AntEditorPreferencePage_13 };
		String[] errorWarningIgnore = new String[] { AntEditorPreferenceConstants.BUILDFILE_ERROR, AntEditorPreferenceConstants.BUILDFILE_WARNING,
				AntEditorPreferenceConstants.BUILDFILE_IGNORE };

		int nColumns = 3;

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;

		Composite othersComposite = new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);

		String labelText = AntPreferencesMessages.AntEditorPreferencePage_28;
		fIgnoreAllProblems = addCheckBox(othersComposite, labelText, AntEditorPreferenceConstants.BUILDFILE_IGNORE_ALL, 0);

		fIgnoreAllProblems.addSelectionListener(getSelectionListener());

		fBuildFilesToIgnoreProblemsDescription = new Label(othersComposite, SWT.WRAP);
		fBuildFilesToIgnoreProblemsDescription.setText(AntPreferencesMessages.AntEditorPreferencePage_29);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = nColumns;
		fBuildFilesToIgnoreProblemsDescription.setLayoutData(gd);

		Control[] controls = addLabelledTextField(othersComposite, AntPreferencesMessages.AntEditorPreferencePage_30, AntEditorPreferenceConstants.BUILDFILE_NAMES_TO_IGNORE, -1, 0, null);
		fBuildFilesToIgnoreProblems = getTextControl(controls);
		fBuildFilesToIgnoreProblemsLabel = getLabelControl(controls);

		fSeverityLabel = new Label(othersComposite, SWT.WRAP);
		fSeverityLabel.setText(AntPreferencesMessages.AntEditorPreferencePage_14);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = nColumns;
		fSeverityLabel.setLayoutData(gd);

		String label = AntPreferencesMessages.AntEditorPreferencePage_18;
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_TASKS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = AntPreferencesMessages.AntEditorPreferencePage_15;
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_CLASSPATH, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = AntPreferencesMessages.AntEditorPreferencePage_16;
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_PROPERTIES, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = AntPreferencesMessages.AntEditorPreferencePage_17;
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_IMPORTS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = AntPreferencesMessages.AntEditorPreferencePage_27;
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_SECURITY, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		updateControlsForProblemReporting(!AntUIPlugin.getDefault().getCombinedPreferenceStore().getBoolean(AntEditorPreferenceConstants.BUILDFILE_IGNORE_ALL));
		return othersComposite;
	}

	private void initializeWorkingValues() {
		fWorkingValues = new HashMap<>(fProblemPreferenceKeys.length);
		for (String key : fProblemPreferenceKeys) {
			fWorkingValues.put(key, getPreferenceStore().getString(key));
		}
	}

	private void restoreWorkingValuesToDefaults() {
		fWorkingValues = new HashMap<>(fProblemPreferenceKeys.length);
		for (String key : fProblemPreferenceKeys) {
			fWorkingValues.put(key, getPreferenceStore().getDefaultString(key));
		}
		updateControls();
	}

	protected Combo addComboBox(Composite parent, String label, String key, String[] values, String[] valueLabels, int indent) {
		ControlData data = new ControlData(key, values);

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indent;

		Label labelControl = new Label(parent, SWT.LEFT | SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);

		Combo comboBox = new Combo(parent, SWT.READ_ONLY);
		comboBox.setItems(valueLabels);
		comboBox.setData(data);
		comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		comboBox.addSelectionListener(getSelectionListener());

		Label placeHolder = new Label(parent, SWT.NONE);
		placeHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		String currValue = fWorkingValues.get(key);
		comboBox.select(data.getSelection(currValue));

		fProblemLabels.add(labelControl);
		fComboBoxes.add(comboBox);
		return comboBox;
	}

	protected SelectionListener getSelectionListener() {
		if (fSelectionListener == null) {
			fSelectionListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					controlChanged(e.widget);
				}
			};
		}
		return fSelectionListener;
	}

	protected void controlChanged(Widget widget) {
		ControlData data = (ControlData) widget.getData();
		String newValue = null;
		if (widget instanceof Button) {
			if (widget == fIgnoreAllProblems) {
				updateControlsForProblemReporting(!((Button) widget).getSelection());
				return;
			}
			newValue = data.getValue(((Button) widget).getSelection());
		} else if (widget instanceof Combo) {
			newValue = data.getValue(((Combo) widget).getSelectionIndex());
		} else {
			return;
		}
		fWorkingValues.put(data.getKey(), newValue);
	}

	private void updateControlsForProblemReporting(boolean reportProblems) {
		for (int i = fComboBoxes.size() - 1; i >= 0; i--) {
			fComboBoxes.get(i).setEnabled(reportProblems);
			fProblemLabels.get(i).setEnabled(reportProblems);
		}
		fSeverityLabel.setEnabled(reportProblems);
		fBuildFilesToIgnoreProblems.setEnabled(reportProblems);
		fBuildFilesToIgnoreProblemsDescription.setEnabled(reportProblems);
		fBuildFilesToIgnoreProblemsLabel.setEnabled(reportProblems);
	}

	protected void updateControls() {
		// update the UI
		for (int i = fComboBoxes.size() - 1; i >= 0; i--) {
			Combo curr = fComboBoxes.get(i);
			ControlData data = (ControlData) curr.getData();

			String currValue = fWorkingValues.get(data.getKey());
			curr.select(data.getSelection(currValue));
		}
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		for (String key : fWorkingValues.keySet()) {
			store.putValue(key, fWorkingValues.get(key));
		}
		if (store.needsSaving()) {
			// any AntModels listen for changes to the "PROBLEM" pref
			// this is so that the models do not update for each and every pref modification
			store.putValue(AntEditorPreferenceConstants.PROBLEM, "changed"); //$NON-NLS-1$
			// ensure to clear as there may not be any models open currently
			store.setToDefault(AntEditorPreferenceConstants.PROBLEM);
		}
		return super.performOk();
	}

	private void createHeader(Composite contents) {
		final Link link = new Link(contents, SWT.NONE);
		link.setText(AntPreferencesMessages.AntEditorPreferencePage_0);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ("org.eclipse.ui.preferencePages.GeneralTextEditor".equals(e.text)) { //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, null);
				} else if ("org.eclipse.ui.preferencePages.ColorsAndFonts".equals(e.text)) { //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, "selectFont:org.eclipse.jface.textfont"); //$NON-NLS-1$
				}
			}
		});
		String linktooltip = AntPreferencesMessages.AntEditorPreferencePage_3;
		link.setToolTipText(linktooltip);
	}

	@Override
	protected String getHelpContextId() {
		return IAntUIHelpContextIds.ANT_EDITOR_PREFERENCE_PAGE;
	}
}
