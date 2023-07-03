/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Bug 552773 - Simplify logging in platform code base
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.registry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.cheatsheets.CheatSheetListener;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.osgi.framework.Bundle;

/**
 *	Instances represent registered cheatsheets or cheatsheets opened using the content file.
 */
public class CheatSheetElement extends WorkbenchAdapter implements IAdaptable, IPluginContribution {
	private String contentFile;
	private String id;
	private String name;
	private String description;
	private IConfigurationElement configurationElement;
	private String listenerClass;
	private boolean composite;
	private boolean registered = false;
	private String contentXml;
	private String href;

	/**
	 *	Create a new instance of this class
	 *
	 *	@param name java.lang.String
	 */
	public CheatSheetElement(String name) {
		this.name = name;
	}

	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 *
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getConfigurationElement() {
		return configurationElement;
	}

	/**
	 *	Get the content File path of this element
	 *
	 *	@return java.lang.String
	 */
	public String getContentFile() {
		return contentFile;
	}

	/**
	 *	Answer the description parameter of this element
	 *
	 *	@return java.lang.String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 *	Answer the id as specified in the extension.
	 *
	 *	@return java.lang.String
	 */
	public String getID() {
		return id;
	}

	/**
	 * Returns the name of this cheatsheet element.
	 */
	@Override
	public String getLabel(Object element) {
		return name;
	}

	/**
	 * Returns the listener class name of this cheatsheet element.
	 */
	public String getListenerClass() {
		return listenerClass;
	}

	/**
	 *
	 * @param newConfigurationElement IConfigurationElement
	 */
	public void setConfigurationElement(IConfigurationElement newConfigurationElement) {
		configurationElement = newConfigurationElement;
	}

	/**
	 *	Set the contentFile parameter of this element
	 *
	 *	@param value the path of the content file
	 */
	public void setContentFile(String value) {
		contentFile = value;
	}

	/**
	 *	Set the description parameter of this element
	 *
	 *	@param value java.lang.String
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 *	Set the id parameter of this element
	 *
	 *	@param value java.lang.String
	 */
	public void setID(String value) {
		id = value;
	}

	/**
	 * Set the listener class name of this element.
	 */
	public void setListenerClass(String value) {
		listenerClass = value;
	}

	public CheatSheetListener createListenerInstance() {
		if(listenerClass == null || configurationElement == null) {
			return null;
		}

		Class<?> extClass = null;
		CheatSheetListener listener = null;
		String pluginId = configurationElement.getContributor().getName();

		try {
			Bundle bundle = Platform.getBundle(pluginId);
			extClass = bundle.loadClass(listenerClass);
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_LOADING_CLASS, (new Object[] {listenerClass}));
			CheatSheetPlugin.getPlugin().getLog().error(message, e);
		}
		try {
			if (extClass != null) {
				listener = (CheatSheetListener) extClass.getDeclaredConstructor().newInstance();
			}
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_CREATING_CLASS, (new Object[] {listenerClass}));
			CheatSheetPlugin.getPlugin().getLog().error(message, e);
		}

		if (listener != null){
			return listener;
		}

		return null;
	}

	@Override
	public String getLocalId() {
		return id;
	}

	@Override
	public String getPluginId() {
		return configurationElement.getContributor().getName();
	}

	public void setComposite(boolean composite) {
		this.composite = composite;
	}

	public boolean isComposite() {
		return composite;
	}

	/**
	 * Get a URL which is saved with the state so the cheatsheet can later be
	 * reopened from the state file.
	 * @return null if the cheatsheet was opened from the registry otherwise
	 * the URL of the content file.
	 */
	public String getRestorePath() {
		if (!registered) {
			return contentFile;
		}
		return null;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setContentXml(String xml) {
		this.contentXml = xml;
	}

	public String getContentXml() {
		return contentXml;
	}

	public void setHref(String contentPath) {
		this.href = contentPath;
	}

	public String getHref() {
		return href;
	}

}
