/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ModelCompareEditorInput;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchModelCompareEditorInput;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriberMergeContext;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Action to open a compare editor from a SyncInfo object.
 *
 * @see SyncInfoCompareInput
 * @since 3.0
 */
public class OpenInCompareAction extends Action {

	private final ISynchronizePageConfiguration configuration;

	public OpenInCompareAction(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		Utils.initAction(this, "action.openInCompareEditor."); //$NON-NLS-1$
	}

	@Override
	public void run() {
		ISelection selection = configuration.getSite().getSelectionProvider().getSelection();
		if(selection instanceof IStructuredSelection) {
			if (!isOkToRun(selection))
				return;

			boolean reuseEditorIfPossible = ((IStructuredSelection) selection).size()==1;
			for (Object obj : ((IStructuredSelection) selection)) {
				if (obj instanceof SyncInfoModelElement) {
					SyncInfo info = ((SyncInfoModelElement) obj).getSyncInfo();
					if (info != null) {
						// Use the open strategy to decide if the editor or the sync view should have focus
						openCompareEditorOnSyncInfo(configuration, info, !OpenStrategy.activateOnOpen(), reuseEditorIfPossible);
					}
				} else if (obj != null){
					openCompareEditor(configuration, obj, !OpenStrategy.activateOnOpen(), reuseEditorIfPossible);
				}
			}
		}
	}

	private boolean isOkToRun(ISelection selection) {
		// do not open Compare Editor unless all elements have input
		Object[] elements = ((IStructuredSelection) selection).toArray();
		ISynchronizeParticipant participant = configuration
				.getParticipant();
		// model synchronize
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			for (Object element : elements) {
				// TODO: This is inefficient
				if (!msp.hasCompareInputFor(element)) {
					return false;
				}
			}
		} else {
			// all files
			IResource resources[] = Utils.getResources(elements);
			for (IResource resource : resources) {
				if (resource.getType() != IResource.FILE) {
					// Only supported if all the items are files.
					return false;
				}
			}
		}
		return true;
	}

	public static IEditorInput openCompareEditor(ISynchronizePageConfiguration configuration, Object object, boolean keepFocus, boolean reuseEditorIfPossible) {
		Assert.isNotNull(object);
		Assert.isNotNull(configuration);
		ISynchronizeParticipant participant = configuration.getParticipant();
		ISynchronizePageSite site = configuration.getSite();
		if (object instanceof SyncInfoModelElement) {
			SyncInfo info = ((SyncInfoModelElement) object).getSyncInfo();
			if (info != null)
				return openCompareEditorOnSyncInfo(configuration, info, keepFocus, reuseEditorIfPossible);
		}
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			ICompareInput input = msp.asCompareInput(object);
			IWorkbenchPage workbenchPage = getWorkbenchPage(site);
			if (input != null && workbenchPage != null && isOkToOpen(site, participant, input)) {
				if (isApplyPatchModelPresent(configuration))
					return openCompareEditor(workbenchPage, new ApplyPatchModelCompareEditorInput(msp, input, workbenchPage, configuration), keepFocus, site, reuseEditorIfPossible);
				else
					return openCompareEditor(workbenchPage, new ModelCompareEditorInput(msp, input, workbenchPage, configuration), keepFocus, site, reuseEditorIfPossible);
			}
		}
		return null;
	}

	private static boolean isApplyPatchModelPresent(
			ISynchronizePageConfiguration configuration) {
		Object object = configuration.getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
		return object instanceof ApplyPatchSubscriberMergeContext;
	}

	private static boolean isOkToOpen(final ISynchronizePageSite site, final ISynchronizeParticipant participant, final ICompareInput input) {
		if (participant instanceof ModelSynchronizeParticipant && input instanceof ISynchronizationCompareInput) {
			final ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			final boolean[] result = new boolean[] { false };
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
					try {
						result[0] = msp.checkForBufferChange(site.getShell(), (ISynchronizationCompareInput) input,
								true, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				});
			} catch (InvocationTargetException e) {
				Utils.handleError(site.getShell(), e, null, null);
			} catch (InterruptedException e) {
				return false;
			}
			return result[0];
		}
		return true;
	}

	public static CompareEditorInput openCompareEditorOnSyncInfo(ISynchronizePageConfiguration configuration, SyncInfo info, boolean keepFocus, boolean reuseEditorIfPossible) {
		Assert.isNotNull(info);
		Assert.isNotNull(configuration);
		if(info.getLocal().getType() != IResource.FILE) return null;
		SyncInfoCompareInput input = new SyncInfoCompareInput(configuration, info);
		return openCompareEditor(getWorkbenchPage(configuration.getSite()), input, keepFocus, configuration.getSite(), reuseEditorIfPossible);
	}

	public static CompareEditorInput openCompareEditor(ISynchronizeParticipant participant, SyncInfo info, ISynchronizePageSite site) {
		Assert.isNotNull(info);
		Assert.isNotNull(participant);
		if(info.getLocal().getType() != IResource.FILE) return null;
		SyncInfoCompareInput input = new SyncInfoCompareInput(participant, info);
		return openCompareEditor(getWorkbenchPage(site), input, false, site, false);
	}

	private static CompareEditorInput openCompareEditor(
			IWorkbenchPage page,
			CompareEditorInput input,
			boolean keepFocus,
			ISynchronizePageSite site,
			boolean reuseEditorIfPossible) {
		if (page == null)
			return null;

		openCompareEditor(input, page, reuseEditorIfPossible);
		if(site != null && keepFocus) {
			site.setFocus();
		}
		return input;
	}

	private static IWorkbenchPage getWorkbenchPage(ISynchronizePageSite site) {
		IWorkbenchPage page = null;
		if(site == null || site.getWorkbenchSite() == null) {
			IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null)
				page = window.getActivePage();
		} else {
			page = site.getWorkbenchSite().getPage();
		}
		return page;
	}

	public static void openCompareEditor(CompareEditorInput input, IWorkbenchPage page) {
		// try to reuse editors, if possible
		openCompareEditor(input, page, true);
	}

	public static void openCompareEditor(CompareEditorInput input, IWorkbenchPage page, boolean reuseEditorIfPossible) {
		if (page == null || input == null)
			return;
		IEditorPart editor = Utils.findReusableCompareEditor(input, page,
				new Class[] { SyncInfoCompareInput.class,
						ModelCompareEditorInput.class });
		// reuse editor only for single selection
		if(editor != null && reuseEditorIfPossible) {
			IEditorInput otherInput = editor.getEditorInput();
			if(otherInput.equals(input)) {
				// simply provide focus to editor
				page.activate(editor);
			} else {
				// if editor is currently not open on that input either re-use existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor)editor);
				page.activate(editor);
			}
		} else {
			CompareUI.openCompareEditorOnPage(input, page);
		}
	}

	/**
	 * Returns an editor handle if a SyncInfoCompareInput compare editor is opened on
	 * the given IResource.
	 *
	 * @param site the view site in which to search for editors
	 * @param resource the resource to use to find the compare editor
	 * @return an editor handle if found and <code>null</code> otherwise
	 */
	public static IEditorPart findOpenCompareEditor(IWorkbenchPartSite site, IResource resource) {
		IWorkbenchPage page = site.getPage();
		IEditorReference[] editorRefs = page.getEditorReferences();
		for (IEditorReference editorRef : editorRefs) {
			final IEditorPart part = editorRef.getEditor(false /* don't restore editor */);
			if(part != null) {
				IEditorInput input = part.getEditorInput();
				if(part != null && input instanceof SyncInfoCompareInput) {
					SyncInfo inputInfo = ((SyncInfoCompareInput)input).getSyncInfo();
					if(inputInfo.getLocal().equals(resource)) {
						return part;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns an editor handle if a compare editor is opened on
	 * the given object.
	 * @param site the view site in which to search for editors
	 * @param object the object to use to find the compare editor
	 * @param participant
	 * @return an editor handle if found and <code>null</code> otherwise
	 */
	public static IEditorPart findOpenCompareEditor(IWorkbenchPartSite site, Object object, ISynchronizeParticipant participant) {
		if (object instanceof SyncInfoModelElement) {
			SyncInfoModelElement element = (SyncInfoModelElement) object;
			SyncInfo info = element.getSyncInfo();
			return findOpenCompareEditor(site, info.getLocal());
		}
		IWorkbenchPage page = site.getPage();
		IEditorReference[] editorRefs = page.getEditorReferences();
		for (IEditorReference editorRef : editorRefs) {
			final IEditorPart part = editorRef.getEditor(false /* don't restore editor */);
			if(part != null) {
				IEditorInput input = part.getEditorInput();
				if(input instanceof ModelCompareEditorInput) {
					if(((ModelCompareEditorInput)input).matches(object, participant)) {
						return part;
					}
				}
			}
		}
		return null;
	}
}
