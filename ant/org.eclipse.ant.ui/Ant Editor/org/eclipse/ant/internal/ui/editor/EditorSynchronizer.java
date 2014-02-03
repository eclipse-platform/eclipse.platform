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
package org.eclipse.ant.internal.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;

/**
 * Synchronizes the Ant editor with the state of its linked mode
 * 
 * @since 3.1
 */
public class EditorSynchronizer implements ILinkedModeListener {
    
    private final AntEditor fEditor;
    private final boolean fWasOccurrencesOn;
    
    /**
     * Creates a new synchronizer.
     * 
     * @param editor the Ant editor that will be 
     *        synchonized with the linked mode
     */
    public EditorSynchronizer(AntEditor editor) {
        Assert.isLegal(editor != null);
        fEditor= editor;
        fWasOccurrencesOn= fEditor.isMarkingOccurrences();
        fEditor.setInLinkedMode(true, fWasOccurrencesOn);
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    public void left(LinkedModeModel environment, int flags) {
    	fEditor.setInLinkedMode(false, fWasOccurrencesOn);
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