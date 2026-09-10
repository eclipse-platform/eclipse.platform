/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui;

import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.AntEditorPartitionScanner;
import org.eclipse.ant.internal.ui.editor.text.AntEditorProcInstrScanner;
import org.eclipse.ant.internal.ui.editor.text.AntEditorTagScanner;
import org.eclipse.ant.internal.ui.editor.text.MultilineDamagerRepairer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class AntSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private AntEditorTagScanner tagScanner;
	private AntEditorProcInstrScanner instructionScanner;
	private MultilineDamagerRepairer damageRepairer;
	private MultilineDamagerRepairer dtdDamageRepairer;
	private TextAttribute xmlCommentAttribute;
	private TextAttribute xmlDtdAttribute;

	public AntSourceViewerConfiguration() {
		super(AntUIPlugin.getDefault().getCombinedPreferenceStore());
	}

	private AntEditorProcInstrScanner getDefaultScanner() {
		if (instructionScanner == null) {
			instructionScanner = new AntEditorProcInstrScanner();
		}
		return instructionScanner;
	}

	private AntEditorTagScanner getTagScanner() {
		if (tagScanner == null) {
			tagScanner = new AntEditorTagScanner();
		}
		return tagScanner;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		MultilineDamagerRepairer dr = new MultilineDamagerRepairer(getDefaultScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new MultilineDamagerRepairer(getTagScanner());
		reconciler.setDamager(dr, AntEditorPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, AntEditorPartitionScanner.XML_TAG);

		xmlCommentAttribute = new TextAttribute(AntUIPlugin.getThemeColor(SyntaxThemeConstants.COMMENT_COLOR));
		damageRepairer = new MultilineDamagerRepairer(null, xmlCommentAttribute);
		reconciler.setDamager(damageRepairer, AntEditorPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(damageRepairer, AntEditorPartitionScanner.XML_COMMENT);

		xmlDtdAttribute = new TextAttribute(AntUIPlugin.getThemeColor(SyntaxThemeConstants.DIRECTIVE_COLOR));
		dtdDamageRepairer = new MultilineDamagerRepairer(null, xmlDtdAttribute);
		reconciler.setDamager(dtdDamageRepairer, AntEditorPartitionScanner.XML_DTD);
		reconciler.setRepairer(dtdDamageRepairer, AntEditorPartitionScanner.XML_DTD);

		return reconciler;
	}

	/**
	 * Preference colors have changed. Update the default tokens of the scanners.
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (tagScanner == null) {
			return; // property change before the editor is fully created
		}
		tagScanner.adaptToPreferenceChange(event);
		instructionScanner.adaptToPreferenceChange(event);
		String property = event.getProperty();
		if (property.endsWith(SyntaxThemeConstants.COMMENT_COLOR)) {
			xmlCommentAttribute = adaptTextAttribute(SyntaxThemeConstants.COMMENT_COLOR, damageRepairer);
		} else if (property.endsWith(SyntaxThemeConstants.DIRECTIVE_COLOR)) {
			xmlDtdAttribute = adaptTextAttribute(SyntaxThemeConstants.DIRECTIVE_COLOR, dtdDamageRepairer);
		}
	}

	private TextAttribute adaptTextAttribute(String colorId, MultilineDamagerRepairer repairer) {
		TextAttribute textAttribute = new TextAttribute(AntUIPlugin.getThemeColor(colorId));
		repairer.setDefaultTextAttribute(textAttribute);
		return textAttribute;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, AntEditorPartitionScanner.XML_COMMENT, AntEditorPartitionScanner.XML_TAG,
				AntEditorPartitionScanner.XML_CDATA, AntEditorPartitionScanner.XML_DTD };
	}

	@Override
	public int getTabWidth(ISourceViewer sourceViewer) {
		return fPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.endsWith(SyntaxThemeConstants.DIRECTIVE_COLOR) || property.endsWith(SyntaxThemeConstants.STRING_COLOR)
				|| property.endsWith(SyntaxThemeConstants.TAG_COLOR) || property.endsWith(SyntaxThemeConstants.ATTRIBUTE_NAME_COLOR)
				|| property.endsWith(SyntaxThemeConstants.COMMENT_COLOR);
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return AntDocumentSetupParticipant.ANT_PARTITIONING;
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (AntEditorPartitionScanner.XML_TAG.equals(contentType)) {
			return new AntDoubleClickStrategy();
		}
		return super.getDoubleClickStrategy(sourceViewer, contentType);
	}
}
