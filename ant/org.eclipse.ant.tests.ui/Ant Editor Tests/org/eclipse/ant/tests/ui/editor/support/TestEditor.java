/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.support;

import org.eclipse.ant.internal.ui.editor.AntEditor;

public class TestEditor extends AntEditor {
	
	TestEditor() {
	}
	
    public void initializeEditor() {
    }
    /** 
     * Returns '10'.
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#getCursorPosition()
     */
    protected String getCursorPosition() {
        return "10";
    }

}