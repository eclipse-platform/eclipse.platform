/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.platform.internal;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.cheatsheets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.IStandbyContentPart;

public final class CheatSheetStandbyContent implements IStandbyContentPart {

    private IIntroPart introPart;
    private ICheatSheetViewer viewer;
    private Composite container;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#createControl(org.eclipse.swt.widgets.Composite,
     *      org.eclipse.ui.forms.widgets.FormToolkit)
     */
    public void createPartControl(Composite parent, FormToolkit toolkit) {
        container = toolkit.createComposite(parent);
        FillLayout layout = new FillLayout();
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);

        viewer = CheatSheetViewerFactory.createCheatSheetView();
        viewer.createPartControl(container);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#getControl()
     */
    public Control getControl() {
        return container;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#init(org.eclipse.ui.intro.IIntroPart)
     */
    public void init(IIntroPart introPart) {
        this.introPart = introPart;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#setInput(java.lang.Object)
     */
    public void setInput(Object input) {
        viewer.setInput((String)input);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#setFocus()
     */
    public void setFocus() {
    	viewer.setFocus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#dispose()
     */
    public void dispose() {
    	// do nothing
    }
}
