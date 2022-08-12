/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.registry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * A team content provider descriptor associates a model provider with a
 * navigator content extension
 */
public class TeamContentProviderDescriptor implements ITeamContentProviderDescriptor {

	private static final String TAG_TEAM_CONTENT_PROVIDER = "teamContentProvider"; //$NON-NLS-1$

	private static final String ATT_MODEL_PROVIDER_ID = "modelProviderId"; //$NON-NLS-1$
	private static final String ATT_CONTENT_EXTENSION_ID = "contentExtensionId"; //$NON-NLS-1$
	private static final String ATT_ICON = "icon"; //$NON-NLS-1$
	private static final String ATT_PREFERENCE_PAGE = "preferencePage"; //$NON-NLS-1$
	private static final String ATT_SUPPORTS_FLAT_LAYOUT = "supportsFlatLayout"; //$NON-NLS-1$

	private static final String PREF_TEAM_CONTENT_DESCRIPTORS = "teamContentDescriptors"; //$NON-NLS-1$
	private static final String PREF_ENABLED = "enabled"; //$NON-NLS-1$

	private String modelProviderId;
	private String contentExtensionId;
	private String contentProviderName;

	private ImageDescriptor imageDescriptor;

	private IConfigurationElement configElement;

	private boolean supportsFlatLayout;

	public TeamContentProviderDescriptor(IExtension extension) throws CoreException {
		readExtension(extension);
	}

	/**
	 * Initialize this descriptor based on the provided extension point.
	 */
	protected void readExtension(IExtension extension) throws CoreException {
		// read the extension
		String id = extension.getUniqueIdentifier(); // id not required
		IConfigurationElement[] elements = extension.getConfigurationElements();

		// there has to be exactly one team content provider element
		// in the provided extension (see teamContentProviders.exsd)
		if (elements.length == 1) {
			IConfigurationElement element = elements[0];
			String name = element.getName();
			if (name.equalsIgnoreCase(TAG_TEAM_CONTENT_PROVIDER)) {
				configElement = element;
				modelProviderId = element.getAttribute(ATT_MODEL_PROVIDER_ID);
				contentExtensionId = element
						.getAttribute(ATT_CONTENT_EXTENSION_ID);
				String supportsFlatLayoutString = element
						.getAttribute(ATT_SUPPORTS_FLAT_LAYOUT);
				if (supportsFlatLayoutString != null) {
					supportsFlatLayout = Boolean.parseBoolean(supportsFlatLayoutString);
				}
				contentProviderName = extension.getLabel();
			}
		} else {
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_2,
					new String[] { TAG_TEAM_CONTENT_PROVIDER,
							id == null ? "" : id })); //$NON-NLS-1$
		}
		if (modelProviderId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_1,
					new String[] { ATT_MODEL_PROVIDER_ID,
							TAG_TEAM_CONTENT_PROVIDER, id == null ? "" : id })); //$NON-NLS-1$
		if (contentExtensionId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_1,
					new String[] { ATT_CONTENT_EXTENSION_ID,
							TAG_TEAM_CONTENT_PROVIDER, id == null ? "" : id })); //$NON-NLS-1$
	}

	protected void fail(String reason) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, reason, null));
	}

	@Override
	public String getContentExtensionId() {
		return contentExtensionId;
	}

	@Override
	public String getModelProviderId() {
		return modelProviderId;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (imageDescriptor != null)
			return imageDescriptor;
		String iconName = configElement.getAttribute(ATT_ICON);
		if (iconName == null)
			return null;
		imageDescriptor = TeamUIPlugin.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), iconName);
		return imageDescriptor;
	}

	@Override
	public IPreferencePage createPreferencePage() throws CoreException {
		if (configElement.getAttribute(ATT_PREFERENCE_PAGE) == null)
			return null;
		Object obj = RegistryReader.createExtension(configElement, ATT_PREFERENCE_PAGE);
		return (IPreferencePage) obj;
	}

	@Override
	public boolean isEnabled() {
		if (!hasPreferences()) {
			return true;
		}
		return getPreferences().getBoolean(PREF_ENABLED, true);
	}

	public void setEnabled(boolean enable) {
		if (isEnabled() != enable) {
			getPreferences().putBoolean(PREF_ENABLED, enable);
			flushPreferences();
		}
	}

	public Preferences getParentPreferences() {
		return TeamUIPlugin.getPlugin().getInstancePreferences().node(PREF_TEAM_CONTENT_DESCRIPTORS);
	}
	/*
	 * Return the preferences node for this repository
	 */
	public Preferences getPreferences() {
		if (!hasPreferences()) {
			ensurePreferencesStored();
		}
		return internalGetPreferences();
	}

	private Preferences internalGetPreferences() {
		return getParentPreferences().node(getPreferenceName());
	}

	private boolean hasPreferences() {
		try {
			return getParentPreferences().nodeExists(getPreferenceName());
		} catch (BackingStoreException e) {
			TeamUIPlugin.log(IStatus.ERROR, NLS.bind("Error accessing team content preference store for {0}", new String[] { getModelProviderId() }), e);  //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Return a unique name that identifies this location but
	 * does not contain any slashes (/). Also, do not use ':'.
	 * Although a valid path character, the initial core implementation
	 * didn't handle it well.
	 */
	private String getPreferenceName() {
		return getModelProviderId();
	}

	public void storePreferences() {
		Preferences prefs = internalGetPreferences();
		// Must store at least one preference in the node
		prefs.putBoolean(PREF_ENABLED, true);
		flushPreferences();
	}

	private void flushPreferences() {
		try {
			internalGetPreferences().flush();
		} catch (BackingStoreException e) {
			TeamUIPlugin.log(IStatus.ERROR, NLS.bind("Error flushing team content preference store for {0}", new String[] { getModelProviderId() }), e);  //$NON-NLS-1$
		}
	}

	private void ensurePreferencesStored() {
		if (!hasPreferences()) {
			storePreferences();
		}
	}

	@Override
	public String getName() {
		if (contentProviderName != null)
			return contentProviderName;

		return null;
	}

	@Override
	public boolean isFlatLayoutSupported() {
		return supportsFlatLayout;
	}
}
