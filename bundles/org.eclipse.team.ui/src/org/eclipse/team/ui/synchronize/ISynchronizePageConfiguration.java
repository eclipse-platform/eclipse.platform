/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Configures the model, actions and label decorations of an
 * {@link ISynchronizePage}. Clients can:
 * <ul>
 * <li>set properties to affect the page contents and react to property changes
 * <li>add and configure the actions available to the user (context menu,
 * toolbar and view menu)
 * </ul>
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISynchronizePageConfiguration {

	/**
	 * Property constant for the <code>SyncInfoSet</code> that is being
	 * displayed by the page. Some pages may not be displaying the
	 * contents of a <code>SyncInfoSet</code> in which case the value
	 * associated with this property will be <code>null</code>.
	 */
	public static final String P_SYNC_INFO_SET = TeamUIPlugin.ID  + ".P_SYNC_INFO_SET"; //$NON-NLS-1$

	/**
	 * Property constant for the list of label decorators
	 * (instance of <code>ILabelDecorator[]</code>) that will be
	 * applied to the text and image from the label provider.
	 */
	public static final String P_LABEL_DECORATORS = TeamUIPlugin.ID  + ".P_LABEL_DECORATORS"; //$NON-NLS-1$

	/**
	 * Property constant that defines the groups in the toolbar
	 * menu of the page. The value for this
	 * property should be a string array. If this property is
	 * set to <code>null</code>, the <code>DEFAULT_TOOLBAR_MENU</code>
	 * is used. Also, the groups mentioned in the <code>DEFAULT_TOOLBAR_MENU</code>
	 * can be removed but will always appear in the same order if
	 * included.
	 */
	public static final String P_TOOLBAR_MENU = TeamUIPlugin.ID + ".P_TOOLBAR_MENU"; //$NON-NLS-1$

	/**
	 * The configuration property that defines
	 * the groups in the context menu of the page. The value for this
	 * property should be a string array.
	 */
	public static final String P_CONTEXT_MENU = TeamUIPlugin.ID + ".P_CONTEXT_MENU"; //$NON-NLS-1$

	/**
	 * Property constant that defines the groups in the drop-down view
	 * menu of the page. The value for this
	 * property should be a string array. If this property is
	 * set to <code>null</code>, the <code>DEFAULT_VIEW_MENU</code>
	 * is used. Also, the groups mentioned in the <code>DEFAULT_VIEW_MENU</code>
	 * can be removed but will always appear in the same order if
	 * included.
	 */
	public static final String P_VIEW_MENU = TeamUIPlugin.ID + ".P_VIEW_MENU"; //$NON-NLS-1$

	/**
	 * The configuration property that defines the filter id that
	 * determines which object contribution actions appear in the
	 * context menu for the page. This defaults to the id of the
	 * participant but can be set to another id or <code>null</code>
	 * @since 3.1
	 */
	public static final String P_OBJECT_CONTRIBUTION_ID = TeamUIPlugin.ID +  ".P_OBJECT_CONTRIBUTION_ID"; //$NON-NLS-1$

	/**
	 * Property constant for the type of comparison used to create
	 * the <code>SyncInfo</code> in the P_SYNC_INFO_SET property.
	 * If the comparison type is <code>THREE_WAY</code> then
	 * modes selection applies.
	 */
	public static final String P_COMPARISON_TYPE = TeamUIPlugin.ID + ".P_COMPARISON_TYPE"; //$NON-NLS-1$

	/**
	 * Property constant for the mode used to filter the visible
	 * elements of the model. The value can be one of the mode integer
	 * constants.
	 */
	public static final String P_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$

	/**
	 * Property constant which indicates which modes are to be available to the user.
	 * The value is to be an integer that combines one or more of the
	 * mode bit values.
	 * Either <code>null</code> or <code>0</code> can be used to indicate that
	 * mode filtering is not supported.
	 */
	public static final String P_SUPPORTED_MODES = TeamUIPlugin.ID  + ".P_SUPPORTED_MODES";	 //$NON-NLS-1$

	/**
	 * Property constant for the id of the viewer to be used for this page.
	 * The viewer id corresponds to the viewer definition in the Common
	 * Navigator framework.
	 *
	 * @since 3.2
	 */
	public static final String P_VIEWER_ID = TeamUIPlugin.ID + ".P_VIEWER_ID"; //$NON-NLS-1$

	/**
	 * Property constant for the description (String) of the page that appears
	 * as the description of the view when the page is active.
	 *
	 * @since 3.2
	 */
	public static final String P_PAGE_DESCRIPTION = TeamUIPlugin.ID + ".P_PAGE_DESCRIPTION"; //$NON-NLS-1$

	/**
	 * The id of the synchronize group the determines where the synchronize
	 * actions appear.
	 */
	public static final String SYNCHRONIZE_GROUP = "synchronize"; //$NON-NLS-1$

	/**
	 * The id of the navigate group that determines where the navigation
	 * actions appear
	 */
	public static final String NAVIGATE_GROUP = "navigate"; //$NON-NLS-1$

	/**
	 * The id of the sort group that determines where sort
	 * actions or submenus appear.
	 * @since 3.1
	 */
	public final static String SORT_GROUP = "sort"; //$NON-NLS-1$

	/**
	 * The id of the mode group that determines where the mode selection
	 * actions appear
	 */
	public static final String MODE_GROUP = "modes"; //$NON-NLS-1$

	/**
	 * The id of the file group that determines where the file
	 * actions appear. File actions include the open actions.
	 */
	public static final String FILE_GROUP = "file"; //$NON-NLS-1$

	/**
	 * The id of the edit group that determines where the edit
	 * actions appear (e.g. move and delete).
	 */
	public static final String EDIT_GROUP = "edit"; //$NON-NLS-1$

	/**
	 * The id of the preferences group that determines whether the preferences
	 * actions appear in the view drop-down.
	 */
	public static final String PREFERENCES_GROUP = "preferences"; //$NON-NLS-1$

	/**
	 * The id of the group that determines where workbench object contributions
	 * should appear. This group will only be used if there is an
	 * OBJECT_CONTRIBUTION_ID set in the configuration
	 * @since 3.1
	 */
	public static final String OBJECT_CONTRIBUTIONS_GROUP = IWorkbenchActionConstants.MB_ADDITIONS;

	/**
	 * The id of the layout group that determines whether the layout selection
	 * actions appear in the view drop-down or toolbar.
	 */
	public static final String LAYOUT_GROUP = "layout"; //$NON-NLS-1$

	/**
	 * These are the default groups used for the context menu of a page.
	 * Clients can remove, add and change the ordering for groups in
	 * the context menu.
	 */
	public static final String[] DEFAULT_CONTEXT_MENU = new String[] { FILE_GROUP, EDIT_GROUP, SYNCHRONIZE_GROUP, NAVIGATE_GROUP, SORT_GROUP, OBJECT_CONTRIBUTIONS_GROUP};

	/**
	 * These are the default groups used for the toolbar of a page.
	 * These groups will always appear in this order in the toolbar.
	 * Clients can disable one or more of these groups by setting
	 * the <code>P_TOOLBAR_MENU</code> property to an array that
	 * contains a subset of these. Clients can also add groups
	 * by adding new unique group ids to the array. Added groups
	 * will appear in the order specified but after the default groups.
	 */
	public static final String[] DEFAULT_TOOLBAR_MENU = new String[] { SYNCHRONIZE_GROUP, NAVIGATE_GROUP, MODE_GROUP, LAYOUT_GROUP };

	/**
	 * These are the default groups used for the drop-down view menu of a page.
	 * These groups will always appear in this order in the view menu.
	 * Clients can disable one or more of these groups by setting
	 * the <code>P_VIEW_MENU</code> property to an array that
	 * contains a subset of these. Clients can also add groups
	 * by adding new unique group ids to the array. Added groups
	 * will appear in the order specified but after the default groups.
	 */
	public static final String[] DEFAULT_VIEW_MENU = new String[] { LAYOUT_GROUP, MODE_GROUP, SYNCHRONIZE_GROUP, PREFERENCES_GROUP };

	/**
	 * Comparison type constants
	 */
	public final static String TWO_WAY = "two-way"; //$NON-NLS-1$
	public final static String THREE_WAY = "three-way"; //$NON-NLS-1$

	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;

	/**
	 * Return the participant associated with this configuration.
	 * @return the participant
	 */
	public abstract ISynchronizeParticipant getParticipant();

	/**
	 * Return the site which provides access to certain workbench
	 * services.
	 * @return the page site
	 */
	public abstract ISynchronizePageSite getSite();

	/**
	 * Return the page created from and associated with this
	 * configuration.
	 * @return Returns the page for this configuration
	 */
	public ISynchronizePage getPage();

	/**
	 * Set the page for this configuration. This method should only
	 * be called once by the {@link ISynchronizeParticipant} that created
	 * the page.
	 * @param page the configuration's page
	 */
	public void setPage(ISynchronizePage page);

	/**
	 * Add a property change listener to the configuration.
	 * Registered listeners will receive notification when
	 * any property changes.
	 * @param listener a property change listener
	 */
	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Remove the registered change listener. Removing an unregistered listener
	 * has no effects.
	 * @param listener a property change listener
	 */
	public abstract void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Sets the property with the given name.
	 * If the new value differs from the old a <code>PropertyChangeEvent</code>
	 * is sent to registered listeners.
	 *
	 * @param key the name of the property to set
	 * @param newValue the new value of the property
	 */
	public abstract void setProperty(String key, Object newValue);

	/**
	 * Returns the property with the given name, or <code>null</code>
	 * if no such property exists.
	 *
	 * @param key the name of the property to retrieve
	 * @return the property with the given name, or <code>null</code> if not found
	 */
	public abstract Object getProperty(String key);

	/**
	 * Register the action group with the configuration. The
	 * registered action groups will have the opportunity to add
	 * actions to the action bars and context menu of the synchronize
	 * page created using the configuration.
	 * @param group a synchronize page action group
	 */
	public abstract void addActionContribution(SynchronizePageActionGroup group);

	/**
	 * Remove a previously registered action group. Removing
	 * a group that is not registered has no effect.
	 * @param group a synchronize page action group
	 */
	public abstract void removeActionContribution(SynchronizePageActionGroup group);

	/**
	 * Add a label decorator to the page configuration.
	 * @param decorator a label decorator
	 */
	public void addLabelDecorator(ILabelDecorator decorator);

	/**
	 * Set the groups that are to be added to the menu identified
	 * by the menu property id.
	 * @param menuPropertyId the menu property id (one of <code>P_CONTEXT_MENU</code>,
	 * <code>P_VIEW_MENU</code> or <code>P_TOOLBAR_MENU</code>)
	 * @param groups a array of groups Ids
	 */
	public void setMenuGroups(String menuPropertyId, String[] groups);

	/**
	 * Adds a menu group of the given id to the end of the menu groups list
	 * for the given menu property id.
	 * @param menuPropertyId the menu property id (one of <code>P_CONTEXT_MENU</code>,
	 * <code>P_VIEW_MENU</code> or <code>P_TOOLBAR_MENU</code>)
	 * @param groupId the id of the group to be added to the end of the menu
	 * group list
	 */
	public void addMenuGroup(String menuPropertyId, String groupId);

	/**
	 * Returns whether the given group appears in the given menu
	 * @param menuPropertyId the property id that identifies the menu
	 * @param groupId the id of the group
	 * @return <code>true</code> if the group identified by the groupId appears
	 * in the menu identified by the menuPropertyId and <code>false</code>
	 * otherwise
	 */
	public abstract boolean hasMenuGroup(String menuPropertyId, String groupId);

	/**
	 * Return the value of the P_MODE property of this configuration.
	 * @return the mode property value
	 */
	int getMode();

	/**
	 * Set the P_MODE property of this configuration to the
	 * given mode flag (one of <code>INCOMING_MODE</code>,
	 * <code>OUTGOING_MODE</code>, <code>BOTH_MODE</code>
	 * or <code>CONFLICTING_MODE</code>).
	 * @param mode the mode value
	 */
	void setMode(int mode);

	/**
	 * Return the value of the P_SUPPORTED_MODES property of this configuration.
	 * @return the supported modes property value
	 */
	int getSupportedModes();

	/**
	 * Set the P_SUPPORTED_MODES property of this configuration to the
	 * given combination of one or more mode flags (<code>INCOMING_MODE</code>,
	 * <code>OUTGOING_MODE</code>, <code>BOTH_MODE</code>
	 * and <code>CONFLICTING_MODE</code>).
	 * @param modes the supported modes
	 */
	void setSupportedModes(int modes);

	/**
	 * Return the set associated with the P_SYNC_INFO_SET property
	 * or <code>null</code> if the property is not set.
	 * @return the set associated with the P_SYNC_INFO_SET property
	 * or <code>null</code> if the property is not set
	 */
	public abstract SyncInfoSet getSyncInfoSet();

	/**
	 * Return the comparison type used by the page's <code>SyncInfo</code>
	 * modes.
	 * @return comparison type (could be <code>TWO_WAY</code>, <code>THREE_WAY</code>
	 * or a custom type).
	 */
	String getComparisonType();

	/**
	 * Set the comparison type used by the page's <code>SyncInfo</code>
	 * modes. The default type is <code>THREE_WAY</code>.
	 * @param type the comparison type (could be <code>TWO_WAY</code>, <code>THREE_WAY</code>
	 * or a custom type).
	 */
	void setComparisonType(String type);

	/**
	 * Sets the runnable context that can be used by the page's
	 * actions to display progress.
	 * @param context a runnable context (or null)
	 */
	void setRunnableContext(IRunnableContext context);

	/**
	 * Return the runnable context. If <code>null</code> is returned,
	 * actions can use their own method of progress feedback either
	 * using a background job or the progress service
	 * @return a runnable context (or <code>null</code>)
	 */
	IRunnableContext getRunnableContext();

	/**
	 * Return the id of the viewer to which this configuration is
	 * associated.
	 * @return the id of the viewer to which this configuration is
	 * associated
	 * @since 3.2
	 */
	String getViewerId();
}
