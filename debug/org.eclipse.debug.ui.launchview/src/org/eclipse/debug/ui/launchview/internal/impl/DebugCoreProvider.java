/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.ui.launchview.internal.AbstractLaunchObjectProvider;
import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.debug.ui.launchview.services.ILaunchObjectProvider;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = ILaunchObjectProvider.class)
public class DebugCoreProvider extends AbstractLaunchObjectProvider implements ILaunchObjectProvider, ILaunchConfigurationListener, ILaunchesListener2 {

	private final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

	@Activate
	public void createService() {
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(this);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	@Deactivate
	public void destroyService() {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(this);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	@Override
	public Set<ILaunchObject> getLaunchObjects() {
		try {
			return Arrays.stream(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()).map(DebugCoreLaunchObject::new).collect(Collectors.toCollection(TreeSet::new));
		} catch (CoreException e) {
			org.eclipse.core.runtime.ILog.of(this.getClass()).error(LaunchViewMessages.DebugCoreProvider_FailedLookup, e);
			return Collections.emptySet();
		}
	}

	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		fireUpdate();
	}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		fireUpdate();
	}

	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		fireUpdate();
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		fireUpdate(); // process added, thus can terminate
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		fireUpdate();
	}

	@Override
	public void contributeViewMenu(Supplier<Set<ILaunchObject>> selected, MMenu menu) {
		// nothing to contribute for now...
	}

	@Override
	public void contributeContextMenu(Supplier<Set<ILaunchObject>> selected, MMenu menu) {
		MDirectMenuItem delete = MMenuFactory.INSTANCE.createDirectMenuItem();
		delete.setLabel(LaunchViewMessages.DebugCoreProvider_delete);
		delete.setTooltip(LaunchViewMessages.DebugCoreProvider_deleteHint);
		delete.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/icons/delete_exc.png"); //$NON-NLS-1$ //$NON-NLS-2$
		delete.setObject(new Object() {

			@Execute
			public void delete() throws CoreException {
				for (ILaunchObject e : selected.get()) {
					findLaunchConfiguration(e.getType(), e.getId()).delete();
				}

				fireUpdate();
			}

			@CanExecute
			public boolean isEnabled() {
				Set<ILaunchObject> elements = selected.get();
				return !elements.isEmpty() && elements.stream().allMatch(e -> e instanceof DebugCoreLaunchObject && findLaunchConfiguration(e.getType(), e.getId()) != null);
			}
		});

		menu.getChildren().add(MMenuFactory.INSTANCE.createMenuSeparator());
		menu.getChildren().add(delete);
	}

	ILaunchConfiguration findLaunchConfiguration(ILaunchConfigurationType type, String name) {
		try {
			for (ILaunchConfiguration config : manager.getLaunchConfigurations(type)) {
				if (config.getName().equals(name)) {
					return config;
				}
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(LaunchViewMessages.DebugCoreProvider_cannotFetchError, e);
		}
	}

}
