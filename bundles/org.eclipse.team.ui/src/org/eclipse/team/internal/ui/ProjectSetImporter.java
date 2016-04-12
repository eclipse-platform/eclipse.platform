/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.*;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.ui.*;

public class ProjectSetImporter {

	/**
	 * Imports a psf file based on a file content. This may be used when psf
	 * file is imported from any other location that local filesystem.
	 *
	 * @param psfContents
	 *            the content of the psf file.
	 * @param filename
	 *            the name of the source file. This is included in case the
	 *            provider needs to deduce relative paths
	 * @param shell
	 * @param monitor
	 * @return list of new projects
	 * @throws InvocationTargetException
	 */
	public static IProject[] importProjectSetFromString(String psfContents,
			String filename, Shell shell, IProgressMonitor monitor)
			throws InvocationTargetException {
		XMLMemento xmlMemento = stringToXMLMemento(psfContents);
		return importProjectSet(xmlMemento, filename, shell, monitor);
	}

	/**
	 * Imports a psf file.
	 *
	 * @param filename
	 * @param shell
	 * @param monitor
	 * @return list of new projects
	 * @throws InvocationTargetException
	 */
	public static IProject[] importProjectSet(String filename, Shell shell,
			IProgressMonitor monitor) throws InvocationTargetException {
		XMLMemento xmlMemento = filenameToXMLMemento(filename);
		return importProjectSet(xmlMemento, filename, shell, monitor);
	}

	private static IProject[] importProjectSet(XMLMemento xmlMemento,
			String filename, Shell shell, IProgressMonitor monitor)
			throws InvocationTargetException {
		try {
			String version = xmlMemento.getString("version"); //$NON-NLS-1$

			List newProjects = new ArrayList();
			if (version.equals("1.0")){ //$NON-NLS-1$
				IProjectSetSerializer serializer = Team.getProjectSetSerializer("versionOneSerializer"); //$NON-NLS-1$
				if (serializer != null) {
					IProject[] projects = serializer.addToWorkspace(new String[0], filename, shell, monitor);
					if (projects != null)
						newProjects.addAll(Arrays.asList(projects));
				}
			} else {
				UIProjectSetSerializationContext context = new UIProjectSetSerializationContext(shell, filename);
				List errors = new ArrayList();
			  	IMemento[] providers = xmlMemento.getChildren("provider"); //$NON-NLS-1$
			  	for (int i = 0; i < providers.length; i++) {
					ArrayList referenceStrings= new ArrayList();
					IMemento[] projects = providers[i].getChildren("project"); //$NON-NLS-1$
					for (int j = 0; j < projects.length; j++) {
						referenceStrings.add(projects[j].getString("reference")); //$NON-NLS-1$
					}
					try {
                        String id = providers[i].getString("id"); //$NON-NLS-1$
                        TeamCapabilityHelper.getInstance().processRepositoryId(id,
                        		PlatformUI.getWorkbench().getActivitySupport());
                        RepositoryProviderType providerType = RepositoryProviderType.getProviderType(id);
                        if (providerType == null) {
                            // The provider type is absent. Perhaps there is another provider that can import this type
                            providerType = TeamPlugin.getAliasType(id);
                        }
                        if (providerType == null) {
                            throw new TeamException(new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, NLS.bind(TeamUIMessages.ProjectSetImportWizard_0, new String[] { id }), null));
                        }
                    	ProjectSetCapability serializer = providerType.getProjectSetCapability();
                    	ProjectSetCapability.ensureBackwardsCompatible(providerType, serializer);
                    	if (serializer != null) {
                    		IProject[] allProjects = serializer.addToWorkspace((String[])referenceStrings.toArray(new String[referenceStrings.size()]), context, monitor);
                    		if (allProjects != null)
                    			newProjects.addAll(Arrays.asList(allProjects));
                    	}
                    } catch (TeamException e) {
                        errors.add(e);
                    }
				}
			  	if (!errors.isEmpty()) {
					TeamException[] exceptions= (TeamException[])errors.toArray(new TeamException[errors.size()]);
					IStatus[] status= new IStatus[exceptions.length];
					for (int i= 0; i < exceptions.length; i++) {
						status[i]= exceptions[i].getStatus();
					}
					throw new TeamException(new MultiStatus(TeamUIPlugin.ID, 0, status, TeamUIMessages.ProjectSetImportWizard_1, null));
				}

			  	//try working sets
			  	IMemento[] sets = xmlMemento.getChildren("workingSets"); //$NON-NLS-1$
			  	IWorkingSetManager wsManager = TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager();
			  	boolean replaceAll = false;
			  	boolean mergeAll = false;
			  	boolean skipAll = false;

			  	for (int i = 0; i < sets.length; i++) {
					IWorkingSet newWs = wsManager.createWorkingSet(sets[i]);
					if (newWs != null) {
						IWorkingSet oldWs = wsManager.getWorkingSet(newWs
								.getName());
						if (oldWs == null) {
							wsManager.addWorkingSet(newWs);
						} else if (replaceAll) {
							replaceWorkingSet(wsManager, newWs, oldWs);
						} else if (mergeAll) {
							mergeWorkingSets(newWs, oldWs);
						} else if (!skipAll) {
							// a working set with the same name has been found
							String title = TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_title;
							String msg = NLS
									.bind(
											TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_message,
											newWs.getName());
							String[] buttons = new String[] {
									TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_replace,
									TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_merge,
									TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_skip,
									IDialogConstants.CANCEL_LABEL };
							final AdviceDialog dialog = new AdviceDialog(
									shell, title, null, msg,
									MessageDialog.QUESTION, buttons, 0);

							shell.getDisplay().syncExec(new Runnable() {
								@Override
								public void run() {
									 dialog.open();
								}
							});

							switch (dialog.getReturnCode()) {
							case 0: // overwrite
								replaceWorkingSet(wsManager, newWs, oldWs);
								replaceAll = dialog.applyToAll;
								break;
							case 1: // combine
								mergeWorkingSets(newWs, oldWs);
								mergeAll = dialog.applyToAll;
								break;
							case 2: // skip
								skipAll = dialog.applyToAll;
								break;
							case 3: // cancel
							default:
								throw new OperationCanceledException();
							}
						}
					}
				}
			}

			return (IProject[]) newProjects.toArray(new IProject[newProjects.size()]);
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		}
	}

	private static XMLMemento filenameToXMLMemento(String filename) throws InvocationTargetException {
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(filename), "UTF-8"); //$NON-NLS-1$
			return XMLMemento.createReadRoot(reader);
		} catch (UnsupportedEncodingException e) {
			throw new InvocationTargetException(e);
		} catch (FileNotFoundException e) {
			throw new InvocationTargetException(e);
		} catch (WorkbenchException e) {
			throw new InvocationTargetException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				}
			}
		}
	}

	private static XMLMemento stringToXMLMemento(String stringContents)
			throws InvocationTargetException {
		StringReader reader = null;
		try {
			reader = new StringReader(stringContents);
			return XMLMemento.createReadRoot(reader);
		} catch (WorkbenchException e) {
			throw new InvocationTargetException(e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Check if given file is a valid psf file
	 *
	 * @param filename
	 * @return <code>true</code> is file is a valid psf file
	 */
	public static boolean isValidProjectSetFile(String filename) {
		try {
			return filenameToXMLMemento(filename).getString("version") != null; //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			return false;
		}
	}

	/**
	 * Check if given string is a valid project set
	 *
	 * @param psfContent
	 * @return <code>true</code> if psfContent is a valid project set
	 */
	public static boolean isValidProjectSetString(String psfContent) {
		if (psfContent == null) {
			return false;
		}
		try {
			return stringToXMLMemento(psfContent).getString("version") != null; //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			return false;
		}
	}

	private static void mergeWorkingSets(IWorkingSet newWs, IWorkingSet oldWs) {
		IAdaptable[] oldElements = oldWs.getElements();
		IAdaptable[] newElements = newWs.getElements();

		Set combinedElements = new HashSet();
		combinedElements.addAll(Arrays.asList(oldElements));
		combinedElements.addAll(Arrays.asList(newElements));

		oldWs.setElements((IAdaptable[]) combinedElements.toArray(new IAdaptable[0]));
	}

	private static void replaceWorkingSet(IWorkingSetManager wsManager, IWorkingSet newWs, IWorkingSet oldWs) {
		if (oldWs != null)
			wsManager.removeWorkingSet(oldWs);
		wsManager.addWorkingSet(newWs);
	}

	private static class AdviceDialog extends MessageDialog {
		boolean applyToAll;
		public AdviceDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		}
		@Override
		protected Control createCustomArea(Composite parent) {
			final Button checkBox = new Button(parent, SWT.CHECK);
			checkBox.setText(TeamUIMessages.ImportProjectSetDialog_duplicatedWorkingSet_applyToAll);
			checkBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					applyToAll = checkBox.getSelection();
				}
			});
			return checkBox;
		}
	}

}
