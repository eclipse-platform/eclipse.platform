/*******************************************************************************
 * Copyright (c) 2002, 2004 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford - bug 40255
 *     Rob Dingwell - bug 68886
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import org.eclipse.ant.internal.ui.editor.derived.HTMLTextPresenter;
import org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormattingStrategy;
import org.eclipse.ant.internal.ui.editor.formatter.XmlElementFormattingStrategy;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.AntEditorPartitionScanner;
import org.eclipse.ant.internal.ui.editor.text.NotifyingReconciler;
import org.eclipse.ant.internal.ui.editor.text.XMLAnnotationHover;
import org.eclipse.ant.internal.ui.editor.text.XMLReconcilingStrategy;
import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.ColorManager;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

/**
 * The source viewer configuration for the Ant Editor.
 */
public class AntEditorSourceViewerConfiguration extends AbstractAntSourceViewerConfiguration {
    
    private AntEditor fEditor;

    private XMLTextHover fTextHover;
    
    private ContentAssistant contentAssistant;
    
    private AntAutoIndentStrategy autoIndentStrategy;
    
    /**
     * Creates an instance with the specified color manager.
     */
    public AntEditorSourceViewerConfiguration(AntEditor editor) {
	    super();
	    fEditor= editor;
    }    

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(ISourceViewer)
     */
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        contentAssistant= new ContentAssistant();
        AntEditorCompletionProcessor processor = new AntEditorCompletionProcessor(fEditor.getAntModel()); 
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		contentAssistant.setContentAssistProcessor(processor, AntEditorPartitionScanner.XML_TAG);
        contentAssistant.setDocumentPartitioning(AntDocumentSetupParticipant.ANT_PARTITIONING);
        
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
		
		String triggers= store.getString(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS);
		if (triggers != null) {
			processor.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
		}
				
		contentAssistant.enableAutoInsert(store.getBoolean(AntEditorPreferenceConstants.CODEASSIST_AUTOINSERT));
		contentAssistant.enableAutoActivation(store.getBoolean(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION));
		contentAssistant.setAutoActivationDelay(store.getInt(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY));
		contentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		ColorManager manager= ColorManager.getDefault();	
		Color background= getColor(store, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, manager);			
		contentAssistant.setContextInformationPopupBackground(background);
		contentAssistant.setContextSelectorBackground(background);
		contentAssistant.setProposalSelectorBackground(background);

		Color foreground= getColor(store, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, manager);
		contentAssistant.setContextInformationPopupForeground(foreground);
		contentAssistant.setContextSelectorForeground(foreground);
		contentAssistant.setProposalSelectorForeground(foreground);
			
        IInformationControlCreator creator = getInformationControlCreator(true);
		contentAssistant.setInformationControlCreator(creator);

        return contentAssistant;
    }

    protected IInformationControlCreator getInformationControlCreator(final boolean cutDown) {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                int style= cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
                return new DefaultInformationControl(parent, style, new HTMLTextPresenter(cutDown));
            }
        };
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
	    NotifyingReconciler reconciler= new NotifyingReconciler(new XMLReconcilingStrategy(fEditor), true);
	    reconciler.setDelay(XMLReconcilingStrategy.DELAY);
	    reconciler.addReconcilingParticipant(fEditor);
	    return reconciler;
    }

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new XMLAnnotationHover();
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationControlCreator(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return getInformationControlCreator(true);
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (fTextHover == null) {
			fTextHover= new XMLTextHover(fEditor);
		}
		return fTextHover;
	}

	private Color getColor(IPreferenceStore store, String key, ColorManager manager) {
		RGB rgb= PreferenceConverter.getColor(store, key);
		return manager.getColor(rgb);
	}
	
	protected void changeConfiguration(PropertyChangeEvent event) {
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
		String p= event.getProperty();

		ColorManager manager= ColorManager.getDefault();
		if (AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION.equals(p)) {
			boolean enabled= store.getBoolean(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION);
			contentAssistant.enableAutoActivation(enabled);
		} else if (AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY.equals(p) && contentAssistant != null) {
			int delay= store.getInt(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY);
			contentAssistant.setAutoActivationDelay(delay);
		} else if (AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND.equals(p) && contentAssistant != null) {
			Color c= getColor(store, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, manager);
			contentAssistant.setProposalSelectorForeground(c);
		} else if (AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND.equals(p) && contentAssistant != null) {
			Color c= getColor(store, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, manager);
			contentAssistant.setProposalSelectorBackground(c);
		} else if (AntEditorPreferenceConstants.CODEASSIST_AUTOINSERT.equals(p) && contentAssistant != null) {
			boolean enabled= store.getBoolean(AntEditorPreferenceConstants.CODEASSIST_AUTOINSERT);
			contentAssistant.enableAutoInsert(enabled);
		} else if (AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS.equals(p)) {
			changeContentAssistProcessor(store);
		}
	}
	
	private void changeContentAssistProcessor(IPreferenceStore store) {
		String triggers= store.getString(AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS);
		if (triggers != null) {
			AntEditorCompletionProcessor cp= (AntEditorCompletionProcessor)contentAssistant.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
			if (cp != null) {
				cp.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
			}
		}		
	}
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentFormatter(org.eclipse.jface.text.source.ISourceViewer)
     */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		 
		MultiPassContentFormatter formatter = new MultiPassContentFormatter(
				getConfiguredDocumentPartitioning(sourceViewer),
		         IDocument.DEFAULT_CONTENT_TYPE);
		 
		
		formatter.setMasterStrategy(new XmlDocumentFormattingStrategy());
		
		formatter.setSlaveStrategy(new XmlElementFormattingStrategy(), AntEditorPartitionScanner.XML_TAG);
		
		//formatter.setSlaveStrategy(new XmlCommentFormattingStrategy(), AntEditorPartitionScanner.XML_COMMENT);
		 
		return formatter;
	}
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoIndentStrategy(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
		if (AntEditorPartitionScanner.XML_COMMENT.equals(contentType)) {
			return super.getAutoIndentStrategy(sourceViewer, contentType);
		} 
		if (autoIndentStrategy == null) {
			autoIndentStrategy= new AntAutoIndentStrategy(fEditor.getAntModel());
		}
		return autoIndentStrategy;
	}
}