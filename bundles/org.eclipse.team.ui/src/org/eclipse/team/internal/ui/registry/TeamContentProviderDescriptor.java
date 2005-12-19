/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.registry;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * A team content provider descriptor associates a model provider
 * with a navigator content extension
 */
public class TeamContentProviderDescriptor {

	private static final String TAG_TEAM_CONTENT_PROVIDER = "teamContentProvider"; //$NON-NLS-1$
	
	private static final String ATT_MODEL_PROVIDER_ID = "modelProviderId"; //$NON-NLS-1$
	private static final String ATT_CONTENT_EXTENSION_ID = "contentExtensionId"; //$NON-NLS-1$
	private IConfigurationElement configElement;
	private String id;
	private String modelProviderId;
	private String contentExtensionId;

	public TeamContentProviderDescriptor(IExtension extension) throws CoreException {
		readExtension(extension);
	}

	/**
	 * Initialize this descriptor based on the provided extension point.
	 */
	protected void readExtension(IExtension extension) throws CoreException {
		//read the extension
		id = extension.getUniqueIdentifier();
		if (id == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_0, new String[] {TeamContentProviderManager.PT_TEAM_CONTENT_PROVIDERS}));
		IConfigurationElement[] elements = extension.getConfigurationElements();
		int count = elements.length;
		for (int i = 0; i < count; i++) {
			IConfigurationElement element = elements[i];
			String name = element.getName();
			if (name.equalsIgnoreCase(TAG_TEAM_CONTENT_PROVIDER)) {
				modelProviderId = element.getAttribute(ATT_MODEL_PROVIDER_ID);
				contentExtensionId = element.getAttribute(ATT_CONTENT_EXTENSION_ID);
			}
		}
		if (modelProviderId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_1, new String[] { ATT_MODEL_PROVIDER_ID, TAG_TEAM_CONTENT_PROVIDER, id}));
		if (contentExtensionId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_2, new String[] { ATT_CONTENT_EXTENSION_ID, TAG_TEAM_CONTENT_PROVIDER, id}));
	}
	
	protected void fail(String reason) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, reason, null));
	}

	public Object getId() {
		return id;
	}

	public String getContentExtensionId() {
		return contentExtensionId;
	}

	protected String getModelProviderId() {
		return modelProviderId;
	}

}
