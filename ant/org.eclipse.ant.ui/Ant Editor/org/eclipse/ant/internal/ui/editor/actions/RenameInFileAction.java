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

package org.eclipse.ant.internal.ui.editor.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.EditorSynchronizer;
import org.eclipse.ant.internal.ui.editor.OccurrencesFinder;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

public class RenameInFileAction extends SelectionDispatchAction {
	
	private AntEditor fEditor;
	
	public RenameInFileAction(AntEditor antEditor) {
		super(antEditor.getSite());
		fEditor= antEditor;
		setText(AntEditorActionMessages.getString("RenameInFileAction.0")); //$NON-NLS-1$
		setDescription(AntEditorActionMessages.getString("RenameInFileAction.1")); //$NON-NLS-1$
		setToolTipText(AntEditorActionMessages.getString("RenameInFileAction.2")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fEditor == null) {
			return;
		}
		
		ISourceViewer viewer= fEditor.getViewer();
		IDocument document= viewer.getDocument();
		int offset= ((ITextSelection)getSelection()).getOffset();
		LinkedPositionGroup group= new LinkedPositionGroup();
		OccurrencesFinder finder= new OccurrencesFinder(fEditor, fEditor.getAntModel(), document, offset);
		List positions= finder.perform();
		if (positions == null) {
			return;
		}
	
		addPositionsToGroup(offset, positions, document, group);
		if (group.isEmpty()) {
		    return;         
        }
		try {
			LinkedModeModel model= new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();
            model.addLinkingListener(new EditorSynchronizer(fEditor));
			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
			ui.enter();
			viewer.setSelectedRange(offset, 0);
		} catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
	}
	
    
    private void addPositionsToGroup(int offset, List positions, IDocument document, LinkedPositionGroup group) {
        Iterator iter= positions.iterator();
        int i= 0;
        int j= 0;
        int firstPosition= -1;
        try {
            while (iter.hasNext()) {
                Position position = (Position) iter.next();
                if (firstPosition == -1) {
                    if (position.overlapsWith(offset, 0)) {
                        firstPosition= i;
                        group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), j++));
                    }
                } else {
                    group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), j++));
                }
                i++;
            }
            
            for (i = 0; i < firstPosition; i++) {
                Position position= (Position) positions.get(i);
                group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), j++));
            }
        } catch (BadLocationException be) {
            AntUIPlugin.log(be);
        }
    }
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null);
	}

	/**
	 * Set the Ant editor associated with the action
	 * @param editor the ant editor to do the renames
	 */
	public void setEditor(AntEditor editor) {
		fEditor= editor;
	}
}