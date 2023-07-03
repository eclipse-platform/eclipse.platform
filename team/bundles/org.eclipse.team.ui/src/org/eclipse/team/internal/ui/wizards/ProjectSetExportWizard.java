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
package org.eclipse.team.internal.ui.wizards;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.UIProjectSetSerializationContext;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProjectSetExportWizard extends Wizard implements IExportWizard {
	ExportProjectSetMainPage mainPage;
	ExportProjectSetLocationPage locationPage;
	IStructuredSelection selection;

	public ProjectSetExportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(TeamUIMessages.ProjectSetExportWizard_Project_Set_1);
	}

	@Override
	public void addPages() {
		mainPage = new ExportProjectSetMainPage("projectSetMainPage", TeamUIMessages.ProjectSetExportWizard_Export_a_Project_Set_3, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_PROJECTSET_EXPORT_BANNER)); //$NON-NLS-1$
		IProject[] projects = (IProject[])selection.toList().toArray(new IProject[0]);
		addPage(mainPage);
		mainPage.setSelectedProjects(projects);
		locationPage = new ExportProjectSetLocationPage("projectSetLocationPage", TeamUIMessages.ProjectSetExportWizard_Export_a_Project_Set_3, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_PROJECTSET_EXPORT_BANNER)); //$NON-NLS-1$
		addPage(locationPage);
	}

	@Override
	public boolean performFinish() {
		final boolean[] result = new boolean[] {false};
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					String filename = locationPage.getFileName();
					Path path = new Path(filename);
					if (path.getFileExtension() == null) {
						filename = filename + ".psf"; //$NON-NLS-1$
					}
					PsfFilenameStore.getInstance().remember(filename);
					File file = new File(filename);
					File parentFile = file.getParentFile();
					if (parentFile != null && !parentFile.exists()) {
						boolean r = MessageDialog.openQuestion(getShell(), TeamUIMessages.ProjectSetExportWizard_Question_4, TeamUIMessages.ProjectSetExportWizard_Target_directory_does_not_exist__Would_you_like_to_create_it__5); //
						if (!r) {
							result[0] = false;
							return;
						}
						r = parentFile.mkdirs();
						if (!r) {
							MessageDialog.openError(getShell(), TeamUIMessages.ProjectSetExportWizard_Export_Problems_6, TeamUIMessages.ProjectSetExportWizard_An_error_occurred_creating_the_target_directory_7); //
							result[0] = false;
							return;
						}
					}
					if (file.exists() && file.isFile()) {
						boolean r = MessageDialog.openQuestion(getShell(), TeamUIMessages.ProjectSetExportWizard_Question_8, TeamUIMessages.ProjectSetExportWizard_Target_already_exists__Would_you_like_to_overwrite_it__9); //
						if (!r) {
							result[0] = false;
							return;
						}
					}

					IWorkingSet[] workingSets = null;
					if (mainPage.exportWorkingSets.getSelection()){
						workingSets = mainPage.getSelectedWorkingSets();
					}
					// Hash the projects by provider
					IProject[] projects = mainPage.getSelectedProjects();
					Map<String, Set<IProject>> map = new HashMap<>();
					for (IProject project : projects) {
						RepositoryProvider provider = RepositoryProvider.getProvider(project);
						if (provider != null) {
							String id = provider.getID();
							Set<IProject> list = map.get(id);
							if (list == null) {
								list = new TreeSet<>((o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
								map.put(id, list);
							}
							list.add(project);
						}
					}


					UIProjectSetSerializationContext context = new UIProjectSetSerializationContext(getShell(), filename);

					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) { //$NON-NLS-1$
						// if file was written to the workspace, perform the validateEdit
						if (!locationPage.isSaveToFileSystem())
							locationPage.validateEditWorkspaceFile(getShell());

						//
						XMLMemento xmlMemento = getXMLMementoRoot();
						Iterator it = map.keySet().iterator();
						monitor.beginTask(null, 1000 * map.size());
						while (it.hasNext()) {
							String id = (String)it.next();
							IMemento memento = xmlMemento.createChild("provider"); //$NON-NLS-1$
							memento.putString("id", id); //$NON-NLS-1$
							Set<IProject> list = map.get(id);
							IProject[] projectArray = list.toArray(new IProject[list.size()]);
							RepositoryProviderType providerType = RepositoryProviderType.getProviderType(id);
							ProjectSetCapability serializer = providerType.getProjectSetCapability();
							ProjectSetCapability.ensureBackwardsCompatible(providerType, serializer);
							if (serializer != null) {
								String[] references = serializer.asReference(projectArray, context, SubMonitor.convert(monitor, 990));
								for (String reference : references) {
									IMemento proj = memento.createChild("project"); //$NON-NLS-1$
									proj.putString("reference", reference); //$NON-NLS-1$
								}
							}
						}
						if (workingSets != null){
							for (IWorkingSet workingSet : workingSets) {
								IMemento memento =xmlMemento.createChild("workingSets"); //$NON-NLS-1$
								workingSet.saveState(memento);
							}
						}
						xmlMemento.save(writer);
						result[0] = true;
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}

					// if file was written to the workspace, refresh it
					if (!locationPage.isSaveToFileSystem())
						try {
							locationPage.refreshWorkspaceFile(monitor);
						} catch (CoreException e) {
							//throw away
						}

					// notify provider types of the project set write
					for (String id : map.keySet()) {
						RepositoryProviderType type = RepositoryProviderType.getProviderType(id);
						if (type != null) {
							ProjectSetCapability capability = type.getProjectSetCapability();
							if (capability != null) {
								capability.projectSetCreated(file, context, SubMonitor.convert(monitor, 10));
							}
						}
					}

					monitor.done();
				}

				private XMLMemento getXMLMementoRoot() {
					Document document;
					try {
						document = DocumentBuilderFactory.newInstance()
								.newDocumentBuilder().newDocument();
						Element element = document.createElement("psf"); //$NON-NLS-1$
						element.setAttribute("version", "2.0"); //$NON-NLS-1$ //$NON-NLS-2$
						document.appendChild(element);
						return new XMLMemento(document, element);
					} catch (ParserConfigurationException e) {
						throw new Error(e.getMessage());
					}
				}

			});
		} catch (InterruptedException e) {
			return true;
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof TeamException) {
				ErrorDialog.openError(getShell(), null, null, ((TeamException)target).getStatus());
				return false;
			}
			if (target instanceof RuntimeException) {
				throw (RuntimeException)target;
			}
			if (target instanceof Error) {
				throw (Error)target;
			}
		}
		return result[0];
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
