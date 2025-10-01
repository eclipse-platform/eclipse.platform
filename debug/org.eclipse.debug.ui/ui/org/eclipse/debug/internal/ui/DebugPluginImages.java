/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 114664
 *     Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 *     Lars.Vogel <Lars.Vogel@gmail.com> - Bug 430620
 *     Lucas Bullen <lbullen@redhat.com> - Bug 518652
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui;


import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The images provided by the debug plugin.
 */
public class DebugPluginImages {

	/**
	 * The image registry containing <code>Image</code>s and <code>ImageDescriptor</code>s.
	 */
	private static ImageRegistry imageRegistry;

	private static final String ATTR_LAUNCH_CONFIG_TYPE_ICON = "icon"; //$NON-NLS-1$
	private static final String ATTR_LAUNCH_CONFIG_TYPE_ID = "configTypeID"; //$NON-NLS-1$

	private static String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	private final static String CTOOL= ICONS_PATH + "etool16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String ELCL= ICONS_PATH + "elcl16/"; //enabled - size 16x16 //$NON-NLS-1$
	private final static String OBJECT= ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String WIZBAN= ICONS_PATH + "wizban/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OVR= ICONS_PATH + "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	private final static String VIEW= ICONS_PATH + "eview16/"; // views //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Actions
		declareRegistryImage(IDebugUIConstants.IMG_ACT_DEBUG, CTOOL + "debug_exc.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ACT_RUN, CTOOL + "run_exc.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ACT_SYNCED, ELCL + "synced.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_SKIP_BREAKPOINTS, ELCL + "skip_brkp.svg"); //$NON-NLS-1$

		//menus
		declareRegistryImage(IDebugUIConstants.IMG_LCL_CHANGE_VARIABLE_VALUE,
				IInternalDebugUIConstants.IMG_DLCL_CHANGE_VARIABLE_VALUE, ELCL + "changevariablevalue_co.svg"); //$NON-NLS-1$

		declareRegistryImage(IDebugUIConstants.IMG_LCL_CONTENT_ASSIST, ELCL + "metharg_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ELCL_CONTENT_ASSIST, IDebugUIConstants.IMG_DLCL_CONTENT_ASSIST,
				ELCL + "metharg_obj.svg"); //$NON-NLS-1$

		// Local toolbars
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE, IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE,
				ELCL + "toggledetailpane_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_UNDER,
				IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_UNDER, ELCL + "det_pane_under.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_RIGHT,
				IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_RIGHT, ELCL + "det_pane_right.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_HIDE,
				IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_HIDE, ELCL + "det_pane_hide.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_LOCK, IInternalDebugUIConstants.IMG_DLCL_LOCK,
				ELCL + "lock_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_TYPE_NAMES, IInternalDebugUIConstants.IMG_DLCL_TYPE_NAMES,
				ELCL + "tnames_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DISCONNECT, IInternalDebugUIConstants.IMG_DLCL_DISCONNECT,
				ELCL + "disconnect_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_REMOVE_ALL, IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL,
				ELCL + "rem_all_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_REMOVE, IInternalDebugUIConstants.IMG_DLCL_REMOVE,
				ELCL + "rem_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_ADD, IInternalDebugUIConstants.IMG_DLCL_MONITOR_EXPRESSION,
				ELCL + "monitorexpression_tsk.svg"); //$NON-NLS-1$

		// local toolbars
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_AUTO,
				IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_AUTO, ELCL + "det_pane_auto.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DEBUG_VIEW_COMPACT_LAYOUT,
				ELCL + "debug_view_compact.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_SHOW_LOGICAL_STRUCTURE,
				IInternalDebugUIConstants.IMG_DLCL_SHOW_LOGICAL_STRUCTURE, ELCL + "var_cntnt_prvdr.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_COLLAPSE_ALL,
				IInternalDebugUIConstants.IMG_DLCL_COLLAPSE_ALL, ELCL + "collapseall.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TERMINATE, IInternalDebugUIConstants.IMG_DLCL_TERMINATE,
				ELCL + "terminate_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_RUN_TO_LINE,
				IInternalDebugUIConstants.IMG_DLCL_RUN_TO_LINE, ELCL + "runtoline_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_REMOVE_MEMORY,
				IInternalDebugUIConstants.IMG_DLCL_REMOVE_MEMORY, ELCL + "removememory_tsk.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_RESET_MEMORY,
				IInternalDebugUIConstants.IMG_DLCL_RESET_MEMORY, ELCL + "memoryreset_tsk.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_COPY_VIEW_TO_CLIPBOARD,
				IInternalDebugUIConstants.IMG_DLCL_COPY_VIEW_TO_CLIPBOARD, ELCL + "copyviewtoclipboard_tsk.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_PRINT_TOP_VIEW_TAB,
				IInternalDebugUIConstants.IMG_DLCL_PRINT_TOP_VIEW_TAB, ELCL + "printview_tsk.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_HIERARCHICAL, ELCL + "hierarchicalLayout.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_FILTER_CONFIGS,
				IInternalDebugUIConstants.IMG_DLCL_FILTER_CONFIGS, ELCL + "filter_ps.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DUPLICATE_CONFIG,
				IInternalDebugUIConstants.IMG_DLCL_DUPLICATE_CONFIG, ELCL + "copy_edit_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_NEW_CONFIG,
				IInternalDebugUIConstants.IMG_DLCL_NEW_CONFIG, ELCL + "new_con.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DELETE_CONFIG,
				IInternalDebugUIConstants.IMG_DLCL_DELETE_CONFIG, ELCL + "delete_config.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_NEW_PROTO, IInternalDebugUIConstants.IMG_DLCL_NEW_PROTO,
				ELCL + "new_proto.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_LINK_PROTO,
				IInternalDebugUIConstants.IMG_DLCL_LINK_PROTO, ELCL + "link_proto.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_UNLINK_PROTO,
				IInternalDebugUIConstants.IMG_DLCL_UNLINK_PROTO, ELCL + "unlink_proto.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_RESET_PROTO,
				IInternalDebugUIConstants.IMG_DLCL_RESET_PROTO, ELCL + "reset_proto.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_SUSPEND, IInternalDebugUIConstants.IMG_DLCL_SUSPEND,
				ELCL + "suspend_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_RESUME, IInternalDebugUIConstants.IMG_DLCL_RESUME,
				ELCL + "resume_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_STEP_RETURN,
				IInternalDebugUIConstants.IMG_DLCL_STEP_RETURN, ELCL + "stepreturn_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_STEP_OVER, IInternalDebugUIConstants.IMG_DLCL_STEP_OVER,
				ELCL + "stepover_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_STEP_INTO, IInternalDebugUIConstants.IMG_DLCL_STEP_INTO,
				ELCL + "stepinto_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DROP_TO_FRAME, ELCL + "drop_to_frame.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_REMOVE,
				IInternalDebugUIConstants.IMG_DLCL_TERMINATE_AND_REMOVE, ELCL + "terminate_rem_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_ALL,
				IInternalDebugUIConstants.IMG_DLCL_TERMINATE_ALL, ELCL + "terminate_all_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_RELAUNCH,
				IInternalDebugUIConstants.IMG_DLCL_TERMINATE_AND_RELAUNCH, CTOOL + "term_restart.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TOGGLE_STEP_FILTERS,
				IInternalDebugUIConstants.IMG_DLCL_TOGGLE_STEP_FILTERS, ELCL + "stepbystep_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_STANDARD_OUT, ELCL + "writeout_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_STANDARD_ERR, ELCL + "writeerr_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_NEXT_THREAD,
				IInternalDebugUIConstants.IMG_DLCL_NEXT_THREAD, ELCL + "next_thread_nav.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_PREVIOUS_THREAD,
				IInternalDebugUIConstants.IMG_DLCL_PREVIOUS_THREAD, ELCL + "prev_thread_nav.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_RESTART, IInternalDebugUIConstants.IMG_DLCL_RESTART,
				ELCL + "restart_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_EXPORT_CONFIG,
				IInternalDebugUIConstants.IMG_DLCL_EXPORT_CONFIG, ELCL + "export_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_IMPORT_CONFIG,
				IInternalDebugUIConstants.IMG_DLCL_IMPORT_CONFIG, ELCL + "import_wiz.svg"); //$NON-NLS-1$

		//Object
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_DEBUG, OBJECT + "ldebug_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_RUN, OBJECT + "lrun_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_RUN_TERMINATED, OBJECT + "terminatedlaunch_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET, OBJECT + "debugt_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_SUSPENDED, OBJECT + "debugts_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_TERMINATED, OBJECT + "debugtt_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_RUNNING, OBJECT + "thread_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_SUSPENDED, OBJECT + "threads_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_TERMINATED, OBJECT + "threadt_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_STACKFRAME, OBJECT + "stckframe_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_STACKFRAME_RUNNING, OBJECT + "stckframe_running_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_VARIABLE, OBJECT + "genericvariable_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_REGISTER, OBJECT + "genericregister_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_REGISTER_GROUP, OBJECT + "genericreggroup_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT, OBJECT + "brkp_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED, OBJECT + "brkpd_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_GROUP, OBJECT + "brkp_grp.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_GROUP_DISABLED, OBJECT + "brkp_grp_disabled.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_WATCHPOINT, OBJECT + "readwrite_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_WATCHPOINT_DISABLED, OBJECT + "readwrite_obj_disabled.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_ACCESS_WATCHPOINT, OBJECT + "read_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_ACCESS_WATCHPOINT_DISABLED, OBJECT + "read_obj_disabled.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT, OBJECT + "write_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT_DISABLED,
				OBJECT + "write_obj_disabled.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_OS_PROCESS, OBJECT + "osprc_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_OS_PROCESS_TERMINATED, OBJECT + "osprct_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_EXPRESSION, OBJECT + "expression_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER_TOP, OBJECT + "inst_ptr_top.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER, OBJECT + "inst_ptr.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_ARRAY_PARTITION, OBJECT + "arraypartition_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_ENV_VAR, OBJECT + "envvar_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY_CHANGED, OBJECT + "memorychanged_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY, OBJECT + "memory_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_BREAKPOINT_TYPE, OBJECT + "brkp_type.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_LAUNCH_GROUP, OBJECT + "lgroup_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_CHECK, OBJECT + "check.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_UNCHECK, OBJECT + "uncheck.svg"); //$NON-NLS-1$

		// tabs
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_COMMON_TAB, OBJECT + "common_tab.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_REFRESH_TAB, OBJECT + "refresh_tab.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_PERSPECTIVE_TAB, OBJECT + "persp_tab.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_ENVIRONMENT, OBJECT + "environment_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_PROTO_TAB, OBJECT + "proto_tab.svg"); //$NON-NLS-1$

		// Views
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_BREAKPOINTS, VIEW + "breakpoint_view.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_EXPRESSIONS, VIEW + "watchlist_view.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_LAUNCHES, VIEW + "debug_view.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_VARIABLES, VIEW + "variable_view.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_CVIEW_MEMORY_VIEW, VIEW + "memory_view.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_CVIEW_MODULES_VIEW, VIEW + "module_view.svg"); //$NON-NLS-1$

		// Perspectives
		declareRegistryImage(IDebugUIConstants.IMG_PERSPECTIVE_DEBUG, VIEW + "debug_persp.svg"); //$NON-NLS-1$

		//Wizard Banners
		declareRegistryImage(IDebugUIConstants.IMG_WIZBAN_DEBUG, WIZBAN + "debug_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_WIZBAN_RUN, WIZBAN + "run_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_WIZBAN_IMPORT_BREAKPOINTS, WIZBAN + "import_brkpts_wizban.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_WIZBAN_EXPORT_BREAKPOINTS, WIZBAN + "export_brkpts_wizban.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_WIZBAN_IMPORT_CONFIGS, WIZBAN + "import_config_wizban.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_WIZBAN_EXPORT_CONFIGS, WIZBAN + "export_config_wizban.svg"); //$NON-NLS-1$

		// Overlays
		declareRegistryImage(IDebugUIConstants.IMG_OVR_ERROR, OVR + "error.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OVR_TRANSPARENT, OVR + "transparent.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OVR_SKIP_BREAKPOINT, OVR + "skip_breakpoint_ov.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OVR_SHOW_LOGICAL_STRUCTURE, OVR + "var_cntnt_prvdr_ov.svg"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OVR_PROTOTYPE, OVR + "prototype.svg"); //$NON-NLS-1$

		//source location
		declareRegistryImage(IInternalDebugUIConstants.IMG_SRC_LOOKUP_MENU, ELCL + "edtsrclkup_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_SRC_LOOKUP_MENU_ELCL,
				IInternalDebugUIConstants.IMG_SRC_LOOKUP_MENU_DLCL, ELCL + "edtsrclkup_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_SRC_LOOKUP_TAB, ELCL + "edtsrclkup_co.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ADD_SRC_LOC_WIZ, WIZBAN + "addsrcloc_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_EDIT_SRC_LOC_WIZ, WIZBAN + "edtsrclkup_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ADD_SRC_DIR_WIZ, WIZBAN + "adddir_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_EDIT_SRC_DIR_WIZ, WIZBAN + "editdir_wiz.svg"); //$NON-NLS-1$

		// launch configuration types
		//try to get the images from the config types themselves, cache those that could not be found
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPE_IMAGES);
		IConfigurationElement[] configElements= extensionPoint.getConfigurationElements();
		for (IConfigurationElement configElement : configElements) {
			ImageDescriptor descriptor = DebugUIPlugin.getImageDescriptor(configElement, ATTR_LAUNCH_CONFIG_TYPE_ICON);
			if (descriptor == null) {
				descriptor = ImageDescriptor.getMissingImageDescriptor();
			}
			String configTypeID = configElement.getAttribute(ATTR_LAUNCH_CONFIG_TYPE_ID);
			if (configTypeID == null) {
				// bug 12652
				configTypeID = configElement.getAttribute(IConfigurationElementConstants.TYPE);
			}
			imageRegistry.put(configTypeID, descriptor);
		}
	}

	/**
	 * Declare an Image in the registry table.
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plugin class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
		declareRegistryImage(key, null, path);
	}

	private final static void declareRegistryImage(String key, String disabledKey, String path) {
		Bundle bundle = FrameworkUtil.getBundle(DebugPluginImages.class);
		ImageDescriptor imageDescriptor;
		if (bundle == null) {
			imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
		} else {
			imageDescriptor = ImageDescriptor.createFromURLSupplier(true,
					() -> FileLocator.find(bundle, IPath.fromOSString(path), null));
		}
		imageRegistry.put(key, imageDescriptor);
		if (disabledKey != null) {
			ImageDescriptor disabledImageDescriptor = ImageDescriptor.createWithFlags(imageDescriptor,
					SWT.IMAGE_DISABLE);
			imageRegistry.put(disabledKey, disabledImageDescriptor);
		}
	}

	/**
	 * Returns the ImageRegistry.
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			initializeImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 * Returns whether the image registry has been initialized.
	 *
	 * @return whether the image registry has been initialized
	 */
	public synchronized static boolean isInitialized() {
		return imageRegistry != null;
	}

	/**
	 *	Initialize the image registry by declaring all of the required
	 *	graphics. This involves creating JFace image descriptors describing
	 *	how to create/find the image should it be needed.
	 *	The image is not actually allocated until requested.
	 *
	 * 	Prefix conventions
	 *		Wizard Banners			WIZBAN_
	 *		Preference Banners		PREF_BAN_
	 *		Property Page Banners	PROPBAN_
	 *		Color toolbar			CTOOL_
	 *		Enable toolbar			ETOOL_
	 *		Disable toolbar			DTOOL_
	 *		Local enabled toolbar	ELCL_
	 *		Local Disable toolbar	DLCL_
	 *		Object large			OBJL_
	 *		Object small			OBJS_
	 *		View 					VIEW_
	 *		Product images			PROD_
	 *		Misc images				MISC_
	 *
	 *	Where are the images?
	 *		The images (typically gifs) are found in the same location as this plugin class.
	 *		This may mean the same package directory as the package holding this class.
	 *		The images are declared using this.getClass() to ensure they are looked up via
	 *		this plugin class.
	 * @see org.eclipse.jface.resource.ImageRegistry
	 */
	public synchronized static ImageRegistry initializeImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry(DebugUIPlugin.getStandardDisplay());
			declareImages();
		}
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image</code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor</code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getImageRegistry().getDescriptor(key);
	}
}


