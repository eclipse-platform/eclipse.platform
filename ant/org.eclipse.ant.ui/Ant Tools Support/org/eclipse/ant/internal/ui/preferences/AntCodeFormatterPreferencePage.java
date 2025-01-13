/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

/*
 * The page to configure the code formatter options.
 */
public class AntCodeFormatterPreferencePage extends AbstractAntEditorPreferencePage {

	private SourceViewer fPreviewViewer;

	@Override
	protected OverlayPreferenceStore createOverlayStore() {
		List<OverlayPreferenceStore.OverlayKey> overlayKeys = new ArrayList<>();
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.FORMATTER_WRAP_LONG));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.FORMATTER_ALIGN));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.FORMATTER_TAB_CHAR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AntEditorPreferenceConstants.FORMATTER_TAB_SIZE));

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return new OverlayPreferenceStore(getPreferenceStore(), keys);
	}

	@Override
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		getOverlayStore().load();
		getOverlayStore().start();
		int numColumns = 2;
		Composite result = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		result.setLayout(layout);

		Group indentationGroup = createGroup(numColumns, result, AntPreferencesMessages.AntCodeFormatterPreferencePage_0);

		String labelText = AntPreferencesMessages.AntCodeFormatterPreferencePage_1;
		String[] errorMessages = new String[] { AntPreferencesMessages.AntCodeFormatterPreferencePage_2,
				AntPreferencesMessages.AntCodeFormatterPreferencePage_3 };
		addTextField(indentationGroup, labelText, AntEditorPreferenceConstants.FORMATTER_TAB_SIZE, 3, 0, errorMessages);

		labelText = AntPreferencesMessages.AntCodeFormatterPreferencePage_4;
		addCheckBox(indentationGroup, labelText, AntEditorPreferenceConstants.FORMATTER_TAB_CHAR, 1);

		Group wrappingGroup = createGroup(numColumns, result, AntPreferencesMessages.AntCodeFormatterPreferencePage_6);
		labelText = AntPreferencesMessages.AntCodeFormatterPreferencePage_7;
		errorMessages = new String[] { AntPreferencesMessages.AntCodeFormatterPreferencePage_8,
				AntPreferencesMessages.AntCodeFormatterPreferencePage_9 };
		addTextField(wrappingGroup, labelText, AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH, 3, 0, errorMessages);
		labelText = AntPreferencesMessages.AntCodeFormatterPreferencePage_10;
		addCheckBox(wrappingGroup, labelText, AntEditorPreferenceConstants.FORMATTER_WRAP_LONG, 1);
		labelText = AntPreferencesMessages.AntCodeFormatterPreferencePage_5;
		addCheckBox(wrappingGroup, labelText, AntEditorPreferenceConstants.FORMATTER_ALIGN, 1);

		Label label = new Label(result, SWT.LEFT);
		label.setText(AntPreferencesMessages.AntEditorPreferencePage_9);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control previewer = createPreviewer(result);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(20);
		gd.heightHint = convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		initializeFields();

		applyDialogFont(result);

		return result;
	}

	/**
	 * Convenience method to create a group
	 */
	private Group createGroup(int numColumns, Composite parent, String text) {
		final Group group = new Group(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = numColumns;
		gd.widthHint = 0;
		group.setLayoutData(gd);
		group.setFont(parent.getFont());

		final GridLayout layout = new GridLayout(numColumns, false);
		group.setLayout(layout);
		group.setText(text);
		return group;
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

		String content = loadPreviewContentFromFile("FormatPreviewCode.txt"); //$NON-NLS-1$
		content = formatContent(content, store);
		IDocument document = new Document(content);
		new AntDocumentSetupParticipant().setup(document);
		fPreviewViewer.setDocument(document);

		return fPreviewViewer.getControl();
	}

	private String formatContent(String content, IPreferenceStore preferenceStore) {
		FormattingPreferences prefs = new FormattingPreferences();
		prefs.setPreferenceStore(preferenceStore);
		return XmlFormatter.format(content, prefs);
	}

	@Override
	protected void handleDefaults() {
		// do nothing
	}

	@Override
	protected String getHelpContextId() {
		return IAntUIHelpContextIds.ANT_EDITOR_FORMATTER_PREFERENCE_PAGE;
	}
}