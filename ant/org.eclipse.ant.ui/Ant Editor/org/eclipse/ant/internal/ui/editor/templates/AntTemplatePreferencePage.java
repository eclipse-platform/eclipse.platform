/*******************************************************************************
 * Copyright (c) 2000, 2011  John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * 	   John-Mason P. Shackelford - initial API and implementation
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.templates;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

public class AntTemplatePreferencePage extends TemplatePreferencePage {

	private final FormattingPreferences fFormattingPreferences = new FormattingPreferences();

	public AntTemplatePreferencePage() {
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
		setTemplateStore(AntTemplateAccess.getDefault().getTemplateStore());
		setContextTypeRegistry(AntTemplateAccess.getDefault().getContextTypeRegistry());
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean performOk() {
		boolean ok = super.performOk();
		AntUIPlugin.getDefault().savePluginPreferences();
		return ok;
	}

	@Override
	protected SourceViewer createViewer(Composite parent) {
		SourceViewer viewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		SourceViewerConfiguration configuration = new AntTemplateViewerConfiguration();
		IDocument document = new Document();
		new AntDocumentSetupParticipant().setup(document);
		viewer.configure(configuration);
		viewer.setDocument(document);
		viewer.setEditable(false);
		Font font = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);

		return viewer;
	}

	@Override
	protected String getFormatterPreferenceKey() {
		return AntEditorPreferenceConstants.TEMPLATES_USE_CODEFORMATTER;
	}

	@Override
	protected void updateViewerInput() {
		IStructuredSelection selection = (IStructuredSelection) getTableViewer().getSelection();
		SourceViewer viewer = getViewer();

		if (selection.size() == 1 && selection.getFirstElement() instanceof TemplatePersistenceData) {
			TemplatePersistenceData data = (TemplatePersistenceData) selection.getFirstElement();
			Template template = data.getTemplate();
			if (AntUIPlugin.getDefault().getPreferenceStore().getBoolean(getFormatterPreferenceKey())) {
				String formatted = XmlFormatter.format(template.getPattern(), fFormattingPreferences);
				viewer.getDocument().set(formatted);
			} else {
				viewer.getDocument().set(template.getPattern());
			}
		} else {
			viewer.getDocument().set(IAntCoreConstants.EMPTY_STRING);
		}
	}

	@Override
	protected boolean isShowFormatterSetting() {
		return false;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IAntUIHelpContextIds.ANT_EDITOR_TEMPLATE_PREFERENCE_PAGE);
	}
}