/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

public class AntUtilTests extends AbstractAntUITest {

    public AntUtilTests(String name) {
        super(name);
    }
    
    public void testGetTargetsLaunchConfiguration() throws CoreException {
       String buildFileName= "echoing";
       File buildFile= getBuildFile(buildFileName + ".xml");
       String arguments= null;
       Map properties= null;
       String propertyFiles= null;
       AntTargetNode[] targets= AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
       assertTrue(targets != null);
       assertTrue("Incorrect number of targets retrieved; should be 4 was: " + targets.length, targets.length == 4);
       assertContains("echo3", targets);
    }
    
    public void testGetTargetsLaunchConfigurationMinusD() throws CoreException {
        String buildFileName= "importRequiringUserProp";
        File buildFile= getBuildFile(buildFileName + ".xml");
        String arguments= "-DimportFileName=toBeImported.xml";
        Map properties= null;
        String propertyFiles= null;
        AntTargetNode[] targets= AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
        assertTrue(targets != null);
        assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3);
        assertContains("import-default", targets);
     }
    
    public void testGetTargetsLaunchConfigurationMinusDAndProperty() throws CoreException {
        String buildFileName= "importRequiringUserProp";
        File buildFile= getBuildFile(buildFileName + ".xml");
        String arguments= "-DimportFileName=toBeImported.xml";
        //arguments should win
        Map properties= new HashMap();
        properties.put("importFileName", "notToBeImported.xml");
        String propertyFiles= null;
        AntTargetNode[] targets= AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
        assertTrue(targets != null);
        assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3);
        assertContains("import-default", targets);
     }
    
    
    public void testGetTargetsLaunchConfigurationProperty() throws CoreException {
        String buildFileName= "importRequiringUserProp";
        File buildFile= getBuildFile(buildFileName + ".xml");
        String arguments= null;
        Map properties= new HashMap();
        properties.put("importFileName", "toBeImported.xml");
        String propertyFiles= null;
        AntTargetNode[] targets= AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
        assertTrue(targets != null);
        assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3);
        assertContains("import-default", targets);
     }
    
    public void testGetTargetsLaunchConfigurationPropertyFile() throws CoreException {
        String buildFileName= "importRequiringUserProp";
        File buildFile= getBuildFile(buildFileName + ".xml");
        String arguments= null;
        Map properties= null;
        String propertyFiles= "buildtest1.properties";
        AntTargetNode[] targets= AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
        assertTrue(targets != null);
        assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3);
        assertContains("import-default", targets);
     }
    
    protected ILaunchConfiguration getLaunchConfiguration(String buildFileName, String arguments, Map properties, String propertyFiles) throws CoreException {
        ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		if (arguments != null) {
		    copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);
		}
		if (properties != null) {
		    copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
		}
		if (propertyFiles != null) {
		    copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTY_FILES, propertyFiles);
		}
		return copy;
    }

    /**
 	 * Asserts that <code>displayString</code> is in one of the 
 	 * completion proposals.
 	 */
    private void assertContains(String targetName, AntTargetNode[] targets) {
        boolean found = false;
        for (int i = 0; i < targets.length; i++) {
            AntTargetNode target = targets[i];
            String foundName = target.getTargetName();
            if(targetName.equals(foundName)) {
                found = true;
                break;
            }
        }
        assertEquals("Did not find target: " + targetName, true, found);
    }        
}
