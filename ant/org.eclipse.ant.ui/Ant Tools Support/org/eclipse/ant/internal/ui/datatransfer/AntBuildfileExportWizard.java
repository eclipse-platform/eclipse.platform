/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.util.List;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Exports currently selected Eclipse Java project as an Ant buildfile.
 */
public class AntBuildfileExportWizard extends Wizard implements IExportWizard
{
    private IStructuredSelection fSelection;
    private AntBuildfileExportPage fMainPage;
    
    /**
     * Creates buildfile.
     */
    public boolean performFinish()
    {
        return fMainPage.generateBuildfiles();
    }
 
    public void addPages()
    {
        fMainPage = new AntBuildfileExportPage();
        List projects = fSelection.toList();
        fMainPage.setSelectedProjects(projects);
        addPage(fMainPage);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
        setWindowTitle(DataTransferMessages.AntBuildfileExportWizard_0);
    	setDefaultPageImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_EXPORT_WIZARD_BANNER));
    	setNeedsProgressMonitor(true);
        fSelection= selection;
    }
}
