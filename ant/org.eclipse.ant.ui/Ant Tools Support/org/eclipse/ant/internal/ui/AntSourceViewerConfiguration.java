/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui;

import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.AntEditorPartitionScanner;
import org.eclipse.ant.internal.ui.editor.text.AntEditorProcInstrScanner;
import org.eclipse.ant.internal.ui.editor.text.AntEditorTagScanner;
import org.eclipse.ant.internal.ui.editor.text.IAntEditorColorConstants;
import org.eclipse.ant.internal.ui.editor.text.MultilineDamagerRepairer;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
	    PresentationReconciler reconciler = new PresentationReconciler();
	    reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
	    
		MultilineDamagerRepairer dr = new MultilineDamagerRepairer(getDefaultScanner());
	    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
	    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	
	    dr = new MultilineDamagerRepairer(getTagScanner());
	    reconciler.setDamager(dr, AntEditorPartitionScanner.XML_TAG);
	    reconciler.setRepairer(dr, AntEditorPartitionScanner.XML_TAG);
	
	    int style= getStyle(IAntEditorColorConstants.XML_COMMENT_COLOR);
	    xmlCommentAttribute=  new TextAttribute(AntUIPlugin.getPreferenceColor(IAntEditorColorConstants.XML_COMMENT_COLOR), null, style);
		damageRepairer= new MultilineDamagerRepairer(null, xmlCommentAttribute);
	    reconciler.setDamager(damageRepairer, AntEditorPartitionScanner.XML_COMMENT);
	    reconciler.setRepairer(damageRepairer, AntEditorPartitionScanner.XML_COMMENT);
	    
	    style= getStyle(IAntEditorColorConstants.XML_DTD_COLOR);
	    xmlDtdAttribute=  new TextAttribute(AntUIPlugin.getPreferenceColor(IAntEditorColorConstants.XML_DTD_COLOR), null, style);
		dtdDamageRepairer= new MultilineDamagerRepairer(null, xmlDtdAttribute);
	    reconciler.setDamager(dtdDamageRepairer, AntEditorPartitionScanner.XML_DTD);
	    reconciler.setRepairer(dtdDamageRepairer, AntEditorPartitionScanner.XML_DTD);
	
	    return reconciler;
	}

	private int getStyle(String pref) {
		int style= SWT.NORMAL;
	    if (fPreferenceStore.getBoolean(pref + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX)) {
	    	style |= SWT.BOLD;
	    }
	    if (fPreferenceStore.getBoolean(pref + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)) {
	    	style |= SWT.ITALIC;
	    }
		return style;
	}

	/**
	 * Preference colors have changed.  
	 * Update the default tokens of the scanners.
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (tagScanner == null) {
			return; //property change before the editor is fully created
		}
		tagScanner.adaptToPreferenceChange(event);
		instructionScanner.adaptToPreferenceChange(event);
		String property= event.getProperty();
		if (property.startsWith(IAntEditorColorConstants.XML_COMMENT_COLOR)) {
			xmlCommentAttribute= adaptTextAttribute(event, property, xmlCommentAttribute, damageRepairer);
		} else if (property.startsWith(IAntEditorColorConstants.XML_DTD_COLOR)) {
			xmlDtdAttribute= adaptTextAttribute(event, property, xmlDtdAttribute, dtdDamageRepairer);
		} 
	}
	
	private TextAttribute adaptTextAttribute(PropertyChangeEvent event, String property, TextAttribute textAttribute, MultilineDamagerRepairer repairer) {
		if (property.endsWith(AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX)) {
			textAttribute= adaptToStyleChange(event, SWT.BOLD, textAttribute);
		} else if (property.endsWith(AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)) {
			textAttribute= adaptToStyleChange(event, SWT.ITALIC, textAttribute);
		} else {
			textAttribute= adaptToColorChange(event, textAttribute);
		}
		repairer.setDefaultTextAttribute(textAttribute);
		return textAttribute;
	}

	private TextAttribute adaptToStyleChange(PropertyChangeEvent event, int styleAttribute, TextAttribute textAttribute) {
		boolean eventValue= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean) {
			eventValue= ((Boolean) value).booleanValue();
		} else if (IPreferenceStore.TRUE.equals(value)) {
			eventValue= true;
		}
		
		boolean activeValue= (textAttribute.getStyle() & styleAttribute) == styleAttribute;
		if (activeValue != eventValue) { 
			textAttribute= new TextAttribute(textAttribute.getForeground(), textAttribute.getBackground(), eventValue ? textAttribute.getStyle() | styleAttribute : textAttribute.getStyle() & ~styleAttribute);
		}	
		return textAttribute;
	}
	
	 /**
     * Update the text attributes associated with the tokens of this scanner as a color preference has been changed. 
     */
    private TextAttribute adaptToColorChange(PropertyChangeEvent event, TextAttribute textAttribute) {
    	RGB rgb= null;
		
		Object value= event.getNewValue();
		if (value instanceof RGB) {
			rgb= (RGB) value;
		} else if (value instanceof String) {
			rgb= StringConverter.asRGB((String) value);
		}
			
		if (rgb != null) {
			textAttribute= new TextAttribute(ColorManager.getDefault().getColor(rgb), textAttribute.getBackground(), textAttribute.getStyle());
		}
		return textAttribute;
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
	    return new String[] {
	        IDocument.DEFAULT_CONTENT_TYPE,
	        AntEditorPartitionScanner.XML_COMMENT,
	        AntEditorPartitionScanner.XML_TAG,
			AntEditorPartitionScanner.XML_CDATA,
			AntEditorPartitionScanner.XML_DTD};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTabWidth(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return fPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
	}
	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property= event.getProperty();
		return property.startsWith(IAntEditorColorConstants.TEXT_COLOR) ||
			property.startsWith(IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR) ||
			property.startsWith(IAntEditorColorConstants.STRING_COLOR) ||
			property.startsWith(IAntEditorColorConstants.TAG_COLOR) ||
			property.startsWith(IAntEditorColorConstants.XML_COMMENT_COLOR) || 
			property.startsWith(IAntEditorColorConstants.XML_DTD_COLOR);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return AntDocumentSetupParticipant.ANT_PARTITIONING;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getDoubleClickStrategy(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (AntEditorPartitionScanner.XML_TAG.equals(contentType)) {
			return new AntDoubleClickStrategy();
		}
		return super.getDoubleClickStrategy(sourceViewer, contentType);
	}
}
