/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.support;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.w3c.dom.Element;

public class TestTextCompletionProcessor extends AntEditorCompletionProcessor {

	public final static int TEST_PROPOSAL_MODE_NONE = AntEditorCompletionProcessor.PROPOSAL_MODE_NONE;
	public final static int TEST_PROPOSAL_MODE_BUILDFILE = AntEditorCompletionProcessor.PROPOSAL_MODE_BUILDFILE;
	public final static int TEST_PROPOSAL_MODE_TASK_PROPOSAL = AntEditorCompletionProcessor.PROPOSAL_MODE_TASK_PROPOSAL;
	public final static int TEST_PROPOSAL_MODE_PROPERTY_PROPOSAL = AntEditorCompletionProcessor.PROPOSAL_MODE_PROPERTY_PROPOSAL;
	public final static int TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL = AntEditorCompletionProcessor.PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
	public final static int TEST_PROPOSAL_MODE_TASK_PROPOSAL_CLOSING = AntEditorCompletionProcessor.PROPOSAL_MODE_TASK_PROPOSAL_CLOSING;
	public final static int TEST_PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL = AntEditorCompletionProcessor.PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL;
	
	private File fEditedFile;
	private ISourceViewer fViewer;
    private boolean fNeedsToDispose= true;
    
    

	public TestTextCompletionProcessor(AntModel model) {
		super(model);
        fContentAssistant= new IContentAssistantExtension2() {
            public void setEmptyMessage(String message) {
            }
        
            public void setStatusMessage(String message) {
            }
        
            public void setStatusLineVisible(boolean show) {
            }
        
            public void setShowEmptyList(boolean showEmpty) {
            }
        
            public void setRepeatedInvocationMode(boolean cycling) {
            }
        
            public void removeCompletionListener(ICompletionListener listener) {
            }
        
            public void addCompletionListener(ICompletionListener listener) {
        
            }        
        };
	}
	
	public TestTextCompletionProcessor(AntEditor editor) {
		this(editor.getAntModel());
		fViewer= editor.getViewer();
        fNeedsToDispose= false;
	}
	
	public TestTextCompletionProcessor() {
		this((AntModel)null);
	}
	
    public ICompletionProposal[] getAttributeProposals(String taskName, String prefix) {
    	if (cursorPosition == -1) {
    		cursorPosition= taskName.length();
    	}
        return super.getAttributeProposals(taskName, prefix);
    }

    public Element findChildElementNamedOf(Element anElement, String childElementName) {
        return super.findChildElementNamedOf(anElement, childElementName);
    }

    public ICompletionProposal[] getTaskProposals(String text, String parentName, String prefix) {
    	cursorPosition= Math.max(0, text.length() - 1);
        return super.getTaskProposals(new Document(text), parentName, prefix);
    }
    
    public ICompletionProposal[] getTaskProposals(IDocument document, String parentName, String aPrefix) {
    	cursorPosition= Math.max(0, document.getLength() - 1);
    	return super.getTaskProposals(document, parentName, aPrefix);
    }

    public int determineProposalMode(String text, int theCursorPosition, String prefix) {
        return super.determineProposalMode(new Document(text), theCursorPosition, prefix);
    }

    public String getParentName(String text, int aLineNumber, int aColumnNumber) {
        return super.getParentName(new Document(text), aLineNumber, aColumnNumber);
    }
    
    public String getParentName(IDocument doc, int aLineNumber, int aColumnNumber) {
    	return super.getParentName(doc, aLineNumber, aColumnNumber);
    }

    public String getPrefixFromDocument(String aDocumentText, int anOffset) {
        String prefix= super.getPrefixFromDocument(aDocumentText, anOffset);
        currentPrefix= null;
        return prefix;
    }

    public ICompletionProposal[] getPropertyProposals(IDocument document, String prefix, int cursorPos) {
        return super.getPropertyProposals(document, prefix, cursorPos);
    }

    /**
     * Returns the edited File that org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor sets or a temporary 
     * file, which only serves as a dummy.
     * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getEditedFile()
     */
	public File getEditedFile() {
		if (fEditedFile != null){
			return fEditedFile;
		}
		File tempFile = null;
        try {
            tempFile = File.createTempFile("test", null);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

	public void setLineNumber(int aLineNumber) {
    	lineNumber = aLineNumber;
    }

	public void setColumnNumber(int aColumnNumber) {
    	columnNumber = aColumnNumber;
    }
	
	public void setCursorPosition(int cursorPosition) {
		this.cursorPosition = cursorPosition;
	}
    
	public void setEditedFile(File aFile) {
		fEditedFile= aFile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getTargetAttributeValueProposals(org.eclipse.jface.text.IDocument, java.lang.String, java.lang.String, java.lang.String)
	 */
	public ICompletionProposal[] getTargetAttributeValueProposals(IDocument document, String textToSearch, String prefix, String attributeName) {
		return super.getTargetAttributeValueProposals(document, textToSearch, prefix, attributeName);
	}
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getAntCallAttributeValueProposals(org.eclipse.jface.text.IDocument, java.lang.String, java.lang.String)
     */
    public ICompletionProposal[] getAntCallAttributeValueProposals(IDocument document, String prefix, String attributeName) {
        return super.getAntCallAttributeValueProposals(document, prefix, attributeName);
    }
	/**
	 * Since the testing occurs without necessarily having an associated viewer, return
	 * a dummy value.
	 */
	protected char getPreviousChar() {
		return '?';
	}
	
	 /**
     * Returns whether the specified task name is known.
     */
    protected boolean isKnownElement(String elementName) {
    	if (antModel != null) {
    		return super.isKnownElement(elementName);
    	} 
    	return getDtd().getElement(elementName) != null ;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getProposalsFromDocument(org.eclipse.jface.text.IDocument, java.lang.String)
	 */
	public ICompletionProposal[] getProposalsFromDocument(IDocument document, String prefix) {
		return super.getProposalsFromDocument(document, prefix);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getBuildFileProposals(org.eclipse.jface.text.IDocument, java.lang.String)
	 */
	public ICompletionProposal[] getBuildFileProposals(String text, String prefix) {
		return super.getBuildFileProposals(new Document(text), prefix);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#determineTemplateProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] determineTemplateProposals() {
		return super.determineTemplateProposals(fViewer, cursorPosition);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(int documentOffset) {
		return super.computeCompletionProposals(fViewer, documentOffset);
	}
    
    public void dispose() {
        if (fNeedsToDispose && antModel != null) {
            //not working with an editor
            antModel.dispose();
        }
    }
}
