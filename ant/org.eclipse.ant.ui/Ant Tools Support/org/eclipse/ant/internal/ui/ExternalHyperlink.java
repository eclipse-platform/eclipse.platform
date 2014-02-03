/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ExternalHyperlink implements IHyperlink {

    private File fFile;
    private int fLineNumber;

    public ExternalHyperlink(File file, int lineNumber) {
        super();
        fFile = file;
        fLineNumber= lineNumber;
    }

    public void linkEntered() {
    }

    public void linkExited() {
    }

    public void linkActivated() {
    	IEditorInput input;
    	IFileStore fileStore;
		try {
			fileStore= EFS.getStore(fFile.toURI());
			input = new FileStoreEditorInput(fileStore);
		} catch (CoreException e) {
			// unable to link
			AntUIPlugin.log(e);
			return;
		}
    	
        IWorkbenchPage activePage = AntUIPlugin.getActiveWorkbenchWindow().getActivePage();
        try {
            IEditorPart editorPart= activePage.openEditor(input, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
            if (fLineNumber > 0 && editorPart instanceof ITextEditor) {
                ITextEditor textEditor = (ITextEditor)editorPart;
                
                    IDocumentProvider provider = textEditor.getDocumentProvider();
                    try {
                        provider.connect(input);
                    } catch (CoreException e) {
                        // unable to link
                        AntUIPlugin.log(e);
                        return;
                    }
                    IDocument document = provider.getDocument(input);
                    try {
                        IRegion lineRegion= document.getLineInformation(fLineNumber);
                        textEditor.selectAndReveal(lineRegion.getOffset(), lineRegion.getLength());
                    } catch (BadLocationException e) {
                        // unable to link
                        AntUIPlugin.log(e);
                    }
                    provider.disconnect(input);
                }
            
        } catch (PartInitException e) {
        }
    }
}