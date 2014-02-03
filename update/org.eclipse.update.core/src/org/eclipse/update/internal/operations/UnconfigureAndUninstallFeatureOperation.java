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
package org.eclipse.update.internal.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.operations.IOperationListener;
import org.eclipse.update.operations.IUnconfigFeatureOperation;
import org.eclipse.update.operations.OperationsManager;

public class UnconfigureAndUninstallFeatureOperation extends FeatureOperation
		implements IUnconfigureAndUninstallFeatureOperation {

	public UnconfigureAndUninstallFeatureOperation(IConfiguredSite targetSite, IFeature feature) {
		super(targetSite, feature);
	}

	public boolean execute(IProgressMonitor pm, IOperationListener listener)
			throws CoreException, InvocationTargetException {
		
		IUnconfigFeatureOperation unconfigOperation = OperationsManager.getOperationFactory().createUnconfigOperation(targetSite, feature);

		/*boolean isRestartNeeded = */unconfigOperation.execute(pm, listener);
		

		URL platformXML = ConfiguratorUtils.getCurrentPlatformConfiguration().getConfigurationLocation();
		
		File f = new File(platformXML.getFile());

		f = new File(f.getParentFile(), "toBeUninstalled"); //$NON-NLS-1$
		try {
			if (!f.exists()) {
				f.createNewFile();
			}
			FileInputStream fis = new FileInputStream(f);
			Properties toBeUninstalled = new Properties();
			toBeUninstalled.load(fis);
			toBeUninstalled.put(new Integer(toBeUninstalled.size()+1).toString(), targetSite.getSite().getURL() + ";" + feature.getVersionedIdentifier().toString()); //$NON-NLS-1$
			fis.close();
			FileOutputStream fos = new FileOutputStream(f);
			toBeUninstalled.store(fos, "to be uninstalled on start-up"); //$NON-NLS-1$
			fos.close();
			
		} catch (IOException ioe) {
			
		}

		
		return true;
	}

}
