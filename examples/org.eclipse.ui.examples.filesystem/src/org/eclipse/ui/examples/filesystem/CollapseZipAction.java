package org.eclipse.ui.examples.filesystem;

import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

public class CollapseZipAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public CollapseZipAction() {
		super();
	}

	private void collapseZip(IFolder folder) {
		try {
			URI zipURI = new URI(folder.getLocationURI().getQuery());
			//check if the zip file is physically stored below the folder in the workspace
			IFileStore parentStore = EFS.getStore(folder.getParent().getLocationURI());
			URI childURI = parentStore.getChild(folder.getName()).toURI();
			if (URIUtil.equals(zipURI, childURI)) {
				//the zip file is in the workspace so just delete the link 
				// and refresh the parent to create the resource
				folder.delete(IResource.NONE, null);
				folder.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
			} else {
				//otherwise the zip file must be a linked resource
				IFile file = folder.getParent().getFile(new Path(folder.getName()));
				file.createLink(zipURI, IResource.REPLACE, null);
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Error opening zip file");
			e.printStackTrace();
		}
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
		if (!(element instanceof IFolder))
			return;
		collapseZip((IFolder) element);

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
