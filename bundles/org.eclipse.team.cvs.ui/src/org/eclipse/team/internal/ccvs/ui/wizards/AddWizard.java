/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.IFileContentManager;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.AddOperation;
import org.eclipse.ui.PlatformUI;

public class AddWizard extends ResizableWizard {

	private final AddOperation op;
	private final IFile[] unknowns;
	private CommitWizardFileTypePage fFileTypePage;

	public static void run(Shell shell, final AddOperation op) throws InvocationTargetException, InterruptedException {
		// Prompt if there are files of unknown type being added
		PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
			try {
				op.buildScope(monitor);
			} catch (CVSException e) {
				throw new InvocationTargetException(e);
			}
		});
		
		IFile[] unknowns = getUnaddedWithUnknownFileType(op.getTraversals());
		if (unknowns.length == 0) {
			op.run();
		} else {
			AddWizard wizard = new AddWizard(op, unknowns);
			ResizableWizard.open(shell, wizard);
		}
	}
	
	private static IFile[] getUnaddedWithUnknownFileType(final ResourceTraversal[] traversals) throws InvocationTargetException, InterruptedException {
		final List<IResource> unadded = new ArrayList<>();
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
			final IFileContentManager manager = Team.getFileContentManager();
			for (ResourceTraversal traversal : traversals) {
				IResource[] resources = traversal.getResources();
				for (IResource resource : resources) {
					try {
						resource.accept((IResourceVisitor) resource1 -> {
							if (resource1.getType() == IResource.FILE) {
								ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile) resource1);
								if (!file.isManaged()) {
									if (!file.isIgnored() || file.equals(resource1)) {
										final String extension = ((IFile) resource1).getFileExtension();
										if (manager.getType((IFile) resource1) == Team.UNKNOWN) {
											if (extension != null && !manager.isKnownExtension(extension)) {
												unadded.add(resource1);
											} else {
												final String name = file.getName();
												if (extension == null && name != null && !manager.isKnownFilename(name))
													unadded.add(resource1);
											}
										}
									}
								}
							}
							return true;
						}, traversal.getDepth(), false);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			}
		});
		return unadded.toArray(new IFile[unadded.size()]);
	}
	
	public AddWizard(AddOperation op, IFile[] unknowns) {
		super("AddWizard", CVSUIPlugin.getPlugin().getDialogSettings()); //$NON-NLS-1$
		this.op = op;
		this.unknowns = unknowns;
		setWindowTitle(CVSUIMessages.AddWizard_0); 
		setDefaultPageImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION));
	}
	
	@Override
	public void addPages() {
		
		final Collection<String> names= new HashSet<>();
		final Collection<String> extensions= new HashSet<>();
		getUnknownNamesAndExtension(unknowns, names, extensions);
		
		if (names.size() + extensions.size() > 0) {
			fFileTypePage= new CommitWizardFileTypePage(extensions, names); 
			addPage(fFileTypePage);
		}
		
		super.addPages();
	}
	
	private static void getUnknownNamesAndExtension(IFile[] files, Collection<String> names, Collection<String> extensions) {
		
		final IFileContentManager manager= Team.getFileContentManager();
		
		for (IFile file : files) {
			final String extension= file.getFileExtension();
			if (extension != null && !manager.isKnownExtension(extension)) {
				extensions.add(extension);
			}
			
			final String name= file.getName();
			if (extension == null && name != null && !manager.isKnownFilename(name))
				names.add(name);
		}
	}
	
	@Override
	public boolean performFinish() {
		final Map extensionsToSave= new HashMap();
		final Map extensionsNotToSave= new HashMap();
		
		fFileTypePage.getModesForExtensions(extensionsToSave, extensionsNotToSave);
		CommitWizardFileTypePage.saveExtensionMappings(extensionsToSave);
		op.addModesForExtensions(extensionsNotToSave);
		
		final Map namesToSave= new HashMap();
		final Map namesNotToSave= new HashMap();
		
		fFileTypePage.getModesForNames(namesToSave, namesNotToSave);
		CommitWizardFileTypePage.saveNameMappings(namesToSave);
		op.addModesForNames(namesNotToSave);
		
		try {
			op.run();
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		
		return super.performFinish();
	}
	
}
