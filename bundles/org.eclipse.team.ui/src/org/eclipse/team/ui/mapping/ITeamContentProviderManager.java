/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * The team content provider manager provides access to the content
 * extenstions registered with the <code>org.eclipse.team.ui.teamContentProviders</code>
 * extension point. A team content provider defines a mapping between
 * a {@link ModelProvider} and a content extension registered with
 * the <code>org.eclipse.ui.navigator.navigatorContent</code> extension point.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @see TeamUI#getTeamContentProviderManager()
 * @see ModelProvider
 * @since 3.2
 */
public interface ITeamContentProviderManager {

	/**
	 * Property constant used to store and retrieve the synchronization page
	 * configuration from the
	 * {@link org.eclipse.ui.navigator.IExtensionStateModel} used by the Common
	 * Navigator framework.
	 */
	public static final String P_SYNCHRONIZATION_PAGE_CONFIGURATION = TeamUIPlugin.ID +  ".synchronizationPageConfiguration"; //$NON-NLS-1$
	/**
	 * Property constant used to store and retrieve the synchronization context
	 * from the {@link org.eclipse.ui.navigator.IExtensionStateModel} used by
	 * the Common Navigator framework. It is also used to associate a context
	 * with an {@link ISynchronizePageConfiguration} when models are being
	 * shown in an {@link ISynchronizePage}.
	 */
	public static final String P_SYNCHRONIZATION_CONTEXT = TeamUIPlugin.ID + ".synchronizationContext"; //$NON-NLS-1$

	/**
	 * Property constant used to store and retrieve the resource mapping scope
	 * from the {@link org.eclipse.ui.navigator.IExtensionStateModel} used by
	 * the Common Navigator framework. It is also used to associate a scope
	 * with an {@link ISynchronizePageConfiguration} when models are being
	 * shown in an {@link ISynchronizePage}.
	 */
	public static final String P_SYNCHRONIZATION_SCOPE = TeamUIPlugin.ID + ".synchronizationScope"; //$NON-NLS-1$

	/**
	 * Property constant used to store and retrieve the page layout
	 * from the {@link ISynchronizePageConfiguration} when models are being
	 * shown in an {@link ISynchronizePage}. At this time, there are two layouts,
	 * TREE_LAYOUT and FLAT_LAYOUT. Other may be added
	 * @since 3.3
	 */
	public static final String PROP_PAGE_LAYOUT = TeamUIPlugin.ID + ".pageLayout"; //$NON-NLS-1$

	/**
	 * Value for the PROP_PAGE_LAYOUT that indicates that the models should display
	 * their elements in tree form.
	 * @since 3.3
	 */
	public static final String TREE_LAYOUT = TeamUIPlugin.ID + ".treeLayout"; //$NON-NLS-1$

	/**
	 * Value for the PROP_PAGE_LAYOUT that indicates that the models should display
	 * their elements as a flat list. Only models that indicate in their <code>teamContentProviders</code>
	 * that they support the flat layout will be enabled when the PROP_PAGE_LAYOUT is set
	 * to FLAT_LAYOUT.
	 * @since 3.3
	 */
	public static final String FLAT_LAYOUT = TeamUIPlugin.ID + ".flatLayout"; //$NON-NLS-1$

	/**
	 * Property constant used during property change notification to indicate
	 * that one one or more model providers have either been enabled or disabled.
	 */
	public static final String PROP_ENABLED_MODEL_PROVIDERS = TeamUIPlugin.ID + ".ENABLED_MODEL_PROVIDERS"; //$NON-NLS-1$

	/**
	 * Return descriptors for all the registered content extensions.
	 * @return descriptors for all the registered content extensions
	 */
	public ITeamContentProviderDescriptor[] getDescriptors();

	/**
	 * Return the team content provider descriptor for the
	 * given model provider id. A <code>null</code> is
	 * returned if no extension is registered.
	 * @param modelProviderId the model provider id
	 * @return the team content provider descriptor for the
	 * given model provider id or <code>null</code>
	 */
	public ITeamContentProviderDescriptor getDescriptor(
			String modelProviderId);

	/**
	 * Add a property change listener to the manager.
	 * @param listener the listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Remove a property change listener from the manager.
	 * @param listener the listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Convenience method that returns the list of all enabled content extension ids for
	 * models that have mappings in the given scope.
	 *
	 * @param scope
	 *            the scope
	 * @return the list of all content extension ids for models that have
	 *         mappings in the given scope
	 */
	public String[] getContentProviderIds(ISynchronizationScope scope);

	/**
	 * Enable the given content descriptors, disabling all others.
	 * This method will fire a {@link ITeamContentProviderManager#PROP_ENABLED_MODEL_PROVIDERS}
	 * property change event to any registered listeners.
	 * @param descriptors the descriptors to be enabled.
	 * @see ITeamContentProviderDescriptor#isEnabled()
	 * @since 3.3
	 */
	public void setEnabledDescriptors(ITeamContentProviderDescriptor[] descriptors);

}
