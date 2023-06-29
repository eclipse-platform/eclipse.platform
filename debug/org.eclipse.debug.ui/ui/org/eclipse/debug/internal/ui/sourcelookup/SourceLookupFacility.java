/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Freescale - Teodor Madan - Show IP for active frame only (Bug 49730)
 *     RedHat - Andrew Ferrazzutti - Source lookup ignores ISourceLocator if artifact was cached (Bug 436411)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.InstructionPointerManager;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.debug.internal.ui.views.launch.Decoration;
import org.eclipse.debug.internal.ui.views.launch.DecorationManager;
import org.eclipse.debug.internal.ui.views.launch.SourceNotFoundEditorInput;
import org.eclipse.debug.internal.ui.views.launch.StandardDecoration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugEditorPresentation;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IInstructionPointerPresentation;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditorInput;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility methods for looking up and displaying source.
 *
 * @since 3.1
 */
public class SourceLookupFacility implements IPageListener, IPartListener2, IPropertyChangeListener, IDebugEventSetListener {

	/**
	 * Provides an LRU cache with a given max size
	 *
	 * @since 3.10
	 */
	static class LRU extends LinkedHashMap<Object, SourceLookupResult> {
		private static final long serialVersionUID = 1L;

		int fSize;

		/**
		 * Constructor
		 *
		 * @param size The desired size
		 */
		LRU(int size) {
			// true == use this map like LRU cache
			super(size, 0.75f, true);
			fSize = size;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Object, SourceLookupResult> eldest) {
			return size() > fSize;
		}
	}

	/**
	 * Singleton source lookup facility
	 */
	private static SourceLookupFacility fgDefault;

	/**
	 * Contains a map of the editor to use for each workbench page, when the
	 * 'reuse editor' preference is on.
	 */
	private Map<IWorkbenchPage, IEditorPart> fEditorsByPage;

	/**
	 * Contains a mapping of artifacts to the source element that was computed
	 * for them.
	 *
	 * @since 3.10
	 */
	private final Map<Object, SourceLookupResult> fLookupResults = Collections.synchronizedMap(new LRU(10));

	/**
	 * Used to generate annotations for stack frames
	 */
	private IInstructionPointerPresentation fPresentation = (IInstructionPointerPresentation) DebugUITools.newDebugModelPresentation();

	/**
	 * Whether to re-use editors when displaying source.
	 */
	private boolean fReuseEditor = DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_REUSE_EDITOR);

	/** Singleton job to process source lookup requests */
	private final SourceLookupJob sourceLookupJob;

	/**
	 * Constructs singleton source display adapter for stack frames.
	 */
	/**
	 * Returns the source lookup facility
	 *
	 * @return
	 */
	public static SourceLookupFacility getDefault() {
		if (fgDefault == null) {
			fgDefault = new SourceLookupFacility();
		}
		return fgDefault;
	}

	/**
	 * Performs cleanup
	 */
	public static void shutdown() {
		if (fgDefault != null) {
			fgDefault.dispose();
		}
	}

	/**
	 * Constructs a source lookup facility.
	 */
	private SourceLookupFacility() {
		fEditorsByPage = new HashMap<>();
		sourceLookupJob = new SourceLookupJob();
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		IStackFrame frame = null;
		for (DebugEvent event : events) {
			switch (event.getKind()) {
				case DebugEvent.TERMINATE:
				case DebugEvent.RESUME:
					if (!event.isEvaluation()) {
						Job uijob = new UIJob("clear source selection") { //$NON-NLS-1$
							@Override
							public IStatus runInUIThread(IProgressMonitor monitor) {
								clearSourceSelection(event.getSource());
								return Status.OK_STATUS;
							}

						};
						uijob.setSystem(true);
						uijob.schedule();
					}
					break;
				case DebugEvent.CHANGE:
					if (event.getSource() instanceof IStackFrame) {
						if (event.getDetail() == DebugEvent.CONTENT) {
							frame = (IStackFrame) event.getSource();
							fLookupResults.remove(new ArtifactWithLocator(frame, frame.getLaunch().getSourceLocator()));
						}
					}
					break;
				default:
					break;
			}
		}
	}

	private static class ArtifactWithLocator {
		public final Object artifact;
		public final ISourceLocator locator;
		public ArtifactWithLocator(Object artifact, ISourceLocator locator) {
			this.artifact = artifact;
			this.locator = locator;
		}

		@Override
		public int hashCode() {
			return 31 + Objects.hash(artifact, locator);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ArtifactWithLocator)) {
				return false;
			}
			ArtifactWithLocator other = (ArtifactWithLocator) obj;
			return Objects.equals(artifact, other.artifact) && Objects.equals(locator, other.locator);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ArtifactWithLocator ["); //$NON-NLS-1$
			if (artifact != null) {
				builder.append("artifact="); //$NON-NLS-1$
				builder.append(artifact);
				builder.append(", "); //$NON-NLS-1$
			}
			if (locator != null) {
				builder.append("locator="); //$NON-NLS-1$
				builder.append(locator);
			}
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}

	}

	/**
	 * Performs source lookup for the given artifact and returns the result.
	 *
	 * @param artifact object for which source is to be resolved
	 * @param locator the source locator to use, or <code>null</code>. When
	 *            <code>null</code> a source locator is determined from the
	 *            artifact, if possible. If the artifact is a debug element, the
	 *            source locator from its associated launch is used.
	 * @param force If we should ignore the cached value and re-look up
	 * @return a source lookup result
	 */
	public SourceLookupResult lookup(Object artifact, ISourceLocator locator, boolean force) {
		SourceLookupResult result = null;
		ArtifactWithLocator key = new ArtifactWithLocator(artifact, locator);
		if (!force) {
			result = fLookupResults.get(key);
			if (result != null) {
				return result;
			}
		}
		IDebugElement debugElement = null;
		if (artifact instanceof IDebugElement) {
			debugElement = (IDebugElement) artifact;
		}
		ISourceLocator localLocator = locator;
		if (localLocator == null) {
			ILaunch launch = null;
			if (debugElement != null) {
				launch = debugElement.getLaunch();
			}
			if (launch != null) {
				localLocator = launch.getSourceLocator();
			}
		}
		if (localLocator == null) {
			return new SourceLookupResult(artifact, null, null, null);
		}
		String editorId = null;
		IEditorInput editorInput = null;
		Object sourceElement = null;
		if (localLocator instanceof ISourceLookupDirector) {
			ISourceLookupDirector director = (ISourceLookupDirector) localLocator;
			sourceElement = director.getSourceElement(artifact);
		} else {
			if (artifact instanceof IStackFrame) {
				sourceElement = localLocator.getSourceElement((IStackFrame) artifact);
			}
		}
		if (sourceElement == null) {
			if (localLocator instanceof AbstractSourceLookupDirector) {
				editorInput = new CommonSourceNotFoundEditorInput(artifact);
				editorId = IDebugUIConstants.ID_COMMON_SOURCE_NOT_FOUND_EDITOR;
			} else {
				if (artifact instanceof IStackFrame) {
					IStackFrame frame = (IStackFrame) artifact;
					editorInput = new SourceNotFoundEditorInput(frame);
					editorId = IInternalDebugUIConstants.ID_SOURCE_NOT_FOUND_EDITOR;
				}
			}
		} else {
			ISourcePresentation presentation = null;
			if (localLocator instanceof ISourcePresentation) {
				presentation = (ISourcePresentation) localLocator;
			} else {
				if (debugElement != null) {
					presentation = getPresentation(debugElement.getModelIdentifier());
				}
			}
			if (presentation != null) {
				editorInput = presentation.getEditorInput(sourceElement);
			}
			if (editorInput != null && presentation != null) {
				editorId = presentation.getEditorId(editorInput, sourceElement);
			}
		}
		result = new SourceLookupResult(artifact, sourceElement, editorId, editorInput);
		fLookupResults.put(key, result);
		return result;
	}

	/**
	 * Returns the model presentation for the given debug model, or <code>null</code>
	 * if none.
	 *
	 * @param id debug model id
	 * @return presentation for the model, or <code>null</code> if none.
	 */
	protected IDebugModelPresentation getPresentation(String id) {
		return ((DelegatingModelPresentation)DebugUIPlugin.getModelPresentation()).getPresentation(id);
	}

	/**
	 * Returns an editor presentation.
	 *
	 * @return an editor presentation
	 */
	protected IDebugEditorPresentation getEditorPresentation() {
		return (DelegatingModelPresentation)DebugUIPlugin.getModelPresentation();
	}

	/**
	 * Opens an editor in the given workbench page for the given source lookup
	 * result. Has no effect if the result has an unknown editor id or editor input.
	 * The editor is opened, positioned, and annotated.
	 * <p>
	 * Honor's the user preference of whether to re-use editors when displaying source.
	 * </p>
	 * @param result source lookup result to display
	 * @param page the page to display the result in
	 */
	public void display(ISourceLookupResult result, IWorkbenchPage page) {
		IEditorPart editor= openEditor(result, page);
		if (editor == null) {
			return;
		}
		IStackFrame frame = null;
		if (result.getArtifact() instanceof IStackFrame) {
			frame = (IStackFrame) result.getArtifact();
		}
		// position and annotate editor for stack frame
		if (frame != null) {
			IDebugEditorPresentation editorPresentation = getEditorPresentation();
			if (editorPresentation.addAnnotations(editor, frame)) {
				Decoration decoration = new StandardDecoration(editorPresentation, editor, frame.getThread());
				DecorationManager.addDecoration(decoration);
			} else {
				// perform standard positioning and annotations
				ITextEditor textEditor = null;
				if (editor instanceof ITextEditor) {
					textEditor = (ITextEditor)editor;
				} else {
					textEditor = editor.getAdapter(ITextEditor.class);
				}
				if (textEditor != null) {
					positionEditor(textEditor, frame);
					InstructionPointerManager.getDefault().removeAnnotations(textEditor);
					Annotation annotation = fPresentation.getInstructionPointerAnnotation(textEditor, frame);
					if (annotation != null) {
						InstructionPointerManager.getDefault().addAnnotation(textEditor, frame, annotation);
					}
				}
			}
		}
	}

	/**
	 * Opens the editor used to display the source for an element selected in
	 * this view and returns the editor that was opened or <code>null</code> if
	 * no editor could be opened.
	 */
	private IEditorPart openEditor(ISourceLookupResult result, IWorkbenchPage page) {
		IEditorPart editor = null;
		IEditorInput input= result.getEditorInput();
		String id= result.getEditorId();
		if (input == null || id == null) {
			return null;
		}

		if (fReuseEditor) {
			IEditorReference[] references = page.findEditors(input, id, IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT);
			if (references.length > 0) {
				// activate the editor we want to reuse
				IEditorPart refEditor= references[0].getEditor(false);
				editor = refEditor;
				page.bringToTop(editor);
			}
			if (editor == null) {
				IEditorPart editorForPage = getEditor(page);
				if (editorForPage == null || editorForPage.isDirty() || page.isEditorPinned(editorForPage)) {
					// open a new editor
					editor = openEditor(page, input, id);
					editorForPage = editor;
				} else if (canReuseEditor(input, id, editorForPage)) {
					// re-use editor
					page.reuseEditor((IReusableEditor)editorForPage, input);
					editor = editorForPage;
					if(!page.isPartVisible(editor)) {
						page.bringToTop(editor);
					}
				} else {
					// close editor, open a new one
					editor = openEditor(page, input, id);
					if (editor != editorForPage) {
						page.closeEditor(editorForPage, false);
						editorForPage = editor;
					}
				}
				setEditor(page, editorForPage);
			}
		} else {
			// Open a new editor
			editor = openEditor(page, input, id);
		}
		return editor;
	}

	private static boolean canReuseEditor(IEditorInput input, String id, IEditorPart editorForPage) {
		if (!(editorForPage instanceof IReusableEditor)) {
			return false;
		}
		IWorkbenchPartSite site = editorForPage.getSite();
		if (site == null) {
			// editor is disposed
			return false;
		}
		if (site.getId().equals(id)) {
			return true;
		}
		IEditorInput editorInput = editorForPage.getEditorInput();
		return editorInput != null && input.equals(editorInput);
	}

	/**
	 * Positions the text editor for the given stack frame
	 */
	private void positionEditor(ITextEditor editor, IStackFrame frame) {
		try {
			int charStart = frame.getCharStart();
			if (charStart >= 0) {
				editor.selectAndReveal(charStart, 0);
				return;
			}
			int lineNumber = frame.getLineNumber();
			lineNumber--; // Document line numbers are 0-based. Debug line numbers are 1-based.
			IRegion region= getLineInformation(editor, lineNumber);
			if (region != null) {
				editor.selectAndReveal(region.getOffset(), 0);
			}
		} catch (DebugException e) {
		}
	}

	/**
	 * Returns the line information for the given line in the given editor, or
	 * {@code null} if no information could be retrieved or error happens
	 */
	private IRegion getLineInformation(ITextEditor editor, int lineNumber) {
		IDocumentProvider provider= editor.getDocumentProvider();
		if (provider == null) {
			return null;
		}
		IEditorInput input= editor.getEditorInput();
		try {
			provider.connect(input);
		} catch (CoreException e) {
			return null;
		}
		try {
			IDocument document= provider.getDocument(input);
			if (document != null) {
				return document.getLineInformation(lineNumber);
			}
		} catch (BadLocationException e) {
		} finally {
			provider.disconnect(input);
		}
		return null;
	}
	/**
	 * Opens an editor in the workbench and returns the editor that was opened
	 * or <code>null</code> if an error occurred while attempting to open the
	 * editor.
	 */
	private IEditorPart openEditor(final IWorkbenchPage page, final IEditorInput input, final String id) {
		final IEditorPart[] editor = new IEditorPart[] {null};
		Runnable r = () -> {
			if (!isClosing(page)) {
				try {
					editor[0] = page.openEditor(input, id, false, IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT);
				} catch (PartInitException e) {
					DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), DebugUIViewsMessages.LaunchView_Error_1,
							DebugUIViewsMessages.LaunchView_Exception_occurred_opening_editor_for_debugger__2, e);
				}
			}
		};
		BusyIndicator.showWhile(DebugUIPlugin.getStandardDisplay(), r);
		return editor[0];
	}

	private boolean isClosing(final IWorkbenchPage page) {
		IWorkbenchWindow pageWindow = page.getWorkbenchWindow();

		boolean isWorkbenchClosing = pageWindow.getWorkbench().isClosing();
		if (isWorkbenchClosing) {
			return true;
		}

		boolean isWorkbenchPageWindowClosing = pageWindow.isClosing();
		if (isWorkbenchPageWindowClosing) {
			return true;
		}

		return false;
	}

	@Override
	public void pageActivated(IWorkbenchPage page) {
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		fEditorsByPage.remove(page);
		page.removePartListener(this);
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// clear the cached editor for the page if it has been closed
		IWorkbenchPage page = partRef.getPage();
		IEditorPart editor = getEditor(page);
		IWorkbenchPart part = partRef.getPart(false);
		if (part != null && part.equals(editor)) {
			fEditorsByPage.remove(page);
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(IDebugUIConstants.PREF_REUSE_EDITOR)) {
			fReuseEditor = DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_REUSE_EDITOR);
		}
	}

	/**
	 * Returns the editor to use to display source in the given page, or
	 * <code>null</code> if a new editor should be opened.
	 *
	 * @param page workbench page
	 * @return the editor to use to display source in the given page, or
	 * <code>null</code> if a new editor should be opened
	 */
	protected IEditorPart getEditor(IWorkbenchPage page) {
		return fEditorsByPage.get(page);
	}

	/**
	 * Sets the editor to use to display source in the given page, or
	 * <code>null</code> if a new editor should be opened.
	 *
	 * @param page workbench page
	 * @return the editor to use to display source in the given page, or
	 * <code>null</code> if a new editor should be opened
	 */
	protected void setEditor(IWorkbenchPage page, IEditorPart editorPart) {
		if (editorPart == null) {
			fEditorsByPage.remove(page);
		} else {
			fEditorsByPage.put(page, editorPart);
		}
		page.addPartListener(this);
		page.getWorkbenchWindow().addPageListener(this);
	}

	/**
	 * Performs cleanup.
	 */
	protected void dispose() {
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		DebugPlugin.getDefault().removeDebugEventListener(this);
		fEditorsByPage.clear();
		fPresentation.dispose();
		fLookupResults.clear();
		sourceLookupJob.cancel();
	}

	/**
	 * A singleton job to perform source lookups via given {@link SourceLookupTask}
	 * objects. The tasks are put in the queue to process in the background,
	 * duplicated tasks are ignored. Job re-schedules itself if new task is added to
	 * the queue.
	 */
	final class SourceLookupJob extends Job {

		private final LinkedHashSet<SourceLookupTask> queue;
		private final SourceDisplayJob sourceDisplayJob;

		public SourceLookupJob() {
			super("Debug Source Lookup"); //$NON-NLS-1$
			this.sourceDisplayJob = new SourceDisplayJob();
			this.queue = new LinkedHashSet<>();
			setSystem(true);
			setPriority(Job.INTERACTIVE);
			// Note: Be careful when trying to use scheduling rules with this
			// job, in order to avoid blocking nested jobs (bug 339542).
		}

		@Override
		public boolean belongsTo(Object family) {
			return family instanceof SourceLookupFacility;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SourceLookupTask next;
			while ((next = poll()) != null && !monitor.isCanceled()) {
				SourceDisplayRequest uiTask = next.run(monitor);
				if (uiTask != null) {
					sourceDisplayJob.schedule(uiTask);
				}
			}

			synchronized (queue) {
				if (monitor.isCanceled()) {
					queue.clear();
					return Status.CANCEL_STATUS;
				} else if (!queue.isEmpty()) {
					schedule(100);
				}
			}
			return Status.OK_STATUS;
		}

		private SourceLookupTask poll() {
			SourceLookupTask next = null;
			synchronized (queue) {
				if (!queue.isEmpty()) {
					Iterator<SourceLookupTask> iterator = queue.iterator();
					next = iterator.next();
					iterator.remove();
				}
			}
			return next;
		}

		void schedule(SourceLookupTask task) {
			synchronized (queue) {
				boolean added = queue.add(task);
				if (added) {
					schedule(100);
				}
			}
		}
	}

	/**
	 * A task to perform source lookup on the currently selected stack frame.
	 */
	class SourceLookupTask {

		final IStackFrame fTarget;
		final ISourceLocator fLocator;
		final IWorkbenchPage fPage;
		final boolean fForce;

		/**
		 * Constructs a new source lookup task.
		 */
		public SourceLookupTask(IStackFrame frame, ISourceLocator locator, IWorkbenchPage page, boolean force) {
			fTarget = frame;
			fLocator = locator;
			fPage = page;
			fForce = force;
		}

		protected SourceDisplayRequest run(IProgressMonitor monitor) {
			if (!monitor.isCanceled()) {
				if (!fTarget.isTerminated()) {
					SourceLookupResult result = lookup(fTarget, fLocator, fForce);
					if (!monitor.isCanceled() && !fTarget.isTerminated() && fPage != null && result != null) {
						return new SourceDisplayRequest(result, fPage);
					}
				}
			}
			return null;
		}

		@Override
		public int hashCode() {
			return 31 + Objects.hash(fForce, fLocator, fPage, fTarget);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof SourceLookupTask)) {
				return false;
			}
			SourceLookupTask other = (SourceLookupTask) obj;
			return fForce == other.fForce && Objects.equals(fPage, other.fPage)
					&& Objects.equals(fLocator, other.fLocator) && Objects.equals(fTarget, other.fTarget);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SourceLookupTask ["); //$NON-NLS-1$
			if (fTarget != null) {
				builder.append("target="); //$NON-NLS-1$
				builder.append(fTarget);
				builder.append(", "); //$NON-NLS-1$
			}
			builder.append("force="); //$NON-NLS-1$
			builder.append(fForce);
			builder.append(", "); //$NON-NLS-1$
			if (fLocator != null) {
				builder.append("locator="); //$NON-NLS-1$
				builder.append(fLocator);
				builder.append(", "); //$NON-NLS-1$
			}
			if (fPage != null) {
				builder.append("page="); //$NON-NLS-1$
				builder.append(fPage);
			}
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}

	}

	/**
	 * A request to show the result of the source lookup in the UI
	 */
	static class SourceDisplayRequest {

		final SourceLookupResult fResult;
		final IWorkbenchPage fPage;

		public SourceDisplayRequest(SourceLookupResult result, IWorkbenchPage page) {
			fResult = result;
			fPage = page;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fPage, fResult);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof SourceDisplayRequest)) {
				return false;
			}
			SourceDisplayRequest other = (SourceDisplayRequest) obj;
			return Objects.equals(fPage, other.fPage) && Objects.equals(fResult, other.fResult);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SourceDisplayRequest ["); //$NON-NLS-1$
			if (fResult != null) {
				builder.append("result="); //$NON-NLS-1$
				builder.append(fResult);
				builder.append(", "); //$NON-NLS-1$
			}
			if (fPage != null) {
				builder.append("page="); //$NON-NLS-1$
				builder.append(fPage);
			}
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}

	}

	/**
	 * A singleton job to show the result of the source lookup in the UI for given
	 * {@link SourceDisplayRequest} objects. The requests are put in the queue to
	 * process in the background, duplicated requests are ignored. Job re-schedules
	 * itself if new request is added to the queue.
	 */
	class SourceDisplayJob extends UIJob {

		private final LinkedHashSet<SourceDisplayRequest> queue;

		public SourceDisplayJob() {
			super("Debug Source Display"); //$NON-NLS-1$
			setSystem(true);
			setPriority(Job.INTERACTIVE);
			this.queue = new LinkedHashSet<>();
		}


		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			SourceDisplayRequest next;
			// Do not break on cancelled monitor, to allow remove debugger
			// annotations from already opened editors
			while ((next = poll()) != null) {
				IWorkbenchPage page = next.fPage;
				if (page.getWorkbenchWindow() == null) {
					// don't try to update if page is closed
					continue;
				}
				ISourceLookupResult result = next.fResult;
				if (!monitor.isCanceled()) {
					display(result, page);
				} else {
					// termination may have occurred while displaying source
					Object artifact = result.getArtifact();
					if (artifact instanceof IStackFrame) {
						clearSourceSelection(((IStackFrame) artifact).getThread());
					}
				}
			}
			return Status.OK_STATUS;
		}

		private SourceDisplayRequest poll() {
			SourceDisplayRequest next = null;
			synchronized (queue) {
				if (!queue.isEmpty()) {
					Iterator<SourceDisplayRequest> iterator = queue.iterator();
					next = iterator.next();
					iterator.remove();
				}
			}
			return next;
		}

		void schedule(SourceDisplayRequest task) {
			synchronized (queue) {
				boolean added = queue.add(task);
				if (added) {
					schedule(100);
				}
			}
		}

		@Override
		public boolean belongsTo(Object family) {
			return family instanceof SourceLookupFacility;
		}

	}

	/*
	 * See org.eclipse.debug.ui.sourcelookup.ISourceDisplay
	 */
	public void displaySource(Object context, IWorkbenchPage page, boolean force) {
		IStackFrame frame = (IStackFrame) context;
		SourceLookupTask slj = new SourceLookupTask(frame, frame.getLaunch().getSourceLocator(), page, force);
		// will drop any existing equal source lookup jobs
		sourceLookupJob.schedule(slj);
	}

	/**
	 * Clears any source decorations associated with the given thread or debug
	 * target.
	 *
	 * @param source thread or debug target
	 */
	private void clearSourceSelection(Object source) {
		if (source instanceof IThread) {
			IThread thread = (IThread) source;
			DecorationManager.removeDecorations(thread);
			InstructionPointerManager.getDefault().removeAnnotations(thread);
		} else if (source instanceof IDebugTarget) {
			IDebugTarget target = (IDebugTarget) source;
			DecorationManager.removeDecorations(target);
			InstructionPointerManager.getDefault().removeAnnotations(target);
		}
	}
}
