/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Eugene Kuleshov <eu@md.pp.ru> - Bug 173959 add mechanism for navigating from team annotation to corresponding task
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.AnnotateListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * An operation to fetch the annotations for a file from the repository and
 * display them in the annotations view.
 */
public class ShowAnnotationOperation extends CVSOperation {
	
	private final ICVSResource fCVSResource;
	private final String fRevision;
	private final boolean fBinary;

	public ShowAnnotationOperation(IWorkbenchPart part, ICVSResource cvsResource, String revision, boolean binary) {
		super(part);
		fCVSResource= cvsResource;
		fRevision= revision;
		fBinary = binary;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		
		monitor.beginTask(null, 100);

		// Get the annotations from the repository.
		final AnnotateListener listener= new AnnotateListener();
		fetchAnnotation(listener, fCVSResource, fRevision, Policy.subMonitorFor(monitor, 80));

		// this is not needed if there is no live annotate
		final RevisionInformation information = createRevisionInformation(listener, Policy.subMonitorFor(monitor, 20));

		// Open the view and display it from the UI thread.
		final Display display= getPart().getSite().getShell().getDisplay();
		display.asyncExec(() -> {
			try {
				// is there an open editor for the given input? If yes, use live annotate
				final AbstractDecoratedTextEditor editor = getEditor(listener);
				if (editor != null) {
					editor.showRevisionInformation(information, "org.eclipse.quickdiff.providers.CVSReferenceProvider"); //$NON-NLS-1$
					final IWorkbenchPage page = getPart().getSite().getPage();
					showHistoryView(page, editor);
					page.activate(editor);
				}
			} catch (PartInitException e) {
				CVSException.wrapException(e);
			}
		});
		
		monitor.done();
	}
	
	/**
	 * Shows the history view, creating it if necessary, but does not give it focus.
	 * 
	 * @param page the workbench page to operate in
	 * @param editor the editor that is showing the file
	 * @return the history view
	 * @throws PartInitException
	 */
	private IHistoryView showHistoryView(IWorkbenchPage page, AbstractDecoratedTextEditor editor) throws PartInitException {
		Object object = fCVSResource.getIResource();
		if (object == null)
			object = editor.getEditorInput();
		IHistoryView historyView= TeamUI.showHistoryFor(page, object, null);
		IHistoryPage historyPage = historyView.getHistoryPage();
		if (historyPage instanceof CVSHistoryPage){
			CVSHistoryPage cvsHistoryPage = (CVSHistoryPage) historyPage;
			cvsHistoryPage.setMode(CVSHistoryPage.REMOTE_MODE);
			// We need to call link to ensure that the history page gets linked
			// even if the page input did not change
			cvsHistoryPage.linkWithEditor();
		}
		return historyView;
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.ShowAnnotationOperation_taskName;
	}

	protected boolean hasCharset(ICVSResource cvsResource, InputStream contents) {
		try {
			return TeamPlugin.getCharset(cvsResource.getName(), contents) != null;
		} catch (IOException e) {
			// Assume that the contents do have a charset
			return true;
		}
	}

	@SuppressWarnings("resource")
	private AbstractDecoratedTextEditor getEditor(AnnotateListener listener) throws PartInitException {
		IResource resource= fCVSResource.getIResource();
		if (resource instanceof IFile){
			return RevisionAnnotationController.openEditor(getPart().getSite().getPage(), (IFile)resource);
		}
		if (fCVSResource instanceof ICVSRemoteResource) {
			return RevisionAnnotationController.openEditor(getPart().getSite().getPage(), fCVSResource, new RemoteAnnotationStorage((ICVSRemoteFile)fCVSResource, listener.getContents()));
		}
		return null;
	}

	private void fetchAnnotation(AnnotateListener listener, ICVSResource cvsResource, String revision, IProgressMonitor monitor) throws CVSException {
	
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		
		final ICVSFolder folder = cvsResource.getParent();
		final FolderSyncInfo info = folder.getFolderSyncInfo();
		final ICVSRepositoryLocation location = KnownRepositories.getInstance().getRepository(info.getRoot());
		
		final Session session = new Session(location, folder, true /*output to console*/);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			final Command.QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
			try {
				CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
				List<Object> localOptions = new ArrayList<>();
				if (revision != null) {
					localOptions.add(Annotate.makeRevisionOption(revision));
				}
				if (fBinary) {
					localOptions.add(Annotate.FORCE_BINARY_ANNOTATE);
				}
				final IStatus status = Command.ANNOTATE.execute(session, Command.NO_GLOBAL_OPTIONS, localOptions.toArray(new LocalOption[localOptions.size()]), new ICVSResource[]{cvsResource}, listener, Policy.subMonitorFor(monitor, 90));
				if (status.getCode() == CVSStatus.SERVER_ERROR) {
					throw new CVSServerException(status);
				}
			} finally {
				CVSProviderPlugin.getPlugin().setQuietness(quietness);
				monitor.done();
			}
		} finally {
			session.close();
		}
	}

	private RevisionInformation createRevisionInformation(final AnnotateListener listener, IProgressMonitor monitor) throws CVSException {
		Map<String, ILogEntry> logEntriesByRevision = new HashMap<>();
		if (fCVSResource instanceof ICVSFile) {
			try {
				ILogEntry[] logEntries= ((ICVSFile) fCVSResource).getLogEntries(monitor);
				for (ILogEntry entry : logEntries) {
					logEntriesByRevision.put(entry.getRevision(), entry);
				}
			} catch (CVSException e) {
				throw e;
			} catch (TeamException e) {
				// XXX why does getLogEntries throw TeamException?
				throw CVSException.wrapException(e);
			}
		}

		final CommitterColors colors= CommitterColors.getDefault();
		RevisionInformation info= new RevisionInformation();

		class AnnotationControlCreator implements IInformationControlCreator {
			private final boolean isResizable;

			public AnnotationControlCreator(boolean isResizable) {
				this.isResizable= isResizable;
			}

			@Override
			public IInformationControl createInformationControl(Shell parent) {
				IInformationPresenter presenter = (display, hoverInfo, presentation, maxWidth, maxHeight) -> {

					// decorate header
					StyleRange styleRange = new StyleRange();
					styleRange.start = 0;
					styleRange.length = hoverInfo.indexOf('\n');
					styleRange.fontStyle = SWT.BOLD;
					presentation.addStyleRange(styleRange);

					return hoverInfo;
				};
				if (isResizable)
					return new DefaultInformationControl(parent, (ToolBarManager) null, presenter);
				else
					return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString(), presenter);
			}
		}

		info.setHoverControlCreator(new AnnotationControlCreator(false));
		info.setInformationPresenterControlCreator(new AnnotationControlCreator(true));
		
		HashMap<String, Revision> sets = new HashMap<>();
		List annotateBlocks= listener.getCvsAnnotateBlocks();
		for (Iterator blocks= annotateBlocks.iterator(); blocks.hasNext();) {
			final CVSAnnotateBlock block= (CVSAnnotateBlock) blocks.next();
			final String revisionString= block.getRevision();
			Revision revision= sets.get(revisionString);
			if (revision == null) {
				final ILogEntry entry= logEntriesByRevision.get(revisionString);
				if (entry == null)
					continue;
				
				revision= new Revision() {
					private String fCommitter= null;
					
					@Override
					public Object getHoverInfo() {
						return entry.getAuthor()
								+ " " + entry.getRevision() + " " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(entry.getDate()) //$NON-NLS-1$ //$NON-NLS-2$
								+ "\n\n" + entry.getComment(); //$NON-NLS-1$
					}
					
					private String getCommitterId() {
						if (fCommitter == null)
							fCommitter= block.toString().substring(0, block.toString().indexOf(' '));
						return fCommitter;
					}
					
					@Override
					public String getId() {
						return revisionString;
					}
					
					@Override
					public Date getDate() {
						return entry.getDate();
					}
					
					@Override
					public RGB getColor() {
						return colors.getCommitterRGB(getCommitterId());
					}

					@Override
					public String getAuthor() {
						return getCommitterId();
					}
				};
				sets.put(revisionString, revision);
				info.addRevision(revision);
			}
			revision.addRange(new LineRange(block.getStartLine(), block.getEndLine() - block.getStartLine() + 1));
		}
		
		return info;
	}
}
