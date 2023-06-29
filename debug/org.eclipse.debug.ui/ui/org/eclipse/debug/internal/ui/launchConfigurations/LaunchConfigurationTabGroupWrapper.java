/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Doug <doug.satchwell@btinternet.com> - Bug 243053
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.LaunchConfigurationTabExtension;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;

/**
 * This class is used to wrap a contributed <code>ILaunchConfigurationTabGroup</code> with any contributed tabs
 * for that group (from a <code>launchConfigurationTabs</code> extension point).
 *
 * @since 3.3
 */
public class LaunchConfigurationTabGroupWrapper implements ILaunchConfigurationTabGroup {

	/**
	 * Collects all tabs and contributed tabs in the correct ordering
	 *
	 * @since 3.5
	 */
	static class TabCollector implements Iterator<ILaunchConfigurationTab> {

		private HashSet<String> idSet = null;
		private ArrayList<ILaunchConfigurationTab> tabList = null;
		private ArrayList<LaunchConfigurationTabExtension> extList = null;

		public TabCollector(List<ILaunchConfigurationTab> tabs, List<LaunchConfigurationTabExtension> exts) {
			tabList = new ArrayList<>(tabs);
			extList = new ArrayList<>(exts);
			idSet = new HashSet<>(tabList.size() + extList.size());
		}

		/**
		 * Get the tab for any extension that is 'relative' to any of the previously returned tabs
		 *
		 * @return the next tab extension tab
		 */
		private ILaunchConfigurationTab nextExtensionTab() {
			for (Iterator<LaunchConfigurationTabExtension> iterator = extList.iterator(); iterator.hasNext();) {
				LaunchConfigurationTabExtension launchConfigurationTabExtension = iterator.next();
				String relativeTab = launchConfigurationTabExtension.getRelativeTabId();
				if (relativeTab == null || idSet.contains(relativeTab)) {
					iterator.remove();
					return launchConfigurationTabExtension.getTab();
				}
			}
			return null;
		}

		@Override
		public boolean hasNext() {
			return extList.size() > 0 || tabList.size() > 0;
		}

		@Override
		public ILaunchConfigurationTab next() {
			ILaunchConfigurationTab nextTab = nextExtensionTab();
			if (nextTab == null) {
				if (tabList.size() > 0) {
					nextTab = tabList.remove(0);
				}
				else {
					LaunchConfigurationTabExtension launchConfigurationTabExtension = extList.remove(0);
					nextTab = launchConfigurationTabExtension.getTab();
				}
			}
			if (nextTab instanceof AbstractLaunchConfigurationTab) {
				String id = ((AbstractLaunchConfigurationTab)nextTab).getId();
				if (id != null) {
					idSet.add(id);
				}
			}
			return nextTab;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private ILaunchConfigurationTabGroup fGroup = null;
	private String fGroupId = null;
	/**
	 * listing of tab extensions that we have to create
	 */
	private List<ILaunchConfigurationTab> fTabs = null;
	private String fMode = null;
	private ILaunchConfiguration fConfig = null;

	/**
	 * Constructor
	 * @param group the existing group to wrapper
	 * @param groupId the string id of the associated tab group
	 * @param config the launch configuration this tab group is opened on
	 */
	public LaunchConfigurationTabGroupWrapper(ILaunchConfigurationTabGroup group, String groupId, ILaunchConfiguration config) {
		fGroup = group;
		fGroupId = groupId;
		fConfig = config;
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		if(fGroup != null) {
			fGroup.createTabs(dialog, mode);
			fMode = mode;
		}
	}

	@Override
	public void dispose() {
		fGroup.dispose();
		if(fTabs != null) {
			List<ILaunchConfigurationTab> tabs = Arrays.asList(fGroup.getTabs());
			ILaunchConfigurationTab tab = null;
			for(int i = 0; i < fTabs.size(); i++) {
				tab = fTabs.get(i);
				if(!tabs.contains(tab)) {
					tab.dispose();
				}
			}
			fTabs.clear();
		}
	}

	@Override
	public ILaunchConfigurationTab[] getTabs() {
		if(fTabs == null) {
			try {
				fTabs = new ArrayList<>();
				LaunchConfigurationTabExtension[] ext = LaunchConfigurationPresentationManager.getDefault().getTabExtensions(fGroupId, fConfig, fMode);
				//if there are no extensions bypass and do a raw copy into
				if(ext.length > 0) {
					TabCollector collector = new TabCollector(Arrays.asList(fGroup.getTabs()), Arrays.asList(ext));
					while(collector.hasNext()) {
						ILaunchConfigurationTab next = collector.next();
						if (next != null) {
							fTabs.add(next);
						}
					}
				}
				else {
					ILaunchConfigurationTab[] tabs = fGroup.getTabs();
					Collections.addAll(fTabs, tabs);
				}
			}
			catch (CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return fTabs.toArray(new ILaunchConfigurationTab[fTabs.size()]);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		fGroup.initializeFrom(configuration);
		if(fTabs != null) {
			List<ILaunchConfigurationTab> tabs = Arrays.asList(fGroup.getTabs());
			ILaunchConfigurationTab tab = null;
			for(int i = 0; i < fTabs.size(); i++) {
				tab = fTabs.get(i);
				if(!tabs.contains(tab)) {
					tab.initializeFrom(configuration);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void launched(ILaunch launch) {
		if(fGroup != null) {
			fGroup.launched(launch);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fGroup.performApply(configuration);
		if(fTabs != null) {
			List<ILaunchConfigurationTab> tabs = Arrays.asList(fGroup.getTabs());
			ILaunchConfigurationTab tab = null;
			for(int i = 0; i < fTabs.size(); i++) {
				tab = fTabs.get(i);
				if(!tabs.contains(tab)) {
					tab.performApply(configuration);
				}
			}
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		fGroup.setDefaults(configuration);
		if(fTabs != null) {
			List<ILaunchConfigurationTab> tabs = Arrays.asList(fGroup.getTabs());
			ILaunchConfigurationTab tab = null;
			for(int i = 0; i < fTabs.size(); i++) {
				tab = fTabs.get(i);
				if(!tabs.contains(tab)) {
					tab.setDefaults(configuration);
				}
			}
		}
	}

}
