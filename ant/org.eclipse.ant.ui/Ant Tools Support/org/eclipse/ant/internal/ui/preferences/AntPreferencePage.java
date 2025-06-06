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

import java.text.MessageFormat;

import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.ant.internal.launching.IAntLaunchingPreferenceConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class AntPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Allows us to override the default behavior of the default {@link IntegerFieldEditor} to work on a core preference context
	 *
	 * @since 3.6
	 */
	class AntIntegerFieldEditor extends IntegerFieldEditor {
		String node = null, key = null;
		int defaultvalue = -1;

		/**
		 * Constructor
		 *
		 * @param node
		 *            the identifier of the node we want to set the preference in, i.e. org.eclipse.ant.launching
		 * @param key
		 *            the preference key to map the value to
		 * @param title
		 *            the title of the field editor
		 * @param parent
		 *            the parent to add the field editor to
		 * @param defaultvalue
		 *            the default value to return when looking up stored values
		 */
		public AntIntegerFieldEditor(String node, String key, String title, Composite parent, int defaultvalue) {
			super(key, title, parent);
			this.node = node;
			this.key = key;
			this.defaultvalue = defaultvalue;
		}

		@Override
		protected void doStore() {
			InstanceScope.INSTANCE.getNode(node).putInt(key, Integer.parseInt(getStringValue()));
		}

		@Override
		public void load() {
			setStringValue(Integer.toString(Platform.getPreferencesService().getInt(node, key, defaultvalue, null)));
		}

		@Override
		public void loadDefault() {
			setStringValue(Integer.toString(DefaultScope.INSTANCE.getNode(AntLaunching.getUniqueIdentifier()).getInt(key, defaultvalue)));
		}
	}

	private List fConsoleColorList;
	private ColorSelector fConsoleColorSelector;
	private IntegerFieldEditor timeout;
	private BooleanFieldEditor workspacejre = null;

	private BooleanFieldEditor fToolsWarningEditor = null;

	// Array containing the message to display, the preference key, and the
	// default value (initialized in storeInitialValues()) for each color preference
	private final String[][] fAppearanceColorListModel = new String[][] {
			{ AntPreferencesMessages.AntPreferencePage__Error__2, IAntUIPreferenceConstants.CONSOLE_ERROR_COLOR, null },
			{ AntPreferencesMessages.AntPreferencePage__Warning__3, IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR, null },
			{ AntPreferencesMessages.AntPreferencePage_I_nformation__4, IAntUIPreferenceConstants.CONSOLE_INFO_COLOR, null },
			{ AntPreferencesMessages.AntPreferencePage_Ve_rbose__5, IAntUIPreferenceConstants.CONSOLE_VERBOSE_COLOR, null },
			{ AntPreferencesMessages.AntPreferencePage_Deb_ug__6, IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR, null }, };

	/**
	 * Create the Ant page.
	 */
	public AntPreferencePage() {
		super(GRID);
		setDescription(AntPreferencesMessages.AntPreferencePage_General);
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		storeAppliedValues();

		Font font = getFieldEditorParent().getFont();
		Label label = new Label(getFieldEditorParent(), SWT.WRAP);
		label.setText(AntPreferencesMessages.AntPreferencePage_Enter);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		gd.widthHint = convertWidthInCharsToPixels(60);
		label.setLayoutData(gd);
		label.setLayoutData(gd);
		label.setFont(font);

		StringFieldEditor editor = new StringFieldEditor(IAntUIPreferenceConstants.ANT_FIND_BUILD_FILE_NAMES, AntPreferencesMessages.AntPreferencePage__Names__3, getFieldEditorParent());
		addField(editor);

		timeout = new AntIntegerFieldEditor(AntLaunching.getUniqueIdentifier(), IAntLaunchingPreferenceConstants.ANT_COMMUNICATION_TIMEOUT, AntPreferencesMessages.AntPreferencePage_13, getFieldEditorParent(), 20000);
		int minValue = DefaultScope.INSTANCE.getNode(AntLaunching.getUniqueIdentifier()).getInt(IAntLaunchingPreferenceConstants.ANT_COMMUNICATION_TIMEOUT, 20000);
		int maxValue = 1200000;
		timeout.setValidRange(minValue, maxValue);
		timeout.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
		timeout.setErrorMessage(MessageFormat.format(AntPreferencesMessages.AntPreferencePage_14, Integer.valueOf(minValue), Integer.valueOf(maxValue)));
		addField(timeout);

		editor = new URLFieldEditor(IAntUIPreferenceConstants.DOCUMENTATION_URL, AntPreferencesMessages.AntPreferencePage_2, getFieldEditorParent());
		addField(editor);

		workspacejre = new BooleanFieldEditor(IAntUIPreferenceConstants.USE_WORKSPACE_JRE, AntPreferencesMessages.always_run_in_workspace_jre, getFieldEditorParent());
		workspacejre.fillIntoGrid(getFieldEditorParent(), 3);
		addField(workspacejre);

		createSpace();

		if (!AntUIPlugin.isMacOS()) {
			// the mac does not have a tools.jar Bug 40778
			label = new Label(getFieldEditorParent(), SWT.WRAP);
			label.setText(AntPreferencesMessages.AntPreferencePage_0);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan = 3;
			gd.widthHint = convertWidthInCharsToPixels(60);
			label.setLayoutData(gd);
			label.setFont(font);
			fToolsWarningEditor = new BooleanFieldEditor(IAntUIPreferenceConstants.ANT_TOOLS_JAR_WARNING, AntPreferencesMessages.AntPreferencePage_1, getFieldEditorParent());
			addField(fToolsWarningEditor);
			createSpace();
		}

		addField(new BooleanFieldEditor(IAntUIPreferenceConstants.ANT_ERROR_DIALOG, AntPreferencesMessages.AntPreferencePage_12, getFieldEditorParent()));
		createSpace();

		addField(new BooleanFieldEditor(IAntUIPreferenceConstants.ANT_CREATE_MARKERS, AntPreferencesMessages.AntPreferencePage_15, getFieldEditorParent()));
		label = new Label(getFieldEditorParent(), SWT.WRAP);
		label.setText(AntPreferencesMessages.AntPreferencePage_16);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		gd.widthHint = convertWidthInCharsToPixels(60);
		label.setLayoutData(gd);
		label.setFont(font);

		createSpace();
		createColorComposite();
		getPreferenceStore().addPropertyChangeListener(this);
	}

	private void createSpace() {
		Label label = new Label(getFieldEditorParent(), SWT.NONE);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
	}

	/**
	 * Stores the initial values of the color preferences. The preference values are updated on the fly as the user edits them (instead of only when
	 * they press "Apply"). We need to store the old values so that we can reset them when the user chooses "Cancel".
	 */
	private void storeAppliedValues() {
		IPreferenceStore store = getPreferenceStore();
		for (int i = 0; i < fAppearanceColorListModel.length; i++) {
			String preference = fAppearanceColorListModel[i][1];
			fAppearanceColorListModel[i][2] = store.getString(preference);
		}
	}

	private void createColorComposite() {
		Font font = getFieldEditorParent().getFont();
		Label label = new Label(getFieldEditorParent(), SWT.LEFT);
		label.setText(AntPreferencesMessages.AntPreferencePage_Ant_Color_Options__6);
		label.setFont(font);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		Composite editorComposite = new Composite(getFieldEditorParent(), SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		editorComposite.setLayout(layout);
		editorComposite.setFont(font);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan = 2;
		editorComposite.setLayoutData(gd);

		fConsoleColorList = new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint = convertHeightInCharsToPixels(8);
		fConsoleColorList.setLayoutData(gd);
		fConsoleColorList.setFont(font);

		Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		stylesComposite.setFont(font);

		label = new Label(stylesComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntPreferencePage_Color__7);
		label.setFont(font);
		gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		label.setLayoutData(gd);

		fConsoleColorSelector = new ColorSelector(stylesComposite);
		Button foregroundColorButton = fConsoleColorSelector.getButton();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		foregroundColorButton.setFont(font);

		fConsoleColorList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAppearanceColorListSelection();
			}
		});
		foregroundColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int i = fConsoleColorList.getSelectionIndex();
				if (i == -1) { // bug 85590
					return;
				}
				String key = fAppearanceColorListModel[i][1];
				PreferenceConverter.setValue(getPreferenceStore(), key, fConsoleColorSelector.getColorValue());
			}
		});
	}

	/**
	 * Restore all color preferences to their values when the page was opened.
	 */
	@Override
	public boolean performCancel() {
		for (String[] colorModel : fAppearanceColorListModel) {
			String preference = colorModel[1];
			PreferenceConverter.setValue(getPreferenceStore(), preference, StringConverter.asRGB(colorModel[2]));
		}
		return super.performCancel();
	}

	/**
	 * When the user applies the preferences, update the set of stored preferences so that we will fall back to the applied values on Cancel.
	 */
	@Override
	public boolean performOk() {
		storeAppliedValues();
		return super.performOk();
	}

	private void handleAppearanceColorListSelection() {
		int i = fConsoleColorList.getSelectionIndex();
		if (i == -1) { // bug 85590
			return;
		}
		String key = fAppearanceColorListModel[i][1];
		RGB rgb = PreferenceConverter.getColor(getPreferenceStore(), key);
		fConsoleColorSelector.setColorValue(rgb);
	}

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAntUIHelpContextIds.ANT_PREFERENCE_PAGE);
		return super.createContents(parent);
	}

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	protected void initialize() {
		super.initialize();
		for (String[] colorModel : fAppearanceColorListModel) {
			fConsoleColorList.add(colorModel[0]);
		}
		fConsoleColorList.getDisplay().asyncExec(() -> {
			if (fConsoleColorList != null && !fConsoleColorList.isDisposed()) {
				fConsoleColorList.select(0);
				handleAppearanceColorListSelection();
			}
		});
	}

	@Override
	protected void performDefaults() {
		for (String[] colorModel : fAppearanceColorListModel) {
			String key = colorModel[1];
			PreferenceConverter.setValue(getPreferenceStore(), key, PreferenceConverter.getDefaultColor(getPreferenceStore(), key));
		}
		handleAppearanceColorListSelection();
		super.performDefaults();
	}

	@Override
	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IAntUIPreferenceConstants.ANT_TOOLS_JAR_WARNING)) {
			if (fToolsWarningEditor != null) {
				fToolsWarningEditor.load();
			}
		} else {
			super.propertyChange(event);
		}
	}
}
