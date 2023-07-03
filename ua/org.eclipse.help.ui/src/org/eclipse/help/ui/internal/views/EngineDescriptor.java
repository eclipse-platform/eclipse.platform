/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.help.search.ISearchEngine;
import org.eclipse.help.search.ISearchScope;
import org.eclipse.help.ui.IEngineDescriptor;
import org.eclipse.help.ui.RootScopePage;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Descriptor for a federated search engine participant.
 */
public class EngineDescriptor implements IEngineDescriptor {
	public static final String P_MASTER = "__enabled__"; //$NON-NLS-1$

	private ISearchEngine engine;

	private IConfigurationElement config;

	private EngineDescriptorManager manager;

	private EngineTypeDescriptor etdesc;

	private Hashtable<String, Object> parameters;

	//private boolean removable;

	//private boolean enabled;

	private String id;

	private String label;

	private String desc;

	private boolean userDefined;

	/**
	 *
	 */
	public EngineDescriptor(IConfigurationElement config) {
		this.config = config;
	}

	public EngineDescriptor(EngineDescriptorManager manager) {
		this.manager = manager;
	}

	public void setEngineType(EngineTypeDescriptor etdesc) {
		this.etdesc = etdesc;
	}

	public void setEngineDescriptorManager(EngineDescriptorManager manager) {
		this.manager = manager;
	}

	public IConfigurationElement getConfig() {
		return config;
	}

	@Override
	public String getLabel() {
		if (label != null)
			return label;
		String clabel = null;
		if (config != null)
			clabel = config.getAttribute(IHelpUIConstants.ATT_LABEL);
		if (clabel == null)
			clabel = etdesc.getLabel();
		return clabel;
	}

	@Override
	public String getId() {
		if (id != null)
			return id;
		return config.getAttribute(IHelpUIConstants.ATT_ID);
	}

	@Override
	public String getEngineTypeId() {
		if (etdesc != null)
			return etdesc.getId();
		return config.getAttribute(IHelpUIConstants.ATT_ENGINE_TYPE_ID);
	}

	public boolean isEnabled() {
		if (userDefined)
			return true;
		String aenabled = config.getAttribute(IHelpUIConstants.ATT_ENABLED);
		if (aenabled != null)
			return aenabled.equals("true"); //$NON-NLS-1$
		return false;
	}

	public Image getIconImage() {
		return etdesc.getIconImage();
	}

	@Override
	public String getDescription() {
		if (desc != null)
			return desc;
		String cdesc = null;
		if (config != null) {
			IConfigurationElement[] children = config
					.getChildren(IHelpUIConstants.TAG_DESC);
			if (children.length == 1)
				cdesc = children[0].getValue();
		}
		if (cdesc == null)
			return etdesc.getDescription();
		return cdesc;
	}

	public IConfigurationElement[] getPages() {
		return etdesc.getPages();
	}

	public ImageDescriptor getImageDescriptor() {
		return etdesc.getImageDescriptor();
	}

	public RootScopePage createRootPage(String scopeSetName) {
		RootScopePage page = etdesc.createRootPage(scopeSetName);
		if (page != null) {
			//Dictionary parameters = getParameters();
			page.init(this, scopeSetName);
		}
		return page;
	}

	@Override
	public Dictionary<String, Object> getParameters() {
		if (parameters != null)
			return parameters;
		parameters = new Hashtable<>();
		parameters.put(P_MASTER, isEnabled() ? Boolean.TRUE : Boolean.FALSE);
		if (config != null) {
			IConfigurationElement[] params = config.getChildren("param"); //$NON-NLS-1$
			for (int i = 0; i < params.length; i++) {
				IConfigurationElement param = params[i];
				String name = param.getAttribute(IHelpUIConstants.ATT_NAME);
				String value = param.getAttribute(IHelpUIConstants.ATT_VALUE);
				if (name != null && value != null)
					parameters.put(name, value);
			}
		}
		return parameters;
	}

	public ISearchEngine getEngine() {
		if (engine == null) {
			engine = etdesc.createEngine();
		}
		return engine;
	}

	public ISearchScope createSearchScope(IPreferenceStore store) {
		return etdesc.createSearchScope(store, getId(), getParameters());
	}

	void setId(String id) {
		this.id = id;
	}

	@Override
	public void setLabel(String label) {
		if (isUserDefined()) {
			this.label = label;
			if (manager!=null)
				manager.notifyPropertyChange(this);
		}
	}

	@Override
	public void setDescription(String desc) {
		if (isUserDefined()) {
			this.desc = desc;
			if (manager!=null)
				manager.notifyPropertyChange(this);
		}
	}

	@Override
	public boolean isUserDefined() {
		return userDefined;
	}

	public void setUserDefined(boolean userDefined) {
		this.userDefined = userDefined;
	}
}
