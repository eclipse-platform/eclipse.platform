/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.internal.model.ConfigurationActivityModel;
public class ConfigurationActivity
	extends ConfigurationActivityModel
	implements IActivity {
		
	/**
	 * Default constructor
	 */		
	public ConfigurationActivity() {
	}
	
	/**
	 * Constructor with action
	 */
	public ConfigurationActivity(int action) {
		super();
		setAction(action);
		setStatus(STATUS_NOK);
	}
	
	/*
	 * @see IActivity#getInstallConfiguration()
	 */
	public IInstallConfiguration getInstallConfiguration() {
		return (IInstallConfiguration) getInstallConfigurationModel();
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof ConfigurationActivity))
			return false;
		if (this == other)
			return true;
		
		ConfigurationActivity activity = (ConfigurationActivity) other;
		return getAction() == activity.getAction()
				&& getLabel().equals(activity.getLabel())
				&& getStatus() == activity.getStatus();
	}
}
