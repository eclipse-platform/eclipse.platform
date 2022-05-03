package org.eclipse.ui.examples.filesystem;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

public class IncludeResourceAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public IncludeResourceAction() {
		super();
	}

	private Shell getShell() {
		return targetPart.getSite().getShell();
	}

	private void include(IResource resource) {
		try {
			resource.delete(IResource.NONE, null);
			resource.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Error including resource");
			e.printStackTrace();
		}
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection))
			return;
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (!(element instanceof IResource))
			return;
		IResource resource = (IResource) element;
		if (!resource.isLinked())
			return;
		include(resource);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}
