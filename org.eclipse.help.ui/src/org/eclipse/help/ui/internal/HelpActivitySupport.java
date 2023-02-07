/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.help.ui.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpActivitySupport;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IIdentifier;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Wrapper for eclipse ui activity support
 */
public class HelpActivitySupport implements IHelpActivitySupport {
	private static final String PREF_KEY_SHOW_DISABLED_ACTIVITIES = "showDisabledActivityTopics"; //$NON-NLS-1$
	private static final String SHOW_DISABLED_ACTIVITIES_NEVER = "never"; //$NON-NLS-1$
	private static final String SHOW_DISABLED_ACTIVITIES_OFF = "off"; //$NON-NLS-1$
	private static final String SHOW_DISABLED_ACTIVITIES_ON = "on"; //$NON-NLS-1$
//	private static final String SHOW_DISABLED_ACTIVITIES_ALWAYS = "always"; //$NON-NLS-1$

	private IWorkbenchActivitySupport activitySupport;
	private boolean userCanToggleFiltering;
	private boolean filteringEnabled;
	private ActivityDescriptor activityDescriptor;

	static class ActivityDescriptor {
		private IConfigurationElement config;
		private String documentMessage;
		private boolean needsLiveHelp;

		public ActivityDescriptor() {
			load();
		}

		private void load() {
			IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.help.base.activitySupport");//$NON-NLS-1$
			if (elements.length==1 && elements[0].getName().equals("support")) //$NON-NLS-1$
				config = elements[0];
			else if (elements.length>0) {
				IProduct product = Platform.getProduct();
				if (product==null) return;
				String productId = product.getId();
				for (int i=0; i<elements.length; i++) {
					IConfigurationElement element = elements[i];
					if (element.getAttribute("productId").equals(productId)) { //$NON-NLS-1$
						config = element;
						break;
					}
				}
			}
		}
		private IConfigurationElement getChild(String name) {
			IConfigurationElement [] children = config.getChildren(name);
			return children.length==1?children[0]:null;
		}
		public String getShowAllMessage() {
			if (config==null)
				return null;
			IConfigurationElement child = getChild("showAllMessage"); //$NON-NLS-1$
			if (child!=null)
				return child.getValue();
			return null;
		}
		public boolean needsLiveHelp(boolean embedded) {
			getDocumentMessage(embedded);
			return needsLiveHelp;
		}
		public String getDocumentMessage(boolean embedded) {
			if (config!=null && documentMessage==null) {
				IConfigurationElement child = getChild("documentMessage"); //$NON-NLS-1$
				if (child!=null) {
					String value = child.getValue();
					String pluginId = child.getAttribute("pluginId"); //$NON-NLS-1$
					String className = child.getAttribute("class"); //$NON-NLS-1$
					int loc = value.indexOf("ACTIVITY_EDITOR"); //$NON-NLS-1$
					if (loc!= -1 && className!=null) {
						needsLiveHelp=true;
						StringBuilder buffer = new StringBuilder();
						buffer.append(value.substring(0, loc));
						buffer.append(getActivityEditorValue(pluginId, className, embedded));
						buffer.append(value.substring(loc+15));
						documentMessage = buffer.toString();
					}
					else
						documentMessage = value;
				}
			}
			return documentMessage;
		}
		private String getActivityEditorValue(String pluginId, String className, boolean embedded) {
			String evalue = embedded?"narrow":""; //$NON-NLS-1$ //$NON-NLS-2$
			return "javascript:liveAction(\""+pluginId+"\", \""+className+"\",\""+evalue+"\")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}


	public HelpActivitySupport(IWorkbench workbench) {
		activitySupport = workbench.getActivitySupport();
		activityDescriptor = new ActivityDescriptor();

		String showDisabledActivities =
			Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, PREF_KEY_SHOW_DISABLED_ACTIVITIES, "", null); //$NON-NLS-1$
		userCanToggleFiltering = SHOW_DISABLED_ACTIVITIES_OFF
				.equalsIgnoreCase(showDisabledActivities)
				|| SHOW_DISABLED_ACTIVITIES_ON
						.equalsIgnoreCase(showDisabledActivities);
		userCanToggleFiltering = userCanToggleFiltering
				&& isWorkbenchFiltering();

		filteringEnabled = SHOW_DISABLED_ACTIVITIES_OFF
				.equalsIgnoreCase(showDisabledActivities)
				|| SHOW_DISABLED_ACTIVITIES_NEVER
						.equalsIgnoreCase(showDisabledActivities);
		filteringEnabled = filteringEnabled && isWorkbenchFiltering();
	}

	@Override
	public boolean isFilteringEnabled() {
		return filteringEnabled;
	}

	@Override
	public void setFilteringEnabled(boolean enabled) {
		if (userCanToggleFiltering) {
			filteringEnabled = enabled;
			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
			if (enabled) {
				prefs.put(PREF_KEY_SHOW_DISABLED_ACTIVITIES,
						SHOW_DISABLED_ACTIVITIES_OFF);
			} else {
				prefs.put(PREF_KEY_SHOW_DISABLED_ACTIVITIES,
						SHOW_DISABLED_ACTIVITIES_ON);
			}
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
			}
		}
	}

	@Override
	public boolean isUserCanToggleFiltering() {
		return userCanToggleFiltering;
	}
	@Override
	public boolean isEnabled(String href) {
		if (!isFilteringEnabled()) {
			return true;
		}
		return isRoleEnabled(href);
	}

	@Override
	public boolean isRoleEnabled(String href) {
		if (href.startsWith("/")) { //$NON-NLS-1$
			href = href.substring(1);
		}

		return activitySupport.getActivityManager().getIdentifier(href)
				.isEnabled();
	}

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
	@Override
	public boolean isEnabledTopic(String href, String locale) {
		if (href == null) {
			return false;
		}
		if (!isFilteringEnabled()) {
			return true;
		}
		int ix = href.indexOf("?resultof="); //$NON-NLS-1$
		if (ix >= 0) {
			href = href.substring(0, ix);
		}
		// Find out if description topic for enabled top level TOCs matches the
		// topic
		Toc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		for (int t = 0; t < tocs.length; t++) {
			String descriptionHref = tocs[t].getTopic(null).getHref();
			if (descriptionHref != null
					&& descriptionHref.length() > 0
					&& descriptionHref.equals(href)
					&& HelpBasePlugin.getActivitySupport().isEnabled(
							tocs[t].getHref())) {
				return true;
			}
		}
		// Find out if any contributed toc that is enabled contains the topic
		return isInTocSubtree(href, tocs);
	}
	/**
	 * @param href
	 *            href of a topic
	 * @param tocList
	 *            List of IToc
	 * @return true if given topic belongs to one of enabled ITocs
	 */
	private boolean isInTocSubtree(String href, Toc[] tocList) {
		for (int i=0;i<tocList.length;++i) {
			Toc toc = tocList[i];
			if (!HelpBasePlugin.getActivitySupport().isEnabled(toc.getHref())) {
				// TOC is not enabled, check other TOCs
				continue;
			}
			// Check topics in navigation
			if (toc.getTopic(href) != null) {
				return true;
			}
			// Check extra docs
			String[] extraDocs = toc.getTocContribution().getExtraDocuments();
			for (int j=0;j<extraDocs.length;++j) {
				if (extraDocs[j].equals(href)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void enableActivities(String href) {
		if (href.startsWith("/")) { //$NON-NLS-1$
			href = href.substring(1);
		}

		IIdentifier identifier = activitySupport.getActivityManager()
				.getIdentifier(href);
		Set<String> activitityIds = identifier.getActivityIds();
		if (activitityIds.isEmpty()) { // if there are no activities that match
			// this identifier, do nothing.
			return;
		}

		Set<String> enabledIds = new HashSet<>(activitySupport.getActivityManager()
				.getEnabledActivityIds());
		enabledIds.addAll(activitityIds);
		activitySupport.setEnabledActivityIds(enabledIds);
	}

	/**
	 * @return whether the UI is set up to filter contributions (has defined
	 *         activity categories).
	 */
	private static boolean isWorkbenchFiltering() {
		return !PlatformUI.getWorkbench().getActivitySupport()
				.getActivityManager().getDefinedActivityIds().isEmpty();
	}

	@Override
	public String getShowAllMessage() {
		return activityDescriptor.getShowAllMessage();
	}

	@Override
	public String getDocumentMessage(boolean embedded) {
		return activityDescriptor.getDocumentMessage(embedded);
	}

	@Override
	public boolean getDocumentMessageUsesLiveHelp(boolean embedded) {
		return activityDescriptor.needsLiveHelp(embedded);
	}
}
