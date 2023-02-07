/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.base;

/**
 * @since 3.0
 */
public interface IHelpActivitySupport {

	/**
	 * Checks if href matches an enabled activity. If it does not match any
	 * defined activites, it is assumed to be enabled. If Help role filtering is
	 * disabled, this method always returns <code>true</code>.
	 *
	 * @param href
	 * @return
	 */
	public boolean isEnabled(String href);

	/**
	 * Checks if href is matches an enabled activity. If it does not match any
	 * defined activites, it is assumed to be enabled.
	 *
	 * @param href
	 *            the topic href
	 * @return <code>true</code> if role for this href is enabled,
	 *         <code>false</code> otherwise.
	 */
	public boolean isRoleEnabled(String href);

	/**
	 * Checks whether topic belongs to a TOC that mathes enabled activity.
	 * Enabled children TOCs are searched if linked by also enabled TOCs.
	 * Additionally topic may match description topic of a root TOC.
	 *
	 * @return true if topic belongs to an enabled TOC
	 * @param href
	 * @param locale
	 *            locale for which TOCs are checked
	 */
	public boolean isEnabledTopic(String href, String locale);

	/**
	 * Enables activities with patterns matching the href
	 *
	 * @param href
	 */
	public void enableActivities(String href);

	public boolean isFilteringEnabled();

	public void setFilteringEnabled(boolean enabled);

	public boolean isUserCanToggleFiltering();

	/**
	 * Returns the message to show in the warning dialog when filtering is
	 * toggled off.
	 *
	 * @return the message for the filtering warning dialog
	 */
	public String getShowAllMessage();

	/**
	 * Returns document message for disabled activities.
	 *
	 * @param embedded
	 *            if <code>true</code>, the message will be added to a
	 *            document in the workbench window. Otherwise, it will be shown
	 *            in a separate Help window.
	 * @return the document message with Java script string substituted if
	 *         found.
	 */
	public String getDocumentMessage(boolean embedded);

	/**
	 * @param embedded
	 *            if <code>true</code>, the message will be added to a
	 *            document in the workbench window. Otherwise, it will be shown
	 *            in a separate Help window.
	 * @return <code>true</code> if the document message uses live help and
	 *         requires live help Java script header, or <code>false</code>
	 *         otherwise.
	 */
	public boolean getDocumentMessageUsesLiveHelp(boolean embedded);
}
