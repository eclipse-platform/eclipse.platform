/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui.importexport.launchconfigurations;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.dialogs.WizardResourceImportPage;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;

/**
 * This class providers the one and only page for the import launch configurations wizard
 * @since 3.4.0
 */
public class ImportLaunchConfigurationsWizardPage extends WizardResourceImportPage {

	/**
	 * Represents a debug view of the file system, in that we only care about folders and files with the
	 * extension *.launch
	 * @since 3.4.0
	 */
	class DebugFileSystemElement extends FileSystemElement {

		private boolean populated = false;

		public DebugFileSystemElement(String name, FileSystemElement parent, boolean isDirectory) {
			super(name, parent, isDirectory);
		}

		public void setPopulated() {
			populated = true;
		}

		public boolean isPopulated() {
			return populated;
		}

		@Override
		public AdaptableList getFiles() {
			if(!populated) {
				populateElementChildren();
			}
			return super.getFiles();
		}

		@Override
		public AdaptableList getFolders() {
			if(!populated) {
				populateElementChildren();
			}
			return super.getFolders();
		}

		/**
		 * Populates the children of the specified parent <code>FileSystemElement</code>
		 * @param element
		 * @param folderonly
		 */
		private void populateElementChildren() {
			FileSystemStructureProvider provider = FileSystemStructureProvider.INSTANCE;
			List<File> allchildren = provider.getChildren(this.getFileSystemObject());
			DebugFileSystemElement newelement = null;
			for (File child : allchildren) {
				if(child.isFile()) {
					IPath childpath = IPath.fromOSString(child.getAbsolutePath());
					String extension = childpath.getFileExtension();
					if (extension != null && (extension.equals(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION) || extension.equals(ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION))) {
						newelement = new DebugFileSystemElement(provider.getLabel(child), this, provider.isFolder(child));
						newelement.setFileSystemObject(child);
					}
				}
				else {
					newelement = new DebugFileSystemElement(provider.getLabel(child), this, provider.isFolder(child));
					newelement.setFileSystemObject(child);
				}
			}
			setPopulated();
		}
	}

	private String OVERWRITE = "import_config_overwrite"; //$NON-NLS-1$
	private String OLD_PATH = "import_config_oldpath"; //$NON-NLS-1$

	private Text fFromDirectory = null;
	private Button fOverwrite = null;

	/**
	 * Constructor
	 */
	public ImportLaunchConfigurationsWizardPage() {
		super(WizardMessages.ImportLaunchConfigurationsWizardPage_0, new StructuredSelection());
		setTitle(WizardMessages.ImportLaunchConfigurationsWizardPage_0);
		setMessage(WizardMessages.ImportLaunchConfigurationsWizardPage_5);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		createRootDirectoryGroup(comp);
		createFileSelectionGroup(comp);
		IDialogSettings settings = getDialogSettings();
		fOverwrite = SWTFactory.createCheckButton(comp, WizardMessages.ImportLaunchConfigurationsWizardPage_1, null, settings.getBoolean(OVERWRITE), 1);
		String oldpath = settings.get(OLD_PATH);
		oldpath = (oldpath == null ? IInternalDebugCoreConstants.EMPTY_STRING : oldpath);
		fFromDirectory.setText((oldpath == null ? IInternalDebugCoreConstants.EMPTY_STRING : oldpath));
		resetSelection(IPath.fromOSString(oldpath));
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IDebugHelpContextIds.IMPORT_LAUNCH_CONFIGURATIONS_PAGE);
		setPageComplete(false);
		//do not set page complete, Eclipse UI guidelines states wizards cannot start off with an error showing
	}

	/**
	 * Performs the actual work of the wizard page and return is the work was successful
	 * @return true if the import completed normally, false otherwise
	 */
	public boolean finish() {
		IDialogSettings settings = getDialogSettings();
		settings.put(OVERWRITE, fOverwrite.getSelection());
		settings.put(OLD_PATH, fFromDirectory.getText().trim());
		boolean overwrite = fOverwrite.getSelection();
		File config, newconfig = null;
		boolean owall = false, nowall = false;
		MessageDialog dialog = null;
		final List<File> filesToImport = new ArrayList<>();
		for (Object resource :  getSelectedResources()) {
			config = (File) ((DebugFileSystemElement) resource).getFileSystemObject();
			newconfig = new File(
					LaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH.append(config.getName()).toOSString());
			if(newconfig.exists() && !overwrite) {
				if(nowall) {
					continue;
				}
				if(!owall) {
					dialog = new MessageDialog(DebugUIPlugin.getShell(),
							WizardMessages.ExportLaunchConfigurationsWizardPage_11,
							null,
 MessageFormat.format(WizardMessages.ExportLaunchConfigurationsWizardPage_12, new Object[] { config.getName() }),
							MessageDialog.QUESTION, new String[] {WizardMessages.ExportLaunchConfigurationsWizardPage_13, WizardMessages.ExportLaunchConfigurationsWizardPage_14, WizardMessages.ExportLaunchConfigurationsWizardPage_15, WizardMessages.ExportLaunchConfigurationsWizardPage_16, WizardMessages.ExportLaunchConfigurationsWizardPage_17}, 0);
					int ret = dialog.open();
					switch(ret) {
						case 0: {
							filesToImport.add(config);
							break;
						}
						case 1: {
							owall = true;
							filesToImport.add(config);
							break;
						}
						case 3: {
							nowall = true;
							break;
						}
						case 4: {
							return true;
						}
						default:
							break;
					}
				} else if(!nowall) {
					filesToImport.add(config);
				}
			} else {
				filesToImport.add(config);
			}
		}

		if (!filesToImport.isEmpty()) {
			Job job = new Job(WizardMessages.ExportLaunchConfigurationsWizard_0) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					LaunchManager launchManager = (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
					try {
						launchManager.importConfigurations(filesToImport.toArray(new File[filesToImport.size()]), monitor);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
		return true;
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_WIZBAN_IMPORT_CONFIGS);
	}

	@Override
	protected void updateWidgetEnablements() {
		setPageComplete(determinePageCompletion());
	}

	@Override
	protected boolean determinePageCompletion() {
		if(fFromDirectory.getText().trim().equals(IInternalDebugCoreConstants.EMPTY_STRING)) {
			setErrorMessage(WizardMessages.ImportLaunchConfigurationsWizardPage_3);
			return false;
		}
		if (getSelectedResources().size() < 1) {
			setErrorMessage(WizardMessages.ImportLaunchConfigurationsWizardPage_4);
			return false;
		}
		setErrorMessage(null);
		setMessage(WizardMessages.ImportLaunchConfigurationsWizardPage_5);
		return true;
	}

	@Override
	protected void createSourceGroup(Composite parent) {}

	/**
	 *	Create the group for creating the root directory
	 */
	protected void createRootDirectoryGroup(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 3, 1, GridData.FILL_HORIZONTAL, 0, 0);
		SWTFactory.createLabel(comp, WizardMessages.ImportLaunchConfigurationsWizardPage_6, 1);
		// source name entry field
		fFromDirectory = SWTFactory.createText(comp, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		// source browse button
		Button browse = SWTFactory.createPushButton(comp, WizardMessages.ImportLaunchConfigurationsWizardPage_7, null);
		browse.addSelectionListener(new SelectionAdapter () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getContainer().getShell(), SWT.SHEET);
				dd.setText(WizardMessages.ImportLaunchConfigurationsWizardPage_0);
				String filename = dd.open();
				if(filename != null) {
					IPath path = IPath.fromOSString(filename);
					if (path != null) {
						fFromDirectory.setText(path.toString());
						resetSelection(path);
						setPageComplete(determinePageCompletion());
					}
				}
			}
		});
	}

	/**
	 * Resets the selection of the tree root element for the viewer
	 * @param path the path from the text widget
	 */
	protected void resetSelection(final IPath path) {
		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			File file = new File(path.toOSString());
			DebugFileSystemElement dummyparent = new DebugFileSystemElement(IInternalDebugCoreConstants.EMPTY_STRING,
					null, true);
			dummyparent.setPopulated();
			DebugFileSystemElement element = new DebugFileSystemElement(
					FileSystemStructureProvider.INSTANCE.getLabel(file), dummyparent, file.isDirectory());
			element.setFileSystemObject(file);
			element.getFiles();
			selectionGroup.setRoot(dummyparent);
		});
	}

	@Override
	protected ITreeContentProvider getFileProvider() {
		return new WorkbenchContentProvider() {
			@Override
			public Object[] getChildren(Object o) {
				if (o instanceof DebugFileSystemElement) {
					DebugFileSystemElement element = (DebugFileSystemElement) o;
					return element.getFiles().getChildren(element);
				}
				return new Object[0];
			}
		};
	}

	@Override
	protected ITreeContentProvider getFolderProvider() {
		return new WorkbenchContentProvider() {
			@Override
			public Object[] getChildren(Object o) {
				if (o instanceof DebugFileSystemElement) {
					DebugFileSystemElement element = (DebugFileSystemElement) o;
					return element.getFolders().getChildren();
				}
				return new Object[0];
			}

			@Override
			public boolean hasChildren(Object o) {
				if (o instanceof DebugFileSystemElement) {
					DebugFileSystemElement element = (DebugFileSystemElement) o;
					if (element.isPopulated()) {
						return getChildren(element).length > 0;
					}
					//If we have not populated then wait until asked
					return true;
				}
				return false;
			}
		};
	}

}
