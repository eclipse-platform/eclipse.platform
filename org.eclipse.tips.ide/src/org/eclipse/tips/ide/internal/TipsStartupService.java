/*******************************************************************************
 * Copyright (c) 2018, 2020 Remain Software and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.tips.ide.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.TipManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Early startup to run the TipManager in the IDE.
 *
 */
@Component(property = EventConstants.EVENT_TOPIC + '=' + UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
@SuppressWarnings("restriction")
public class TipsStartupService implements EventHandler {

	private static final String DBLQUOTE = "\""; //$NON-NLS-1$
	private static final String EQ = "="; //$NON-NLS-1$
	private static final String SLASH = "/"; //$NON-NLS-1$
	private static final String LT = "<"; //$NON-NLS-1$
	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String GT = ">"; //$NON-NLS-1$
	private static final String SPACE = " "; //$NON-NLS-1$

	@Override
	public void handleEvent(Event event) {
		if (TipsPreferences.getStartupBehavior() != TipManager.START_DISABLE) {
			Job job = new Job(Messages.Startup_1) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					start();
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return TipsStartupService.class.equals(family);
				}
			};
			job.setSystem(true);
			job.setUser(false);
			job.schedule();
		}
	}

	public void start() {
		loadProviders();
		openManager();
	}

	public static void loadProviders() {
		Set<String> disabledProviders = new HashSet<>(TipsPreferences.getDisabledProviderIds());
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.tips.core.tips"); //$NON-NLS-1$
		for (IConfigurationElement element : elements) {
			if (element.getName().equals("provider")) { //$NON-NLS-1$
				try {
					TipProvider provider = (TipProvider) element.createExecutableExtension("class"); //$NON-NLS-1$
					provider.setExpression(getExpression(element));
					String providerId = provider.getID();
					boolean isDisabled = disabledProviders.contains(providerId);
					if (!isDisabled) {
						IDETipManager.getInstance().register(provider);
					}
				} catch (CoreException e) {
					log(e);
				}
			}
		}
	}

	/**
	 * @return the core expression
	 */
	private static String getExpression(IConfigurationElement element) {
		IConfigurationElement[] enablements = element.getChildren("enablement"); //$NON-NLS-1$
		if (enablements.length == 0) {
			return null;
		}
		IConfigurationElement enablement = enablements[0];
		String result = getXML(enablement.getChildren());
		return result;
	}

	private static String getXML(IConfigurationElement[] children) {
		String result = EMPTY;
		for (IConfigurationElement element : children) {
			IConfigurationElement[] myChildren = element.getChildren();
			result += LT + element.getName() + SPACE + getXMLAttributes(element) + GT;
			if (myChildren.length > 0) {
				result += getXML(myChildren);
			} else {
				String value = element.getValue();
				result += value == null ? EMPTY : value;
			}
			result += LT + SLASH + element.getName() + GT;
		}
		return result;
	}

	private static String getXMLAttributes(IConfigurationElement element) {
		String result = EMPTY;
		for (String name : element.getAttributeNames()) {
			result += name;
			result += EQ + DBLQUOTE;
			result += element.getAttribute(name);
			result += DBLQUOTE + SPACE;
		}
		return result;
	}

	private static void openManager() {
		if (IDETipManager.getInstance().hasContent()) {
			getOpenUIJob().schedule();
		} else {
			getWaitJob().schedule();
		}
	}

	private static Job getWaitJob() {
		Job waitJob = new Job(Messages.Startup_18) {

			@Override
			protected IStatus run(IProgressMonitor pMonitor) {
				int attempts = 3;
				SubMonitor monitor = SubMonitor.convert(pMonitor, attempts);
				for (int i = 0; i < attempts; i++) {
					monitor.setTaskName(Messages.Startup_19 + i);
					if (openOrSleep(monitor)) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						} else {
							monitor.done();
							return Status.OK_STATUS;
						}
					}
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}

			private boolean openOrSleep(SubMonitor pMonitor) {
				if (IDETipManager.getInstance().hasContent()) {
					getOpenUIJob().schedule();
					return true;
				}
				if (sleep(1000)) {
					pMonitor.setCanceled(true);
					return true;
				}
				return false;
			}

			private boolean sleep(int millis) {
				try {
					Thread.sleep(millis);
					return false;
				} catch (InterruptedException e) {
					return true;
				}
			}
		};
		waitJob.setSystem(true);
		return waitJob;
	}

	private static UIJob getOpenUIJob() {
		UIJob uiJob = new UIJob(PlatformUI.getWorkbench().getDisplay(), Messages.Startup_20) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IDETipManager.getInstance().open(true);
				return Status.OK_STATUS;
			}
		};
		return uiJob;
	}

	private static void log(CoreException e) {
		Bundle bundle = FrameworkUtil.getBundle(TipsStartupService.class);
		Platform.getLog(bundle).error(e.getMessage(), e);
	}
}