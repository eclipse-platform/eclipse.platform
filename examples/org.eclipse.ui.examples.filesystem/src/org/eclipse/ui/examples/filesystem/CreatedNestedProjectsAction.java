package org.eclipse.ui.examples.filesystem;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

/**
 * Popup menu action to find and create the projects nested within the
 * selected projects.
 *
 */
public class CreatedNestedProjectsAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public CreatedNestedProjectsAction() {
		super();
	}

	private Shell getShell() {
		return targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection))
			return;
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (!(element instanceof IProject))
			return;
		new NestedProjectCreator().createNestedProjects(new IProject[] {((IProject) element)}, getShell());

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
