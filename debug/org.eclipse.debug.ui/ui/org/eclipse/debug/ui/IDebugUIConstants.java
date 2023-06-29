/*******************************************************************************
 *  Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     QNX Software Systems - Mikhail Khodjaiants - Registers View (Bug 53640)
 *     Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.ui;


import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.ISharedImages;

/**
 * Constant definitions for debug UI plug-in.
 * <p>
 * Popup menus in the debug UI support action contribution via the
 * <code>org.eclipse.ui.popupMenus</code>  extension. Actions may be
 * contributed to any group on the menu. To facilitate insertion of actions
 * in between existing groups, empty groups have been defined
 * in the menu. Each group prefixed by "empty" indicates an empty group.
 * </p>
 * <h3>Debug View Popup Menu</h3>
 * <ul>
 *   <li>Empty edit group</li>
 *   <li>Edit group</li>
 *   <li>Copy stack action</li>
 *   <li>Empty step group</li>
 *   <li>Step group</li>
 *   <li>Step into group</li>
 *   <li>Step into action</li>
 * 	 <li>Step over group</li>
 *   <li>Step over action</li>
 *   <li>Step return group</li>
 *   <li>Step return action</li>
 *   <li>Empty thread group</li>
 *   <li>Thread group</li>
 *   <li>Resume action</li>
 *   <li>Suspend action</li>
 *   <li>Terminate action</li>
 *   <li>Disconnect action</li>
 *   <li>Empty launch group</li>
 *   <li>Launch group</li>
 *   <li>Remove all terminated action</li>
 *   <li>Terminate and Remove action</li>
 *   <li>Relaunch action</li>
 *   <li>Terminate all action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 *   <li>Property group</li>
 *   <li>Property dialog action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Variables View Popup Menus</h3>
 * <ul>
 *   <li>Empty variable group</li>
 *   <li>Variable group</li>
 *   <li>Select all action</li>
 *   <li>Copy to clipboard action</li>
 *   <li>Find action</li>
 *   <li>Change value action</li>
 *   <li>Available logical structures action</li>
 *   <li>Available detail panes action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 *   <li>Show type names action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Breakpoints View Popup Menu</h3>
 * <ul>
 *   <li>Empty Navigation group</li>
 *   <li>Navigation group</li>
 *   <li>Open action</li>
 *   <li>Empty Breakpoint group</li>
 *   <li>Breakpoint group</li>
 *   <li>Enable action</li>
 *   <li>Disable action</li>
 *   <li>Remove action</li>
 *   <li>Remove all action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 * 	 <li>Show breakpoints for model action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Expressions View Popup Menu</h3>
 * <ul>
 *   <li>Empty Expression group</li>
 *   <li>Expression group</li>
 *   <li>Select all action</li>
 * 	 <li>Copy to clipboard action</li>
 *   <li>Remove action</li>
 *   <li>Remove all action</li>
 *   <li>Find action</li>
 *   <li>Change variable value action</li>
 *   <li>Available logical structures action</li>
 *   <li>Available detail panes action</li>
 *   <li>Empty Render group</li>
 *   <li>Render group</li>
 * 	 <li>Show type names action</li>
 *   <li>Additions group</li>
 * </ul>
 * <p>
 * Constants only.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IDebugUIConstants {

	/**
	 * Debug UI plug-in identifier (value <code>"org.eclipse.debug.ui"</code>).
	 */
	String PLUGIN_ID = "org.eclipse.debug.ui"; //$NON-NLS-1$;

	/**
	 * Debug perspective identifier (value <code>"org.eclipse.debug.ui.DebugPerspective"</code>).
	 */
	String ID_DEBUG_PERSPECTIVE = PLUGIN_ID + ".DebugPerspective"; //$NON-NLS-1$

	/**
	 * Console type identifier (value <code>"org.eclipse.debug.ui.ProcessConsoleType"</code>).
	 *
	 * @since 3.1
	 */
	String ID_PROCESS_CONSOLE_TYPE = PLUGIN_ID + ".ProcessConsoleType"; //$NON-NLS-1$

	/**
	 * Constant for referring to no perspective.
	 */
	String PERSPECTIVE_NONE = "perspective_none"; //$NON-NLS-1$

	/**
	 * Constant for referring to a default perspective.
	 */
	String PERSPECTIVE_DEFAULT = "perspective_default"; //$NON-NLS-1$

	// Preferences

	/**
	 * String preference that identifies the default 'switch to perspective id' when running a
	 * launch configuration.  This default is used if a particular launch configuration does not
	 * override the 'switch to perspective when in run mode' attribute with something else.
	 */
	String PREF_SHOW_RUN_PERSPECTIVE_DEFAULT= PLUGIN_ID + ".show_run_perspective_default";  //$NON-NLS-1$

	/**
	 * String preference that identifies the default 'switch to perspective id' when debugging a
	 * launch configuration.  This default is used if a particular launch configuration does not
	 * override the 'switch to perspective when in debug mode' attribute with something else.
	 */
	String PREF_SHOW_DEBUG_PERSPECTIVE_DEFAULT= PLUGIN_ID + ".show_debug_perspective_default";  //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether a build is done before
	 * launching a program (if one is needed).
	 */
	String PREF_BUILD_BEFORE_LAUNCH= PLUGIN_ID + ".build_before_launch"; //$NON-NLS-1$
	/**
	 * Boolean preference controlling automatic removal of terminated launches
	 * when a new launch is registered.
	 * @since 2.0
	 */
	String PREF_AUTO_REMOVE_OLD_LAUNCHES= PLUGIN_ID + ".auto_remove_old_launches"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debugger re-uses non-dirty editors
	 * that it opens when displaying source. When <code>true</code> the debugger
	 * re-uses the same editor when showing source for a selected stack frame (unless
	 * the editor is dirty).
	 *
	 * @since 2.0
	 */
	String PREF_REUSE_EDITOR = PLUGIN_ID + ".reuse_editor"; //$NON-NLS-1$

	/**
	 * Integer preference that specifies the length of the Run &amp; Debug launch
	 * history lists.
	 *
	 * @since 2.0
	 */
	String PREF_MAX_HISTORY_SIZE = PLUGIN_ID + ".max_history_size"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debugger shows types names
	 * in its variable view. When <code>true</code> the debugger
	 * will display type names in new variable views.
	 *
	 * @since 2.0
	 * @deprecated no longer used
	 */
	@Deprecated String PREF_SHOW_TYPE_NAMES = PLUGIN_ID + ".show_type_names"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debugger shows the detail pane
	 * in its variable view. When <code>true</code> the debugger
	 * will show the detail panes in new variable views.
	 *
	 * @since 2.0
	 * @deprecated no longer used
	 */
	@Deprecated String PREF_SHOW_DETAIL_PANE = PLUGIN_ID + ".show_detail_pane"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debugger will force activate the active
	 * shell/window of the Eclipse workbench when a breakpoint is hit.
	 *
	 * @since 2.1
	 */
	String PREF_ACTIVATE_WORKBENCH= PLUGIN_ID + ".activate_workbench"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether breakpoints are
	 * automatically skipped during a Run To Line operation.
	 * If true, breakpoints will be skipped automatically
	 * during Run To Line. If false, they will be hit.
	 *
	 * @since 3.0
	 */
	String PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE= PLUGIN_ID + ".skip_breakpoints_during_run_to_line"; //$NON-NLS-1$

	/**
	 * String preference controlling in which perspectives view management will
	 * occur.  This preference, if set, overrides the perspectives enabled
	 * through the <code>contextViewBindings</code> extension point. The value
	 * is a comma-separated list of perspective IDs, an empty string (no perspectives),
	 * or the value of <code>PREF_MANAGE_VIEW_PERSPECTIVES_DEFAULT</code> ("<code>DEFAULT</code>").
	 *
	 * @since 3.0
	 */
	String PREF_MANAGE_VIEW_PERSPECTIVES= PLUGIN_ID + ".manage_view_perspectives"; //$NON-NLS-1$

	/**
	 * The default value of the {@link IDebugUIConstants#PREF_MANAGE_VIEW_PERSPECTIVES} preference.
	 *
	 * @see IDebugUIConstants#PREF_MANAGE_VIEW_PERSPECTIVES
	 *
	 * @since 3.5
	 */
	String PREF_MANAGE_VIEW_PERSPECTIVES_DEFAULT= "DEFAULT"; //$NON-NLS-1$

	/**
	 * Font preference setting for the process console.
	 *
	 * @since 3.3
	 */
	String PREF_CONSOLE_FONT= "org.eclipse.debug.ui.consoleFont"; //$NON-NLS-1$

	/**
	 * Debug view identifier (value <code>"org.eclipse.debug.ui.DebugView"</code>).
	 */
	String ID_DEBUG_VIEW= "org.eclipse.debug.ui.DebugView"; //$NON-NLS-1$

	/**
	 * Breakpoint view identifier (value <code>"org.eclipse.debug.ui.BreakpointView"</code>).
	 */
	String ID_BREAKPOINT_VIEW= "org.eclipse.debug.ui.BreakpointView"; //$NON-NLS-1$

	/**
	 * Variable view identifier (value <code>"org.eclipse.debug.ui.VariableView"</code>).
	 */
	String ID_VARIABLE_VIEW= "org.eclipse.debug.ui.VariableView"; //$NON-NLS-1$

	/**
	 * Expression view identifier (value <code>"org.eclipse.debug.ui.ExpressionView"</code>).
	 * @since 2.0
	 */
	String ID_EXPRESSION_VIEW= "org.eclipse.debug.ui.ExpressionView"; //$NON-NLS-1$

	/**
	 * Register view identifier (value <code>"org.eclipse.debug.ui.RegisterView"</code>).
	 * @since 3.0
	 */
	String ID_REGISTER_VIEW= "org.eclipse.debug.ui.RegisterView"; //$NON-NLS-1$

	/**
	 * Module view identifier (value <code>"org.eclipse.debug.ui.ModuleView"</code>).
	 * @since 3.4
	 */
	String ID_MODULE_VIEW= "org.eclipse.debug.ui.ModuleView"; //$NON-NLS-1$

	/**
	 * Console view identifier (value <code>"org.eclipse.debug.ui.ConsoleView"</code>).
	 * @deprecated Use org.eclipse.ui.console.IConsoleConstants.ID_CONSOLE_VIEW
	 * @since 3.0
	 */
	@Deprecated String ID_CONSOLE_VIEW= "org.eclipse.debug.ui.ConsoleView"; //$NON-NLS-1$

	// Console stream identifiers

	/**
	 * Identifier for the standard out stream.
	 *
	 * @see org.eclipse.debug.ui.console.IConsoleColorProvider#getColor(String)
	 * @since 2.1
	 */
	String ID_STANDARD_OUTPUT_STREAM = IDebugUIConstants.PLUGIN_ID + ".ID_STANDARD_OUTPUT_STREAM"; //$NON-NLS-1$

	/**
	 * Identifier for the standard error stream.
	 *
	 * @see org.eclipse.debug.ui.console.IConsoleColorProvider#getColor(String)
	 * @since 2.1
	 */
	String ID_STANDARD_ERROR_STREAM = IDebugUIConstants.PLUGIN_ID + ".ID_STANDARD_ERROR_STREAM"; //$NON-NLS-1$

	/**
	 * Identifier for the standard input stream.
	 *
	 * @see org.eclipse.debug.ui.console.IConsoleColorProvider#getColor(String)
	 * @since 2.1
	 */
	String ID_STANDARD_INPUT_STREAM = IDebugUIConstants.PLUGIN_ID + ".ID_STANDARD_INPUT_STREAM"; //$NON-NLS-1$

	// Debug Action images

	/**
	 * Debug action image identifier.
	 */
	String IMG_ACT_DEBUG= "IMG_ACT_DEBUG"; //$NON-NLS-1$

	/**
	 * Run action image identifier.
	 */
	String IMG_ACT_RUN= "IMG_ACT_RUN"; //$NON-NLS-1$

	/** "Link with View" action image identifier. */
	String IMG_ACT_SYNCED= "IMG_ACT_SYNCED"; //$NON-NLS-1$

	/** "Skip Breakpoints" action image identifier */
	String IMG_SKIP_BREAKPOINTS= "IMG_SKIP_BREAKPOINTS"; //$NON-NLS-1$

	/** Clear action image identifier.
	 * @deprecated use the platform shared image for clear - {@link ISharedImages#IMG_ETOOL_CLEAR}*/
	@Deprecated String IMG_LCL_CLEAR= "IMG_LCL_CLEAR"; //$NON-NLS-1$

	/** Display variable type names action image identifier. */
	String IMG_LCL_TYPE_NAMES= "IMG_LCL_TYPE_NAMES"; //$NON-NLS-1$

	/** Toggle detail pane action image identifier.*/
	String IMG_LCL_DETAIL_PANE= "IMG_LCL_DETAIL_PANE"; //$NON-NLS-1$

	/** Change variable value action image identifier.*/
	String IMG_LCL_CHANGE_VARIABLE_VALUE= "IMG_LCL_CHANGE_VARIABLE_VALUE"; //$NON-NLS-1$

	/**
	 * Disconnect action image identifier
	 *
	 * @since 2.0
	 */
	String IMG_LCL_DISCONNECT= "IMG_LCL_DISCONNECT"; //$NON-NLS-1$

	/**
	 * Scroll lock action image identifier
	 *
	 * @since 2.1
	 */
	String IMG_LCL_LOCK = "IMG_LCL_LOCK"; //$NON-NLS-1$

	/**
	 * Add action image identifier.
	 *
	 * @since 3.8
	 */
	String IMG_LCL_ADD = "IMG_LCL_MONITOR_EXPRESSION"; //$NON-NLS-1$

	/**
	 * Remove all action image identifier
	 *
	 * @since 2.1
	 */
	String IMG_LCL_REMOVE_ALL = "IMG_LCL_REMOVE_ALL"; //$NON-NLS-1$

	/**
	 * Remove action image identifier
	 *
	 * @since 3.2
	 */
	String IMG_LCL_REMOVE = "IMG_LCL_REMOVE"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier.
	 */
	String IMG_LCL_CONTENT_ASSIST= "IMG_LCL_CONTENT_ASSIST"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier (enabled).
	 */
	String IMG_ELCL_CONTENT_ASSIST= "IMG_ELCL_CONTENT_ASSIST"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier (disabled).
	 */
	String IMG_DLCL_CONTENT_ASSIST= "IMG_DLCL_CONTENT_ASSIST"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier.
	 */
	String IMG_LCL_DETAIL_PANE_UNDER= "IMG_LCL_DETAIL_PANE_UNDER"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier.
	 */
	String IMG_LCL_DETAIL_PANE_RIGHT= "IMG_LCL_DETAIL_PANE_RIGHT"; //$NON-NLS-1$

	/**
	 * Content assist action image identifier.
	 */
	String IMG_LCL_DETAIL_PANE_HIDE= "IMG_LCL_DETAIL_PANE_HIDE"; //$NON-NLS-1$

	// Debug element images

	/** Debug mode launch image identifier. */
	String IMG_OBJS_LAUNCH_DEBUG= "IMG_OBJS_LAUNCH_DEBUG"; //$NON-NLS-1$

	/** Run mode launch image identifier. */
	String IMG_OBJS_LAUNCH_RUN= "IMG_OBJS_LAUNCH_RUN"; //$NON-NLS-1$

	/** Terminated run mode launch image identifier. */
	String IMG_OBJS_LAUNCH_RUN_TERMINATED= "IMG_OBJS_LAUNCH_RUN_TERMINATED"; //$NON-NLS-1$

	/** Running debug target image identifier. */
	String IMG_OBJS_DEBUG_TARGET= "IMG_OBJS_DEBUG_TARGET"; //$NON-NLS-1$

	/** Suspended debug target image identifier. */
	String IMG_OBJS_DEBUG_TARGET_SUSPENDED= "IMG_OBJS_DEBUG_TARGET_SUSPENDED"; //$NON-NLS-1$

	/** Terminated debug target image identifier. */
	String IMG_OBJS_DEBUG_TARGET_TERMINATED= "IMG_OBJS_DEBUG_TARGET_TERMINATED"; //$NON-NLS-1$

	/** Running thread image identifier. */
	String IMG_OBJS_THREAD_RUNNING= "IMG_OBJS_THREAD_RUNNING"; //$NON-NLS-1$

	/** Suspended thread image identifier. */
	String IMG_OBJS_THREAD_SUSPENDED= "IMG_OBJS_THREAD_SUSPENDED"; //$NON-NLS-1$

	/** Terminated thread image identifier. */
	String IMG_OBJS_THREAD_TERMINATED= "IMG_OBJS_THREAD_TERMINATED"; //$NON-NLS-1$

	/** Stack frame (suspended) image identifier. */
	String IMG_OBJS_STACKFRAME= "IMG_OBJS_STACKFRAME"; //$NON-NLS-1$

	/** Stack frame (running) image identifier. */
	String IMG_OBJS_STACKFRAME_RUNNING= "IMG_OBJS_STACKFRAME_RUNNING"; //$NON-NLS-1$

	/** Enabled breakpoint image identifier. */
	String IMG_OBJS_BREAKPOINT= "IMG_OBJS_BREAKPOINT"; //$NON-NLS-1$

	/** Disabled breakpoint image identifier. */
	String IMG_OBJS_BREAKPOINT_DISABLED= "IMG_OBJS_BREAKPOINT_DISABLED"; //$NON-NLS-1$

	/**
	 * Breakpoint group image identifier.
	 *
	 * @since 3.1
	 */
	String IMG_OBJS_BREAKPOINT_GROUP = "IMG_OBJS_BREAKPOINT_GROUP"; //$NON-NLS-1$

	/**
	 * Disabled breakpoint group image identifier.
	 *
	 * @since 3.1
	 */
	String IMG_OBJS_BREAKPOINT_GROUP_DISABLED = "IMG_OBJS_BREAKPOINT_GROUP_DISABLED"; //$NON-NLS-1$

	/**
	 * Enabled watchpoint image identifier (access &amp; modification).
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_WATCHPOINT= "IMG_OBJS_WATCHPOINT"; //$NON-NLS-1$

	/**
	 * Disabled watchpoint image identifier (access &amp; modification).
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_WATCHPOINT_DISABLED= "IMG_OBJS_WATCHPOINT_DISABLED"; //$NON-NLS-1$

	/**
	 * Enabled access watchpoint image identifier.
	 * @since 3.1
	 */
	String IMG_OBJS_ACCESS_WATCHPOINT= "IMG_OBJS_ACCESS_WATCHPOINT"; //$NON-NLS-1$

	/**
	 * Disabled access watchpoint image identifier.
	 * @since 3.1
	 */
	String IMG_OBJS_ACCESS_WATCHPOINT_DISABLED= "IMG_OBJS_ACCESS_WATCHPOINT_DISABLED"; //$NON-NLS-1$

	/**
	 * Enabled modification watchpoint image identifier.
	 * @since 3.1
	 */
	String IMG_OBJS_MODIFICATION_WATCHPOINT= "IMG_OBJS_MODIFICATION_WATCHPOINT"; //$NON-NLS-1$

	/**
	 * Disabled modification watchpoint image identifier.
	 * @since 3.1
	 */
	String IMG_OBJS_MODIFICATION_WATCHPOINT_DISABLED= "IMG_OBJS_MODIFICATION_WATCHPOINT_DISABLED"; //$NON-NLS-1$

	/** Running system process image identifier. */
	String IMG_OBJS_OS_PROCESS= "IMG_OBJS_OS_PROCESS"; //$NON-NLS-1$

	/** Terminated system process image identifier. */
	String IMG_OBJS_OS_PROCESS_TERMINATED= "IMG_OBJS_OS_PROCESS_TERMINATED"; //$NON-NLS-1$

	/**
	 * Expression image identifier.
	 *
	 * @since 2.0
	 */
	String IMG_OBJS_EXPRESSION= "IMG_OBJS_EXPRESSION"; //$NON-NLS-1$

	/**
	 * Generic variable image identifier.
	 *
	 * @since 2.0
	 */
	String IMG_OBJS_VARIABLE= "IMG_OBJS_VARIABLE"; //$NON-NLS-1$

	/**
	 * Generic identifier of register group image.
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_REGISTER_GROUP= "IMG_OBJS_REGISTER_GROUP"; //$NON-NLS-1$

	/**
	 * Generic register image identifier.
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_REGISTER= "IMG_OBJS_REGISTER"; //$NON-NLS-1$

	/**
	 * Environment image identifier.
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_ENVIRONMENT = "IMG_OBJS_ENVIRONMENT"; //$NON-NLS-1$

	/**
	 * Environment variable image identifier.
	 *
	 * @since 3.0
	 */
	String IMG_OBJS_ENV_VAR = "IMG_OBJS_ENV_VAR"; //$NON-NLS-1$

	// views

	/**
	 * Launches view image identifier
	 *
	 * @since 2.0
	 */
	String IMG_VIEW_LAUNCHES= "IMG_VIEW_LAUNCHES"; //$NON-NLS-1$

	/**
	 * Breakpoints view image identifier
	 *
	 * @since 2.0
	 */
	String IMG_VIEW_BREAKPOINTS= "IMG_VIEW_BREAKPOINTS"; //$NON-NLS-1$

	/**
	 * Variables view image identifier
	 *
	 * @since 2.0
	 */
	String IMG_VIEW_VARIABLES= "IMG_VIEW_VARIABLES"; //$NON-NLS-1$

	/**
	 * Expressions view image identifier
	 *
	 * @since 2.0
	 */
	String IMG_VIEW_EXPRESSIONS= "IMG_VIEW_EXPRESSIONS"; //$NON-NLS-1$

	/**
	 * Console view image identifier
	 *
	 * @since 2.0
	 */
	String IMG_VIEW_CONSOLE= "IMG_VIEW_CONSOLE"; //$NON-NLS-1$

	// perspective
	/**
	 * Debug perspective image identifier
	 *
	 * @since 2.0
	 */
	String IMG_PERSPECTIVE_DEBUG= "IMG_PERSPECTIVE_DEBUG"; //$NON-NLS-1$

	// wizard banners
	/** Debug wizard banner image identifier. */
	String IMG_WIZBAN_DEBUG= "IMG_WIZBAN_DEBUG"; //$NON-NLS-1$

	/** Run wizard banner image identifier. */
	String IMG_WIZBAN_RUN= "IMG_WIZBAN_RUN"; //$NON-NLS-1$

	// overlays
	/** Error overlay image identifier. */
	String IMG_OVR_ERROR = "IMG_OVR_ERROR";  //$NON-NLS-1$

	/**
	 * Skip breakpoint image overlay identifier.
	 * @since 3.1
	 */
	String IMG_OVR_SKIP_BREAKPOINT = "IMG_OVR_SKIP_BREAKPOINT"; //$NON-NLS-1$

	/**
	 * Prototype image overlay identifier.
	 *
	 * @since 3.13
	 */
	String IMG_OVR_PROTOTYPE = "IMG_OVR_PROTOTYPE";  //$NON-NLS-1$

	/**
	 * Debug action set identifier (value <code>"org.eclipse.debug.ui.debugActionSet"</code>).
	 */
	String DEBUG_ACTION_SET= PLUGIN_ID + ".debugActionSet"; //$NON-NLS-1$

	/**
	 * Debug Toolbar action set identifier (value <code>"org.eclipse.debug.ui.debugToolbarActionSet"</code>).
	 *
	 * @since 3.8
	 */
	String DEBUG_TOOLBAR_ACTION_SET= PLUGIN_ID + ".debugToolbarActionSet"; //$NON-NLS-1$

	/**
	 * System property which indicates whether the common debugging actions
	 * should be shown in the Debug view, as opposed to the top level
	 * toolbar.  Actions contributing to the debug view can use this property
	 * to control their visibility.
	 *
	 * @since 3.8
	 */
	String DEBUG_VIEW_TOOBAR_VISIBLE = PLUGIN_ID + ".debugViewToolbarVisible"; //$NON-NLS-1$

	/**
	 * Launch action set identifier (value <code>"org.eclipse.debug.ui.launchActionSet"</code>).
	 */
	String LAUNCH_ACTION_SET= PLUGIN_ID + ".launchActionSet"; //$NON-NLS-1$

	// extensions
	/**
	 * Identifier for the standard 'debug' launch group.
	 * @since 2.1
	 */
	String ID_DEBUG_LAUNCH_GROUP = PLUGIN_ID + ".launchGroup.debug"; //$NON-NLS-1$

	/**
	 * Identifier for the standard 'run' launch group.
	 * @since 2.1
	 */
	String ID_RUN_LAUNCH_GROUP = PLUGIN_ID + ".launchGroup.run"; //$NON-NLS-1$

	/**
	 * Identifier for the standard 'profile' launch group.
	 * @since 3.0
	 */
	String ID_PROFILE_LAUNCH_GROUP = PLUGIN_ID + ".launchGroup.profile"; //$NON-NLS-1$

	// menus

	/**
	 * Identifier for an empty group preceding an
	 * edit group in a menu (value <code>"emptyEditGroup"</code>).
	 */
	String EMPTY_EDIT_GROUP = "emptyEditGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an edit group in a menu (value <code>"editGroup"</code>).
	 */
	String EDIT_GROUP = "editGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * step group in a menu (value <code>"emptyStepGroup"</code>).
	 */
	String EMPTY_STEP_GROUP = "emptyStepGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a step group in a menu or toolbar (value <code>"stepGroup"</code>).
	 */
	String STEP_GROUP = "stepGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a step into group in a menu or toolbar (value <code>"stepIntoGroup"</code>).
	 */
	String STEP_INTO_GROUP = "stepIntoGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a step over group in a menu or toolbar (value <code>"stepOverGroup"</code>).
	 */
	String STEP_OVER_GROUP = "stepOverGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a step return group in a menu or toolbar (value <code>"stepReturnGroup"</code>).
	 */
	String STEP_RETURN_GROUP = "stepReturnGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * thread group in a menu (value <code>"emptyThreadGroup"</code>).
	 */
	String EMPTY_THREAD_GROUP = "emptyThreadGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a thread group in a menu or toolbar(value <code>"threadGroup"</code>).
	 */
	String THREAD_GROUP = "threadGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * launch group in a menu (value <code>"emptyLaunchGroup"</code>).
	 */
	String EMPTY_LAUNCH_GROUP = "emptyLaunchGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a launch group in a menu (value <code>"launchGroup"</code>).
	 */
	String LAUNCH_GROUP = "launchGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an output group in a menu (value
	 * <code>"outputGroup"</code>).
	 */
	String OUTPUT_GROUP = "outputGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * variable group in a menu (value <code>"emptyVariableGroup"</code>).
	 */
	String EMPTY_VARIABLE_GROUP = "emptyVariableGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a variable group in a menu (value <code>"variableGroup"</code>).
	 */
	String VARIABLE_GROUP = "variableGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * navigation group in a menu (value <code>"emptyNavigationGroup"</code>).
	 */
	String EMPTY_NAVIGATION_GROUP = "emptyNavigationGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a navigation group in a menu (value <code>"navigationGroup"</code>).
	 */
	String NAVIGATION_GROUP = "navigationGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * breakpoint group in a menu (value <code>"emptyBreakpointGroup"</code>).
	 */
	String EMPTY_BREAKPOINT_GROUP = "emptyBreakpointGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a breakpoint group in a menu (value <code>"breakpointGroup"</code>).
	 */
	String BREAKPOINT_GROUP = "breakpointGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a "breakpoint group" group in a menu (value <code>"breakpointGroupGroup"</code>).
	 *
	 * @since 3.1
	 */
	String BREAKPOINT_GROUP_GROUP = "breakpointGroupGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding an
	 * expression group in a menu (value <code>"emptyExpressionGroup"</code>).
	 *
	 * @since 2.0
	 */
	String EMPTY_EXPRESSION_GROUP = "emptyExpressionGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an expression group in a menu (value <code>"expressionGroup"</code>).
	 *
	 * @since 2.0
	 */
	String EXPRESSION_GROUP = "expressionGroup"; //$NON-NLS-1$
	/**
	 * Identifier for an empty group preceding a
	 * render group in a menu (value <code>"emptyRenderGroup"</code>).
	 */
	String EMPTY_RENDER_GROUP = "emptyRenderGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a render group in a menu or toolbar(value <code>"renderGroup"</code>).
	 */
	String RENDER_GROUP = "renderGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a property group in a menu (value <code>"propertyGroup"</code>).
	 */
	String PROPERTY_GROUP = "propertyGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * register group in a menu (value <code>"emptyRegisterGroup"</code>).
	 */
	String EMPTY_REGISTER_GROUP = "emptyRegisterGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a register group in a menu (value <code>"registerGroup"</code>).
	 */
	String REGISTER_GROUP = "registerGroup"; //$NON-NLS-1$

	/**
	 * Identifier for an empty group preceding a
	 * modules group in a menu (value <code>"emptyModulesGroup"</code>).
	 * @since 3.4
	 */
	String EMPTY_MODULES_GROUP = "emptyModulesGroup"; //$NON-NLS-1$

	/**
	 * Identifier for a modules group in a menu (value <code>"modulesGroup"</code>).
	 * @since 3.4
	 */
	String MODULES_GROUP = "modulesGroup"; //$NON-NLS-1$

	/**
	 * Id for the popup menu associated with the variables (tree viewer) part of the VariableView
	 */
	String VARIABLE_VIEW_VARIABLE_ID = "org.eclipse.debug.ui.VariableView.variables"; //$NON-NLS-1$

	/**
	 * Id for the popup menu associated with the detail (text viewer) part of the VariableView
	 */
	String VARIABLE_VIEW_DETAIL_ID = "org.eclipse.debug.ui.VariableView.detail"; //$NON-NLS-1$

	// status codes
	/**
	 * Status indicating an invalid extension definition.
	 */
	int STATUS_INVALID_EXTENSION_DEFINITION = 100;

	/**
	 * Status code indicating an unexpected internal error.
	 */
	int INTERNAL_ERROR = 120;

	// launch configuration attribute keys
	/**
	 * Launch configuration attribute - the perspective to
	 * switch to when a launch configuration is launched in
	 * run mode (value <code>org.eclipse.debug.ui.target_run_perspective</code>).
	 * Value is a string corresponding to a perspective identifier,
	 * or <code>null</code> indicating no perspective change.
	 *
	 * @since 2.0
	 * @deprecated Since 3.0, this launch configuration attribute is no longer supported.
	 *  Use <code>DebugUITools.setLaunchPerspective(ILaunchConfigurationType type, String mode, String perspective)</code>.
	 */
	@Deprecated String ATTR_TARGET_RUN_PERSPECTIVE = PLUGIN_ID + ".target_run_perspective";	 //$NON-NLS-1$

	/**
	 * Launch configuration attribute - the perspective to
	 * switch to when a launch configuration is launched in
	 * debug mode (value <code>org.eclipse.debug.ui.target_debug_perspective</code>).
	 * Value is a string corresponding to a perspective identifier,
	 * or <code>null</code> indicating no perspective change.
	 *
	 * @since 2.0
	 * @deprecated Since 3.0, this launch configuration attribute is no longer supported.
	 *  Use <code>DebugUITools.setLaunchPerspective(ILaunchConfigurationType type, String mode, String perspective)</code>.
	 */
	@Deprecated String ATTR_TARGET_DEBUG_PERSPECTIVE = PLUGIN_ID + ".target_debug_perspective";		 //$NON-NLS-1$

	/**
	 * Launch configuration attribute - the container where the configuration file
	 * is stored.  The container is set via the 'setContainer()' call on ILaunchConfigurationWorkingCopy.
	 * This constant is only needed for persisting and reading the default value of the
	 * container value for individual resources.
	 *
	 * @since 2.0
	 */
	String ATTR_CONTAINER = PLUGIN_ID + ".container"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute - a boolean value that indicates if the launch configuration
	 * is 'private'.  A private configuration is one that does not appear to the user in the launch
	 * history or the launch configuration dialog.
	 *
	 * @since 2.0
	 */
	String ATTR_PRIVATE = ILaunchManager.ATTR_PRIVATE;

	/**
	 * Launch configuration attribute - a boolean value that indicates if the launch configuration
	 * is displayed in the debug favorites menu. Default value is
	 * <code>false</code> if absent.
	 *
	 * @since 2.0
	 * @deprecated use <code>ATTR_FAVORITE_GROUPS</code> instead
	 */
	@Deprecated String ATTR_DEBUG_FAVORITE = PLUGIN_ID + ".debugFavorite"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute - a boolean value that indicates if the launch configuration
	 * is displayed in the run favorites menu.Default value is
	 * <code>false</code> if absent.
	 *
	 * @since 2.0
	 * @deprecated use <code>ATTR_FAVORITE_GROUPS</code> instead
	 */
	@Deprecated String ATTR_RUN_FAVORITE = PLUGIN_ID + ".runFavorite"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute - a list of launch group identifiers
	 * representing the favorite histories a launch configuration should appear
	 * in. When <code>null</code>, the configuration does not appear in any
	 * favorite lists.
	 *
	 * @since 2.1
	 */
	String ATTR_FAVORITE_GROUPS = PLUGIN_ID + ".favoriteGroups"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute - a boolean value indicating whether a
	 * configuration should be launched in the background. Default value is <code>true</code>.
	 *
	 * @since 3.0
	 */
	String ATTR_LAUNCH_IN_BACKGROUND = PLUGIN_ID + ".ATTR_LAUNCH_IN_BACKGROUND"; //$NON-NLS-1$

	/**
	 * ProcessConsole attribute - references the process that was launched.
	 *
	 * @since 3.1
	 */
	String ATTR_CONSOLE_PROCESS = PLUGIN_ID + ".ATTR_CONSOLE_PROCESS"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute specifying the encoding that the console should use.
	 * When unspecified, the default encoding is used. Encoding names are available
	 * from {@link org.eclipse.ui.WorkbenchEncoding}.
	 *
	 * @since 3.1
	 * @deprecated in 3.3 Please use DebugPlugin.ATTR_CONSOLE_ENCODING instead.
	 */
	@Deprecated String ATTR_CONSOLE_ENCODING = DebugPlugin.ATTR_CONSOLE_ENCODING;

	/**
	 * Launch configuration boolean attribute specifying whether output from the launched process will
	 * be captured and written to the console. Default value is <code>true</code>.
	 *
	 * @since 3.1
	 */
	String ATTR_CAPTURE_IN_CONSOLE = PLUGIN_ID + ".ATTR_CONSOLE_OUTPUT_ON"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute to specifying a file whose content is supplied
	 * to the launched process input stream. Default value is <code>null</code>.
	 *
	 * @since 3.11
	 */
	String ATTR_CAPTURE_STDIN_FILE = PLUGIN_ID + ".ATTR_CAPTURE_STDIN_FILE"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute specifying a file name that console output
	 * should be written to or <code>null</code> if none. Default value is
	 * <code>null</code>. When specified, all output from the launched process
	 * will be written to the file. The file name attribute may contain
	 * variables which will be resolved by the
	 * {@link org.eclipse.core.variables.IStringVariableManager}.
	 *
	 * @since 3.1
	 */
	String ATTR_CAPTURE_IN_FILE = PLUGIN_ID + ".ATTR_CAPTURE_IN_FILE"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute specifying whether process output will be appended to the
	 * file specified by the <code>ATTR_CAPTURE_IN_FILE</code> attribute instead of overwriting
	 * the file. Default value is <code>false</code>.
	 *
	 * @since 3.1
	 */
	String ATTR_APPEND_TO_FILE = PLUGIN_ID + ".ATTR_APPEND_TO_FILE"; //$NON-NLS-1$

	// Extension points

	/**
	 * Debug model presentation simple extension point identifier (value <code>"debugModelPresentations"</code>).
	 */
	String ID_DEBUG_MODEL_PRESENTATION= "debugModelPresentations"; //$NON-NLS-1$

	/**
	 * Debug action groups extension point identifier
	 * (value <code>"debugActionGroups"</code>).
	 *
	 * @since 2.0
	 * @deprecated The Debug Action Groups extension point no longer exists. Product
	 *  vendors should use Activities instead.
	 */
	@Deprecated String EXTENSION_POINT_DEBUG_ACTION_GROUPS= "debugActionGroups";	 //$NON-NLS-1$

	/**
	 * Launch configuration tab groups extension point identifier
	 * (value <code>"launchConfigurationTabGroups"</code>).
	 *
	 * @since 2.0
	 */
	String EXTENSION_POINT_LAUNCH_CONFIGURATION_TAB_GROUPS= "launchConfigurationTabGroups";	 //$NON-NLS-1$

	/**
	 * Contributed Launch Configuration Tab extension point identifier
	 * (value <code>"launchConfigurationTabs"</code>).
	 *
	 * @since 3.3
	 */
	String EXTENSION_POINT_LAUNCH_TABS = "launchConfigurationTabs"; //$NON-NLS-1$

	/**
	 * Launch shortcuts extension point identifier
	 * (value <code>"launchShortcuts"</code>).
	 *
	 * @since 2.0
	 */
	String EXTENSION_POINT_LAUNCH_SHORTCUTS= "launchShortcuts";	 //$NON-NLS-1$

	/**
	 * Extension point for launch configuration type images.
	 *
	 * @since 2.0
	 */
	String EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPE_IMAGES = "launchConfigurationTypeImages"; //$NON-NLS-1$

	/**
	 * Console document color provider extension point identifier
	 * (value <code>"consoleColorProviders"</code>).
	 *
	 * @since 2.1
	 */
	String EXTENSION_POINT_CONSOLE_COLOR_PROVIDERS = "consoleColorProviders";	 //$NON-NLS-1$

	/**
	 * Launch groups extension point identifier (value
	 * <code>"launchGroups"</code>).
	 *
	 * @since 2.1
	 */
	String EXTENSION_POINT_LAUNCH_GROUPS = "launchGroups";	 //$NON-NLS-1$

	/**
	 * Console line trackers extension point identifier (value
	 * <code>"consoleLineTrackers"</code>).
	 *
	 * @since 2.1
	 */
	String EXTENSION_POINT_CONSOLE_LINE_TRACKERS = "consoleLineTrackers";	 //$NON-NLS-1$

	/**
	 * Variables content providers extension point identifier (value
	 * <code>"variablesContentProviders"</code>).
	 *
	 * @since 3.0
	 */
	String EXTENSION_POINT_OBJECT_BROWSERS = "objectBrowsers";	 //$NON-NLS-1$

	/**
	 * Launch variable components extension point identifier (value
	 * <code>"launchVariableComponents"</code>). The launch variable
	 * components extension point specifies an <code>IVariableComponent</code>
	 * for an <code>IContextLaunchVariable</code>.
	 */
	String EXTENSION_POINT_LAUNCH_VARIABLE_COMPONENTS = "launchVariableComponents";		//$NON-NLS-1$

	/**
	 * Variable value editors extension point identifier (value
	 * <code>"variableValueEditors"</code>
	 *
	 * @since 3.1
	 */
	String EXTENSION_POINT_VARIABLE_VALUE_EDITORS = "variableValueEditors"; //$NON-NLS-1$

	/**
	 * Memory renderings extension point identifier (value
	 * <code>"memoryRenderings"</code>
	 *
	 * @since 3.1
	 * @deprecated  As of release 3.4, replaced by {@link #EXTENSION_POINT_MEMORY_RENDERINGS}
	 */
	@Deprecated String EXTENSION_POINT_MEMORY_RENDERIGNS = "memoryRenderings"; //$NON-NLS-1$

	/**
	 * Memory renderings extension point identifier (value
	 * <code>"memoryRenderings"</code>
	 *
	 * @since 3.4
	 */
	String EXTENSION_POINT_MEMORY_RENDERINGS = "memoryRenderings"; //$NON-NLS-1$

	/**
	 * Breakpoint organizers extension point identifier (value
	 * <code>"breakpointOrganizers"</code>
	 *
	 * @since 3.1
	 */
	String EXTENSION_POINT_BREAKPOINT_ORGANIZERS = "breakpointOrganizers"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"detailPaneFactories"</code>) for the
	 * detail pane factories extension point.
	 *
	 * @since 3.3
	 */
	String EXTENSION_POINT_DETAIL_FACTORIES = "detailPaneFactories"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"toggleBreakpointsTargetFactories"</code>) for the
	 * toggle breakpoint targets extension point.
	 *
	 * @since 3.5
	 */
	String EXTENSION_POINT_TOGGLE_BREAKPOINTS_TARGET_FACTORIES = "toggleBreakpointsTargetFactories"; //$NON-NLS-1$

	/**
	 * Update policies extension point identifier (value
	 * <code>"updatePolicies"</code>
	 *
	 * @since 3.2
	 */
	String EXTENSION_POINT_UPDATE_POLICIES = "updatePolicies"; //$NON-NLS-1$

	/**
	 * Padded string preference for renderings.  Padded string is the string to be used in place of
	 * of a memory byte if a rendering cannot render the data properly.
	 * @since 3.1
	 */
	String PREF_PADDED_STR = PLUGIN_ID + ".memory.paddedStr"; //$NON-NLS-1$

	/**
	 * ASCII code page for rendering memory to ASCII strings.
	 * @since 3.1
	 */
	String PREF_DEFAULT_ASCII_CODE_PAGE = PLUGIN_ID + ".defaultAsciiCodePage"; //$NON-NLS-1$

	/**
	 * EBCDIC code page for rendering memory to EBCDIC strings.
	 * @since 3.1
	 */
	String PREF_DEFAULT_EBCDIC_CODE_PAGE = PLUGIN_ID + ".defaultEbcdicCodePage"; //$NON-NLS-1$

	/**
	 * Maximum number of characters to display in the details area of the variables
	 * view, or 0 if unlimited.
	 *
	 * @since 3.2
	 */
	String PREF_MAX_DETAIL_LENGTH = PLUGIN_ID + ".max_detail_length"; //$NON-NLS-1$

	/**
	 * Identifier for breakpoint working set type.
	 * <br>
	 * Value is: <code>org.eclipse.debug.ui.breakpointWorkingSet</code>
	 *
	 * @since 3.2
	 */
	String BREAKPOINT_WORKINGSET_ID = "org.eclipse.debug.ui.breakpointWorkingSet"; //$NON-NLS-1$

	/**
	 * Memory view identifier (value <code>"org.eclipse.debug.ui.MemoryView"</code>).
	 *
	 * @since 3.2
	 */
	String ID_MEMORY_VIEW = "org.eclipse.debug.ui.MemoryView";  //$NON-NLS-1$

	/**
	 * Memory view's rendering view pane identifier for the rendering view pane
	 * on the left. (value <code>"org.eclipse.debug.ui.MemoryView.RenderingViewPane.1"</code>).
	 *
	 * @since 3.2
	 */
	String ID_RENDERING_VIEW_PANE_1 = "org.eclipse.debug.ui.MemoryView.RenderingViewPane.1"; //$NON-NLS-1$

	/**
	 * Memory view's rendering view pane identifier for the rendering view pane
	 * on the right. (value <code>"org.eclipse.debug.ui.MemoryView.RenderingViewPane.2"</code>).
	 *
	 * @since 3.2
	 */
	String ID_RENDERING_VIEW_PANE_2 = "org.eclipse.debug.ui.MemoryView.RenderingViewPane.2"; //$NON-NLS-1$

	/**
	 * Preference color to indicate that a <code>MemoryByte</code> does not have history.
	 * (value <code> org.eclipse.debug.ui.MemoryHistoryUnknownColor </code>)
	 *
	 * @since 3.2
	 */
	String PREF_MEMORY_HISTORY_UNKNOWN_COLOR = PLUGIN_ID + ".MemoryHistoryUnknownColor"; //$NON-NLS-1$

	/**
	 * 	Preference color to indicate that a <code>MemoryByte</code> has history.
	 * (value <code> org.eclipse.debug.ui.MemoryHistoryKnownColor </code>)
	 *
	 * @since 3.2
	 */
	String PREF_MEMORY_HISTORY_KNOWN_COLOR = PLUGIN_ID + ".MemoryHistoryKnownColor"; //$NON-NLS-1$

	/**
	 * Annotation type identifier for default annotation of the current instruction
	 * pointer (top stack frame in a thread). Value is <code>org.eclipse.debug.ui.currentIP</code>,
	 * identifying a <code>org.eclipse.ui.editors.markerAnnotationSpecification</code>
	 * extension.
	 *
	 * @since 3.2
	 */
	String ANNOTATION_TYPE_INSTRUCTION_POINTER_CURRENT = "org.eclipse.debug.ui.currentIP"; //$NON-NLS-1$

	/**
	 * Annotation type identifier for default annotation of secondary instruction pointers
	 * (non top stack frames). Value is <code>org.eclipse.debug.ui.secondaryIP</code>,
	 * identifying a <code>org.eclipse.ui.editors.markerAnnotationSpecification</code>
	 * extension.
	 *
	 * @since 3.2
	 */
	String ANNOTATION_TYPE_INSTRUCTION_POINTER_SECONDARY = "org.eclipse.debug.ui.secondaryIP"; //$NON-NLS-1$

	/**
	 * Editor Identifier for the "Common Source Not Found" editor.
	 * Value is <code>org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor</code>
	 *
	 * @since 3.2
	 */
	String ID_COMMON_SOURCE_NOT_FOUND_EDITOR="org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor"; //$NON-NLS-1$

	/**
	 * Preference color used to render debug elements that have changed
	 * (value <code>org.eclipse.debug.ui.changedDebugElement</code>).
	 * For example, when a variable value changes, variables are rendered in this
	 * color.
	 *
	 * @since 3.2
	 * @see DebugUITools
	 */
	String PREF_CHANGED_DEBUG_ELEMENT_COLOR= "org.eclipse.debug.ui.changedDebugElement"; //$NON-NLS-1$


	/**
	 * Preference for background color in tree columns when a value changes color.
	 *
	 * @since 3.5
	 * @see DebugUITools
	 */
	String PREF_CHANGED_VALUE_BACKGROUND = PLUGIN_ID + ".PREF_CHANGED_VALUE_BACKGROUND"; //$NON-NLS-1$


	/**
	 * The name of the font to use for the variable text in the variables, registers and expression views.
	 * This font is managed via the workbench font preference page.
	 *
	 * @since 3.5
	 */
	String PREF_VARIABLE_TEXT_FONT= "org.eclipse.debug.ui.VariableTextFont"; //$NON-NLS-1$

	/**
	 * The name of the font to use for detail panes. This font is managed via
	 * the workbench font preference page.
	 *
	 * @since 3.5
	 */
	String PREF_DETAIL_PANE_FONT= "org.eclipse.debug.ui.DetailPaneFont"; //$NON-NLS-1$

	/**
	 * Preference to enable variable view inline on text editors.
	 *
	 * @since 3.16
	 */
	String PREF_SHOW_VARIABLES_INLINE = "org.eclipse.debug.ui.ShowVariableInline"; //$NON-NLS-1$

	/**
	 * Instruction pointer image for editor ruler for the currently executing
	 * instruction in the top stack frame.
	 *
	 * @since 3.2
	 */
	String IMG_OBJS_INSTRUCTION_POINTER_TOP = "IMG_OBJS_INSTRUCTION_POINTER_TOP"; //$NON-NLS-1$

	/**
	 * Instruction pointer image for editor ruler for secondary (non-top) stack frames.
	 *
	 * @since 3.2
	 */
	String IMG_OBJS_INSTRUCTION_POINTER = "IMG_OBJS_INSTRUCTION_POINTER"; //$NON-NLS-1$

	/**
	 * A key for a system property that indicates whether there are toggle
	 * breakpoint factories registered in this installation. This can be used
	 * to trigger the UI to include menus related to breakpoint types.
	 *
	 * @since 3.5
	 */
	String SYS_PROP_BREAKPOINT_TOGGLE_FACTORIES_USED = "org.eclipse.debug.ui.breakpoints.toggleFactoriesUsed"; //$NON-NLS-1$

	/**
	 * Name of the debug context variable which can be used in standard
	 * expressions and command handlers to access the active debug context.
	 *
	 * @since 3.5
	 */
	String DEBUG_CONTEXT_SOURCE_NAME = "debugContext"; //$NON-NLS-1$


	/**
	 * ID for the default column layout for the variables, expressions
	 * and registers views.
	 *
	 * @since 3.8
	 */
	String COLUMN_PRESENTATION_ID_VARIABLE = IDebugUIConstants.PLUGIN_ID + ".VARIALBE_COLUMN_PRESENTATION";  //$NON-NLS-1$

	/**
	 * Default ID for the "Name" column in the Variables views.
	 *
	 * @since 3.8
	 */
	String COLUMN_ID_VARIABLE_NAME = COLUMN_PRESENTATION_ID_VARIABLE + ".COL_VAR_NAME"; //$NON-NLS-1$

	/**
	 * Default ID for the "Declared Type" column in the Variables views.
	 *
	 * @since 3.8
	 */
	String COLUMN_ID_VARIABLE_TYPE = COLUMN_PRESENTATION_ID_VARIABLE + ".COL_VAR_TYPE"; //$NON-NLS-1$

	/**
	 * Default ID for the "Value" column in the Variables views.
	 *
	 * @since 3.8
	 */
	String COLUMN_ID_VARIABLE_VALUE = COLUMN_PRESENTATION_ID_VARIABLE + ".COL_VAR_VALUE"; //$NON-NLS-1$

	/**
	 * Default ID for the "Actual Type" column in the Variables views.
	 *
	 * @since 3.8
	 */
	String COLUMN_ID_VARIABLE_VALUE_TYPE = COLUMN_PRESENTATION_ID_VARIABLE + ".COL_VALUE_TYPE"; //$NON-NLS-1$

}
