/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.performance;

import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.tests.ui.editor.performance.EditorTestHelper;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

public class OpenLaunchConfigurationDialogTests extends PerformanceTestCase {

    public static String fgIdentifier= IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP;
    
    public void testOpenAntLaunchConfigurationDialog1() {
        //cold run
        ILaunchConfiguration configuration= getLaunchConfiguration("big");
		IStructuredSelection selection= new StructuredSelection(configuration);
		for (int i = 0; i < 10; i++) {
		    openLCD(selection, fgIdentifier, 20); 
        }
		
		commitMeasurements();
		assertPerformance();
    }
    
    public void testOpenAntLaunchConfigurationDialog2() {
        //warm run
        ILaunchConfiguration configuration= getLaunchConfiguration("big");
		IStructuredSelection selection= new StructuredSelection(configuration);
		tagAsSummary("Open LCD on Targets tab", Dimension.CPU_TIME);
		for (int i = 0; i < 10; i++) {
		    openLCD(selection, fgIdentifier, 20); 
        }
		
		commitMeasurements();
		assertPerformance();
    }

    private ILaunchConfiguration getLaunchConfiguration(String buildFileName) {
        IFile file = AbstractAntUITest.getJavaProject().getProject().getFolder("launchConfigurations").getFile(buildFileName + ".launch");
		ILaunchConfiguration config = AbstractAntUITest.getLaunchManager().getLaunchConfiguration(file);
		assertTrue("Could not find launch configuration for " + buildFileName, config.exists());
		return config;
    }

    private void openLCD(final IStructuredSelection selection, final String groupIdentifier, int numberOfOpens) {
        startMeasuring();
        for (int i = 0; i < numberOfOpens; i++) {
	        //set a status to go to the targets tab
		    IStatus status = new Status(IStatus.INFO, IAntUIConstants.PLUGIN_ID, IAntUIConstants.STATUS_INIT_RUN_ANT, "", null); //$NON-NLS-1$
			LaunchConfigurationsDialog dialog= new LaunchConfigurationsDialog(DebugUIPlugin.getShell(), DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(groupIdentifier));
			dialog.setBlockOnOpen(false);
			dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION);
			dialog.setInitialSelection(selection);
			dialog.setInitialStatus(status);
			
			dialog.open();
            EditorTestHelper.runEventQueue(dialog.getShell());
			dialog.close();
        }
		stopMeasuring();
    }

    /* (non-Javadoc)
     * @see org.eclipse.test.performance.PerformanceTestCase#setUp()
     */
    protected void setUp() throws Exception {
       super.setUp();
       EditorTestHelper.runEventQueue();
    }
}
