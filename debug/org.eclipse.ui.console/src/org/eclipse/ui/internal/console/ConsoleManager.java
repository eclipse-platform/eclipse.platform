/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Andrey Loskutov <loskutov@gmx.de> - bug 489546
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * The singleton console manager.
 *
 * @since 3.0
 */
public class ConsoleManager implements IConsoleManager {
	public static final String CONSOLE_JOB_FAMILY = "CONSOLE_JOB_FAMILY"; //$NON-NLS-1$

	/**
	 * Console listeners
	 */
	private final ListenerList<IConsoleListener> fListeners;

	/**
	 * List of registered consoles
	 */
	private final List<IConsole> fConsoles;


	// change notification constants
	private final static int ADDED = 1;
	private final static int REMOVED = 2;

	private List<PatternMatchListenerExtension> fPatternMatchListeners;

	private List<ConsolePageParticipantExtension> fPageParticipants;

	private List<ConsoleFactoryExtension> fConsoleFactoryExtensions;

	private final List<ConsoleView> fConsoleViews;

	/** Used to trigger redrawing of console pages when links changed */
	private final RedrawJob redrawConsoleJob;

	/** Used to show console view in active window */
	private final ShowConsoleViewJob showConsoleJob;

	/** Show console change indication in all views if console is not visible */
	private final WarnAboutContentChangedJob warnAboutContentChangeJob;

	public ConsoleManager() {
		fListeners = new ListenerList<>();
		fConsoles = new ArrayList<>(10);
		fConsoleViews = new ArrayList<>();
		redrawConsoleJob = new RedrawJob();
		showConsoleJob = new ShowConsoleViewJob();
		warnAboutContentChangeJob = new WarnAboutContentChangedJob();
	}

	private class RedrawJob extends AbstractConsoleJob {

		public RedrawJob() {
			super("Schedule console redraw"); //$NON-NLS-1$
			setSystem(true);
		}

		@Override
		protected void workWith(IConsole console, IProgressMonitor monitor) {
			synchronized (fConsoleViews) {
				for (ConsoleView view : fConsoleViews) {
					if (console.equals(view.getConsole())) {
						IPage currentPage = view.getCurrentPage();
						if (currentPage == null) {
							continue;
						}
						Control control = currentPage.getControl();
						if (control != null && !control.isDisposed()) {
							control.redraw();
						}
					}
					if (monitor.isCanceled()) {
						return;
					}
				}
			}
		}
	}

	/**
	 * Notifies a console listener of additions or removals
	 */
	class ConsoleNotifier implements ISafeRunnable {

		private IConsoleListener fListener;
		private int fType;
		private IConsole[] fChanged;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.INTERNAL_ERROR, ConsoleMessages.ConsoleManager_0, exception);
			ConsolePlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.consolesAdded(fChanged);
					break;
				case REMOVED:
					fListener.consolesRemoved(fChanged);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies the given listener of the adds/removes
		 *
		 * @param consoles the consoles that changed
		 * @param update the type of change
		 */
		public void notify(IConsole[] consoles, int update) {
			fChanged = consoles;
			fType = update;
			for (IConsoleListener iConsoleListener : fListeners) {
				fListener = iConsoleListener;
				SafeRunner.run(this);
			}
			fChanged = null;
			fListener = null;
		}
	}

	public void registerConsoleView(ConsoleView view) {
		synchronized (fConsoleViews) {
			fConsoleViews.add(view);
		}
	}
	public void unregisterConsoleView(ConsoleView view) {
		synchronized (fConsoleViews) {
			fConsoleViews.remove(view);
		}
	}

	@Override
	public void addConsoleListener(IConsoleListener listener) {
		fListeners.add(listener);
	}

	@Override
	public void removeConsoleListener(IConsoleListener listener) {
		fListeners.remove(listener);
	}

	@Override
	public void addConsoles(IConsole[] consoles) {
		List<IConsole> added = new ArrayList<>(consoles.length);
		synchronized (fConsoles) {
			for (IConsole console : consoles) {
				if (!fConsoles.contains(console)) {
					fConsoles.add(console);
					added.add(console);
					if (console instanceof TextConsole ioconsole) {
						createPatternMatchListeners(ioconsole);
					}
				}
			}
		}
		if (!added.isEmpty()) {
			fireUpdate(added.toArray(new IConsole[added.size()]), ADDED);
		}
	}

	@Override
	public void removeConsoles(IConsole[] consoles) {
		List<IConsole> removed = new ArrayList<>(consoles.length);
		synchronized (fConsoles) {
			for (IConsole console : consoles) {
				if (fConsoles.remove(console)) {
					removed.add(console);
				}
			}
		}
		if (!removed.isEmpty()) {
			fireUpdate(removed.toArray(new IConsole[removed.size()]), REMOVED);
		}
	}

	@Override
	public IConsole[] getConsoles() {
		synchronized (fConsoles) {
			return fConsoles.toArray(new IConsole[fConsoles.size()]);
		}
	}

	/**
	 * Fires notification.
	 *
	 * @param consoles consoles added/removed
	 * @param type ADD or REMOVE
	 */
	private void fireUpdate(IConsole[] consoles, int type) {
		new ConsoleNotifier().notify(consoles, type);
	}

	private abstract static class AbstractConsoleJob extends WorkbenchJob {
		private final Set<IConsole> queue = new LinkedHashSet<>();

		AbstractConsoleJob(String name) {
			super(name);
			setSystem(true);
			setPriority(Job.SHORT);
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == ConsoleManager.CONSOLE_JOB_FAMILY;
		}

		protected void addConsole(IConsole console) {
			synchronized (queue) {
				queue.add(console);
			}
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Set<IConsole> consolesToWorkWith;
			synchronized (queue) {
				consolesToWorkWith = new LinkedHashSet<>(queue);
				queue.clear();
			}
			for (IConsole c : consolesToWorkWith) {
				workWith(c, monitor);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
			}
			synchronized (queue) {
				if (!queue.isEmpty()) {
					schedule();
				}
			}
			return Status.OK_STATUS;
		}

		abstract protected void workWith(IConsole console, IProgressMonitor monitor);
	}

	private class ShowConsoleViewJob extends AbstractConsoleJob {

		ShowConsoleViewJob() {
			super("Show Console View"); //$NON-NLS-1$
		}

		@Override
		protected void workWith(IConsole c, IProgressMonitor monitor) {
			boolean consoleFound = false;
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null && c != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					synchronized (fConsoleViews) {
						for (IConsoleView consoleView : fConsoleViews) {
							IWorkbenchPartSite site = consoleView.getSite();
							if (site == null) {
								continue;
							}
							if (site.getPage().equals(page)) {
								boolean consoleVisible = page.isPartVisible(consoleView);
								if (consoleVisible) {
									consoleFound = true;
									boolean bringToTop = shouldBringToTop(c, consoleView);
									if (bringToTop) {
										page.bringToTop(consoleView);
									}
									consoleView.display(c);
								}
							}
							if (monitor.isCanceled()) {
								return;
							}
						}
					}

					if (!consoleFound) {
						try {
							IConsoleView consoleView = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_CREATE);
							boolean bringToTop = shouldBringToTop(c, consoleView);
							if (bringToTop) {
								page.bringToTop(consoleView);
							}
							consoleView.display(c);
						} catch (PartInitException pie) {
							ConsolePlugin.log(pie);
						}
					}
				}
			}
		}
	}

	@Override
	public void showConsoleView(final IConsole console) {
		showConsoleJob.addConsole(console);
		showConsoleJob.schedule(100);
	}

	/**
	 * Returns whether the given console view should be brought to the top. The view
	 * should not be brought to the top if the view is pinned on a console other
	 * than the given console.
	 *
	 * @param console     the console to be shown in the view
	 * @param consoleView the view which should be brought to the top
	 * @return whether the given console view should be brought to the top
	 */
	private boolean shouldBringToTop(IConsole console, IViewPart consoleView) {
		boolean bringToTop = true;
		if (consoleView instanceof IConsoleView cView) {
			if (cView.isPinned()) {
				IConsole pinnedConsole = cView.getConsole();
				bringToTop = console.equals(pinnedConsole);
			}
		}
		return bringToTop;
	}

	@Override
	public void warnOfContentChange(final IConsole console) {
		warnAboutContentChangeJob.addConsole(console);
		warnAboutContentChangeJob.schedule(50);
	}

	private final class WarnAboutContentChangedJob extends AbstractConsoleJob {

		private WarnAboutContentChangedJob() {
			super(ConsoleMessages.ConsoleManager_consoleContentChangeJob);
		}

		@Override
		protected void workWith(IConsole console, IProgressMonitor monitor) {
			List<ConsoleView> viewsToUpdate = new ArrayList<>();
			synchronized (fConsoleViews) {
				for (ConsoleView view : fConsoleViews) {
					IWorkbenchPartSite site = view.getSite();
					if (site == null) {
						continue;
					}
					boolean viewVisible = site.getPage().isPartVisible(view);
					if (viewVisible && view.getConsole() == console) {
						// No need to update the UI if the console is already on top, since
						// user will already see content changes. This also prevents unnecessary
						// redraws which can cause flickering.
						viewsToUpdate.clear();
						break;
					}
					viewsToUpdate.add(view);
				}
			}
			for (ConsoleView view : viewsToUpdate) {
				view.warnOfContentChange(console);
			}
		}
	}

	@Override
	public IPatternMatchListener[] createPatternMatchListeners(IConsole console) {
		if (fPatternMatchListeners == null) {
			fPatternMatchListeners = new ArrayList<>();
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.EXTENSION_POINT_CONSOLE_PATTERN_MATCH_LISTENERS);
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
			for (IConfigurationElement config : elements) {
				PatternMatchListenerExtension extension = new PatternMatchListenerExtension(config);
				fPatternMatchListeners.add(extension);
			}
		}
		ArrayList<PatternMatchListener> list = new ArrayList<>();
		for (Iterator<PatternMatchListenerExtension> i = fPatternMatchListeners.iterator(); i.hasNext();) {
			PatternMatchListenerExtension extension = i.next();
			try {
				if (extension.getEnablementExpression() == null) {
					i.remove();
					continue;
				}

				if (console instanceof TextConsole textConsole && extension.isEnabledFor(console)) {
					PatternMatchListener patternMatchListener = new PatternMatchListener(extension);
					try {
						textConsole.addPatternMatchListener(patternMatchListener);
						list.add(patternMatchListener);
					} catch (PatternSyntaxException e) {
						ConsolePlugin.log(e);
						i.remove();
					}
				}
			} catch (CoreException e) {
				ConsolePlugin.log(e);
			}
		}
		return list.toArray(new PatternMatchListener[0]);
	}

	public IConsolePageParticipant[] getPageParticipants(IConsole console) {
		if(fPageParticipants == null) {
			fPageParticipants = new ArrayList<>();
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.EXTENSION_POINT_CONSOLE_PAGE_PARTICIPANTS);
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
			for (IConfigurationElement config : elements) {
				ConsolePageParticipantExtension extension = new ConsolePageParticipantExtension(config);
				fPageParticipants.add(extension);
			}
		}
		ArrayList<IConsolePageParticipant> list = new ArrayList<>();
		for (ConsolePageParticipantExtension extension : fPageParticipants) {
			try {
				if (extension.isEnabledFor(console)) {
					list.add(extension.createDelegate());
				}
			} catch (CoreException e) {
				ConsolePlugin.log(e);
			}
		}
		return list.toArray(new IConsolePageParticipant[0]);
	}

	public ConsoleFactoryExtension[] getConsoleFactoryExtensions() {
		if (fConsoleFactoryExtensions == null) {
			fConsoleFactoryExtensions = new ArrayList<>();
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.EXTENSION_POINT_CONSOLE_FACTORIES);
			IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
			for (IConfigurationElement configurationElement : configurationElements) {
				fConsoleFactoryExtensions.add(new ConsoleFactoryExtension(configurationElement));
			}
		}
		return fConsoleFactoryExtensions.toArray(new ConsoleFactoryExtension[0]);
	}


	@Override
	public void refresh(final IConsole console) {
		redrawConsoleJob.addConsole(console);
		redrawConsoleJob.schedule(50);
	}

}
