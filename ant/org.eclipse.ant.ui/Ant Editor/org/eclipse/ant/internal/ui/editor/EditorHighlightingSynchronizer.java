/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;


/**
 * Turns off occurrences highlighting on an Ant editor until linked mode is
 * left.
 * 
 * @since 3.1
 */
public class EditorHighlightingSynchronizer implements ILinkedModeListener {
    
    private final AntEditor fEditor;
    private final boolean fWasOccurrencesOn;
    
    /**
     * Creates a new synchronizer.
     * 
     * @param editor the Ant editor the occurrences markers of which will be 
     *        synchonized with the linked mode
     * 
     */
    public EditorHighlightingSynchronizer(AntEditor editor) {
        Assert.isLegal(editor != null);
        fEditor= editor;
        fWasOccurrencesOn= fEditor.isMarkingOccurrences();
        
        if (fWasOccurrencesOn)
            fEditor.uninstallOccurrencesFinder();
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    public void left(LinkedModeModel environment, int flags) {
        if (fWasOccurrencesOn) {
            fEditor.installOccurrencesFinder();
        }
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#suspend(org.eclipse.jface.text.link.LinkedModeModel)
     */
    public void suspend(LinkedModeModel environment) {
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#resume(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    public void resume(LinkedModeModel environment, int flags) {
    }
}