/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.core.ComparePlugin;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;


public class ComparePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	class FakeInput implements ITypedElement, IEncodedStreamContentAccessor {
		static final String UTF_16= "UTF-16"; //$NON-NLS-1$
		String fContent;
		
		FakeInput(String name) {
			fContent= loadPreviewContentFromFile(name);
		}
		public Image getImage() {
			return null;
		}
		public String getName() {
			return "no name";	//$NON-NLS-1$
		}
		public String getType() {
			return "no type";	//$NON-NLS-1$
		}
		public InputStream getContents() {
			return new ByteArrayInputStream(Utilities.getBytes(fContent, UTF_16));
		}
		public String getCharset() {
			return UTF_16;
		}
	}

	private static final String PREFIX= CompareUIPlugin.PLUGIN_ID + "."; //$NON-NLS-1$
	public static final String OPEN_STRUCTURE_COMPARE= PREFIX + "OpenStructureCompare"; //$NON-NLS-1$
	public static final String USE_OUTLINE_VIEW= PREFIX + "UseOutlineView"; //$NON-NLS-1$
	public static final String SYNCHRONIZE_SCROLLING= PREFIX + "SynchronizeScrolling"; //$NON-NLS-1$
	public static final String SHOW_PSEUDO_CONFLICTS= PREFIX + "ShowPseudoConflicts"; //$NON-NLS-1$
	public static final String INITIALLY_SHOW_ANCESTOR_PANE= PREFIX + "InitiallyShowAncestorPane"; //$NON-NLS-1$
	public static final String PREF_SAVE_ALL_EDITORS= PREFIX + "SaveAllEditors"; //$NON-NLS-1$
	public static final String IGNORE_WHITESPACE= PREFIX + "IgnoreWhitespace"; //$NON-NLS-1$
	
	//public static final String USE_SPLINES= PREFIX + "UseSplines"; //$NON-NLS-1$
	public static final String USE_SINGLE_LINE= PREFIX + "UseSingleLine"; //$NON-NLS-1$
	public static final String HIGHLIGHT_TOKEN_CHANGES= PREFIX + "HighlightTokenChanges"; //$NON-NLS-1$
	//public static final String USE_RESOLVE_UI= PREFIX + "UseResolveUI"; //$NON-NLS-1$
	public static final String CAPPING_DISABLED= PREFIX + "CappingDisable"; //$NON-NLS-1$
	public static final String PATH_FILTER= PREFIX + "PathFilter"; //$NON-NLS-1$
	public static final String ADDED_LINES_REGEX= PREFIX + "AddedLinesRegex"; //$NON-NLS-1$
	public static final String REMOVED_LINES_REGEX= PREFIX + "RemovedLinesRegex"; //$NON-NLS-1$
	
	
	private TextMergeViewer fPreviewViewer;
	private IPropertyChangeListener fPreferenceChangeListener;
	private CompareConfiguration fCompareConfiguration;
	private OverlayPreferenceStore fOverlayStore;
	private Map fCheckBoxes= new HashMap();
	private Text fFilters;
	private Text addedLinesRegex;
	private Text removedLinesRegex;
	private SelectionListener fCheckBoxListener;


	public final OverlayPreferenceStore.OverlayKey[] fKeys= new OverlayPreferenceStore.OverlayKey[] {	
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, OPEN_STRUCTURE_COMPARE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_OUTLINE_VIEW),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, SYNCHRONIZE_SCROLLING),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, SHOW_PSEUDO_CONFLICTS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_SHOW_ANCESTOR_PANE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IGNORE_WHITESPACE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PREF_SAVE_ALL_EDITORS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ADDED_LINES_REGEX),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, REMOVED_LINES_REGEX),
		//new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_SPLINES),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_SINGLE_LINE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, HIGHLIGHT_TOKEN_CHANGES),
		//new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_RESOLVE_UI),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CAPPING_DISABLED),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PATH_FILTER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ICompareUIConstants.PREF_NAVIGATION_END_ACTION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL),
	};
	private RadioGroupFieldEditor editor;
	
	
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(OPEN_STRUCTURE_COMPARE, true);
		store.setDefault(USE_OUTLINE_VIEW, false);
		store.setDefault(SYNCHRONIZE_SCROLLING, true);
		store.setDefault(SHOW_PSEUDO_CONFLICTS, false);
		store.setDefault(INITIALLY_SHOW_ANCESTOR_PANE, false);
		store.setDefault(IGNORE_WHITESPACE, false);
		store.setDefault(PREF_SAVE_ALL_EDITORS, false);
		store.setDefault(ADDED_LINES_REGEX, ""); //$NON-NLS-1$
		store.setDefault(REMOVED_LINES_REGEX, ""); //$NON-NLS-1$
		//store.setDefault(USE_SPLINES, false);
		store.setDefault(USE_SINGLE_LINE, true);
		store.setDefault(HIGHLIGHT_TOKEN_CHANGES, true);
		//store.setDefault(USE_RESOLVE_UI, false);
		store.setDefault(CAPPING_DISABLED, false);
		store.setDefault(PATH_FILTER, ""); //$NON-NLS-1$
		store.setDefault(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, ICompareUIConstants.PREF_VALUE_PROMPT);
		store.setDefault(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL, ICompareUIConstants.PREF_VALUE_LOOP);
	}

	public ComparePreferencePage() {
		
		//setDescription(Utilities.getString("ComparePreferencePage.description"));	//$NON-NLS-1$
		
		setPreferenceStore(CompareUIPlugin.getDefault().getPreferenceStore());
		
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
		fPreferenceChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String key= event.getProperty();
				if (key.equals(INITIALLY_SHOW_ANCESTOR_PANE)) {
					boolean b= fOverlayStore.getBoolean(INITIALLY_SHOW_ANCESTOR_PANE);
					if (fCompareConfiguration != null) {
						fCompareConfiguration.setProperty(INITIALLY_SHOW_ANCESTOR_PANE, new Boolean(b));
					}
				}
			}
		};
		fOverlayStore.addPropertyChangeListener(fPreferenceChangeListener);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init()
	 */
	public void init(IWorkbench workbench) {
		// empty
	}	

	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fOverlayStore.setValue(ADDED_LINES_REGEX, addedLinesRegex.getText());
		fOverlayStore.setValue(REMOVED_LINES_REGEX, removedLinesRegex.getText());

		editor.store();
		fOverlayStore.propagate();

		ComparePlugin.getDefault().setCappingDisabled(
				getPreferenceStore().getBoolean(
						ComparePreferencePage.CAPPING_DISABLED));
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		fOverlayStore.loadDefaults();
		initializeFields();
		
		super.performDefaults();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
				
		if (fOverlayStore != null) {
			if (fPreferenceChangeListener != null) {
				fOverlayStore.removePropertyChangeListener(fPreferenceChangeListener);
				fPreferenceChangeListener= null;
			}
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		
		super.dispose();
	}

	static public boolean getSaveAllEditors() {
		IPreferenceStore store= CompareUIPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_SAVE_ALL_EDITORS);
	}
	
	static public void setSaveAllEditors(boolean value) {
		IPreferenceStore store= CompareUIPlugin.getDefault().getPreferenceStore();
		store.setValue(PREF_SAVE_ALL_EDITORS, value);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ICompareContextIds.COMPARE_PREFERENCE_PAGE);
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(Utilities.getString("ComparePreferencePage.generalTab.label"));	//$NON-NLS-1$
		//item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createGeneralPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(Utilities.getString("ComparePreferencePage.textCompareTab.label"));	//$NON-NLS-1$
		//item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createTextComparePage(folder));
		
		initializeFields();
		Dialog.applyDialogFont(folder);
		return folder;
	}
	
	private Control createGeneralPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);
				
		addCheckBox(composite, "ComparePreferencePage.structureCompare.label", OPEN_STRUCTURE_COMPARE, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.structureOutline.label", USE_OUTLINE_VIEW, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.ignoreWhitespace.label", IGNORE_WHITESPACE, 0);	//$NON-NLS-1$
		
		// a spacer
		new Label(composite, SWT.NONE);

		addCheckBox(composite, "ComparePreferencePage.saveBeforePatching.label", PREF_SAVE_ALL_EDITORS, 0);	//$NON-NLS-1$

		// a spacer
		new Label(composite, SWT.NONE);
		
		Label l= new Label(composite, SWT.WRAP);
		l.setText(Utilities.getString("ComparePreferencePage.regex.description")); //$NON-NLS-1$
		
		Composite c2= new Composite(composite, SWT.NONE);
		c2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		c2.setLayout(layout);
		
		l= new Label(c2, SWT.NONE);
		l.setText(Utilities.getString("ComparePreferencePage.regexAdded.label")); //$NON-NLS-1$
		addedLinesRegex = new Text(c2, SWT.BORDER);
		addedLinesRegex.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addedLinesRegex.setText(fOverlayStore.getString(ADDED_LINES_REGEX));
		
		l= new Label(c2, SWT.NONE);
		l.setText(Utilities.getString("ComparePreferencePage.regexRemoved.label")); //$NON-NLS-1$
		removedLinesRegex = new Text(c2, SWT.BORDER);
		removedLinesRegex.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removedLinesRegex.setText(fOverlayStore.getString(REMOVED_LINES_REGEX));

		// a spacer
		new Label(composite, SWT.NONE);
		
		l= new Label(composite, SWT.WRAP);
		l.setText(Utilities.getString("ComparePreferencePage.filter.description")); //$NON-NLS-1$
		
		Composite c3= new Composite(composite, SWT.NONE);
		c3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		c3.setLayout(layout);
		
		l= new Label(c3, SWT.NONE);
		l.setText(Utilities.getString("ComparePreferencePage.filter.label")); //$NON-NLS-1$
		
		fFilters= new Text(c3, SWT.BORDER);
		fFilters.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFilters.setText(fOverlayStore.getString(PATH_FILTER));
		fFilters.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					String filters= fFilters.getText();
					String message= CompareResourceFilter.validateResourceFilters(filters);
					setValid(message == null);
					setMessage(null);
					setErrorMessage(message);
					fOverlayStore.setValue(PATH_FILTER, filters);
				}
			}
		);
		
		return composite;
	}
	
	private Control createTextComparePage(Composite parent) {
		
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);
				
		addCheckBox(composite, "ComparePreferencePage.synchronizeScrolling.label", SYNCHRONIZE_SCROLLING, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.initiallyShowAncestorPane.label", INITIALLY_SHOW_ANCESTOR_PANE, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.showPseudoConflicts.label", SHOW_PSEUDO_CONFLICTS, 0);	//$NON-NLS-1$
		
		//addCheckBox(composite, "ComparePreferencePage.useSplines.label", USE_SPLINES, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.useSingleLine.label", USE_SINGLE_LINE, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.highlightTokenChanges.label", HIGHLIGHT_TOKEN_CHANGES, 0);	//$NON-NLS-1$
		//addCheckBox(composite, "ComparePreferencePage.useResolveUI.label", USE_RESOLVE_UI, 0);	//$NON-NLS-1$
		addCheckBox(composite, "ComparePreferencePage.disableCapping.label", CAPPING_DISABLED, 0);	//$NON-NLS-1$
		
		Composite radioGroup = new Composite(composite, SWT.NULL);
		radioGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		editor = new RadioGroupFieldEditor(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, CompareMessages.ComparePreferencePage_0, 1,
				new String[][] {
					new String[] { CompareMessages.ComparePreferencePage_1, ICompareUIConstants.PREF_VALUE_PROMPT },
				    new String[] { CompareMessages.ComparePreferencePage_2, ICompareUIConstants.PREF_VALUE_LOOP },
				    new String[] { CompareMessages.ComparePreferencePage_3, ICompareUIConstants.PREF_VALUE_NEXT },
					new String[] { CompareMessages.ComparePreferencePage_4, ICompareUIConstants.PREF_VALUE_DO_NOTHING}
				},
		radioGroup, true);
		editor.setPreferenceStore(fOverlayStore);
		editor.fillIntoGrid(radioGroup, 1);
		
		// a spacer
		Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setVisible(false);
		
		Label previewLabel= new Label(composite, SWT.NULL);
		previewLabel.setText(Utilities.getString("ComparePreferencePage.preview.label"));	//$NON-NLS-1$
		
		Control previewer= createPreviewer(composite);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(60);
		gd.heightHint= convertHeightInCharsToPixels(13);
		previewer.setLayoutData(gd);
		
		PreferenceLinkArea area = new PreferenceLinkArea(composite, SWT.NONE,
				"org.eclipse.ui.preferencePages.ColorsAndFonts", Utilities.getString("ComparePreferencePage.colorAndFontLink"), //$NON-NLS-1$ //$NON-NLS-2$
				(IWorkbenchPreferenceContainer)getContainer(), "selectCategory:org.eclipse.compare.contentmergeviewer.TextMergeViewer"); //$NON-NLS-1$

		GridData data= new GridData(SWT.FILL, SWT.CENTER, false, false);
		area.getControl().setLayoutData(data);
		
		return composite;
	}
	
	private Control createPreviewer(Composite parent) {
				
		fCompareConfiguration= new CompareConfiguration(fOverlayStore);
		fCompareConfiguration.setAncestorLabel(Utilities.getString("ComparePreferencePage.ancestor.label"));	//$NON-NLS-1$
		
		fCompareConfiguration.setLeftLabel(Utilities.getString("ComparePreferencePage.left.label"));	//$NON-NLS-1$
		fCompareConfiguration.setLeftEditable(false);
		
		fCompareConfiguration.setRightLabel(Utilities.getString("ComparePreferencePage.right.label"));	//$NON-NLS-1$
		fCompareConfiguration.setRightEditable(false);
		
		fPreviewViewer= new TextMergeViewer(parent, SWT.BORDER, fCompareConfiguration);
		
		fPreviewViewer.setInput(
			new DiffNode(Differencer.CONFLICTING,
				new FakeInput("ComparePreferencePage.previewAncestor"),	//$NON-NLS-1$
				new FakeInput("ComparePreferencePage.previewLeft"),	//$NON-NLS-1$
				new FakeInput("ComparePreferencePage.previewRight")	//$NON-NLS-1$
			)
		);

		Control c= fPreviewViewer.getControl();
		c.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (fCompareConfiguration != null)
					fCompareConfiguration.dispose();
			}
		});
		
		return  c;
	}
			
	private void initializeFields() {
		
		Iterator e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		if (fFilters != null)
			fFilters.setText(fOverlayStore.getString(PATH_FILTER));
		if (addedLinesRegex != null)
			addedLinesRegex.setText(fOverlayStore.getString(ADDED_LINES_REGEX));
		if (removedLinesRegex != null)
			removedLinesRegex.setText(fOverlayStore.getString(REMOVED_LINES_REGEX));
		
		editor.load();
	}

	// overlay stuff
	
	private Button addCheckBox(Composite parent, String labelKey, String key, int indentation) {
		
		String label= Utilities.getString(labelKey);
				
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		
		if (fCheckBoxListener == null) {
			fCheckBoxListener= new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Button button= (Button) e.widget;
					fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
				}
			};
		}
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}
	
	private String loadPreviewContentFromFile(String key) {
		
		String preview= Utilities.getString(key);
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < preview.length(); i++) {
			char c= preview.charAt(i);
			if (c == '\n')
				buffer.append(separator);
			else
				buffer.append(c);
		}
		return buffer.toString();
	}
}
