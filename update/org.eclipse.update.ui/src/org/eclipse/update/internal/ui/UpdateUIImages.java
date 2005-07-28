/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui;

import java.net.*;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.*;

/**
 * Bundle of all images used by the PDE plugin.
 */
public class UpdateUIImages {

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	/**
	 * Set of predefined Image Descriptors.
	 */
	
	private static final String PATH_OBJ= ICONS_PATH+"obj16/"; //$NON-NLS-1$
	private static final String PATH_VIEW = ICONS_PATH+"eview16/"; //$NON-NLS-1$
	private static final String PATH_LCL= ICONS_PATH+"elcl16/"; //$NON-NLS-1$
	private static final String PATH_LCL_DISABLED= ICONS_PATH+"dlcl16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr16/"; //$NON-NLS-1$
	private static final String PATH_WIZBAN = ICONS_PATH + "wizban/"; //$NON-NLS-1$
	public static final String PATH_FORMS = ICONS_PATH + "forms/"; //$NON-NLS-1$


	/**
	 * Frequently used images
	 */

	/**
	 * OBJ16
	 */
	public static final ImageDescriptor DESC_APP_OBJ = create(PATH_OBJ, "app_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BFOLDER_OBJ = create(PATH_OBJ, "bfolder_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CATEGORY_OBJ = create(PATH_OBJ, "category_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CONFIG_OBJ = create(PATH_OBJ, "config_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_OBJ = create(PATH_OBJ, "feature_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EFIX_OBJ = create(PATH_OBJ, "efix2_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HISTORY_OBJ = create(PATH_OBJ, "history_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LSITE_OBJ = create(PATH_OBJ, "lsite_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PSITE_OBJ = create(PATH_OBJ, "psite_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ESITE_OBJ = create(PATH_OBJ, "esite_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SITE_OBJ = create(PATH_OBJ, "site_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WEB_SITE_OBJ = create(PATH_OBJ, "web_bkmrk_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UNCONF_FEATURE_OBJ = create(PATH_OBJ, "unconf_feature_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UPDATES_OBJ = create(PATH_OBJ, "updates_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NOTINST_FEATURE_OBJ = create(PATH_OBJ, "notinstalled_feature_obj.gif");	 //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERR_ST_OBJ = create(PATH_OBJ, "error_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OK_ST_OBJ = create(PATH_OBJ, "ok_st_obj.gif"); //$NON-NLS-1$

	
	/**
	 * OVR16
	 */
	public static final ImageDescriptor DESC_LINKED_CO   = create(PATH_OVR, "linked_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UPDATED_CO   = create(PATH_OVR, "updated_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INSTALLABLE_CO = create(PATH_OVR, "installable_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CURRENT_CO = create(PATH_OVR, "current_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_CO = create(PATH_OVR, "error_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WARNING_CO = create(PATH_OVR, "warning_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_MOD_CO = create(PATH_OVR, "mod_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ADD_CO = create(PATH_OVR, "add_stat.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEL_CO = create(PATH_OVR, "del_stat.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UNCONF_CO = create(PATH_OVR, "unconfigured_co.gif"); //$NON-NLS-1$

	/**
	 * VIEW16
	 */

	public static final ImageDescriptor DESC_CONFIGS_VIEW = create(PATH_VIEW, "configs.gif"); //$NON-NLS-1$

	/**
	 * LCL
	 */

//	public static final ImageDescriptor DESC_SHOW_UNCONF = create(PATH_LCL, "show_unconf.gif"); //$NON-NLS-1$
//	public static final ImageDescriptor DESC_SHOW_UNCONF_H = create(PATH_LCL_HOVER, "show_unconf.gif"); //$NON-NLS-1$
//	public static final ImageDescriptor DESC_SHOW_UNCONF_D = create(PATH_LCL_DISABLED, "show_unconf.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COLLAPSE_ALL = create(PATH_LCL, "collapseall.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SHOW_HIERARCHY = create(PATH_LCL, "hierarchicalLayout.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SHOW_HIERARCHY_D = create(PATH_LCL_DISABLED, "hierarchicalLayout.gif"); //$NON-NLS-1$

	/**
	 * WIZ
	 */
//	public static final ImageDescriptor DESC_INSTALL_WIZ  = create(PATH_WIZBAN, "install_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UPDATE_WIZ  = create(PATH_WIZBAN, "update_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CONFIG_WIZ  = create(PATH_WIZBAN, "config_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_UNINSTALL_WIZ  = create(PATH_WIZBAN, "uninstall_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INSTALL_BANNER  = create(PATH_WIZBAN, "def_wizban.jpg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROVIDER = create(PATH_FORMS, "def_provider.jpg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ITEM = create(PATH_FORMS, "topic.gif"); //$NON-NLS-1$

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeImageURL(prefix, name));
	}


	private static URL makeImageURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return Platform.find(UpdateUI.getDefault().getBundle(), new Path(path));
	}

}
