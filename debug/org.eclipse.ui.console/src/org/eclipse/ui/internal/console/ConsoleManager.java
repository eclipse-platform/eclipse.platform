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
import java.util.HashSet;
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
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * The singleton console manager.
 *
 * @since 3.0
 */
public class ConsoleManager implements IConsoleManager {

	/**
	 * Console listeners
	 */
	private ListenerList<IConsoleListener> fListeners = new ListenerList<>();

	/**
	 * List of registered consoles
	 */
	private List<IConsole> fConsoles = new ArrayList<>(10);


	// change notification constants
	private final static int ADDED = 1;
	private final static int REMOVED = 2;

	private List<PatternMatchListenerExtension> fPatternMatchListeners;

	private List<ConsolePageParticipantExtension> fPageParticipants;

	private List<ConsoleFactoryExtension> fConsoleFactoryExtensions;

	private List<IConsoleView> fConsoleViews = new ArrayList<>();

	private boolean fWarnQueued = false;

	private RepaintJob fRepaintJob = new RepaintJob();

	private class RepaintJob extends WorkbenchJob {
		private Set<IConsole> list = new HashSet<>();

		public RepaintJob() {
			super("schedule redraw() of viewers"); //$NON-NLS-1$
			setSystem(true);
		}

		void addConsole(IConsole console) {
			synchronized (list) {
				list.add(console);
			}
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			synchronized (list) {
				if (list.isEmpty()) {
					return Status.OK_STATUS;
				}

				IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (IWorkbenchWindow window : workbenchWindows) {
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							IViewPart part = page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
							if (part != null && part instanceof IConsoleView) {
								ConsoleView view = (ConsoleView) part;
								if (list.contains(view.getConsole())) {
									Control control = view.getCurrentPage().getControl();
									if (!control.isDisposed()) {
										control.redraw();
									}
								}
							}

						}
					}
				}
				list.clear();
			}
			return Status.OK_STATUS;
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
				if(console instanceof TextConsole) {
					TextConsole ioconsole = (TextConsole)console;
					createPatternMatchListeners(ioconsole);
				}
				if (!fConsoles.contains(console)) {
					fConsoles.add(console);
					added.add(console);
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


	private class ShowConsoleViewJob extends WorkbenchJob {
		private Set<IConsole> queue = new LinkedHashSet<>();

		ShowConsoleViewJob() {
			super("Show Console View"); //$NON-NLS-1$
			setSystem(true);
			setPriority(Job.SHORT);
		}

		void addConsole(IConsole console) {
			synchronized (queue) {
				queue.add(console);
			}
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Set<IConsole> consolesToShow;
			synchronized (queue) {
				consolesToShow = new LinkedHashSet<>(queue);
				queue.clear();
			}
			for (IConsole c : consolesToShow) {
				showConsole(c);
			}
			synchronized (queue) {
				if (!queue.isEmpty()) {
					schedule();
				}
			}
			return Status.OK_STATUS;
		}

		private void showConsole(IConsole c) {
			boolean consoleFound = false;
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null && c != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					synchronized (fConsoleViews) {
						for (IConsoleView consoleView : fConsoleViews) {
							if (consoleView.getSite().getPage().equals(page)) {
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

	private ShowConsoleViewJob showJob = new ShowConsoleViewJob();
	/**
	 * @see IConsoleManager#showConsoleView(IConsole)
	 */
	@Override
	public void showConsoleView(final IConsole console) {
		showJob.addConsole(console);
		showJob.schedule(100);
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
		if (consoleView instanceof IConsoleView) {
			IConsoleView cView = (IConsoleView) consoleView;
			if (cView.isPinned()) {
				IConsole pinnedConsole = cView.getConsole();
				bringToTop = console.equals(pinnedConsole);
			}
		}
		return bringToTop;
	}

	@Override
	public void warnOfContentChange(final IConsole console) {
		if (!fWarnQueued) {
			fWarnQueued = true;
			Job job = new UIJob(ConsolePlugin.getStandardDisplay(), ConsoleMessages.ConsoleManager_consoleContentChangeJob) {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page= window.getActivePage();
						if (page != null) {
							IConsoleView consoleView= (IConsoleView)page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
							if (consoleView != null) {
								consoleView.warnOfContentChange(console);
							}
						}
					}
					fWarnQueued = false;
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
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

				if (console instanceof TextConsole && extension.isEnabledFor(console)) {
					TextConsole textConsole = (TextConsole) console;
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

	/*
	 * @see
	 * org.eclipse.ui.console.IConsoleManager#getPageParticipants(org.eclipse.ui.
	 * console.IConsole)
	 */
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

	/*
	 * @see org.eclipse.ui.console.IConsoleManager#getConsoleFactories()
	 */
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
		fRepaintJob.addConsole(console);
		fRepaintJob.schedule(50);
	}

}
