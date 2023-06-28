/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - added an evaluation context source provider (bug 229219)
 *     Patrick Chuong (Texas Instruments) and Pawel Piech (Wind River) -
 *     		Allow multiple debug views and multiple debug context providers (Bug 327263)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextProvider;
import org.eclipse.debug.ui.contexts.IDebugContextProvider2;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Context service for a specific window.
 *
 * @since 3.2
 */
public class DebugWindowContextService implements IDebugContextService, IPartListener2, IDebugContextListener {

	private Map<String, ListenerList<IDebugContextListener>> fListenersByPartId = new HashMap<>();
	private Map<String, IDebugContextProvider> fProvidersByPartId = new HashMap<>();
	private Map<String, ListenerList<IDebugContextListener>> fPostListenersByPartId = new HashMap<>();

	private IWorkbenchWindow fWindow;
	private List<IDebugContextProvider> fProviders = new ArrayList<>();

	private DebugContextSourceProvider fSourceProvider;

	public DebugWindowContextService(IWorkbenchWindow window, final IEvaluationService evaluationService) {
		fWindow = window;
		fWindow.getPartService().addPartListener(this);

		// need to register source provider on the UI thread (bug 438396)
		window.getShell().getDisplay().asyncExec(() -> {
			if (fWindow != null) {
				fSourceProvider = new DebugContextSourceProvider(DebugWindowContextService.this, evaluationService);
			}
		});
	}

	public void dispose() {
		if (fSourceProvider != null) {
			fSourceProvider.dispose();
		}
		fWindow.getPartService().removePartListener(this);
		fWindow = null;
	}

	@Override
	public synchronized void addDebugContextProvider(IDebugContextProvider provider) {
		if (fWindow == null)
		 {
			return; // disposed
		}

		IWorkbenchPart part = provider.getPart();
		fProvidersByPartId.put( getCombinedPartId(part), provider );

		// Check if provider is a window context provider
		boolean canSetActive = true;
		if (provider instanceof IDebugContextProvider2) {
			canSetActive = ((IDebugContextProvider2) provider).isWindowContextProvider();
		}
		// Make the provider active if matches the active part. Otherwise, it
		// may still become the active provider if fProviders.isEmpty().
		if (canSetActive) {
			IWorkbenchPart activePart = null;
			IWorkbenchPage activePage = fWindow.getActivePage();
			if (activePage != null) {
				activePart = activePage.getActivePart();
			}
			canSetActive = (activePart == null && part == null) || (activePart != null && activePart.equals(part));
		}

		if (canSetActive) {
			fProviders.add(0, provider);
		} else {
			fProviders.add(provider);
		}
		notify(provider);
		provider.addDebugContextListener(this);
	}

	@Override
	public synchronized void removeDebugContextProvider(IDebugContextProvider provider) {
		int index = fProviders.indexOf(provider);
		if (index >= 0) {
			fProvidersByPartId.remove( getCombinedPartId(provider.getPart()) );
			fProviders.remove(index);
			if (fWindow != null && fWindow.isClosing()) {
				provider.removeDebugContextListener(this);
				return;
			}
			IDebugContextProvider activeProvider = getActiveProvider();
			if (index == 0) {
				if (activeProvider != null) {
					notify(activeProvider);
				} else {
					// Removed last provider.  Send empty selection to all listeners.
					notify(new DebugContextEvent(provider, StructuredSelection.EMPTY, DebugContextEvent.ACTIVATED));
				}
			} else {
				// Notify listeners of the removed provider with the active window context.
				notifyPart(provider.getPart(),
					new DebugContextEvent(activeProvider, getActiveContext(), DebugContextEvent.ACTIVATED));
			}
		}
		provider.removeDebugContextListener(this);
	}

	@Override
	public void addDebugContextListener(IDebugContextListener listener) {
		addDebugContextListener(listener, null);
	}

	@Override
	public void addPostDebugContextListener(IDebugContextListener listener, String partId) {
		ListenerList<IDebugContextListener> list = fPostListenersByPartId.get(partId);
		if (list == null) {
			list = new ListenerList<>();
			fPostListenersByPartId.put(partId, list);
		}
		list.add(listener);
	}

	@Override
	public void addPostDebugContextListener(IDebugContextListener listener) {
		addPostDebugContextListener(listener, null);
	}

	@Override
	public void removePostDebugContextListener(IDebugContextListener listener, String partId) {
		ListenerList<IDebugContextListener> list = fPostListenersByPartId.get(partId);
		if (list != null) {
			list.remove(listener);
		}
	}

	@Override
	public void removePostDebugContextListener(IDebugContextListener listener) {
		removePostDebugContextListener(listener, null);
	}

	@Override
	public void removeDebugContextListener(IDebugContextListener listener) {
		removeDebugContextListener(listener, null);
	}

	/**
	 * Notifies listeners of the context in the specified provider.
	 *
	 * @param provdier context provider
	 */
	protected void notify(IDebugContextProvider provdier) {
		ISelection activeContext = provdier.getActiveContext();
		if (activeContext == null) {
			activeContext = new StructuredSelection();
		}
		notify(new DebugContextEvent(provdier, activeContext, DebugContextEvent.ACTIVATED));
	}

	protected void notify(DebugContextEvent event) {
		// Allow handling for case where getActiveProvider() == null.
		// This can happen upon removeContextProvider() called on last available
		// provider (bug 360637).
		IDebugContextProvider provider = getActiveProvider();
		IWorkbenchPart part = event.getDebugContextProvider().getPart();

		// Once for listeners
		if (provider == null || provider == event.getDebugContextProvider()) {
			notify(event, getListeners(null));
		}
		if (part != null) {
			notify(event, getListeners(part));
		}

		// Again for post-listeners
		if (provider == null || provider == event.getDebugContextProvider()) {
			notify(event, getPostListeners(null));
		}
		if (part != null) {
			notify(event, getPostListeners(part));
		}
	}

	protected void notifyPart(IWorkbenchPart part, DebugContextEvent event) {
		if (part != null) {
			notify(event, getListeners(part));
			notify(event, getPostListeners(part));
		}
	}

	protected void notify(final DebugContextEvent event, ListenerList<IDebugContextListener> listeners) {
		for (final IDebugContextListener listener : listeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.debugContextChanged(event);
				}
				@Override
				public void handleException(Throwable exception) {
					DebugUIPlugin.log(exception);
				}
			});
		}
	}

	protected ListenerList<IDebugContextListener> getListeners(IWorkbenchPart part) {
		String id = null;
		if (part != null) {
			id = getCombinedPartId(part);
			ListenerList<IDebugContextListener> listenerList = fListenersByPartId.get(id);
			return listenerList != null ? listenerList : new ListenerList<>();
		} else {
			ListenerList<IDebugContextListener> listenerList = fListenersByPartId.get(null);
			ListenerList<IDebugContextListener> retVal = new ListenerList<>();
			if (listenerList != null) {
				for (IDebugContextListener iDebugContextListener : listenerList) {
					retVal.add(iDebugContextListener);
				}
			}

			outer: for (Map.Entry<String, ListenerList<IDebugContextListener>> entry : fListenersByPartId.entrySet()) {
				String listenerPartId = entry.getKey();
				for (IDebugContextProvider provider : fProviders) {
					String providerPartId = getCombinedPartId(provider.getPart());
					if ((listenerPartId == null && providerPartId == null) ||
						(listenerPartId != null && listenerPartId.equals(providerPartId)))
					{
						continue outer;
					}
				}
				ListenerList<IDebugContextListener> listenersForPart = entry.getValue();
				if (listenersForPart != null) {
					for (IDebugContextListener iDebugContextListener : listenersForPart) {
						// no effect if listener already present
						retVal.add(iDebugContextListener);
					}
				}
			}
			return retVal;
		}
	}

	protected ListenerList<IDebugContextListener> getPostListeners(IWorkbenchPart part) {
		String id = null;
		if (part != null) {
			id = getCombinedPartId(part);
			ListenerList<IDebugContextListener> listenerList = fPostListenersByPartId.get(id);
			return listenerList != null ? listenerList : new ListenerList<>();
		} else {
			ListenerList<IDebugContextListener> retVal = fPostListenersByPartId.get(null);
			if (retVal == null) {
				retVal = new ListenerList<>();
			}

			outer: for (Map.Entry<String, ListenerList<IDebugContextListener>> entry : fPostListenersByPartId.entrySet()) {
				String listenerPartId = entry.getKey();
				for (IDebugContextProvider provider : fProviders) {
					String providerPartId = getCombinedPartId(provider.getPart());
					if ((listenerPartId == null && providerPartId == null) || (listenerPartId != null && listenerPartId.equals(providerPartId))) {
						continue outer;
					}
				}
				ListenerList<IDebugContextListener> listenersForPart = entry.getValue();
				if (listenersForPart != null) {
					for (IDebugContextListener iDebugContextListener : listenersForPart) {
						// no effect if listener already present
						retVal.add(iDebugContextListener);
					}
				}
			}
			return retVal;
		}
	}


	@Override
	public synchronized void addDebugContextListener(IDebugContextListener listener, String partId) {
		ListenerList<IDebugContextListener> list = fListenersByPartId.get(partId);
		if (list == null) {
			list = new ListenerList<>();
			fListenersByPartId.put(partId, list);
		}
		list.add(listener);
	}

	@Override
	public void removeDebugContextListener(IDebugContextListener listener, String partId) {
		ListenerList<IDebugContextListener> list = fListenersByPartId.get(partId);
		if (list != null) {
			list.remove(listener);
			if (list.size() == 0) {
				fListenersByPartId.remove(partId);
			}
		}
	}

	@Override
	public ISelection getActiveContext(String partId) {
		IDebugContextProvider provider = fProvidersByPartId.get(partId);
		if (provider != null) {
			return provider.getActiveContext();
		}
		return getActiveContext();
	}

	@Override
	public ISelection getActiveContext() {
		IDebugContextProvider activeProvider = getActiveProvider();
		if (activeProvider != null) {
			return activeProvider.getActiveContext();
		}
		return null;
	}

	/**
	 * Returns the active provider or <code>null</code>
	 *
	 * @return active provider or <code>null</code>
	 */
	private IDebugContextProvider getActiveProvider() {
		if (!fProviders.isEmpty()) {
			return fProviders.get(0);
		}
		return null;
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IDebugContextProvider provider = fProvidersByPartId.get(partRef.getId());
		if (provider != null) {
			boolean canSetActive = true;
			if (provider instanceof IDebugContextProvider2) {
				canSetActive = ((IDebugContextProvider2) provider).isWindowContextProvider();
			}

			if (canSetActive) {
				int index = fProviders.indexOf(provider);
				if (index > 0) {
					fProviders.remove(index);
					fProviders.add(0, provider);
					notify(provider);
				}
			}
		}

	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public synchronized void partClosed(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		notify(event);
	}

	private String getCombinedPartId(IWorkbenchPart part) {
		if (part == null) {
			return null;
		} else if (part.getSite() instanceof IViewSite) {
			IViewSite site = (IViewSite)part.getSite();
			return getCombinedPartId(site.getId(), site.getSecondaryId());

		} else {
			return part.getSite().getId();
		}
	}

	private String getCombinedPartId(String id, String secondaryId) {
		return id + (secondaryId != null ? ":" + secondaryId : "");   //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public void addDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId) {
		addDebugContextListener(listener, getCombinedPartId(partId, partSecondaryId));
	}

	@Override
	public void removeDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId) {
		removeDebugContextListener(listener, getCombinedPartId(partId, partSecondaryId));
	}

	@Override
	public void addPostDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId) {
		addPostDebugContextListener(listener, getCombinedPartId(partId, partSecondaryId));
	}

	@Override
	public void removePostDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId) {
		removePostDebugContextListener(listener, getCombinedPartId(partId, partSecondaryId));
	}

	@Override
	public ISelection getActiveContext(String partId, String partSecondaryId) {
		return getActiveContext(getCombinedPartId(partId, partSecondaryId));
	}
}
