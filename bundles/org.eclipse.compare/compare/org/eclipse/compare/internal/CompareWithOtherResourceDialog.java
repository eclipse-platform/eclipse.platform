/*******************************************************************************
 * Copyright (c) 2008, 2011 Aleksandra Wozniak and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - initial implementation
 *    IBM Corporation - Bug 73923 (major refactoring and adjustments)
 *    IBM Corporation - Bug 241649 - [Dialogs] Resizing of the "compare with other" dialog
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * This is a dialog that can invoke the compare editor on chosen files.
 */
public class CompareWithOtherResourceDialog extends TitleAreaDialog {
	
	private int MIN_WIDTH = 320;
	private int MIN_HEIGHT_WITH_ANCESTOR = 320;
	private int MIN_HEIGHT_WITHOUT_ANCESTOR = 238;

	private class FileTextDragListener implements DragSourceListener {

		private ContentTypeElement element;

		public FileTextDragListener(ContentTypeElement element) {
			this.element = element;
		}

		public void dragFinished(DragSourceEvent event) {
			element.setText(""); //$NON-NLS-1$
		}

		public void dragSetData(DragSourceEvent event) {
			event.data = element.getText();
		}

		public void dragStart(DragSourceEvent event) {
			if (element.getText() == null)
				event.doit = false;
		}
	}

	private class FileTextDropListener implements DropTargetListener {

		private ContentTypeElement element;
		private ResourceTransfer resourceTransfer;
		private TextTransfer textTransfer;

		public FileTextDropListener(ContentTypeElement element) {
			this.element = element;
			resourceTransfer = ResourceTransfer.getInstance();
			textTransfer = TextTransfer.getInstance();
		}

		public void dragEnter(DropTargetEvent event) {

			if (event.detail == DND.DROP_DEFAULT) {
				if ((event.operations & DND.DROP_COPY) != 0)
					event.detail = DND.DROP_COPY;
				else
					event.detail = DND.DROP_NONE;
			}

			for (int i = 0; i < event.dataTypes.length; i++) {
				if (resourceTransfer.isSupportedType(event.dataTypes[i])
						|| textTransfer.isSupportedType(event.dataTypes[i])) {
					event.currentDataType = event.dataTypes[i];
					if (event.detail != DND.DROP_COPY)
						event.detail = DND.DROP_NONE;
					break;
				}
			}
		}

		public void dragLeave(DropTargetEvent event) {
			// intentionally empty
		}

		public void dragOperationChanged(DropTargetEvent event) {

			if (event.detail == DND.DROP_DEFAULT) {
				if ((event.operations & DND.DROP_COPY) != 0)
					event.detail = DND.DROP_COPY;
				else
					event.detail = DND.DROP_NONE;
			} else if (resourceTransfer.isSupportedType(event.currentDataType)) {
				if (event.detail != DND.DROP_COPY)
					event.detail = DND.DROP_NONE;
			}
		}

		public void dragOver(DropTargetEvent event) {
			// intentionally empty
		}

		public void drop(DropTargetEvent event) {

			if (textTransfer.isSupportedType(event.currentDataType)) {
				String txt = (String) event.data;
				IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(txt);
				if (r != null)
					element.setResource(r);
			} else if (resourceTransfer.isSupportedType(event.currentDataType)) {
				IResource[] files = (IResource[]) event.data;
				if (files.length > 0)
					element.setResource(files[0]);
			}

		}

		public void dropAccept(DropTargetEvent event) {
			// intentionally empty
		}

	}
	
	private abstract class ContentTypeElement {
		
		private Button radioButton;
		protected Button mainButton;
		protected Text text;
		private String type;
		protected InternalSection section;
		private IResource resource;
		
		public ContentTypeElement(Composite parent, String type, InternalSection section) {
			this.type = type;
			this.section = section;
			createContents(parent);
		}
		
		private void createContents(Composite parent) {
			createRadioButton(parent);
			createText(parent);
			createMainButton(parent);
		}
		
		private void createRadioButton(Composite parent) {
			radioButton = new Button(parent, SWT.RADIO);
			radioButton.setText(type);
		}

		protected void createText(Composite parent) {
			text = new Text(parent, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setEditable(false);
		}
		
		protected void createMainButton(Composite parent) {
			mainButton = new Button(parent, SWT.PUSH);
			mainButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		}
		
		protected Button getRadioButton() {
			return radioButton;
		}
		
		protected String getText() {
			return text.getText();
		}
		
		protected void setText(String string) {
			text.setText(string);
		}
		
		protected void setEnabled(boolean enabled) {
			radioButton.setSelection(enabled);
			mainButton.setEnabled(enabled);
			text.setEnabled(enabled);
		}
		
		protected void setResource(IResource resource) {
			this.resource = resource;
			section.setResource(resource);
		}

		public IResource getResource() {
			return resource;
		}
		
		void clearResource() {
			resource = null;
			text.setText(""); //$NON-NLS-1$
		}
		
	}
	
	private class WorkspaceContent extends ContentTypeElement {
		
		public WorkspaceContent(Composite parent, InternalSection section) {
			super(parent, CompareMessages.CompareWithOtherResourceDialog_workspaceRadioButton, section);
		}
		
		protected void createMainButton(Composite parent) {
			super.createMainButton(parent);
			mainButton.setText(CompareMessages.CompareWithOtherResourceDialog_workspaceMainButton);
			// temporarily hide this button. For more information about supporting for browsing workspace see bug 243744.
			mainButton.setVisible(false);
		}
		
		protected void createText(Composite parent) {
			
			super.createText(parent);
			text.setEditable(true);
			
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					section.setResource(text.getText());
					updateErrorInfo();
				}
			});

			text.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					section.setResource(text.getText());
					updateErrorInfo();
				}
			});
			
			initDrag();
			initDrop();
		}

		protected void setResource(IResource resource) {
			super.setResource(resource);
			text.setText(resource.getFullPath().toOSString());
		}

		protected void initDrag() {
			DragSource source = new DragSource(text, DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_DEFAULT);
			Transfer[] types = new Transfer[] { TextTransfer.getInstance(),
					ResourceTransfer.getInstance() };
			source.setTransfer(types);
			source.addDragListener(new FileTextDragListener(this));
		}

		protected void initDrop() {
			DropTarget target = new DropTarget(text, DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_DEFAULT);
			Transfer[] types = new Transfer[] { TextTransfer.getInstance(),
					ResourceTransfer.getInstance() };
			target.setTransfer(types);
			target.addDropListener(new FileTextDropListener(this));
		}
		
	}
	
	private class ExternalFileContent extends ContentTypeElement {
		
		public ExternalFileContent(Composite parent, InternalSection section) {
			super(parent, CompareMessages.CompareWithOtherResourceDialog_externalFileRadioButton, section);
		}
		
		protected void createMainButton(Composite parent) {
			super.createMainButton(parent);
			mainButton.setText(CompareMessages.CompareWithOtherResourceDialog_externalFileMainButton);
			mainButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					IResource r = tmpProject.getExternalFile();
					if (r == null) 
						return;
					setResource(r);
				}
			});
		}

		protected void setResource(IResource resource) {
			super.setResource(resource);
			text.setText(resource.getLocation().toOSString());
		}
		
	}
	
	private class ExternalFolderContent extends ContentTypeElement {
		
		public ExternalFolderContent(Composite parent, InternalSection section) {
			super(parent, CompareMessages.CompareWithOtherResourceDialog_externalFolderRadioButton, section);
		}
		
		protected void createMainButton(Composite parent) {
			super.createMainButton(parent);
			mainButton.setText(CompareMessages.CompareWithOtherResourceDialog_externalFolderMainButton);
			mainButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					IResource r = tmpProject.getExternalFolder();
					if (r == null)
						return;
					setResource(r);
				}
			});
		}

		protected void setResource(IResource resource) {
			super.setResource(resource);
			text.setText(resource.getLocation().toOSString());
		}
		
	}

	private abstract class InternalSection {
		
		// there is no "enum" support in Java 1.4. Sigh...
		public static final int WORKSPACE = 0;
		public static final int EXTERNAL_FILE = 1;
		public static final int EXTERNAL_FOLDER = 2;

		protected Group group;
		private IResource resource;
		
		ExternalFileContent externalFileContent;
		ExternalFolderContent externalFolderContent;
		WorkspaceContent workspaceContent;
		
		private InternalSection() {
			// not to instantiate
		}

		protected void createContents(Composite parent) {

			group = new Group(parent, SWT.NONE);
			group.setLayout(new GridLayout(3, false));
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			workspaceContent = new WorkspaceContent(group, this);
			externalFileContent = new ExternalFileContent(group, this);
			externalFolderContent = new ExternalFolderContent(group, this);
			
			addListenersToRadioButtons();
		}
		
		private void addListenersToRadioButtons() {
			final ContentTypeElement[] elements = new ContentTypeElement[] { workspaceContent, 
					externalFileContent, externalFolderContent };
			for (int i = 0; i < elements.length; i++) {
				elements[i].getRadioButton().addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						for (int j = 0; j < elements.length; j++) {
							if (event.widget != elements[j].getRadioButton())
								elements[j].setEnabled(false);
							else {
								elements[j].setEnabled(true);
								setResource(elements[j].getResource());
							}
						}
					}
				});
			}
		}

		protected IResource getResource() {
			return resource;
		}

		protected void setResource(IResource resource) {
			this.resource = resource;
			updateErrorInfo();
		}

		protected void setResource(String s) {
			IResource tmp = ResourcesPlugin.getWorkspace().getRoot()
					.findMember(s);
			if (tmp instanceof IWorkspaceRoot) 
				resource = null;
			else 
				resource = tmp;
			updateErrorInfo();
		}

		protected void clearResource() {
			resource = null;
			workspaceContent.clearResource();
			externalFileContent.clearResource();
			externalFolderContent.clearResource();
			updateErrorInfo();
		}
		
		protected void setContentType(int type) {
			switch(type) {
			case WORKSPACE: 
				workspaceContent.setEnabled(true);
				externalFileContent.setEnabled(false);
				externalFolderContent.setEnabled(false);
				break;
			case EXTERNAL_FILE:
				workspaceContent.setEnabled(false);
				externalFileContent.setEnabled(true);
				externalFolderContent.setEnabled(false);
				break;
			case EXTERNAL_FOLDER:
				workspaceContent.setEnabled(false);
				externalFileContent.setEnabled(false);
				externalFolderContent.setEnabled(true);
			}
		}
	}

	private class InternalGroup extends InternalSection {

		public InternalGroup(Composite parent) {
			createContents(parent);
		}

		public void setText(String text) {
			group.setText(text);
		}

		public void setLayoutData(GridData layoutData) {
			group.setLayoutData(layoutData);
		}
	}

	private class InternalExpandable extends InternalSection {

		private ExpandableComposite expandable;
		private Button clearButton;

		public InternalExpandable(Composite parent) {
			createContents(parent);
		}

		protected void createContents(Composite parent) {
			final Composite p = parent;
			expandable = new ExpandableComposite(parent, SWT.NONE,
					ExpandableComposite.TREE_NODE | ExpandableComposite.TWISTIE);
			super.createContents(expandable);
			createClearButton(group);
			expandable.setClient(group);
			expandable.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					p.layout();
					adjustSize(e.getState());
				}
			});
		}

		private void createClearButton(Composite parent) {
			clearButton = new Button(parent, SWT.PUSH);
			clearButton.setText(CompareMessages.CompareWithOtherResourceDialog_clear);
			clearButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					clearResource();
				}
			});
		}

		public void setText(String text) {
			expandable.setText(text);
			group.setText(text);
		}

		public void setLayoutData(GridData layoutData) {
			expandable.setLayoutData(layoutData);
		}
	}
	
	private class ExternalResourcesProject {
		
		// Implementation based on org.eclipse.jdt.internal.core.ExternalFoldersManager
		
		private int counter = 0;
		
		private static final String TMP_PROJECT_NAME = ".org.eclipse.compare.tmp"; //$NON-NLS-1$
		
		private final static String TMP_PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<projectDescription>\n" //$NON-NLS-1$
				+ "\t<name>" + TMP_PROJECT_NAME + "\t</name>\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\t<comment></comment>\n" //$NON-NLS-1$
				+ "\t<projects>\n" //$NON-NLS-1$
				+ "\t</projects>\n" //$NON-NLS-1$
				+ "\t<buildSpec>\n" //$NON-NLS-1$
				+ "\t</buildSpec>\n" //$NON-NLS-1$
				+ "\t<natures>\n" + "\t</natures>\n" //$NON-NLS-1$//$NON-NLS-2$
				+ "</projectDescription>"; //$NON-NLS-1$
		
		private final static String TMP_FOLDER_NAME = "tmpFolder"; //$NON-NLS-1$
		
		private ExternalResourcesProject() {
			// nothing to do here
		}
		
		private IProject createTmpProject() throws CoreException {
			IProject project = getTmpProject();
			if (!project.isAccessible()) {
				try {
					IPath stateLocation = CompareUI.getPlugin().getStateLocation();
					if (!project.exists()) {
						IProjectDescription desc = project.getWorkspace()
								.newProjectDescription(project.getName());
						desc.setLocation(stateLocation.append(TMP_PROJECT_NAME));
						project.create(desc, null);
					}
					try {
						project.open(null);
					} catch (CoreException e) { // in case .project file or folder has been deleted
						IPath projectPath = stateLocation.append(TMP_PROJECT_NAME);
						projectPath.toFile().mkdirs();
						FileOutputStream output = new FileOutputStream(
								projectPath.append(".project").toOSString()); //$NON-NLS-1$
						try {
							output.write(TMP_PROJECT_FILE.getBytes());
						} finally {
							output.close();
						}
						project.open(null);
					}
					getTmpFolder(project);
				} catch (IOException ioe) {
					return project;
				} catch (CoreException ce) {
					throw new CoreException(ce.getStatus());
				}
			}
			project.setHidden(true);
			return project;
		}
		
		private IFolder getTmpFolder(IProject project) throws CoreException {
			IFolder folder = project.getFolder(TMP_FOLDER_NAME);
			if (!folder.exists())
				folder.create(IResource.NONE, true, null);
			return folder;
		}
		
		private IFile getExternalFile() {
			FileDialog dialog = new FileDialog(getShell());
			String path = dialog.open();
			if (path != null)
				return (IFile) linkResource(new Path(path));
			return null;
		}

		private IFolder getExternalFolder() {
			DirectoryDialog dialog = new DirectoryDialog(getShell());
			String path = dialog.open();
			if (path != null)
				return (IFolder) linkResource(new Path(path));
			return null;
		}
		
		private IResource linkResource(IPath path) {
			IResource r = null;
			String resourceName = path.lastSegment();
			try {
				IProject project = createTmpProject();
				if (!project.isOpen())
					project.open(null);
				if (path.toFile().isFile()) {
					r = getTmpFolder(project).getFile(resourceName);
					if (r.exists()) { 	// add a number to file's name when there already is a file with that name in a folder
						String extension = path.getFileExtension();
						String fileName = path.removeFileExtension().lastSegment(); 
						r = getTmpFolder(project).getFile(getName(fileName, extension));
					}
					((IFile)r).createLink(path, IResource.REPLACE, null);
				} else { // isDirectory
					r = getTmpFolder(project).getFolder(resourceName);
					if (r.exists()) {
						r = getTmpFolder(project).getFolder(getName(resourceName, null));
					}
					((IFolder)r).createLink(path, IResource.REPLACE, null);
				}
			} catch (CoreException e) {
				CompareUIPlugin.log(e);
				MessageDialog.openError(getShell(),
								CompareMessages.CompareWithOtherResourceDialog_externalFile_errorTitle,
								CompareMessages.CompareWithOtherResourceDialog_externalFile_errorMessage);
			}
			return r;
		}
		
		/**
		 * This method is used to prevent duplicating names of linked resources.
		 * It adds a suffix based on the <code>counter</code> value.
		 * 
		 * @param name
		 * @param extension optional
		 * @return
		 */
		private String getName(String name, String extension) {
			if (counter != 0) {
				name = name + "-" + counter; //$NON-NLS-1$
			}
			// at most 3 resources at the same time with the same name:
			// left, right, ancestor
			counter = (counter + 1) % 3;
			if (extension != null) {
				name += "." + extension; //$NON-NLS-1$
			}
			// don't change the name if counter equals 0
			return name;
		}
		
		private IProject getTmpProject() {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(
					TMP_PROJECT_NAME);
		}
	}
	
	private Button okButton;
	private InternalGroup rightPanel, leftPanel;
	private InternalExpandable ancestorPanel;
	private ISelection selection;
	private ExternalResourcesProject tmpProject = new ExternalResourcesProject();

	/**
	 * Creates the dialog.
	 * 
	 * @param shell
	 *            a shell
	 * @param selection
	 *            if the selection is not null, it will be set as initial files
	 *            for comparison
	 * @since 3.4
	 */
	protected CompareWithOtherResourceDialog(Shell shell, ISelection selection) {
		super(shell);
		setShellStyle(SWT.MODELESS | SWT.RESIZE | SWT.MAX);
		this.selection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	protected Control createDialogArea(Composite parent) {

		Composite mainPanel = new Composite(parent, SWT.NULL);
		mainPanel.setLayout(new GridLayout(1, true));
		mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ancestorPanel = new InternalExpandable(mainPanel);
		ancestorPanel.setText(CompareMessages.CompareWithOtherResourceDialog_ancestor);
		GridData ancestorGD = new GridData(SWT.FILL, SWT.FILL, true, false);
		ancestorPanel.setLayoutData(ancestorGD);

		leftPanel = new InternalGroup(mainPanel);
		leftPanel.setText(CompareMessages.CompareWithOtherResourceDialog_leftPanel);
		leftPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		rightPanel = new InternalGroup(mainPanel);
		rightPanel.setText(CompareMessages.CompareWithOtherResourceDialog_rightPanel);
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		setSelection(selection);
		getShell().setText(CompareMessages.CompareWithOtherResourceDialog_dialogTitle);
		setTitle(CompareMessages.CompareWithOtherResourceDialog_dialogMessage);
		adjustSize(ancestorPanel.expandable.isExpanded());

		return mainPanel;
	}

	private void adjustSize(boolean expanded) {
		int minWidth = convertHorizontalDLUsToPixels(MIN_WIDTH);
		int minHeight = convertVerticalDLUsToPixels(expanded ? MIN_HEIGHT_WITH_ANCESTOR
				: MIN_HEIGHT_WITHOUT_ANCESTOR);
		getShell().setMinimumSize(minWidth, minHeight);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		okButton = getButton(IDialogConstants.OK_ID);
		updateErrorInfo();
		setMessage(CompareMessages.CompareWithOtherResourceDialog_info);
	}

	private void setSelection(ISelection selection) {
		IResource[] selectedResources = Utilities.getResources(selection);
		switch (selectedResources.length) {
		case 1:
			leftPanel.workspaceContent.setResource(selectedResources[0]);
			break;
		case 2:
			leftPanel.workspaceContent.setResource(selectedResources[0]);
			rightPanel.workspaceContent.setResource(selectedResources[1]);
			break;
		case 3:
			ancestorPanel.workspaceContent.setResource(selectedResources[0]);
			ancestorPanel.expandable.setExpanded(true);
			leftPanel.workspaceContent.setResource(selectedResources[1]);
			rightPanel.workspaceContent.setResource(selectedResources[2]);
			break;
		}
		setInitialContentTypes();
	}

	private void setInitialContentTypes() {
		ancestorPanel.setContentType(InternalSection.WORKSPACE);
		leftPanel.setContentType(InternalSection.WORKSPACE);
		rightPanel.setContentType(InternalSection.WORKSPACE);
	}

	private boolean isComparePossible() {
		IResource[] resources;
		if (ancestorPanel.getResource() == null)
			resources = new IResource[] { leftPanel.getResource(),
					rightPanel.getResource() };
		else
			resources = new IResource[] { ancestorPanel.getResource(),
					leftPanel.getResource(), rightPanel.getResource() };

		ResourceCompareInput r = new ResourceCompareInput(
				new CompareConfiguration());
		return r.isEnabled(new StructuredSelection(resources));
	}

	private void updateErrorInfo() {
		if (okButton != null) {
			if (leftPanel.getResource() == null
					|| rightPanel.getResource() == null) {
				setMessage(CompareMessages.CompareWithOtherResourceDialog_error_empty,
						IMessageProvider.ERROR);
				okButton.setEnabled(false);
			} else if (!isComparePossible()) {
				setMessage(
						CompareMessages.CompareWithOtherResourceDialog_error_not_comparable,
						IMessageProvider.ERROR);
				okButton.setEnabled(false);
			} else {
				setMessage(CompareMessages.CompareWithOtherResourceDialog_info);
				okButton.setEnabled(true);
			}
		}
	}
	
	/**
	 * Returns table with selected resources. If any resource wasn't chosen in
	 * the ancestor panel, table has only two elements -- resources chosen in
	 * left and right panel. In the other case table contains all three
	 * resources.
	 * 
	 * @return table with selected resources
	 */
	public IResource[] getResult() {
		IResource[] resources;
		IResource rightResource = rightPanel.getResource();
		IResource leftResource = leftPanel.getResource();
		IResource ancestorResource = ancestorPanel.getResource();
		if (ancestorResource == null)
			resources = new IResource[] { leftResource, rightResource };
		else
			resources = new IResource[] { ancestorResource, leftResource,
					rightResource };
		return resources;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		String sectionName = getClass().getName() + "_dialogBounds"; //$NON-NLS-1$
		IDialogSettings settings = CompareUIPlugin.getDefault()
				.getDialogSettings();
		IDialogSettings section = settings.getSection(sectionName);
		if (section == null)
			section = settings.addNewSection(sectionName);
		return section;
	}

}
