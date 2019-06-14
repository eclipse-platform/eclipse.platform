/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     Sebastian Davids - bug 57208
 *     Maik Schreiber - bug 102461
 *     Eugene Kuleshov (eu@md.pp.ru) - Bug 112742 [Wizards] Add spell check to commit dialog
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 179183 Use spelling support from JFace in CVS commit dialog
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 77944 [Change Sets] Comment dialog: Use comment as title
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 194992 [Wizards] Display quick assists on context menu of commit dialog
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.*;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.*;


/**
 * This area provides the widgets for providing the CVS commit comment
 */
public class CommitCommentArea extends DialogArea {

	private class TextBox implements ModifyListener, TraverseListener, FocusListener, Observer, IDocumentListener {
		
		private final StyledText fTextField; // updated only by modify events
		private final String fMessage;
		private String fText;
		private IDocument fDocument;
		
		public TextBox(Composite composite, String message, String initialText) {
			
			fMessage= message;
			fText= initialText;
			
			AnnotationModel annotationModel = new AnnotationModel();
			IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();

			Composite cc = new Composite(composite, SWT.BORDER);
			cc.setLayout(new FillLayout());
			cc.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			final SourceViewer sourceViewer = new SourceViewer(cc, null, null, true, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
			fTextField = sourceViewer.getTextWidget();
			fTextField.setIndent(2);
			
			final SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(sourceViewer, null, annotationAccess, EditorsUI.getSharedTextColors());
			
			Iterator e= new MarkerAnnotationPreferences().getAnnotationPreferences().iterator();
			while (e.hasNext())
				support.setAnnotationPreference((AnnotationPreference) e.next());
			
			support.install(EditorsUI.getPreferenceStore());
			
			final IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			final IHandlerActivation handlerActivation = installQuickFixActionHandler(handlerService, sourceViewer);
			
			final TextViewerAction cutAction = new TextViewerAction(sourceViewer, ITextOperationTarget.CUT);
			cutAction.setText(CVSUIMessages.CommitCommentArea_7);
			cutAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT);
			
			final TextViewerAction copyAction = new TextViewerAction(sourceViewer, ITextOperationTarget.COPY);
			copyAction.setText(CVSUIMessages.CommitCommentArea_8);
			copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
			
			final TextViewerAction pasteAction = new TextViewerAction(sourceViewer, ITextOperationTarget.PASTE);
			pasteAction.setText(CVSUIMessages.CommitCommentArea_9);
			pasteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);
			
			final TextViewerAction selectAllAction = new TextViewerAction(sourceViewer, ITextOperationTarget.SELECT_ALL);
			selectAllAction.setText(CVSUIMessages.CommitCommentArea_10);
			selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);
			
			MenuManager contextMenu = new MenuManager();
			contextMenu.add(cutAction);
			contextMenu.add(copyAction);
			contextMenu.add(pasteAction);
			contextMenu.add(selectAllAction);
			contextMenu.add(new Separator());
			
			final SubMenuManager quickFixMenu = new SubMenuManager(contextMenu);
			quickFixMenu.setVisible(true);
			quickFixMenu.addMenuListener(manager -> {
				quickFixMenu.removeAll();

				IAnnotationModel annotationModel1 = sourceViewer.getAnnotationModel();
				Iterator annotationIterator = annotationModel1.getAnnotationIterator();
				while (annotationIterator.hasNext()) {
					Annotation annotation = (Annotation) annotationIterator.next();
					if (!annotation.isMarkedDeleted()
							&& includes(annotationModel1.getPosition(annotation),
									sourceViewer.getTextWidget().getCaretOffset())
							&& sourceViewer.getQuickAssistAssistant().canFix(annotation)) {
						ICompletionProposal[] computeQuickAssistProposals = sourceViewer.getQuickAssistAssistant()
								.getQuickAssistProcessor()
								.computeQuickAssistProposals(sourceViewer.getQuickAssistInvocationContext());
						for (ICompletionProposal proposal : computeQuickAssistProposals) {
							quickFixMenu.add(new Action(proposal.getDisplayString()) {

								@Override
								public void run() {
									proposal.apply(sourceViewer.getDocument());
								}

								@Override
								public ImageDescriptor getImageDescriptor() {
									if (proposal.getImage() != null) {
										return ImageDescriptor.createFromImage(proposal.getImage());
									}
									return null;
								}
							});
						}
					}
				}
			});
			
			fTextField.addFocusListener(new FocusListener() {
				
				private IHandlerActivation cutHandlerActivation;
				private IHandlerActivation copyHandlerActivation;
				private IHandlerActivation pasteHandlerActivation;
				private IHandlerActivation selectAllHandlerActivation;

				@Override
				public void focusGained(FocusEvent e) {
					cutAction.update();
					copyAction.update();
					IHandlerService service = PlatformUI.getWorkbench().getService(IHandlerService.class);
					this.cutHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_CUT, new ActionHandler(cutAction), new ActiveShellExpression(getComposite().getShell()));
					this.copyHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, new ActionHandler(copyAction), new ActiveShellExpression(getComposite().getShell()));
					this.pasteHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_PASTE, new ActionHandler(pasteAction), new ActiveShellExpression(getComposite().getShell()));
					this.selectAllHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_SELECT_ALL, new ActionHandler(selectAllAction), new ActiveShellExpression(getComposite().getShell()));
					
					
				}
				
				@Override
				public void focusLost(FocusEvent e) {
					IHandlerService service = PlatformUI.getWorkbench().getService(IHandlerService.class);
					
					if (cutHandlerActivation != null) {
						service.deactivateHandler(cutHandlerActivation);
					}
					
					if (copyHandlerActivation != null) {
						service.deactivateHandler(copyHandlerActivation);
					}
					
					if (pasteHandlerActivation != null) {
						service.deactivateHandler(pasteHandlerActivation);
					}
					
					if (selectAllHandlerActivation != null) {
						service.deactivateHandler(selectAllHandlerActivation);
					}
				}
			
			});
			
			sourceViewer.addSelectionChangedListener(event -> {
				cutAction.update();
				copyAction.update();
			});
			
			sourceViewer.getTextWidget().addDisposeListener(e1 -> {
				support.uninstall();
				handlerService.deactivateHandler(handlerActivation);
			});
			
			fDocument = new Document(initialText);

			// NOTE: Configuration must be applied before the document is set in order for
			// Hyperlink coloring to work. (Presenter needs document object up front)
			sourceViewer.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()));
			sourceViewer.setDocument(fDocument, annotationModel);
			fDocument.addDocumentListener(this);
			fTextField.addTraverseListener(this);
			fTextField.addModifyListener(this);
			fTextField.addFocusListener(this);
			
			fTextField.setMenu(contextMenu.createContextMenu(fTextField));
			fTextField.selectAll();
		}
		
		protected boolean includes(Position position, int caretOffset) {
			return position.includes(caretOffset) || (position.offset + position.length) == caretOffset;
		}
		
		/**
		 * Installs the quick fix action handler
		 * and returns the handler activation.
		 * 
		 * @param handlerService the handler service
		 * @param sourceViewer the source viewer
		 * @return the handler activation
		 * @since 3.4
		 */
		private IHandlerActivation installQuickFixActionHandler(IHandlerService handlerService, SourceViewer sourceViewer) {
			return handlerService.activateHandler(
						ITextEditorActionDefinitionIds.QUICK_ASSIST,
						createQuickFixActionHandler(sourceViewer),
						new ActiveShellExpression(sourceViewer.getTextWidget().getShell()));
		}
		
		/**
		 * Creates and returns a quick fix action handler.
		 * 
		 * @param textOperationTarget the target for text operations
		 * @since 3.4
		 */
		private ActionHandler createQuickFixActionHandler(final ITextOperationTarget textOperationTarget) {
			Action quickFixAction= new Action() {
				@Override
				public void run() {
					textOperationTarget.doOperation(ISourceViewer.QUICK_ASSIST);
				}
			};
			quickFixAction.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);
			return new ActionHandler(quickFixAction);
		}

		@Override
		public void modifyText(ModifyEvent e) {
			final String old = fText;
			fText = fTextField.getText();
			if (!fText.equals(old))
				firePropertyChangeChange(COMMENT_MODIFIED, old, fText);
		}
		
		@Override
		public void keyTraversed(TraverseEvent e) {
			if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) != 0) {
				e.doit = false;
				firePropertyChangeChange(OK_REQUESTED, null, null);
			}
		}
		
		@Override
		public void focusGained(FocusEvent e) {

			if (fText.length() > 0)
				return;
			
			fTextField.removeModifyListener(this);
			fDocument.removeDocumentListener(this);
			try {
				fTextField.setText(fText);
			} finally {
				fTextField.addModifyListener(this);
				fDocument.addDocumentListener(this);
			}
		}
		
		@Override
		public void focusLost(FocusEvent e) {
			
			if (fText.length() > 0)
				return;
			
			fTextField.removeModifyListener(this);
			fDocument.removeDocumentListener(this);
			try {
				fTextField.setText(fMessage);
				fTextField.selectAll();
			} finally {
				fTextField.addModifyListener(this);
				fDocument.addDocumentListener(this);
			}
		}
		
		public void setEnabled(boolean enabled) {
			fTextField.setEnabled(enabled);
		}
		
		@Override
		public void update(Observable o, Object arg) {
			if (arg instanceof String) {
				setText((String)arg); // triggers a modify event
			}
		}
		
		public String getText() {
			return fText;
		}
		
		private void setText(String text) {
			if (text.length() == 0) {
				fTextField.setText(fMessage);
				fTextField.selectAll();
			} else
				fTextField.setText(text);
		}

		public void setFocus() {
			fTextField.setFocus();
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			modifyText(null);
		}
	}
	
	private static class ComboBox extends Observable implements SelectionListener, FocusListener {
		
		private final String fMessage;
		private final String [] fComments;
		private String[] fCommentTemplates;
		private final Combo fCombo;
		
		
		public ComboBox(Composite composite, String message, String [] options,
				String[] commentTemplates) {
			
			fMessage= message;
			fComments= options;
			fCommentTemplates = commentTemplates;
			
			fCombo = new Combo(composite, SWT.READ_ONLY);
			fCombo.setLayoutData(SWTUtils.createHFillGridData());
			fCombo.setVisibleItemCount(20);
			
			// populate the previous comment list
			populateList();
			
			// We don't want to have an initial selection
			// (see bug 32078: http://bugs.eclipse.org/bugs/show_bug.cgi?id=32078)
			fCombo.addFocusListener(this);
			fCombo.addSelectionListener(this);
		}

		private void populateList() {
			fCombo.removeAll();
			
			fCombo.add(fMessage);
			for (String commentTemplate : fCommentTemplates) {
				fCombo.add(CVSUIMessages.CommitCommentArea_6 + ": " + //$NON-NLS-1$
						Util.flattenText(commentTemplate));
			}
			for (String comment : fComments) {
				fCombo.add(Util.flattenText(comment));
			}
			fCombo.setText(fMessage);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int index = fCombo.getSelectionIndex();
			if (index > 0) {
				index--;
				setChanged();
				
				// map from combo box index to array index
				String message;
				if (index < fCommentTemplates.length) {
					message = fCommentTemplates[index];
				} else {
					message = fComments[index - fCommentTemplates.length];
				}
				notifyObservers(message);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
		@Override
		public void focusGained(FocusEvent e) {
		}
		
		@Override
		public void focusLost(FocusEvent e) {
			fCombo.removeSelectionListener(this);
			try {
				fCombo.setText(fMessage);
			} finally {
				fCombo.addSelectionListener(this);
			}
		}
		
		public void setEnabled(boolean enabled) {
			fCombo.setEnabled(enabled);
		}
		
		void setCommentTemplates(String[] templates) {
			fCommentTemplates = templates;
			populateList();
		}
	}
	
	private static final String EMPTY_MESSAGE= CVSUIMessages.CommitCommentArea_0;
	private static final String COMBO_MESSAGE= CVSUIMessages.CommitCommentArea_1;
	private static final String CONFIGURE_TEMPLATES_MESSAGE= CVSUIMessages.CommitCommentArea_5;
	
	public static final String OK_REQUESTED = "OkRequested";//$NON-NLS-1$
	public static final String COMMENT_MODIFIED = "CommentModified";//$NON-NLS-1$
	
	private TextBox fTextBox;
	private ComboBox fComboBox;
	
	private IProject fMainProject;
	private String fProposedComment;
	private Composite fComposite;
	
	@Override
	public void createArea(Composite parent) {
		Dialog.applyDialogFont(parent);
		initializeDialogUnits(parent);
		
		fComposite = createGrabbingComposite(parent, 1);
		initializeDialogUnits(fComposite);
		
		fTextBox= new TextBox(fComposite, EMPTY_MESSAGE, getInitialComment());
		
		final String [] comments = CVSUIPlugin.getPlugin().getRepositoryManager().getPreviousComments();
		final String[] commentTemplates = CVSUIPlugin.getPlugin().getRepositoryManager().getCommentTemplates();
		fComboBox= new ComboBox(fComposite, COMBO_MESSAGE, comments, commentTemplates);
		
		Link templatesPrefsLink = new Link(fComposite, 0);
		templatesPrefsLink.setText("<a href=\"configureTemplates\">" + //$NON-NLS-1$
				CONFIGURE_TEMPLATES_MESSAGE + "</a>"); //$NON-NLS-1$
		templatesPrefsLink.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				openCommentTemplatesPreferencePage();
			}
		
			@Override
			public void widgetSelected(SelectionEvent e) {
				openCommentTemplatesPreferencePage();
			}
		});
		
		fComboBox.addObserver(fTextBox);
	}
	
	void openCommentTemplatesPreferencePage() {
		PreferencesUtil.createPreferenceDialogOn(
				null,
				"org.eclipse.team.cvs.ui.CommentTemplatesPreferences", //$NON-NLS-1$
				new String[] { "org.eclipse.team.cvs.ui.CommentTemplatesPreferences" }, //$NON-NLS-1$
				null).open();
		fComboBox.setCommentTemplates(
				CVSUIPlugin.getPlugin().getRepositoryManager().getCommentTemplates());
	}

	public String getComment(boolean save) {
		final String comment= fTextBox.getText();
		if (comment == null)
			return ""; //$NON-NLS-1$
		
		final String stripped= strip(comment);
		if (save && comment.length() > 0)
			CVSUIPlugin.getPlugin().getRepositoryManager().addComment(comment);

		return stripped;
	}
	
	/**
	 * Calculates a shortened form of the commit message for use as a commit set
	 * title
	 * @return The first line or sentence of the commit message.  The commit template
	 * text will be removed, as will leading and trailing whitespace.
	 */
	public String getFirstLineOfComment() {
		String comment= fTextBox.getText();
		if (comment == null) {
			comment= ""; //$NON-NLS-1$
		}
		
		comment= strip(comment);
		
		
		int cr= comment.indexOf('\r');
		if (cr != -1) {
			comment= comment.substring(0, cr);
		}
		
		int lf= comment.indexOf('\n');
		if (lf != -1) {
			comment= comment.substring(0, lf);
		}
		
		int dot= comment.indexOf(". "); //$NON-NLS-1$
		if (dot != -1) {
			comment= comment.substring(0, dot);
		}

		comment= comment.trim();
		
		return comment;
	}
	
	public String getCommentWithPrompt(Shell shell) {
		final String comment= getComment(false);
		if (comment.length() == 0) {
			final IPreferenceStore store= CVSUIPlugin.getPlugin().getPreferenceStore();
			final String value= store.getString(ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
			
			if (MessageDialogWithToggle.NEVER.equals(value))
				return null;
			
			if (MessageDialogWithToggle.PROMPT.equals(value)) {
				
				final String title= CVSUIMessages.CommitCommentArea_2;
				final String message= CVSUIMessages.CommitCommentArea_3;
				final String toggleMessage= CVSUIMessages.CommitCommentArea_4;
				
				final MessageDialogWithToggle dialog= MessageDialogWithToggle.openYesNoQuestion(shell, title, message, toggleMessage, false, store, ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
				if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
					fTextBox.setFocus();
					return null;
				}
			}
		}
		return getComment(true);
	}

	
	public void setProject(IProject iProject) {
		this.fMainProject = iProject;
	}
	
	public void setFocus() {
		if (fTextBox != null) {
			fTextBox.setFocus();
		}
	}
	
	public void setProposedComment(String proposedComment) {
		if (proposedComment == null || proposedComment.length() == 0) {
			this.fProposedComment = null;
		} else {
			this.fProposedComment = proposedComment;
		}
	}
	
	public boolean hasCommitTemplate() {
		try {
			String commitTemplate = getCommitTemplate();
			return commitTemplate != null && commitTemplate.length() > 0;
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return false;
		}
	}
	
	public void setEnabled(boolean enabled) {
		fTextBox.setEnabled(enabled);
		fComboBox.setEnabled(enabled);
	}
	
	public Composite getComposite() {
		return fComposite;
	}
	
	@Override
	protected void firePropertyChangeChange(String property, Object oldValue, Object newValue) {
		super.firePropertyChangeChange(property, oldValue, newValue);
	}
	
	private String getInitialComment() {
		if (fProposedComment != null)
			return fProposedComment;
		try {
			return getCommitTemplate();
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return ""; //$NON-NLS-1$
		}
	}

	private String strip(String comment) {
		// strip template from the comment entered
		try {
			final String commitTemplate = getCommitTemplate();
			if (comment.startsWith(commitTemplate)) {
				return comment.substring(commitTemplate.length());
			} else if (comment.endsWith(commitTemplate)) {
				return comment.substring(0, comment.length() - commitTemplate.length());
			}
		} catch (CVSException e) {
			// we couldn't get the commit template. Log the error and continue
			CVSUIPlugin.log(e);
		}
		return comment;
	}

	private CVSTeamProvider getProvider() {
		if (fMainProject == null) return null;
		return (CVSTeamProvider) RepositoryProvider.getProvider(fMainProject, CVSProviderPlugin.getTypeId());
	}

	private String getCommitTemplate() throws CVSException {
		CVSTeamProvider provider = getProvider();
		if (provider == null)
			return ""; //$NON-NLS-1$
		final String template = provider.getCommitTemplate();
		return template != null ? template : ""; //$NON-NLS-1$
	}
}
