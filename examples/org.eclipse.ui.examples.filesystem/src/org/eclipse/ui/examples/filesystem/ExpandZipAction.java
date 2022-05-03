package org.eclipse.ui.examples.filesystem;

import java.net.URI;
import org.eclipse.core.internal.filesystem.zip.ZipFileSystem;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

public class ExpandZipAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public ExpandZipAction() {
		super();
	}

	private void expandZip(IFile file) {
		try {
			URI zipURI = new URI(ZipFileSystem.SCHEME_ZIP, null, "/", file.getLocationURI().toString(), null);
			IFolder link = file.getParent().getFolder(new Path(file.getName()));
			link.createLink(zipURI, IResource.REPLACE, null);
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
		if (!(element instanceof IFile))
			return;
		expandZip((IFile) element);

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
