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
package org.eclipse.debug.internal.ui.stringsubstitution;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Maintains the context used to expand variables. The context is based on
 * the selected resource.
 */
public class SelectedResourceManager  {

	// Limit in seconds to wait on UI for accessing data
	private static final int MAX_UI_WAIT_TIME = 10;

	// singleton
	private static SelectedResourceManager fgDefault;

	// Used to avoid deadlocks while accessing UI thread from non UI code
	private static ExecutorService executor = Executors.newSingleThreadExecutor();


	/**
	 * Returns the singleton resource selection manager
	 *
	 * @return VariableContextManager
	 */
	public static SelectedResourceManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new SelectedResourceManager();
		}
		return fgDefault;
	}

	/**
	 * Returns the selection from the currently active part. If the active part is an
	 * editor a new selection of the editor part is made, otherwise the selection
	 * from the parts' selection provider is returned if it is a structured selection. Otherwise
	 * and empty selection is returned, never <code>null</code>.
	 * <br>
	 * <p>
	 * This method is intended to be called from the UI thread.
	 * </p>
	 *
	 * @return the <code>IStructuredSelection</code> from the current parts' selection provider, or
	 * a new <code>IStructuredSelection</code> of the current editor part, depending on what the current part
	 * is.
	 *
	 * @since 3.3
	 */
	public IStructuredSelection getCurrentSelection() {
		IStructuredSelection selection = getFromUI(this::getCurrentSelection0);
		if (selection == null) {
			selection = StructuredSelection.EMPTY;
		}
		return selection;
	}

	/**
	 * Underlying implementation of <code>getCurrentSelection</code>
	 * @return the current selection
	 *
	 * @since 3.4
	 */
	IStructuredSelection getCurrentSelection0() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if(window != null) {
			IWorkbenchPage page  = window.getActivePage();
			if(page != null) {
				IWorkbenchPart part = page.getActivePart();
				if(part instanceof IEditorPart) {
					return new StructuredSelection(part);
				}
				else if(part != null) {
					IWorkbenchSite site = part.getSite();
					if(site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if(provider != null) {
							ISelection selection = provider.getSelection();
							if(selection instanceof IStructuredSelection) {
								return (IStructuredSelection) provider.getSelection();
							}
						}
					}
				}
			}
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Returns the currently selected resource in the active workbench window,
	 * or <code>null</code> if none. If an editor is active, the resource adapter
	 * associated with the editor is returned.
	 *
	 * @return selected resource or <code>null</code>
	 */
	public IResource getSelectedResource() {
		IResource resource = getFromUI(this::getSelectedResource0);
		return resource;
	}

	/**
	 * Returns the currently selected resource from the active part, or <code>null</code> if one cannot be
	 * resolved.
	 * @return the currently selected <code>IResource</code>, or <code>null</code> if none.
	 * @since 3.3
	 */
	protected IResource getSelectedResource0() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		IResource resource = null;
		if(window != null) {
			IWorkbenchPage page  = window.getActivePage();
			if(page != null) {
				IWorkbenchPart part = page.getActivePart();
				if(part instanceof IEditorPart) {
					IEditorPart epart = (IEditorPart) part;
					resource = epart.getEditorInput().getAdapter(IResource.class);
				}
				else if(part != null) {
					IWorkbenchPartSite site = part.getSite();
					if(site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if(provider != null) {
							ISelection selection = provider.getSelection();
							if(selection instanceof IStructuredSelection) {
								IStructuredSelection ss = (IStructuredSelection) selection;
								if(!ss.isEmpty()) {
									Iterator<?> iterator = ss.iterator();
									while (iterator.hasNext() && resource == null) {
										Object next = iterator.next();
										resource = Platform.getAdapterManager().getAdapter(next, IResource.class);
									}
								}
							}
						}
					}
				}
			}
		}
		return resource;
	}

	/**
	 * Returns the current text selection as a <code>String</code>, or <code>null</code> if
	 * none.
	 *
	 * @return the current text selection as a <code>String</code> or <code>null</code>
	 */
	public String getSelectedText() {
		String text = getFromUI(this::getSelectedText0);
		return text;
	}

	/**
	 * Returns the selected text from the most currently active editor. The editor does not have to
	 * have focus at the time this method is called.
	 * @return the currently selected text in the most recent active editor.
	 *
	 * @since 3.3
	 */
	protected String getSelectedText0() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if(window != null) {
			IWorkbenchPage page = window.getActivePage();
			if(page != null) {
				IEditorPart epart = page.getActiveEditor();
				if(epart != null) {
					IEditorSite esite = epart.getEditorSite();
					if(esite != null) {
						ISelectionProvider sprovider = esite.getSelectionProvider();
						if(sprovider != null) {
							ISelection selection = sprovider.getSelection();
							if(selection instanceof ITextSelection) {
								return ((ITextSelection)selection).getText();
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the active workbench window, or <code>null</code> if none.
	 *
	 * @return the active workbench window, or <code>null</code> if none
	 * @since 3.2
	 */
	public IWorkbenchWindow getActiveWindow() {
		IWorkbenchWindow window = getFromUI(DebugUIPlugin::getActiveWorkbenchWindow);
		return window;
	}

	private <T> T getFromUI(Callable<T> call) {
		try {
			if (Display.getCurrent() != null) {
				return call.call();
			} else {
				return runInUIThreadWithTimeout(call, MAX_UI_WAIT_TIME, TimeUnit.SECONDS);
			}
		} catch (TimeoutException e) {
			reportTimeout();
			return null;
		} catch (InterruptedException e) {
			Thread.interrupted();
			// Bug 569486: don't care, tha task was cancelled, see for example
			// org.eclipse.jface.text.TextViewerHoverManager.TextViewerHoverManager()
			// DebugUIPlugin.log(e);
			return null;
		} catch (Exception e) {
			DebugUIPlugin.log(e);
			return null;
		}
	}

	/**
	 * Tries to run the task in the UI thread, and gives up if UI thread does not
	 * answer after given timeout
	 *
	 * @param timeout to wait for the UI lock
	 * @return may return null
	 * @throws Exception
	 */
	static <V> V runInUIThreadWithTimeout(Callable<V> callable, long timeout, TimeUnit units) throws Exception {
		FutureTask<V> task = new FutureTask<>(() -> syncExec(callable));
		executor.execute(task);
		return task.get(timeout, units);
	}

	static <V> V syncExec(Callable<V> callable) throws Exception {
		AtomicReference<V> ref = new AtomicReference<>();
		AtomicReference<Exception> ex = new AtomicReference<>();
		DebugUIPlugin.getStandardDisplay().syncExec(() -> {
			try {
				ref.set(callable.call());
			} catch (Exception e) {
				ex.set(e);
			}
		});
		if (ex.get() != null) {
			throw ex.get();
		}
		return ref.get();
	}

	/**
	 * Reports an error the log with thread stack information for current and UI threads
	 */
	private static void reportTimeout() {
		Thread nonUiThread = Thread.currentThread();

		String msg = "To avoid deadlock while executing Display.syncExec() from a non UI thread '" //$NON-NLS-1$
				+ nonUiThread.getName() + "', operation was cancelled."; //$NON-NLS-1$
		MultiStatus main = new MultiStatus(DebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, msg, null);

		ThreadInfo[] threads = ManagementFactory.getThreadMXBean().getThreadInfo(
				new long[] { nonUiThread.getId(), Display.getDefault().getThread().getId() }, true, true);

		for (ThreadInfo info : threads) {
			String childMsg;
			if (info.getThreadId() == nonUiThread.getId()) {
				childMsg = nonUiThread.getName() + " thread probably holding a lock and trying to acquire UI lock"; //$NON-NLS-1$
			} else {
				childMsg = "UI thread waiting on a job or lock."; //$NON-NLS-1$
			}
			Exception childEx = new IllegalStateException("Call stack for thread " + info.getThreadName()); //$NON-NLS-1$
			childEx.setStackTrace(info.getStackTrace());
			main.add(DebugUIPlugin.newErrorStatus(childMsg, childEx));
		}

		DebugUIPlugin.log(main);
	}
}
