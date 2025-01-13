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

import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.IAntEditorColorConstants;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
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
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.model.WorkbenchViewerComparator;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

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

	/**
	 * Item in the highlighting color list.
	 *
	 * @since 3.0
	 */
	private static class HighlightingColorListItem {
		/** Display name */
		private final String fDisplayName;
		/** Color preference key */
		private final String fColorKey;
		/** Bold preference key */
		private final String fBoldKey;
		/** Italic preference key */
		private final String fItalicKey;
		/** Item color */
		private final Color fItemColor;

		/**
		 * Initialize the item with the given values.
		 *
		 * @param displayName
		 *            the display name
		 * @param colorKey
		 *            the color preference key
		 * @param boldKey
		 *            the bold preference key
		 * @param italicKey
		 *            the italic preference key
		 * @param itemColor
		 *            the item color
		 */
		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, Color itemColor) {
			fDisplayName = displayName;
			fColorKey = colorKey;
			fBoldKey = boldKey;
			fItalicKey = italicKey;
			fItemColor = itemColor;
		}

		/**
		 * @return the bold preference key
		 */
		public String getBoldKey() {
			return fBoldKey;
		}

		/**
		 * @return the bold preference key
		 */
		public String getItalicKey() {
			return fItalicKey;
		}

		/**
		 * @return the color preference key
		 */
		public String getColorKey() {
			return fColorKey;
		}

		/**
		 * @return the display name
		 */
		public String getDisplayName() {
			return fDisplayName;
		}

		/**
		 * @return the item color
		 */
		public Color getItemColor() {
			return fItemColor;
		}
	}

	/**
	 * Color list label provider.
	 *
	 * @since 3.0
	 */
	private static class ColorListLabelProvider extends LabelProvider implements IColorProvider {

		@Override
		public String getText(Object element) {
			return ((HighlightingColorListItem) element).getDisplayName();
		}

		@Override
		public Color getForeground(Object element) {
			return ((HighlightingColorListItem) element).getItemColor();
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}

	/**
	 * Color list content provider.
	 *
	 * @since 3.0
	 */
	private static class ColorListContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>) inputElement).toArray();
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	/** The keys of the overlay store. */
	private String[][] fSyntaxColorListModel;

	private final String[] fProblemPreferenceKeys = new String[] { AntEditorPreferenceConstants.PROBLEM_CLASSPATH,
			AntEditorPreferenceConstants.PROBLEM_PROPERTIES, AntEditorPreferenceConstants.PROBLEM_IMPORTS, AntEditorPreferenceConstants.PROBLEM_TASKS,
			AntEditorPreferenceConstants.PROBLEM_SECURITY };

	private ColorSelector fSyntaxForegroundColorSelector;
	private Button fBoldCheckBox;
	private Button fItalicCheckBox;

	private TableViewer fHighlightingColorListViewer;
	private final List<HighlightingColorListItem> fHighlightingColorList = new ArrayList<>(5);

	private SourceViewer fPreviewViewer;

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
		fSyntaxColorListModel = new String[][] {
				{ AntPreferencesMessages.AntEditorPreferencePage_Ant_editor_text_1, IAntEditorColorConstants.TEXT_COLOR, null },
				{ AntPreferencesMessages.AntEditorPreferencePage_Ant_editor_processing_instuctions_2,
						IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR, null },
				{ AntPreferencesMessages.AntEditorPreferencePage_Ant_editor_constant_strings_3, IAntEditorColorConstants.STRING_COLOR, null },
				{ AntPreferencesMessages.AntEditorPreferencePage_Ant_editor_tags_4, IAntEditorColorConstants.TAG_COLOR, null },
				{ AntPreferencesMessages.AntEditorPreferencePage_Ant_editor_comments_5, IAntEditorColorConstants.XML_COMMENT_COLOR, null },
				{ AntPreferencesMessages.AntEditorPreferencePage_26, IAntEditorColorConstants.XML_DTD_COLOR, null } };
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

		for (String[] colorModel : fSyntaxColorListModel) {
			String colorKey = colorModel[1];
			addTextKeyToCover(overlayKeys, colorKey);
		}

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return new OverlayPreferenceStore(getPreferenceStore(), keys);
	}

	private void addTextKeyToCover(ArrayList<OverlayPreferenceStore.OverlayKey> overlayKeys, String mainKey) {
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey
				+ AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey
				+ AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX));
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
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_1);
		item.setControl(createSyntaxPage(folder));

		item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_10);
		item.setControl(createProblemsTabContent(folder));

		item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntEditorPreferencePage_19);
		item.setControl(createFoldingTabContent(folder));

		initialize();

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

	private void initialize() {

		initializeFields();

		for (String[] colorModel : fSyntaxColorListModel) {
			fHighlightingColorList.add(new HighlightingColorListItem(colorModel[0], colorModel[1], colorModel[1]
					+ AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX, colorModel[1] + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX, null));
		}
		fHighlightingColorListViewer.setInput(fHighlightingColorList);
		fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(0)));
	}

	@Override
	protected void handleDefaults() {
		handleSyntaxColorListSelection();
		restoreWorkingValuesToDefaults();
		updateControlsForProblemReporting(!AntUIPlugin.getDefault().getCombinedPreferenceStore().getBoolean(AntEditorPreferenceConstants.BUILDFILE_IGNORE_ALL));
	}

	private Control createSyntaxPage(Composite parent) {

		Composite colorComposite = new Composite(parent, SWT.NONE);
		colorComposite.setLayout(new GridLayout());

		Label label = new Label(colorComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntEditorPreferencePage_5);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite = new Composite(colorComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		editorComposite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);

		fHighlightingColorListViewer = new TableViewer(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fHighlightingColorListViewer.setLabelProvider(new ColorListLabelProvider());
		fHighlightingColorListViewer.setContentProvider(new ColorListContentProvider());
		fHighlightingColorListViewer.setComparator(new WorkbenchViewerComparator());
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(5);
		fHighlightingColorListViewer.getControl().setLayoutData(gd);

		Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		label = new Label(stylesComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntEditorPreferencePage_6);
		gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		label.setLayoutData(gd);

		fSyntaxForegroundColorSelector = new ColorSelector(stylesComposite);
		Button foregroundColorButton = fSyntaxForegroundColorSelector.getButton();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		fBoldCheckBox = new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(AntPreferencesMessages.AntEditorPreferencePage_7);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 2;
		fBoldCheckBox.setLayoutData(gd);

		fItalicCheckBox = new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText(AntPreferencesMessages.AntEditorPreferencePage_8);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 2;
		fItalicCheckBox.setLayoutData(gd);

		label = new Label(colorComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntEditorPreferencePage_9);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control previewer = createPreviewer(colorComposite);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(20);
		gd.heightHint = convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		fHighlightingColorListViewer.addSelectionChangedListener(event -> handleSyntaxColorListSelection());

		foregroundColorButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				PreferenceConverter.setValue(getOverlayStore(), item.getColorKey(), fSyntaxForegroundColorSelector.getColorValue());
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getOverlayStore().setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
			}
		});

		fItalicCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getOverlayStore().setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
			}
		});

		return colorComposite;
	}

	private Control createPreviewer(Composite parent) {
		fPreviewViewer = new SourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		AntSourceViewerConfiguration configuration = new AntSourceViewerConfiguration();

		fPreviewViewer.configure(configuration);
		fPreviewViewer.setEditable(false);
		Font font = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);

		IPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] { getOverlayStore(), EditorsUI.getPreferenceStore() });
		new AntPreviewerUpdater(fPreviewViewer, configuration, store);

		String content = loadPreviewContentFromFile("SyntaxPreviewCode.txt"); //$NON-NLS-1$
		IDocument document = new Document(content);
		new AntDocumentSetupParticipant().setup(document);
		fPreviewViewer.setDocument(document);

		return fPreviewViewer.getControl();
	}

	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item = getHighlightingColorListItem();
		RGB rgb = PreferenceConverter.getColor(getOverlayStore(), item.getColorKey());
		fSyntaxForegroundColorSelector.setColorValue(rgb);
		fBoldCheckBox.setSelection(getOverlayStore().getBoolean(item.getBoldKey()));
		fItalicCheckBox.setSelection(getOverlayStore().getBoolean(item.getItalicKey()));
	}

	/**
	 * Returns the current highlighting color list item.
	 *
	 * @return the current highlighting color list item
	 * @since 3.0
	 */
	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection = (IStructuredSelection) fHighlightingColorListViewer.getSelection();
		return (HighlightingColorListItem) selection.getFirstElement();
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
				if ("org.eclipse.ui.preferencePages.GeneralTextEditor".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, null);
				else if ("org.eclipse.ui.preferencePages.ColorsAndFonts".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, "selectFont:org.eclipse.jface.textfont"); //$NON-NLS-1$
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
