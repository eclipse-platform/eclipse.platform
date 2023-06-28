/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;

/**
 * This interface provides a repository for the names of <code>IConfigurationElement</code> child node ids.
 * @since 3.3
 */
public interface IConfigurationElementConstants {

	/**
	 * The id node name for a configuration element.
	 * <p>Equal to the word: <code>id</code></p>
	 */
	String ID = "id"; //$NON-NLS-1$

	/**
	 * The name node name for a configuration element.
	 * <p>Equal to the word: <code>name</code></p>
	 */
	String NAME = "name"; //$NON-NLS-1$

	/**
	 * The category node name for a configuration element.
	 * <p>Equal to the word: <code>category</code></p>
	 */
	String CATEGORY = "category"; //$NON-NLS-1$

	/**
	 * The local node name for a configuration element.
	 * <p>Equal to the word: <code>local</code></p>
	 */
	String LOCAL = "local"; //$NON-NLS-1$

	/**
	 * The shortcutId node name for a configuration element.
	 * <p>Equal to the word: <code>shortcutID</code></p>
	 */
	String LAUNCH_SHORTCUT_ID = "shortcutID"; //$NON-NLS-1$

	/**
	 * The launchPerspectives node name for a configuration element.
	 * <p>Equal to the word: <code>launchPerspectives</code></p>
	 */
	String LAUNCH_PERSPECTIVES = "launchPerspectives";  //$NON-NLS-1$

	/**
	 * The launchPerspective node name for a configuration element.
	 * <p>Equal to the word: <code>launchPerspective</code></p>
	 */
	String LAUNCH_PERSPECTIVE = "launchPerspective"; //$NON-NLS-1$

	/**
	 * The markerType node name for a configuration element.
	 * <p>Equal to the word: <code>markerType</code></p>
	 *
	 * @since 3.4
	 */
	String MARKER_TYPE = "markerType";	 //$NON-NLS-1$

	/**
	 * The delegateClass node name for a configuration element.
	 * <p>Equal to the word: <code>delegateClass</code></p>
	 *
	 * @since 3.4
	 */
	String DELEGATE_CLASS = "delegateClass"; //$NON-NLS-1$

	/**
	 * The launchConfiguration node name for a configuration element.
	 * <p>Equal to the word: <code>launchConfiguration</code></p>
	 */
	String LAUNCH_CONFIGURATION = "launchConfiguration"; //$NON-NLS-1$

	/**
	 * The launchMode node name for a configuration element.
	 * <p>Equal to the word: <code>launchMode</code></p>
	 */
	String LAUNCH_MODE = "launchMode"; //$NON-NLS-1$

	/**
	 * The lastLaunch node name for a configuration element.
	 * <p>Equal to the word: <code>lastLaunch</code></p>
	 */
	String LAST_LAUNCH = "lastLaunch"; //$NON-NLS-1$

	/**
	 * The launch node name for a configuration element.
	 * <p>Equal to the word: <code>launch</code></p>
	 */
	String LAUNCH = "launch"; //$NON-NLS-1$

	/**
	 * The launch as label node name for a configuration element.
	 *  <p>Equal to the word: <code>launchAsLabel</code></p>
	 *
	 *  @since 3.4
	 */
	String LAUNCH_AS_LABEL = "launchAsLabel"; //$NON-NLS-1$

	/**
	 * The launchHistory node name for a configuration element.
	 * <p>Equal to the word: <code>launchHistory</code></p>
	 */
	String LAUNCH_HISTORY = "launchHistory"; //$NON-NLS-1$

	/**
	 * The node name for a launch group configuration element.
	 * <p>Equal to the word: <code>launchGroup</code></p>
	 */
	String LAUNCH_GROUP = "launchGroup"; //$NON-NLS-1$

	/**
	 * The node name for a launch history MRU list configuration element.
	 * <p>Equal to the word: <code>mruHistory</code></p>
	 */
	String MRU_HISTORY = "mruHistory"; //$NON-NLS-1$

	/**
	 * The node name for a launch favorites list configuration element.
	 * <p>Equal to the word: <code>favorites</code></p>
	 */
	String FAVORITES = "favorites"; //$NON-NLS-1$

	/**
	 * The after node name for a configuration element.
	 * <p>Equal to the word: <code>after</code></p>
	 */
	String AFTER = "after"; //$NON-NLS-1$

	/**
	 * The path node name for a configuration element.
	 * <p>Equal to the word: <code>path</code></p>
	 */
	String PATH = "path"; //$NON-NLS-1$

	/**
	 * The placement node name for a configuration element.
	 * <p>Equal to the word: <code>placement</code></p>
	 */
	String PLACEMENT = "placement"; //$NON-NLS-1$

	/**
	 * The associated launch delegate node name for a configuration element
	 * <p>Equal to the word: <code>associatedDelegate</code></p>
	 */
	String ASSOCIATED_DELEGATE = "associatedDelegate"; //$NON-NLS-1$

	/**
	 * The label node name for a configuration element.
	 * <p>Equal to the word: <code>label</code></p>
	 */
	String LABEL = "label"; //$NON-NLS-1$

	/**
	 * The description node name for a configuration element.
	 * <p>Equal to the word: <code>description</code></p>
	 */
	String DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The helpContextId node name for a configuration element.
	 * <p>Equal to the word: <code>helpContextId</code></p>
	 */
	String HELP_CONTEXT_ID = "helpContextId"; //$NON-NLS-1$

	/**
	 * The icon node name for a configuration element.
	 * <p>Equal to the word: <code>icon</code></p>
	 */
	String ICON = "icon"; //$NON-NLS-1$

	/**
	 * The public node name for a configuration element
	 * <p>Equal to the word: <code>public</code></p>
	 */
	String PUBLIC = "public"; //$NON-NLS-1$

	/**
	 * The perspective node name for a configuration element.
	 * <p>Equal to the word: <code>perspective</code></p>
	 */
	String PERSPECTIVE = "perspective"; //$NON-NLS-1$

	/**
	 * The preferredDelegates node name for a configuration element.
	 * <p>Equal to the word: <code>preferredDelegates</code></p>
	 */
	String PREFERRED_DELEGATES = "preferredDelegates"; //$NON-NLS-1$

	/**
	 * The modes node name for a configuration element.
	 * <p>Equal to the word: <code>modes</code></p>
	 */
	String MODES = "modes"; //$NON-NLS-1$

	/**
	 * The modesCombination node name for a configuration element.
	 * <p>Equal to the word: <code>modeCombination</code></p>
	 */
	String MODE_COMBINATION = "modeCombination"; //$NON-NLS-1$

	/**
	 * The mode node name for a configuration element.
	 * <p>Equal to the word: <code>mode</code></p>
	 */
	String MODE = "mode"; //$NON-NLS-1$

	/**
	 * The type node name for a configuration element.
	 * <p>Equal to the word: <code>type</code></p>
	 */
	String TYPE = "type"; //$NON-NLS-1$

	/**
	 * The typeid node name for a configuration element.
	 * <p>Equal to the word: <code>typeid</code></p>
	 */
	String TYPE_ID = "typeid"; //$NON-NLS-1$

	/**
	 * The option node name for a configuration element.
	 * <p>Equal to the word: <code>options</code></p>
	 */
	String OPTIONS = "options"; //$NON-NLS-1$

	/**
	 * The delegate node name for a configuration element.
	 * <p>Equal to the word: <code>delegate</code></p>
	 */
	String DELEGATE = "delegate"; //$NON-NLS-1$

	/**
	 * The participant node name for a configuration element.
	 * <p>Equal to the word: <code>participant</code></p>
	 */
	String PARTICIPANT = "participant"; //$NON-NLS-1$

	/**
	 * The defaultShortcut node name for a configuration element.
	 * <p>Equal to the word: <code>defaultShortcut</code></p>
	 */
	String DEFAULT_LAUNCH_SHORTCUT = "defaultShortcut"; //$NON-NLS-1$

	/**
	 * The delegateName node name for a configuration element.
	 * <p>Equal to the word: <code>delegateName</code></p>
	 */
	String DELEGATE_NAME = "delegateName"; //$NON-NLS-1$

	/**
	 * The group node name for a configuration element.
	 * <p>Equal to the word: <code>group</code></p>
	 */
	String GROUP = "group"; //$NON-NLS-1$

	/**
	 * The class node name for a configuration element.
	 * <p>Equal to the word: <code>class</code></p>
	 */
	String CLASS = "class"; //$NON-NLS-1$

	/**
	 * The modelIdentifier node name for a configuration element.
	 * <p>Equal to the word: <code>modelIdentifier</code></p>
	 *
	 * @since 3.4
	 */
	String MODEL_IDENTIFIER = "modelIdentifier"; //$NON-NLS-1$

	/**
	 * The configurationTypes node name for a configuration element.
	 * <p>Equal to the word: <code>configurationType</code></p>
	 */
	String CONFIGURATION_TYPES = "configurationType"; //$NON-NLS-1$

	/**
	 * The contextLabel node name for a configuration element.
	 * <p>Equal to the word: <code>contextLabel</code></p>
	 */
	String CONTEXT_LABEL = "contextLabel"; //$NON-NLS-1$

	/**
	 * The contextualLaunch node name for a configuration element.
	 * <p>Equal to the word: <code>contextualLaunch</code></p>
	 */
	String CONTEXTUAL_LAUNCH = "contextualLaunch"; //$NON-NLS-1$

	/**
	 * The sourcePathComputerId node name for a configuration element.
	 * <p>Equal to the word: <code>sourcePathComputerId</code></p>
	 */
	String SOURCE_PATH_COMPUTER = "sourcePathComputerId"; //$NON-NLS-1$

	/**
	 * The delegateDescription node name for a configuration element.
	 * <p>Equal to the word: <code>delegateDescription</code></p>
	 */
	String DELEGATE_DESCRIPTION = "delegateDescription"; //$NON-NLS-1$

	/**
	 * The sourceLocatorId node name for a configuration element.
	 * <p>Equal to the word: <code>sourceLocatorId</code></p>
	 */
	String SOURCE_LOCATOR = "sourceLocatorId"; //$NON-NLS-1$

	/**
	 * The migrationDelegate node name for a configuration element.
	 * <p>Equal to the word: <code>migrationDelegate</code></p>
	 */
	String MIGRATION_DELEGATE = "migrationDelegate";  //$NON-NLS-1$

	/**
	 * The memento node name for a configuration element.
	 * <p>Equal to the word: <code>memento</code></p>
	 */
	String MEMENTO = "memento"; //$NON-NLS-1$

	/**
	 * The selection node name for a configuration element.
	 * <p>Equal to the word: <code>selection</code></p>
	 */
	String SELECTION = "selection"; //$NON-NLS-1$

	/**
	 * The debugContext node name for a configuration element.
	 * <p>Equal to the word: <code>debugContext</code></p>
	 */
	String DEBUG_CONTEXT = "debugContext"; //$NON-NLS-1$

	/**
	 * The editorInput node name for a configuration element.
	 * <p>Equal to the word: <code>editorInput</code></p>
	 */
	String EDITOR_INPUT = "editorInput"; //$NON-NLS-1$

	/**
	 * The allowPrototypes node name for a configuration element
	 * <p>
	 * Equal to the word: <code>allowPrototypes</code>
	 * </p>
	 *
	 * @since 3.12
	 */
	String ALLOW_PROTOTYPES = "allowPrototypes"; //$NON-NLS-1$

	/**
	 * The allowCommandLine node name for a configuration element
	 * <p>
	 * Equal to the word: <code>allowCommandLine</code>
	 * </p>
	 *
	 * @since 3.13
	 */
	String ALLOW_COMMANDLINE = "allowCommandLine"; //$NON-NLS-1$

	/**
	 * The allowOutputMerging node name for a configuration element
	 * <p>
	 * Equal to the word: <code>allowOutputMerging</code>
	 * </p>
	 *
	 * @since 3.14
	 */
	String ALLOW_OUTPUT_MERGING = "allowOutputMerging"; //$NON-NLS-1$
}
