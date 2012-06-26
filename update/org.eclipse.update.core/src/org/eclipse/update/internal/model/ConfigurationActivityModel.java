/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.model;
 
import java.util.Date;

import org.eclipse.update.core.model.*;

public class ConfigurationActivityModel extends ModelObject{
	
	private String label;
	private int action;
	private Date date;
	private int status;
	private InstallConfigurationModel installConfiguration;
	

	/**
	 * Constructor for ConfigurationActivityModel.
	 */
	public ConfigurationActivityModel() {
		super();
	}

	/**
	 * @since 2.0
	 */
	public int getAction() {
		return action;
	}

	/**
	 * @since 2.0
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @since 2.0
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the date.
	 * @param date The date to set
	 */
	public void setDate(Date date) {
		assertIsWriteable();
		this.date = date;
	}

	/**
	 * Sets the status.
	 * @param status The status to set
	 */
	public void setStatus(int status) {
		assertIsWriteable();
		this.status = status;
	}

	/**
	 * @since 2.0
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label.
	 * @param label The label to set
	 */
	public void setLabel(String label) {
		assertIsWriteable();
		this.label = label;
	}

	/**
	 * Sets the action.
	 * @param action The action to set
	 */
	public void setAction(int action) {
		assertIsWriteable();
		this.action = action;
	}

	/**
	 * Gets the installConfiguration.
	 * @return Returns a InstallConfigurationModel
	 */
	public InstallConfigurationModel getInstallConfigurationModel() {
		return installConfiguration;
	}

	/**
	 * Sets the installConfiguration.
	 * @param installConfiguration The installConfiguration to set
	 */
	public void setInstallConfigurationModel(InstallConfigurationModel installConfiguration) {
		assertIsWriteable();		
		this.installConfiguration = installConfiguration;
	}

}

