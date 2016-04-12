/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * The abstract superclass of all Team actions. This class contains some convenience
 * methods for getting selected objects and mapping selected objects to their
 * providers.
 *
 * Team providers may subclass this class when creating their actions.
 * Team providers may also instantiate or subclass any of the
 * subclasses of TeamAction provided in this package.
 */
public abstract class TeamAction extends AbstractHandler implements IObjectActionDelegate, IViewActionDelegate, IWorkbenchWindowActionDelegate, IActionDelegate2 {
	// The current selection
	private IStructuredSelection selection;

	// The shell, required for the progress dialog
	private Shell shell;

	// Constants for determining the type of progress. Subclasses may
	// pass one of these values to the run method.
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	private IWorkbenchPart targetPart;
	private IWorkbenchWindow window;
	private IPartListener2 targetPartListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			if (targetPart == partRef.getPart(false)) {
				targetPart = null;
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
		}
	};

	private ISelectionListener selectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if(selection instanceof IStructuredSelection)
				TeamAction.this.selection = (IStructuredSelection)selection;
		}
	};

	/**
	 * Creates an array of the given class type containing all the
	 * objects in the selection that adapt to the given class.
	 *
	 * @param selection
	 * @param c
	 * @return the selected adaptables
	 */
	public static Object[] getSelectedAdaptables(ISelection selection, Class c) {
		ArrayList result = null;
		if (selection != null && !selection.isEmpty()) {
			result = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object adapter = getAdapter(elements.next(), c);
				if (c.isInstance(adapter)) {
					result.add(adapter);
				}
			}
		}
		if (result != null && !result.isEmpty()) {
			return result.toArray((Object[])Array.newInstance(c, result.size()));
		}
		return (Object[])Array.newInstance(c, 0);
	}

	/**
	 * Find the object associated with the given object when it is adapted to
	 * the provided class. Null is returned if the given object does not adapt
	 * to the given class
	 *
	 * @param adaptable
	 * @param c
	 * @return Object
	 */
	public static Object getAdapter(Object adaptable, Class c) {
		if (c.isInstance(adaptable)) {
			return adaptable;
		}
		if (adaptable instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) adaptable;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter)) {
				return adapter;
			}
		}
		return null;
	}

	/**
	 * Returns the selected projects.
	 *
	 * @return the selected projects
	 */
	protected IProject[] getSelectedProjects() {
		IResource[] selectedResources = getSelectedResources();
		if (selectedResources.length == 0) return new IProject[0];
		ArrayList projects = new ArrayList();
		for (int i = 0; i < selectedResources.length; i++) {
			IResource resource = selectedResources[i];
			if (resource.getType() == IResource.PROJECT) {
				projects.add(resource);
			}
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * Returns an array of the given class type c that contains all
	 * instances of c that are either contained in the selection or
	 * are adapted from objects contained in the selection.
	 *
	 * @param c
	 * @return the selection adapted to the given class
	 */
	protected Object[] getAdaptedSelection(Class c) {
		return getSelectedAdaptables(selection, c);
	}

	/**
	 * Returns the selected resources.
	 *
	 * @return the selected resources
	 */
	protected IResource[] getSelectedResources() {
		return Utils.getContributedResources(getSelection().toArray());
	}

	protected IStructuredSelection getSelection() {
		if (selection == null)
			selection = StructuredSelection.EMPTY;
		return selection;
	}

	/**
     * Return the selected resource mappins that contain resources in
     * projects that are associated with a repository of the given id.
     * @param providerId the repository provider id
     * @return the resource mappings that contain resources associated with the given provider
	 */
    protected ResourceMapping[] getSelectedResourceMappings(String providerId) {
        Object[] elements = getSelection().toArray();
        ArrayList providerMappings = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            Object object = elements[i];
            Object adapted = getResourceMapping(object);
            if (adapted instanceof ResourceMapping) {
                ResourceMapping mapping = (ResourceMapping) adapted;
                if (providerId == null || isMappedToProvider(mapping, providerId)) {
                    providerMappings.add(mapping);
                }
            }
        }
        return (ResourceMapping[]) providerMappings.toArray(new ResourceMapping[providerMappings.size()]);
    }

    private Object getResourceMapping(Object object) {
        if (object instanceof ResourceMapping)
            return object;
        return Utils.getResourceMapping(object);
    }

    private boolean isMappedToProvider(ResourceMapping element, String providerId) {
        IProject[] projects = element.getProjects();
        for (int k = 0; k < projects.length; k++) {
            IProject project = projects[k];
            RepositoryProvider provider = RepositoryProvider.getProvider(project);
            if (provider != null && provider.getID().equals(providerId)) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Convenience method for getting the current shell.
	 *
	 * @return the shell
	 */
	protected Shell getShell() {
		if (shell != null) {
			return shell;
		} else if (targetPart != null) {
		    return targetPart.getSite().getShell();
		} else if (window != null) {
			return window.getShell();
		} else {
			IWorkbench workbench = TeamUIPlugin.getPlugin().getWorkbench();
			if (workbench == null) return null;
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window == null) return null;
			return window.getShell();
		}
	}
	/**
	 * Convenience method for running an operation with progress and
	 * error feedback.
	 *
	 * @param runnable  the runnable which executes the operation
	 * @param problemMessage  the message to display in the case of errors
	 * @param progressKind  one of PROGRESS_BUSYCURSOR or PROGRESS_DIALOG
	 */
	final protected void run(final IRunnableWithProgress runnable, final String problemMessage, int progressKind) {
		final Exception[] exceptions = new Exception[] {null};
		switch (progressKind) {
			case PROGRESS_BUSYCURSOR :
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					@Override
					public void run() {
						try {
							runnable.run(new NullProgressMonitor());
						} catch (InvocationTargetException e) {
							exceptions[0] = e;
						} catch (InterruptedException e) {
							exceptions[0] = null;
						}
					}
				});
				break;
			default :
			case PROGRESS_DIALOG :
				try {
					new ProgressMonitorDialog(getShell()).run(true, true, runnable);
				} catch (InvocationTargetException e) {
					exceptions[0] = e;
				} catch (InterruptedException e) {
					exceptions[0] = null;
				}
				break;
		}
		if (exceptions[0] != null) {
			handle(exceptions[0], null, problemMessage);
		}
	}

	/*
	 * Method declared on IActionDelegate.
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
			if (action != null) {
				setActionEnablement(action);
			}
		}
	}

	/**
	 * Method invoked from <code>selectionChanged(IAction, ISelection)</code>
	 * to set the enablement status of the action. The instance variable
	 * <code>selection</code> will contain the latest selection so the methods
	 * <code>getSelectedResources()</code> and <code>getSelectedProjects()</code>
	 * will provide the proper objects.
	 *
	 * This method can be overridden by subclasses but should not be invoked by them.
	 */
	protected void setActionEnablement(IAction action) {
		action.setEnabled(isEnabled());
	}

	/**
	 * If an exception occurs during enablement testing, this method is invoked
	 * to determine if the action should be enabled or not.
	 * @param exception the exception
	 * @return whether the action should be enabled or not
	 */
	protected boolean isEnabledForException(TeamException exception) {
		if (exception.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
			// Enable the action to allow the user to discover the problem
			return true;
		}
		// We should not open a dialog when determining menu enablement so log it instead
		TeamPlugin.log(exception);
		return false;
	}

	/*
	 * Method declared on IObjectActionDelegate.
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		if(targetPart != null) {
			this.shell = targetPart.getSite().getShell();
			this.targetPart = targetPart;
		}
	}
	/**
	 * Shows the given errors to the user.
	 *
	 * @param exception  the status containing the error
	 * @param title  the title of the error dialog
	 * @param message  the message for the error dialog
	 */
	protected void handle(Exception exception, String title, String message) {
		Utils.handleError(getShell(), exception, title, message);
	}

	/**
	 * Convenience method that maps the given resources to their providers.
	 * The returned Hashtable has keys which are ITeamProviders, and values
	 * which are Lists of IResources that are shared with that provider.
	 *
	 * @return a hashtable mapping providers to their resources
	 */
	protected Hashtable getProviderMapping(IResource[] resources) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}

	/**
	 * @return IWorkbenchPart
	 */
	protected IWorkbenchPart getTargetPart() {
        if(targetPart == null) {
            IWorkbenchPage  page = TeamUIPlugin.getActivePage();
            if (page != null) {
                targetPart = page.getActivePart();
            }
        }
        return targetPart;

	}

	/**
	 * Return the path that was active when the menu item was selected.
	 * @return IWorkbenchPage
	 */
	protected IWorkbenchPage getTargetPage() {
		if (getTargetPart() == null) return TeamUIPlugin.getActivePage();
		return getTargetPart().getSite().getPage();
	}

	/**
	 * Show the view with the given ID in the perspective from which the action
	 * was executed. Returns null if the view is not registered.
	 *
	 * @param viewId
	 * @return IViewPart
	 */
	protected IViewPart showView(String viewId) {
		try {
			return getTargetPage().showView(viewId);
		} catch (PartInitException pe) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	@Override
	public void init(IViewPart view) {
		if(view != null) {
			this.shell = view.getSite().getShell();
			this.targetPart = view;
		}
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		this.shell = window.getShell();
		window.getSelectionService().addPostSelectionListener(selectionListener);
		window.getActivePage().addPartListener(targetPartListener);
	}

	public IWorkbenchWindow getWindow() {
		return window;
	}

	@Override
	public void dispose() {
		super.dispose();
		if(window != null) {
			window.getSelectionService().removePostSelectionListener(selectionListener);
			if (window.getActivePage() != null) {
				window.getActivePage().removePartListener(targetPartListener);
			}
			targetPartListener = null;
		}
		// Don't hold on to anything when we are disposed to prevent memory leaks (see bug 195521)
        selection = null;
        window = null;
        targetPart = null;
        shell = null;
	}

	/**
	 * Actions must override to do their work.
	 */
	protected abstract void execute(IAction action)
			throws InvocationTargetException, InterruptedException;

	/**
	 * This method is called by the platform UI framework when a command is run for
	 * which this action is the handler. The handler doesn't have an explicit context, for
	 * example unlike a view, editor, or workbench window actions, they are not initialized
	 * with a part. As a result when the action is run it will use the selection service
	 * to determine to elements on which to perform the action.
	 * <p>
	 * CVS actions should ensure that they can run without a proxy action. Meaning that
	 * <code>selectionChanged</code> and <code>run</code> should support passing
	 * <code>null</code> as the IAction parameter.
	 * </p>
	 * @throws ExecutionException
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		if (activeWorkbenchWindow != null) {
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection != null) {
				IWorkbenchPart part = HandlerUtil.getActivePart(event);
				try {
					execute(activeWorkbenchWindow,  part, selection);
				} catch (InvocationTargetException e) {
					throw new ExecutionException(TeamUIMessages.TeamAction_errorTitle, e);
				} catch (InterruptedException e) {
					// Operation was canceled. Ignore
				}
			}
		}
		return null;
	}

	private void execute(IWorkbenchWindow activeWorkbenchWindow,
			IWorkbenchPart part, ISelection selection)
			throws InvocationTargetException, InterruptedException {
		// If the action is run from within an editor, try and find the
		// file for the given editor.
		if (part != null && part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			IFile file = ResourceUtil.getFile(input);
			if (file != null) {
				selectionChanged((IAction) null, new StructuredSelection(file));
			}
		} else {
			// Fallback is to prime the action with the selection
			selectionChanged((IAction) null, selection);
		}
		// Safe guard to ensure that the action is only run when enabled.
		if (isEnabled()) {
			execute((IAction) null);
		} else {
			MessageDialog.openInformation(activeWorkbenchWindow.getShell(),
					TeamUIMessages.TeamAction_handlerNotEnabledTitle,
					TeamUIMessages.TeamAction_handlerNotEnabledMessage);
		}
	}

	/**
	 * Common run method for all Team actions.
	 */
	@Override
	public void run(IAction action) {
		try {
			execute(action);
		} catch (InvocationTargetException e) {
			// Handle the exception and any accumulated errors
			handle(e);
		} catch (InterruptedException e) {
			// Operation was canceled. Ignore.
		}
	}

	/**
	 * This method can be overridden by subclasses but should not be invoked by
	 * them.
	 *
	 * @param e
	 *            Exception to handle
	 */
	protected void handle(Exception e) {
		handle(e, TeamUIMessages.TeamAction_errorTitle, null);
	}

    /**
     * The <code>TeamAction</code> implementation of this
     * <code>IActionDelegate2</code> method does nothing. Subclasses may
     * reimplement.
     */
    @Override
	public void init(IAction action) {
    }

    /**
     * The <code>TeamAction</code> implementation of this
     * <code>IActionDelegate2</code> method redirects to the <code>run</code>
     * method. Subclasses may reimplement.
     */
    @Override
	final public void runWithEvent(IAction action, Event event) {
        run(action);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#setEnabled(java.lang.Object)
	 */
	@Override
	public void setEnabled(Object evaluationContext) {
		IWorkbenchWindow activeWorkbenchWindow = (IWorkbenchWindow) HandlerUtil
				.getVariable(evaluationContext,
						ISources.ACTIVE_WORKBENCH_WINDOW_NAME);
		if (activeWorkbenchWindow != null) {
			ISelection selection = (ISelection) HandlerUtil.getVariable(
					evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection == null) {
				selection = StructuredSelection.EMPTY;
			}
			IWorkbenchPart part = (IWorkbenchPart) HandlerUtil.getVariable(
					evaluationContext, ISources.ACTIVE_PART_NAME);
			updateSelection(part, selection);
		}
	}

	private void updateSelection(IWorkbenchPart part, ISelection selection) {
		// If the action is run from within an editor, try and find the
		// file for the given editor.
		setActivePart(null, part);
		if (part != null && part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			IFile file = ResourceUtil.getFile(input);
			if (file != null) {
				selectionChanged((IAction) null, new StructuredSelection(file));
			}
		} else {
			// Fallback is to prime the action with the selection
			selectionChanged((IAction) null, selection);
		}
	}

}
