/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.testplugin;


import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;


public class AntTestPlugin extends AbstractUIPlugin {
	
	private static AntTestPlugin deflt;
	
	public AntTestPlugin() {
		super();
		deflt= this;
	}
	
	public static AntTestPlugin getDefault() {
		return deflt;
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static void enableAutobuild(boolean enable) throws CoreException {
		// disable auto build
		IWorkspace workspace= AntTestPlugin.getWorkspace();
		IWorkspaceDescription desc= workspace.getDescription();
		desc.setAutoBuilding(enable);
		workspace.setDescription(desc);
	}
	
	public File getFileInPlugin(IPath path) {
		try {
			Bundle bundle = getDefault().getBundle();
			URL installURL= new URL(bundle.getEntry("/"), path.toString());
			URL localURL= Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}
}
