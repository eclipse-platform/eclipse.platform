/*******************************************************************************
 * Copyright (c) 2002, 2009 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 * 	   John-Mason P. Shackelford - bug 40255
 *     Mark Melvin - bug 93378
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.ExternalHyperlink;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.editor.actions.FoldingActionGroup;
import org.eclipse.ant.internal.ui.editor.actions.OpenDeclarationAction;
import org.eclipse.ant.internal.ui.editor.actions.RenameInFileAction;
import org.eclipse.ant.internal.ui.editor.actions.RunToLineAdapter;
import org.eclipse.ant.internal.ui.editor.actions.ToggleLineBreakpointAction;
import org.eclipse.ant.internal.ui.editor.outline.AntEditorContentOutlinePage;
import org.eclipse.ant.internal.ui.editor.text.AntEditorDocumentProvider;
import org.eclipse.ant.internal.ui.editor.text.AntFoldingStructureProvider;
import org.eclipse.ant.internal.ui.editor.text.IReconcilingParticipant;
import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntModelChangeEvent;
import org.eclipse.ant.internal.ui.model.AntModelCore;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.IAntModelListener;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension4;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.KeyBindingSupportForAssistant;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * The actual editor implementation for Eclipse's Ant integration.
 */
public class AntEditor extends TextEditor implements IReconcilingParticipant, IProjectionListener {
	/**
	 * Updates the Ant outline page selection and this editor's range indicator.
	 * 
	 * @since 3.0
	 */
	private class EditorSelectionChangedListener implements ISelectionChangedListener  {
		
		/**
		 * Installs this selection changed listener with the given selection provider. If
		 * the selection provider is a post selection provider, post selection changed
		 * events are the preferred choice, otherwise normal selection changed events
		 * are requested.
		 * 
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider == null || getAntModel() == null) {
				return;
			}
				
			if (selectionProvider instanceof IPostSelectionProvider)  {
				IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
				provider.addPostSelectionChangedListener(this);
			} else  {
				selectionProvider.addSelectionChangedListener(this);
			}
		}

		/**
		 * Removes this selection changed listener from the given selection provider.
		 * 
		 * @param selectionProvider
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider == null || getAntModel() == null) {
				return;
			}
			
			if (selectionProvider instanceof IPostSelectionProvider)  {
				IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
				provider.removePostSelectionChangedListener(this);
			} else  {
				selectionProvider.removeSelectionChangedListener(this);
			}
		}
		
		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			AntModel model= getAntModel();
			ISelection selection= event.getSelection();
			AntElementNode node= null;
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection= (ITextSelection)selection;
				int offset= textSelection.getOffset();
				node= model.getNode(offset, false);
				updateOccurrenceAnnotations(textSelection, model);
			}
		
			if (AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.OUTLINE_LINK_WITH_EDITOR)) {
				synchronizeOutlinePage(node, true);
			}
			setSelection(node, false);
		}
	}
	
	class StatusLineSourceViewer extends ProjectionViewer{
		
		public StatusLineSourceViewer(Composite composite, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, int styles) {
			super(composite, verticalRuler, overviewRuler, isOverviewRulerVisible(), styles);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.ITextOperationTarget#doOperation(int)
		 */
		public void doOperation(int operation) {
			if (getTextWidget() == null || !redraws()) {
				return;
			}

			switch (operation) {
				case CONTENTASSIST_PROPOSALS:
					String msg= fContentAssistant.showPossibleCompletions();
					setStatusLineErrorMessage(msg);
					return;
			}
			
			super.doOperation(operation);
		}
	}
		
	
	/**
	 * Finds and marks occurrence annotations.
	 * 
	 * @since 3.1
	 */
	class OccurrencesFinderJob extends Job {
		
		private IDocument fDocument;
		private ISelection fSelection;
		private ISelectionValidator fPostSelectionValidator;
		private boolean fCanceled= false;
		private IProgressMonitor fProgressMonitor;
		private List fPositions;
		
		public OccurrencesFinderJob(IDocument document, List positions, ISelection selection) {
			super("Occurrences Marker"); //$NON-NLS-1$
			fDocument= document;
			fSelection= selection;
			fPositions= positions;
			
			if (getSelectionProvider() instanceof ISelectionValidator)
				fPostSelectionValidator= (ISelectionValidator)getSelectionProvider();
		}
		
		// cannot use cancel() because it is declared final
		void doCancel() {
			fCanceled= true;
			cancel();
		}
		
		private boolean isCanceled() {
			return fCanceled || fProgressMonitor.isCanceled()
				||  fPostSelectionValidator != null && !(fPostSelectionValidator.isValid(fSelection) || fForcedMarkOccurrencesSelection == fSelection)
				|| LinkedModeModel.hasInstalledModel(fDocument);
		}
		
		/*
		 * @see Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus run(IProgressMonitor progressMonitor) {
			
			fProgressMonitor= progressMonitor;
			
			if (isCanceled())
				return Status.CANCEL_STATUS;
			
			ITextViewer textViewer= getViewer();
			if (textViewer == null)
				return Status.CANCEL_STATUS;
			
			IDocument document= textViewer.getDocument();
			if (document == null)
				return Status.CANCEL_STATUS;
			
			IDocumentProvider documentProvider= getDocumentProvider();
			if (documentProvider == null)
				return Status.CANCEL_STATUS;
		
			IAnnotationModel annotationModel= documentProvider.getAnnotationModel(getEditorInput());
			if (annotationModel == null)
				return Status.CANCEL_STATUS;
			
			// Add occurrence annotations
			int length= fPositions.size();
			Map annotationMap= new HashMap(length);
			for (int i= 0; i < length; i++) {
				
				if (isCanceled())
					return Status.CANCEL_STATUS;
				
				String message;
				Position position= (Position) fPositions.get(i);
				
				// Create & add annotation
				try {
					message= document.get(position.offset, position.length);
				} catch (BadLocationException ex) {
					// Skip this match
					continue;
				}
				annotationMap.put(
						new Annotation("org.eclipse.jdt.ui.occurrences", false, message), //$NON-NLS-1$
						position);
			}
			
			if (isCanceled()) {
				return Status.CANCEL_STATUS;
            }
			
            Object lock= getLockObject(document);
            if (lock == null) {
                updateAnnotations(annotationModel, annotationMap);
            } else {
                synchronized (lock) {
                    updateAnnotations(annotationModel, annotationMap);
                }
            }

			return Status.OK_STATUS;
		}

        private void updateAnnotations(IAnnotationModel annotationModel, Map annotationMap) {
            if (annotationModel instanceof IAnnotationModelExtension) {
            	((IAnnotationModelExtension)annotationModel).replaceAnnotations(fOccurrenceAnnotations, annotationMap);
            } else {
            	removeOccurrenceAnnotations();
            	Iterator iter= annotationMap.entrySet().iterator();
            	while (iter.hasNext()) {
            		Map.Entry mapEntry= (Map.Entry)iter.next();
            		annotationModel.addAnnotation((Annotation)mapEntry.getKey(), (Position)mapEntry.getValue());
            	}
            }
            fOccurrenceAnnotations= (Annotation[])annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
        }
	}
	
	/**
	 * Cancels the occurrences finder job upon document changes.
	 * 
	 * @since 3.1
	 */
	class OccurrencesFinderJobCanceler implements IDocumentListener, ITextInputListener {

		public void install() {
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			StyledText text= sourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			sourceViewer.addTextInputListener(this);
			
			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.addDocumentListener(this);
		}
		
		public void uninstall() {
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer != null)
				sourceViewer.removeTextInputListener(this);

			IDocumentProvider documentProvider= getDocumentProvider();
			if (documentProvider != null) {
				IDocument document= documentProvider.getDocument(getEditorInput());
				if (document != null)
					document.removeDocumentListener(this);
			}
		}
				

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			if (fOccurrencesFinderJob != null)
				fOccurrencesFinderJob.doCancel();
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			if (oldInput == null)
				return;

			oldInput.removeDocumentListener(this);
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			if (newInput == null)
				return;
			newInput.addDocumentListener(this);
		}
	}
	
	/**
	 * Internal activation listener.
	 * @since 3.1
	 */
	private class ActivationListener extends ShellAdapter {
		/*
		 * @see org.eclipse.swt.events.ShellAdapter#shellActivated(org.eclipse.swt.events.ShellEvent)
		 */
		public void shellActivated(ShellEvent e) {
			if (fMarkOccurrenceAnnotations && isActivePart()) {
				ISelection selection= getSelectionProvider().getSelection();
				if (selection instanceof ITextSelection) {
					fForcedMarkOccurrencesSelection= (ITextSelection) selection;
					updateOccurrenceAnnotations(fForcedMarkOccurrencesSelection, getAntModel());
				}
			}
		}
		
		/*
		 * @see org.eclipse.swt.events.ShellAdapter#shellDeactivated(org.eclipse.swt.events.ShellEvent)
		 */
		public void shellDeactivated(ShellEvent e) {
			if (fMarkOccurrenceAnnotations && isActivePart())
				removeOccurrenceAnnotations();
		}
	}

	/**
	 * Selection changed listener for the outline view.
	 */
    protected ISelectionChangedListener fSelectionChangedListener = new ISelectionChangedListener(){
        public void selectionChanged(SelectionChangedEvent event) {
        	fSelectionSetFromOutline= false;
            doSelectionChanged(event);
            fSelectionSetFromOutline= true;
        }
    };
    
    private IAntModelListener fAntModelListener;
    
    /**
     * The page that shows the outline.
     */
    protected AntEditorContentOutlinePage fOutlinePage;
    
	
	private boolean fInitialReconcile= true;
	
	/**
	 * The editor selection changed listener.
	 * 
	 * @since 3.0
	 */
	private EditorSelectionChangedListener fEditorSelectionChangedListener;

	private ProjectionSupport fProjectionSupport;
	
	private AntFoldingStructureProvider fFoldingStructureProvider;
	
	private boolean fSelectionSetFromOutline= false;

    private FoldingActionGroup fFoldingGroup;
	
	/**
	 * Holds the current occurrence annotations.
	 * @since 3.1
	 */
	private Annotation[] fOccurrenceAnnotations= null;

	private OccurrencesFinderJob fOccurrencesFinderJob;
	private OccurrencesFinderJobCanceler fOccurrencesFinderJobCanceler;
	
	private ITextSelection fForcedMarkOccurrencesSelection;
	/**
	 * The internal shell activation listener for updating occurrences.
	 * @since 3.1
	 */
	private ActivationListener fActivationListener= new ActivationListener();
	
	private boolean fMarkOccurrenceAnnotations;
	private boolean fStickyOccurrenceAnnotations;
    
    private AntModel fAntModel;
    
	/**
	 * Key binding support for the content assistant.
	 * @since 3.4
	 */
	private KeyBindingSupportForAssistant fKeyBindingSupportForAssistant;

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     */
    protected void createActions() {
        super.createActions();

        ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.ant.internal.ui.editor.AntEditorMessages"); //$NON-NLS-1$
        IAction action = new ContentAssistAction(bundle, "ContentAssistProposal.", this); //$NON-NLS-1$
        
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ISourceViewerExtension4) {
        	fKeyBindingSupportForAssistant= new KeyBindingSupportForAssistant(((ISourceViewerExtension4) sourceViewer).getContentAssistantFacade());
        }

        // This action definition is associated with the accelerator Ctrl+Space
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction("ContentAssistProposal", action); //$NON-NLS-1$
        
		action = new TextOperationAction(bundle, "ContentFormat.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
        action.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("ContentFormat", action); //$NON-NLS-1$
        
        action = new OpenDeclarationAction(this);
        setAction("OpenDeclaration", action); //$NON-NLS-1$
        
        fFoldingGroup= new FoldingActionGroup(this, getViewer());
        
		action= new RenameInFileAction(this);
		action.setActionDefinitionId("org.eclipse.ant.ui.renameInFile"); //$NON-NLS-1$
		setAction("renameInFile", action); //$NON-NLS-1$
    }

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextEditor#initializeEditor()
	 * Called from TextEditor.<init>
	 */
    protected void initializeEditor() {
		setPreferenceStore(AntUIPlugin.getDefault().getCombinedPreferenceStore());
		setCompatibilityMode(false);
		
		setHelpContextId(IAntUIHelpContextIds.ANT_EDITOR);
		setRulerContextMenuId("org.eclipse.ant.internal.ui.editor.AntEditor.RulerContext"); //$NON-NLS-1$
        setEditorContextMenuId("org.eclipse.ant.internal.ui.editor.AntEditor"); //$NON-NLS-1$
		configureInsertMode(SMART_INSERT, false);
		setInsertMode(INSERT);
		fMarkOccurrenceAnnotations= getPreferenceStore().getBoolean(AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES);
		fStickyOccurrenceAnnotations= getPreferenceStore().getBoolean(AntEditorPreferenceConstants.EDITOR_STICKY_OCCURRENCES);
		
		setSourceViewerConfiguration(new AntEditorSourceViewerConfiguration(this));
		setDocumentProvider(AntUIPlugin.getDefault().getDocumentProvider());
		
		fAntModelListener= new IAntModelListener() {

			/* (non-Javadoc)
			 * @see org.eclipse.ant.internal.ui.editor.outline.IDocumentModelListener#documentModelChanged(org.eclipse.ant.internal.ui.editor.outline.DocumentModelChangeEvent)
			 */
			public void antModelChanged(AntModelChangeEvent event) {
				AntModel model= getAntModel();
				if (event.getModel() == model) {
					if (event.isPreferenceChange()) {
						updateEditorImage(model);
					}
					if (fFoldingStructureProvider != null) {
						fFoldingStructureProvider.updateFoldingRegions(model);
					}
				}
			}
		};
		AntModelCore.getDefault().addAntModelListener(fAntModelListener);

		if (isFoldingEnabled()) {
			fFoldingStructureProvider= new AntFoldingStructureProvider(this);
		}
    }
   
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
    public Object getAdapter(Class key) {
        if (key.equals(IContentOutlinePage.class)) {
			return getOutlinePage();
        }
        
        if (fProjectionSupport != null) {
        	Object adapter= fProjectionSupport.getAdapter(getSourceViewer(), key);
        	if (adapter != null) {
            	return adapter;
            }
        }

        if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV };
				}
			};
		}
        
        if (key == IToggleBreakpointsTarget.class) {
			return new ToggleLineBreakpointAction();
		}
        
        if (key == IRunToLineTarget.class) {
			return new RunToLineAdapter();
		}
        
        return super.getAdapter(key);
    }

	private AntEditorContentOutlinePage getOutlinePage() {
		if (fOutlinePage == null) {
			fOutlinePage= new AntEditorContentOutlinePage(AntModelCore.getDefault(), this);
			fOutlinePage.addPostSelectionChangedListener(fSelectionChangedListener);
			setOutlinePageInput();
		}
		return fOutlinePage;
	}

    private void doSelectionChanged(SelectionChangedEvent selectionChangedEvent) {
        IStructuredSelection selection= (IStructuredSelection)selectionChangedEvent.getSelection();

        if (!isActivePart() && AntUIPlugin.getActivePage() != null) {
			AntUIPlugin.getActivePage().bringToTop(this);
        }
        
        AntElementNode selectedXmlElement = (AntElementNode)selection.getFirstElement();
        if(selectedXmlElement != null) {
			setSelection(selectedXmlElement, !isActivePart());
        }
    }

    private boolean isActivePart() {
        IWorkbenchPart part= getActivePart();
        return part != null && part.equals(this);
    }
    
    public void setSelection(AntElementNode reference, boolean moveCursor) {
    	if (fSelectionSetFromOutline) {
    		//the work has all just been done via a selection setting in the outline
    		fSelectionSetFromOutline= false;
    		return;
    	}
        if (reference == null) {
        	if (moveCursor) {
        		 resetHighlightRange();
                 markInNavigationHistory();
        	}
        	return;
        }
		
		if (moveCursor) {
			markInNavigationHistory();
		}
		
    	while (reference.getImportNode() != null) {
    		reference= reference.getImportNode();
    	}
    	if (reference.isExternal()) {
    		return;
    	}
        
        ISourceViewer sourceViewer= getSourceViewer();
        if (sourceViewer == null) {
            return;
        }
        StyledText textWidget= sourceViewer.getTextWidget();
        if (textWidget == null) {
            return;
        }
            
        try {
            int offset= reference.getOffset();
            if (offset < 0) {
                return;
            }
            int length= reference.getSelectionLength();
            int highLightLength= reference.getLength();
               
            textWidget.setRedraw(false);
            
            if (highLightLength > 0) {
                setHighlightRange(offset, highLightLength, moveCursor);
            }
            
            if (!moveCursor) {
                return;
            }
                                        
            if (offset > -1 && length > 0) {
                sourceViewer.revealRange(offset, length);
                // Selected region begins one index after offset
                sourceViewer.setSelectedRange(offset, length);
                markInNavigationHistory();
            }
        } catch (IllegalArgumentException x) {
        	AntUIPlugin.log(x);
        } finally {
            textWidget.setRedraw(true);
        }
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#affectsTextPresentation(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((AntEditorSourceViewerConfiguration)getSourceViewerConfiguration()).affectsTextPresentation(event);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#handlePreferenceStoreChanged(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		
		if (AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
			Object value= event.getNewValue();
			StatusLineSourceViewer viewer= (StatusLineSourceViewer) getSourceViewer();
			int newValue= -1;
			if (value instanceof Integer) {
				newValue=((Integer) value).intValue();
			} else if (value instanceof String) {
				newValue= Integer.parseInt((String) value);
			}
			if (newValue != -1) {
				viewer.getTextWidget().setTabs(newValue);
			}
			return;
		}
		
		if (AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES.equals(property)) {
			boolean newBooleanValue= Boolean.valueOf(event.getNewValue().toString()).booleanValue();
			if (newBooleanValue != fMarkOccurrenceAnnotations) {
				fMarkOccurrenceAnnotations= newBooleanValue;
				if (fMarkOccurrenceAnnotations) {
					installOccurrencesFinder();
				} else {
					uninstallOccurrencesFinder();
				}
			}
			return;
		}
		if (AntEditorPreferenceConstants.EDITOR_STICKY_OCCURRENCES.equals(property)) {
			boolean newBooleanValue= Boolean.valueOf(event.getNewValue().toString()).booleanValue();
			fStickyOccurrenceAnnotations= newBooleanValue;
			return;
		}
		
		if (AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED.equals(property)) {
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer instanceof ProjectionViewer) {
				ProjectionViewer pv= (ProjectionViewer) sourceViewer;
				if (pv.isProjectionMode() != isFoldingEnabled()) {
					if (pv.canDoOperation(ProjectionViewer.TOGGLE)) {
						pv.doOperation(ProjectionViewer.TOGGLE);
					}
				}
			}
			return;
		}

		AntEditorSourceViewerConfiguration sourceViewerConfiguration= (AntEditorSourceViewerConfiguration)getSourceViewerConfiguration();
        if (sourceViewerConfiguration != null) {
            if (affectsTextPresentation(event)) {
                sourceViewerConfiguration.adaptToPreferenceChange(event);
            }

            sourceViewerConfiguration.changeConfiguration(event);
        }
		super.handlePreferenceStoreChanged(event);
	}
	
	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#doSetInput(org.eclipse.ui.IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		fAntModel= null;
		super.doSetInput(input);
		setOutlinePageInput();
		if (fFoldingStructureProvider != null) {
			fFoldingStructureProvider.setDocument(getDocumentProvider().getDocument(input));
		}
	}

	private void setOutlinePageInput() {
		if (fOutlinePage != null) {
			fOutlinePage.setPageInput(getAntModel());
		}
	}
	
	/**
	 * Returns the Ant model for the current editor input of this editor.
	 * @return the Ant model for this editor or <code>null</code>
	 */
	public AntModel getAntModel() {
        if (fAntModel == null) {
            IDocumentProvider provider= getDocumentProvider();
            if (provider instanceof AntEditorDocumentProvider) {
                AntEditorDocumentProvider documentProvider= (AntEditorDocumentProvider) provider;
                fAntModel= documentProvider.getAntModel(getEditorInput());
            }
        }
		return fAntModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		fOverviewRuler= createOverviewRuler(getSharedColors());
		ISourceViewer viewer= new StatusLineSourceViewer(parent, ruler, getOverviewRuler(), styles);
		//ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}
	
	/**
	 * Set the given message as error message to this editor's status line.
	 * @param msg message to be set
	 */
	protected void setStatusLineErrorMessage(String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, msg, null);
	}

	public void openReferenceElement() {
		ISelection selection= getSelectionProvider().getSelection();
		Object target= null;
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection)selection;
			ISourceViewer viewer= getSourceViewer();
			int textOffset= textSelection.getOffset();
			IRegion region= XMLTextHover.getRegion(viewer, textOffset);
			target= findTarget(region);
		}
		
		openTarget(target);
	}
	
	protected void openTarget(Object node) {
		String errorMessage= null;
		if (node instanceof AntElementNode) {
			errorMessage= openNode((AntElementNode) node);
			if (errorMessage == null) {
				return;
			}
		} else if (node instanceof String){
			errorMessage= openInEditor((String) node, getAntModel().getEditedFile());
			if (errorMessage == null) {
				return;
			}
		}
		if (errorMessage == null || errorMessage.length() == 0) {
			errorMessage= AntEditorMessages.getString("AntEditor.3"); //$NON-NLS-1$
		}
		setStatusLineErrorMessage(errorMessage);
		getSite().getShell().getDisplay().beep();
	}

	/**
     * @param region The region to find the navigation target
     * @return the navigation target at the specified region
     */
    public Object findTarget(IRegion region) {
        ISourceViewer viewer = getSourceViewer();
        AntElementNode node= null;
       
		if (region != null) {
			IDocument document= viewer.getDocument();
			String text= null;
			try {
				text= document.get(region.getOffset(), region.getLength());
			} catch (BadLocationException e) {
			}
			if (text != null && text.length() > 0) {
				AntModel model= getAntModel();
				if (model == null) {
					return null;
				}
				node= model.getReferenceNode(text);
				if (node == null) {
					node= model.getTargetNode(text);
					if (node == null) {
						node= model.getPropertyNode(text);
						if (node == null) {
							String path= model.getPath(text, region.getOffset());
							if (path != null) {
								return path;
							}
                            
                            node= model.getDefininingTaskNode(text);
                            if (node == null) {
                                node= model.getMacroDefAttributeNode(text);
                            }
						}
					}
				}
			}
		}
		return node;
    }


    private String openNode(AntElementNode node) {
		String errorMessage= null;
		if (node.isExternal()) {
			String path= node.getFilePath();
			errorMessage= openInEditor(path, null);
		} else {
			setSelection(node, true);
		}
		return errorMessage;
	}
	
	private String openInEditor(String path, File buildFile) {
		File buildFileParent= null;
		if (buildFile != null) {
			buildFileParent= buildFile.getParentFile();
		}
		IFile file= AntUtil.getFileForLocation(path, buildFileParent);
		if (file != null && file.exists()) {
			try {
				IWorkbenchPage p= getEditorSite().getPage();
				if (p != null) {
					IDE.openEditor(p, file, isActivePart());
				}
				return null;
			} catch (PartInitException e) {
				return e.getLocalizedMessage();
			}
		}
        File externalFile= new File(path);
        if (externalFile.exists()) {
            new ExternalHyperlink(externalFile, -1).linkActivated();
            return null;
        }
        
		return ""; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		
		if (getAntModel() != null) {
			IAction action= getAction("OpenDeclaration"); //$NON-NLS-1$
			if (action != null) {
				String openGroup = "group.open"; //$NON-NLS-1$
	    	    menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, new Separator(openGroup));
				menu.appendToGroup(openGroup, action);
			}
			
			action= getAction("renameInFile"); //$NON-NLS-1$
			String editGroup = "group.edit"; //$NON-NLS-1$
		    menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, new Separator(editGroup));
			menu.appendToGroup(editGroup, action);
			
			action= getAction("ContentFormat"); //$NON-NLS-1$
			menu.appendToGroup(editGroup, action);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		ProjectionViewer projectionViewer= (ProjectionViewer) getSourceViewer();
        createFoldingSupport(projectionViewer);
        if (isFoldingEnabled()) {
        	projectionViewer.doOperation(ProjectionViewer.TOGGLE);
        }
        
		if (fMarkOccurrenceAnnotations) {
			installOccurrencesFinder();
		}
		getEditorSite().getShell().addShellListener(fActivationListener);
		
		fEditorSelectionChangedListener= new EditorSelectionChangedListener();
		fEditorSelectionChangedListener.install(getSelectionProvider());
	}
	
	private void createFoldingSupport(ProjectionViewer projectionViewer) {
		fProjectionSupport= new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
    	fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				return new AntSourceViewerInformationControl(shell);
			}
		});
        fProjectionSupport.install();
		((ProjectionViewer)getViewer()).addProjectionListener(this);
        
	}


	private boolean isFoldingEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#isTabsToSpacesConversionEnabled()
	 * @since 3.3
	 */
	protected boolean isTabsToSpacesConversionEnabled() {
		return super.isTabsToSpacesConversionEnabled(); //provide package visibility
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		if (fEditorSelectionChangedListener != null)  {
			fEditorSelectionChangedListener.uninstall(getSelectionProvider());
			fEditorSelectionChangedListener= null;
		}
		
		((ProjectionViewer)getViewer()).removeProjectionListener(this);
		if (fProjectionSupport != null) {
			fProjectionSupport.dispose();
			fProjectionSupport= null;
		}
		
		uninstallOccurrencesFinder();
		
		if (fActivationListener != null) {
			Shell shell= getEditorSite().getShell();
			if (shell != null && !shell.isDisposed()) {
				shell.removeShellListener(fActivationListener);
			}
			fActivationListener= null;
		}
		
		AntModelCore.getDefault().removeAntModelListener(fAntModelListener);
		fAntModel= null;
		
		if (fKeyBindingSupportForAssistant != null) {
			fKeyBindingSupportForAssistant.dispose();
			fKeyBindingSupportForAssistant = null;
		}
		
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		AntModel model= getAntModel();
		model.updateMarkers();
		updateEditorImage(model);
	}
	
	private void updateEditorImage(AntModel model) {
		Image titleImage= getTitleImage();
		if (titleImage == null) {
			return;
		}
		AntProjectNode node= model.getProjectNode();
		if (node != null) {
			postImageChange(node);
		}
	}
	
	private void updateForInitialReconcile() {
		IDocumentProvider provider=  getDocumentProvider();
		if (provider == null) {//disposed
			return;
		}
		if (getAntModel() == null) {
			return;
		}
        IDocument doc= provider.getDocument(getEditorInput());
        if (doc == null) {
            return; //disposed
        }
        Object lock= getLockObject(doc);
		//ensure to synchronize so that the AntModel is not nulled out underneath in the AntEditorDocumentProvider
		//when the editor/doc provider are disposed
        if (lock == null) {
            updateModelForInitialReconcile();
        } else {
            synchronized (lock) {
                updateModelForInitialReconcile();
            }
        }
	}
    
    private void updateModelForInitialReconcile() {
        AntModel model= getAntModel();
        if (model == null) {
            return;
        }

        fInitialReconcile= false;
        updateEditorImage(model);
        model.updateForInitialReconcile();
    }
    
    private Object getLockObject(IDocument doc) {
        Object lock= null;
        if (doc instanceof ISynchronizable) {
            lock= ((ISynchronizable) doc).getLockObject();
        } else {
            lock= getAntModel();
        }
        return lock;
    }
	
	private void postImageChange(final AntElementNode node) {
		Shell shell= getSite().getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (getSite().getShell() == null || getSite().getShell().isDisposed()) {
						return;
					}
					Image titleImage= getTitleImage();
					Image newImage= node.getImage();
					if (titleImage != newImage) {
						setTitleImage(newImage);
					}
				}
			});
		}
	}

	public void synchronizeOutlinePage(boolean checkIfOutlinePageActive) {
		if (getSelectionProvider() == null) {
			return;
		}
		AntElementNode node= getNode();
		synchronizeOutlinePage(node, checkIfOutlinePageActive);
		
	}
	
	protected void synchronize(boolean checkIfOutlinePageActive) {
		if (getSelectionProvider() == null) {
			return;
		}
		AntElementNode node= getNode();
		if (AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.OUTLINE_LINK_WITH_EDITOR)) {
			synchronizeOutlinePage(node, checkIfOutlinePageActive);
		}
		setSelection(node, false);
		
	}
	
	private AntElementNode getNode() {
		AntModel model= getAntModel();
		if (model == null) {
			return null;
		}
		AntElementNode node= null;
		ISelection selection= getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection)selection;
			int offset= textSelection.getOffset();
			node= model.getNode(offset, false);
		}
		return node;
	}
	
	protected void synchronizeOutlinePage(AntElementNode node, boolean checkIfOutlinePageActive) {
		if (fOutlinePage != null && !(checkIfOutlinePageActive && isAntOutlinePageActive())) {
			fOutlinePage.removePostSelectionChangedListener(fSelectionChangedListener);
			fOutlinePage.select(node);
			fOutlinePage.addPostSelectionChangedListener(fSelectionChangedListener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.text.IReconcilingParticipant#reconciled()
	 */
	public void reconciled() {
		if (fInitialReconcile) {
			updateForInitialReconcile();
		}
		
		SourceViewerConfiguration config= getSourceViewerConfiguration();
		if (config == null) {
			return; //editor has been disposed.
		}
		IAutoEditStrategy[] strategies= config.getAutoEditStrategies(getViewer(), null);
		for (int i = 0; i < strategies.length; i++) {
			IAutoEditStrategy strategy = strategies[i];
			if (strategy instanceof AntAutoEditStrategy) {
				((AntAutoEditStrategy)strategy).reconciled();
			}
		}
		
		Shell shell= getSite().getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (getSite().getShell() == null || getSite().getShell().isDisposed()) {
						return;
					}
					synchronize(true);
				}
			});
		}
	}
	
	private boolean isAntOutlinePageActive() {
		IWorkbenchPart part= getActivePart();
		return part instanceof ContentOutline && ((ContentOutline)part).getCurrentPage() == fOutlinePage;
	}

	private IWorkbenchPart getActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		return service.getActivePart();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetSelection(org.eclipse.jface.viewers.ISelection)
	 */
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
		synchronizeOutlinePage(true);
	}

    /**
     * Returns the viewer associated with this editor
     * @return The viewer associated with this editor
     */
    public ISourceViewer getViewer() {
        return getSourceViewer();
    }

    protected FoldingActionGroup getFoldingActionGroup() {
        return fFoldingGroup;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionEnabled()
     */
    public void projectionEnabled() {
    	fFoldingStructureProvider= new AntFoldingStructureProvider(this);
		fFoldingStructureProvider.setDocument(getDocumentProvider().getDocument(getEditorInput()));
		fFoldingStructureProvider.updateFoldingRegions(getAntModel());
		IPreferenceStore preferenceStore = AntUIPlugin.getDefault().getPreferenceStore();
		preferenceStore.setValue(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionDisabled()
     */
    public void projectionDisabled() {
    	fFoldingStructureProvider= null;
    	IPreferenceStore preferenceStore = AntUIPlugin.getDefault().getPreferenceStore();
		preferenceStore.setValue(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, false);
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeKeyBindingScopes()
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "org.eclipse.ant.ui.AntEditorScope" });  //$NON-NLS-1$
	}
	
	protected IPreferenceStore getEditorPreferenceStore() {
		return getPreferenceStore();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#collectContextMenuPreferencePages()
	 * @since 3.1
	 */
	protected String[] collectContextMenuPreferencePages() {
		String[] ids= super.collectContextMenuPreferencePages();
		String[] more= new String[ids.length + 6];
		more[0]= "org.eclipse.ant.ui.AntEditorPreferencePage"; //$NON-NLS-1$
		more[1]= "org.eclipse.ant.ui.AntCodeFormatterPreferencePage"; //$NON-NLS-1$
		more[2]= "org.eclipse.ant.ui.AntCodeAssistPreferencePage"; //$NON-NLS-1$
		more[3]= "org.eclipse.ant.ui.TemplatesPreferencePage"; //$NON-NLS-1$
		more[4]= "org.eclipse.ant.ui.AntPreferencePage"; //$NON-NLS-1$
		more[5]= "org.eclipse.ant.ui.AntRuntimePreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 6, ids.length);
		return more;
	}
	
	/**
	 * Updates the occurrences annotations based
	 * on the current selection.
	 * 
	 * @param selection the text selection
	 * @param antModel the model for the buildfile
	 * @since 3.1
	 */
	protected void updateOccurrenceAnnotations(ITextSelection selection, AntModel antModel) {

		if (fOccurrencesFinderJob != null)
			fOccurrencesFinderJob.cancel();

		if (!fMarkOccurrenceAnnotations) {
			return;
		}
		
		if (selection == null || antModel == null) {
			return;
		}
		
		IDocument document= getSourceViewer().getDocument();
		if (document == null) {
			return;
		}
		
		List positions= null;
		
		OccurrencesFinder finder= new OccurrencesFinder(this, antModel, document, selection.getOffset());
		positions= finder.perform();
		
		if (positions == null || positions.size() == 0) {
			if (!fStickyOccurrenceAnnotations) {
				removeOccurrenceAnnotations();
			}
			return;
		}
			
		fOccurrencesFinderJob= new OccurrencesFinderJob(document, positions, selection);
		fOccurrencesFinderJob.run(new NullProgressMonitor());
	}
	
	private void removeOccurrenceAnnotations() {
		IDocumentProvider documentProvider= getDocumentProvider();
		if (documentProvider == null) {
			return;
		}
		
		IAnnotationModel annotationModel= documentProvider.getAnnotationModel(getEditorInput());
		if (annotationModel == null || fOccurrenceAnnotations == null) {
			return;
		}

		IDocument document= documentProvider.getDocument(getEditorInput());
        Object lock= getLockObject(document);
        if (lock == null) {
            updateAnnotationModelForRemoves(annotationModel);
        } else {
            synchronized (lock) {
                updateAnnotationModelForRemoves(annotationModel);
            }
        }
	}


    private void updateAnnotationModelForRemoves(IAnnotationModel annotationModel) {
        if (annotationModel instanceof IAnnotationModelExtension) {
        	((IAnnotationModelExtension)annotationModel).replaceAnnotations(fOccurrenceAnnotations, null);
        } else {
        	for (int i= 0, length= fOccurrenceAnnotations.length; i < length; i++) {
        		annotationModel.removeAnnotation(fOccurrenceAnnotations[i]);
        	}
        }
        fOccurrenceAnnotations= null;
    }

	protected void installOccurrencesFinder() {
		fMarkOccurrenceAnnotations= true;
		
		if (getSelectionProvider() != null) {
			ISelection selection= getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				fForcedMarkOccurrencesSelection= (ITextSelection) selection;
				updateOccurrenceAnnotations(fForcedMarkOccurrencesSelection, getAntModel());
			}
		}
		if (fOccurrencesFinderJobCanceler == null) {
			fOccurrencesFinderJobCanceler= new OccurrencesFinderJobCanceler();
			fOccurrencesFinderJobCanceler.install();
		}
	}
	
	protected void uninstallOccurrencesFinder() {
		fMarkOccurrenceAnnotations= false;
		
		if (fOccurrencesFinderJob != null) {
			fOccurrencesFinderJob.cancel();
			fOccurrencesFinderJob= null;
		}

		if (fOccurrencesFinderJobCanceler != null) {
			fOccurrencesFinderJobCanceler.uninstall();
			fOccurrencesFinderJobCanceler= null;
		}
		
		removeOccurrenceAnnotations();
	}
	
	public boolean isMarkingOccurrences() {
		return fMarkOccurrenceAnnotations;
	}


	/**
	 * The editor has entered or exited linked mode.
	 * @param inLinkedMode whether an enter or exit has occurred
	 * @param effectsOccurrences whether to change the state of the occurrences finder
	 */
	public void setInLinkedMode(boolean inLinkedMode, boolean effectsOccurrences) {
		if (inLinkedMode) {
			getAntModel().setShouldReconcile(false);
			if (effectsOccurrences) {
				uninstallOccurrencesFinder();
			}
		} else {
			getAntModel().setShouldReconcile(true);
			if (effectsOccurrences) {
				installOccurrencesFinder();
			}
		}
	}
}