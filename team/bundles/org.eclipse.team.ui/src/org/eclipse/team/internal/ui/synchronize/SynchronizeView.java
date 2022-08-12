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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.team.internal.ui.synchronize.actions.PasteAction;
import org.eclipse.team.internal.ui.synchronize.actions.PinParticipantAction;
import org.eclipse.team.internal.ui.synchronize.actions.RemoveSynchronizeParticipantAction;
import org.eclipse.team.internal.ui.synchronize.actions.SynchronizeAndRefreshAction;
import org.eclipse.team.internal.ui.synchronize.actions.SynchronizePageDropDownAction;
import org.eclipse.team.internal.ui.synchronize.actions.ToggleLinkingAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantListener;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.osgi.framework.FrameworkUtil;

/**
 * Implements a Synchronize View that contains multiple synchronize participants.
 */
public class SynchronizeView extends PageBookView implements ISynchronizeView, ISynchronizeParticipantListener, IPropertyChangeListener, ISaveablesSource, ISaveablePart, IShowInTarget {

	/**
	 * Suggested maximum length of participant names when shown in certain menus and dialog.
	 */
	public final static int MAX_NAME_LENGTH = 100;

	/**
	 * The participant being displayed, or <code>null</code> if none
	 */
	private ISynchronizeParticipant activeParticipantRef = null;

	/**
	 * Map of participants to dummy participant parts (used to close pages)
	 */
	private Map<ISynchronizeParticipant, IWorkbenchPart> fParticipantToPart;

	/**
	 * Map of parts to participants
	 */
	private Map<IWorkbenchPart, ISynchronizeParticipant> fPartToParticipant;

	/**
	 * Drop down action to switch between participants
	 */
	private SynchronizePageDropDownAction fPageDropDown;

	/**
	 * Action to remove the selected participant
	 */
	private PinParticipantAction fPinAction;

	/**
	 * Action to remove the currently shown participant
	 */
	private RemoveSynchronizeParticipantAction fRemoveCurrentAction;

	/**
	 * Action to remove all non-pinned participants
	 */
	private RemoveSynchronizeParticipantAction fRemoveAllAction;

	private ToggleLinkingAction fToggleLinkingAction;

	/**
	 * Refresh action.
	 * @since 3.7
	 */
	private SynchronizeAndRefreshAction fRefreshAction;


	/**
	 * Action to paste patch into the view, starting a new synchronization.
	 */
	private PasteAction fPastePatchAction;
	private boolean fLinkingEnabled;
	private OpenAndLinkWithEditorHelper fOpenAndLinkWithEditorHelper;


	/**
	 * Preference key to save
	 */
	private static final String KEY_LAST_ACTIVE_PARTICIPANT_ID = "lastactiveparticipant_id"; //$NON-NLS-1$
	private static final String KEY_LAST_ACTIVE_PARTICIPANT_SECONDARY_ID = "lastactiveparticipant_sec_id"; //$NON-NLS-1$
	private static final String KEY_LINK_WITH_EDITOR = "linkWithEditor"; //$NON-NLS-1$
	private static final String KEY_SETTINGS_SECTION= "SynchronizeViewSettings"; //$NON-NLS-1$


	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Object source = event.getSource();
		if (source instanceof ISynchronizeParticipant) {
			if (event.getProperty().equals(IBasicPropertyConstants.P_TEXT)) {
				if (source.equals(getParticipant())) {
					updateTitle();
				}
			} else if (event.getProperty().equals(ModelSynchronizeParticipant.PROP_DIRTY)) {
				Display.getDefault().asyncExec(() -> firePropertyChange(PROP_DIRTY));
			} else if (event.getProperty().equals(ModelSynchronizeParticipant.PROP_ACTIVE_SAVEABLE)) {
				Saveable oldSaveable = (Saveable)event.getOldValue();
				Saveable newSaveable = (Saveable)event.getNewValue();
				ISaveablesLifecycleListener listener = getSite().getPage().getWorkbenchWindow()
					.getService(ISaveablesLifecycleListener.class);
				if (listener != null && oldSaveable != null)
					listener.handleLifecycleEvent(new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_CLOSE, new Saveable[] { oldSaveable }, false));
				if (listener != null && newSaveable != null)
					listener.handleLifecycleEvent(new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_OPEN, new Saveable[] { newSaveable }, false));
			} else if (event.getProperty().equals(ISynchronizeParticipant.P_CONTENT)) {
				final IWorkbenchSiteProgressService ps = getSite().getAdapter(IWorkbenchSiteProgressService.class);
				if (ps != null)
					Display.getDefault().asyncExec(() -> ps.warnOfContentChange());
			}
		}
		if (source instanceof ISynchronizePageConfiguration) {
			ISynchronizePageConfiguration configuration = (ISynchronizePageConfiguration) source;
			if (event.getProperty().equals(ISynchronizePageConfiguration.P_PAGE_DESCRIPTION)) {
				if (configuration.getParticipant().equals(getParticipant())) {
					updateTitle();
				}
			} else if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODE)) {
				if (configuration.getParticipant().equals(getParticipant())) {
					updateTitle();
				}
			}
		}
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		super.partClosed(part);
	}

	@Override
	public ISynchronizeParticipant getParticipant() {
		return activeParticipantRef;
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		super.showPageRec(pageRec);
		activeParticipantRef = fPartToParticipant.get(pageRec.part);
		updateActionEnablements();
		updateTitle();
	}

	/*
	 * Updates the view title based on the active participant
	 */
	protected void updateTitle() {
		ISynchronizeParticipant participant = getParticipant();
		if (participant == null) {
			setContentDescription(""); //$NON-NLS-1$
		} else {
			SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)fParticipantToPart.get(participant);
			ISynchronizePageConfiguration configuration = part.getConfiguration();
			String description = (String)configuration.getProperty(ISynchronizePageConfiguration.P_PAGE_DESCRIPTION);
			if (description == null)
				description = part.getParticipant().getName();
			// TODO: Get the description from the configuration
			// TODO: listen to the configuration for description changes
			setContentDescription(Utils.shortenText(MAX_NAME_LENGTH, description));
			setStatusLineMessage(description, configuration.getMode());
		}
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();
		SynchronizeViewWorkbenchPart syncPart = (SynchronizeViewWorkbenchPart) part;
		ISynchronizeParticipant participant = syncPart.getParticipant();
		clearCrossReferenceCache(part, participant);
	}

	private void clearCrossReferenceCache(IWorkbenchPart part, ISynchronizeParticipant participant) {
		participant.removePropertyChangeListener(this);
		if (part == null)
			return;
		ISynchronizePageConfiguration configuration = ((SynchronizeViewWorkbenchPart)part).getConfiguration();
		if (configuration != null)
			configuration.removePropertyChangeListener(this);
		fPartToParticipant.remove(part);
		fParticipantToPart.remove(participant);
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart dummyPart) {
		SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)dummyPart;
		ISynchronizeParticipant participant = part.getParticipant();
		participant.addPropertyChangeListener(this);
		ISynchronizePageConfiguration configuration = participant.createPageConfiguration();
		part.setConfiguration(configuration);
		configuration.addPropertyChangeListener(this);
		IPageBookViewPage page = participant.createPage(configuration);
		if(page != null) {
			initPage(page);
			initPage(configuration, page);
			page.createControl(getPageBook());
			PageRec rec = new PageRec(dummyPart, page);
			return rec;
		}
		return null;
	}

	protected void initPage(ISynchronizePageConfiguration configuration, IPageBookViewPage page) {
		// A page site does not provide everything the page may need
		// Also provide the synchronize page site if the page is a synchronize view page
		((SynchronizePageConfiguration)configuration).setSite(new WorkbenchPartSynchronizePageSite(this, page.getSite(), getDialogSettings(configuration.getParticipant())));
		if (page instanceof ISynchronizePage) {
			try {
				((ISynchronizePage)page).init(configuration.getSite());
			} catch (PartInitException e) {
				TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		page.getSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), fRefreshAction);
		page.getSite().getActionBars().updateActionBars();
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof SynchronizeViewWorkbenchPart;
	}

	@Override
	public void dispose() {
		super.dispose();
		TeamUI.getSynchronizeManager().removeSynchronizeParticipantListener(this);
		// Pin action is hooked up to listeners, must call dispose to un-register.
		fPinAction.dispose();
		fPastePatchAction.dispose();
		// Remember the last active participant
		if(activeParticipantRef != null) {
			rememberCurrentParticipant();
		}
		fParticipantToPart = null;
		fPartToParticipant = null;
		// Remove 'Link with Editor' listener when closing the view
		getSite().getPage().removePartListener(fLinkWithEditorListener);
	}

	/**
	 *
	 */
	private void rememberCurrentParticipant() {
		IDialogSettings section = getDialogSettings();
		section.put(KEY_LAST_ACTIVE_PARTICIPANT_ID, activeParticipantRef.getId());
		section.put(KEY_LAST_ACTIVE_PARTICIPANT_SECONDARY_ID, activeParticipantRef.getSecondaryId());
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		Page page = new MessagePage();
		page.createControl(getPageBook());
		initPage(page);
		return page;
	}

	@Override
	public void participantsAdded(final ISynchronizeParticipant[] participants) {
		for (ISynchronizeParticipant participant : participants) {
			if (isAvailable() && select(TeamUI.getSynchronizeManager().get(participant.getId(), participant.getSecondaryId()))) {
				SynchronizeViewWorkbenchPart part = new SynchronizeViewWorkbenchPart(participant, getSite());
				fParticipantToPart.put(participant, part);
				fPartToParticipant.put(part, participant);
			}
		}
		Display.getDefault().asyncExec(() -> firePropertyChange(PROP_DIRTY));
	}

	@Override
	public void participantsRemoved(final ISynchronizeParticipant[] participants) {
		if (isAvailable()) {
			Runnable r = () -> {
				for (ISynchronizeParticipant participant : participants) {
					if (isAvailable()) {
						SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)fParticipantToPart.get(participant);
						if (part != null) {
							partClosed(part);
							clearCrossReferenceCache(part, participant);
						}
						// Remove any settings created for the participant
						removeDialogSettings(participant);
						if (getParticipant() == null) {
							ISynchronizeParticipantReference[] available = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
							if (available.length > 0) {
								ISynchronizeParticipant p;
								try {
									p = available[available.length - 1].getParticipant();
								} catch (TeamException e) {
									return;
								}
								display(p);
							} else {
								/*
								 * Remove 'Link with Editor' listener if
								 * there are no more participants available.
								 */
								getSite().getPage().removePartListener(fLinkWithEditorListener);
							}
						}
					}
				}
				firePropertyChange(PROP_DIRTY);
			};
			asyncExec(r);
		}
	}

	/**
	 * Constructs a synchronize view
	 */
	public SynchronizeView() {
		super();
		fParticipantToPart = new HashMap<>();
		fPartToParticipant = new HashMap<>();
		updateTitle();
	}

	/**
	 * Create the default actions for the view. These will be shown regardless of the
	 * participant being displayed.
	 */
	protected void createActions() {
		fPageDropDown= new SynchronizePageDropDownAction(this);
		fRefreshAction= new SynchronizeAndRefreshAction(this);
		fPinAction = new PinParticipantAction();
		fToggleLinkingAction = new ToggleLinkingAction(this);
		fRemoveCurrentAction = new RemoveSynchronizeParticipantAction(this, false);
		fRemoveAllAction = new RemoveSynchronizeParticipantAction(this, true);
		fPastePatchAction = new PasteAction(this);
		updateActionEnablements();
	}

	private void updateActionEnablements() {
		if (fPinAction != null) {
			fPinAction.setParticipant(activeParticipantRef);
		}
		if (fToggleLinkingAction != null) {
			fToggleLinkingAction.setEnabled(getParticipant() != null);
		}
		if (fRemoveAllAction != null) {
			fRemoveAllAction.setEnabled(getParticipant() != null);
		}
		if (fRemoveCurrentAction != null) {
			fRemoveCurrentAction.setEnabled(getParticipant() != null);
		}
		if (fPastePatchAction != null) {
			// The action is always enabled
			fPastePatchAction.setEnabled(true);
		}
	}

	/**
	 * Add the actions to the toolbar
	 *
	 * @param bars the action bars
	 */
	protected void configureToolBar(IActionBars bars) {
		IToolBarManager mgr = bars.getToolBarManager();
		mgr.add(fPageDropDown);
		mgr.add(fPinAction);
		IMenuManager menu = bars.getMenuManager();
		menu.add(fPinAction);
		menu.add(fToggleLinkingAction);
		menu.add(fRemoveCurrentAction);
		menu.add(fRemoveAllAction);

		IHandlerService handlerService= this.getViewSite().getService(IHandlerService.class);
		handlerService.activateHandler(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR, new ActionHandler(fToggleLinkingAction));
		handlerService.activateHandler(ActionFactory.PASTE.getCommandId(), new ActionHandler(fPastePatchAction));
	}

	@Override
	public void display(ISynchronizeParticipant participant) {
		SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)fParticipantToPart.get(participant);
		if (part != null) {
			partActivated(part);
			fPageDropDown.update();
			createOpenAndLinkWithEditorHelper(getViewer());
			rememberCurrentParticipant();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getPageBook().getParent(), participant.getHelpContextId());
		}
	}

	private void createOpenAndLinkWithEditorHelper(StructuredViewer viewer) {
		if (fOpenAndLinkWithEditorHelper != null)
			fOpenAndLinkWithEditorHelper.dispose();
		fOpenAndLinkWithEditorHelper= new OpenAndLinkWithEditorHelper(viewer) {
			@Override
			protected void activate(ISelection selection) {
				try {
					final Object selectedElement = getSingleElement(selection);
					if (isOpenInEditor(selectedElement) != null)
						if (selectedElement instanceof IFile)
							openInEditor((IFile) selectedElement, true);
				} catch (PartInitException ex) {
					// ignore if no editor input can be found
				}
			}

			@Override
			protected void linkToEditor(ISelection selection) {
				SynchronizeView.this.linkToEditor(selection);
			}

			@Override
			protected void open(ISelection selection, boolean activate) {
				// TODO: implement, bug 291211
			}
		};
		fOpenAndLinkWithEditorHelper.setLinkWithEditor(isLinkingEnabled());
		setLinkingEnabled(isLinkingEnabled());
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return null;
	}

	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * @param runnable a runnable
	 */
	public void asyncExec(Runnable runnable) {
		if (isAvailable()) {
			getPageBook().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Creates this view's underlying viewer and actions.
	 * Hooks a pop-up menu to the underlying viewer's control,
	 * as well as a key listener. When the delete key is pressed,
	 * the <code>REMOVE_ACTION</code> is invoked. Hooks help to
	 * this view. Subclasses must implement the following methods
	 * which are called in the following order when a view is
	 * created:<ul>
	 * <li><code>createViewer(Composite)</code> - the context
	 *   menu is hooked to the viewer's control.</li>
	 * <li><code>createActions()</code></li>
	 * <li><code>configureToolBar(IToolBarManager)</code></li>
	 * <li><code>getHelpContextId()</code></li>
	 * </ul>
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		restoreLinkingEnabled();
		createActions();
		configureToolBar(getViewSite().getActionBars());
		updateForExistingParticipants();
		getViewSite().getActionBars().updateActionBars();
		updateTitle();

		IWorkbenchSiteProgressService progress = getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if(progress != null) {
			progress.showBusyForFamily(ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION);
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.SYNC_VIEW);
	}

	/**
	 * Initialize for existing participants
	 */
	private void updateForExistingParticipants() {
		ISynchronizeManager manager = TeamUI.getSynchronizeManager();
		List participants = Arrays.asList(getParticipants());
		boolean errorOccurred = false;
		for (Object participant : participants) {
			try {
				ISynchronizeParticipantReference ref = (ISynchronizeParticipantReference)participant;
				participantsAdded(new ISynchronizeParticipant[] {ref.getParticipant()});
			} catch (TeamException e) {
				errorOccurred = true;
				continue;
			}

		}
		if (errorOccurred) {
			participants = Arrays.asList(getParticipants());
		}
		try {
			// decide which participant to show	on startup
			if (participants.size() > 0) {
				ISynchronizeParticipantReference participantToSelect = (ISynchronizeParticipantReference)participants.get(0);
				IDialogSettings section = getDialogSettings();
				String selectedParticipantId = section.get(KEY_LAST_ACTIVE_PARTICIPANT_ID);
				String selectedParticipantSecId = section.get(KEY_LAST_ACTIVE_PARTICIPANT_SECONDARY_ID);
				if(selectedParticipantId != null) {
					ISynchronizeParticipantReference selectedParticipant = manager.get(selectedParticipantId, selectedParticipantSecId);
					if(selectedParticipant != null) {
						participantToSelect = selectedParticipant;
					}
				}
				display(participantToSelect.getParticipant());
			}

			// add as a listener to update when new participants are added
			manager.addSynchronizeParticipantListener(this);
		} catch (TeamException e) {
			Utils.handle(e);
		}
	}

	private ISynchronizeParticipantReference[] getParticipants() {
		ISynchronizeManager manager = TeamUI.getSynchronizeManager();
		// create pages
		List<ISynchronizeParticipantReference> participants = new ArrayList<>();
		ISynchronizeParticipantReference[] refs = manager.getSynchronizeParticipants();
		for (ISynchronizeParticipantReference ref : refs) {
			if(select(ref)) {
				participants.add(ref);
			}
		}
		return participants.toArray(new ISynchronizeParticipantReference[participants.size()]);
	}

	private boolean isAvailable() {
		return getPageBook() != null && !getPageBook().isDisposed();
	}

	/*
	 * Method used by test cases to access the page for a participant
	 */
	public IPage getPage(ISynchronizeParticipant participant) {
		IWorkbenchPart part = fParticipantToPart.get(participant);
		if (part == null) return null;
		try {
			return getPageRec(part).page;
		} catch (NullPointerException e) {
			// The PageRec class is not visible so we can't do a null check
			// before accessing the page.
			return null;
		}
	}

	protected boolean select(ISynchronizeParticipantReference ref) {
		return true;
	}

	/*
	 * Return the dialog settings for the view
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings workbenchSettings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(SynchronizeView.class)).getDialogSettings();
		IDialogSettings syncViewSettings = workbenchSettings.getSection(KEY_SETTINGS_SECTION);
		if (syncViewSettings == null) {
			syncViewSettings = workbenchSettings.addNewSection(KEY_SETTINGS_SECTION);
		}
		return syncViewSettings;
	}

	private String getSettingsKey(ISynchronizeParticipant participant) {
		String id = participant.getId();
		String secondaryId = participant.getSecondaryId();
		return secondaryId == null ? id : id + '.' + secondaryId;
	}

	private IDialogSettings getDialogSettings(ISynchronizeParticipant participant) {
		String key = getSettingsKey(participant);
		IDialogSettings viewsSettings = getDialogSettings();
		IDialogSettings settings = viewsSettings.getSection(key);
		if (settings == null) {
			settings = viewsSettings.addNewSection(key);
		}
		return settings;
	}

	private void removeDialogSettings(ISynchronizeParticipant participant) {
		String key = getSettingsKey(participant);
		IDialogSettings settings = getDialogSettings();
		if (settings.getSection(key) != null) {
			// There isn't an explicit remove so just make sure
			// That the old settings are forgotten
			getDialogSettings().addSection(new DialogSettings(key));
		}
	}

	@Override
	public Saveable[] getSaveables() {
		Set<Saveable> result = new HashSet<>();
		for (Object element : fPartToParticipant.keySet()) {
			SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart) element;
			Saveable saveable = getSaveable(part.getParticipant());
			if (saveable != null) {
				result.add(saveable);
			}
		}
		return result.toArray(new Saveable[result.size()]);
	}

	private Saveable getSaveable(ISynchronizeParticipant participant) {
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			return msp.getActiveSaveable();
		}
		return null;
	}

	@Override
	public Saveable[] getActiveSaveables() {
		ISynchronizeParticipant participant = getParticipant();
		Saveable s = getSaveable(participant);
		if (s != null)
			return new Saveable[] { s };
		return new Saveable[0];
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Saveable[] saveables = getSaveables();
		if (saveables.length == 0)
			return;
		monitor.beginTask(null, 100* saveables.length);
		for (Saveable saveable : saveables) {
			try {
				saveable.doSave(Policy.subMonitorFor(monitor, 100));
			} catch (CoreException e) {
				ErrorDialog.openError(getSite().getShell(), null, e.getMessage(), e.getStatus());
			}
			Policy.checkCanceled(monitor);
		}
		monitor.done();
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
		// Not allowed
	}

	@Override
	public boolean isDirty() {
		Saveable[] saveables = getSaveables();
		for (Saveable saveable : saveables) {
			if (saveable.isDirty())
				return true;
		}
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		return true;
	}

	private void setStatusLineMessage(String description, int mode) {
		String syncMode = null;
		ResourceBundle bundle = Policy.getActionBundle();
		switch (mode) {
		case ISynchronizePageConfiguration.INCOMING_MODE:
			syncMode = Utils.getString("action.directionFilterIncoming.tooltip", bundle); //$NON-NLS-1$
			break;
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			syncMode = Utils.getString("action.directionFilterOutgoing.tooltip", bundle); //$NON-NLS-1$
			break;
		case ISynchronizePageConfiguration.BOTH_MODE:
			syncMode = Utils.getString("action.directionFilterBoth.tooltip", bundle); //$NON-NLS-1$
			break;
		case ISynchronizePageConfiguration.CONFLICTING_MODE:
			syncMode = Utils.getString("action.directionFilterConflicts.tooltip", bundle); //$NON-NLS-1$
			break;
		}

		IViewSite viewSite = getViewSite();
		if (viewSite != null && syncMode != null) {
			viewSite.getActionBars().getStatusLineManager().setMessage(
					NLS.bind(TeamUIMessages.SynchronizeView_statusLine,
							new String[] { description, syncMode }));
		}
	}

	// copy-pasted from org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart and modified

	private IPartListener2 fLinkWithEditorListener= new IPartListener2() {
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {}
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {}
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {}
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {}
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			if (partRef instanceof IEditorReference) {
				editorActivated(((IEditorReference) partRef).getEditor(true));
			}
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (partRef instanceof IEditorReference) {
				editorActivated(((IEditorReference) partRef).getEditor(true));
			}
		}

	};

	public boolean isLinkingEnabled() {
		return fLinkingEnabled;
	}

	private static IElementComparer COMPARER = new IElementComparer() {

		private Object getContributedResourceOrResourceVariant(Object o) {
			IResource[] resources = Utils.getContributedResources(new Object[] {o});
			if (resources.length > 0)
				return resources[0];
			if (o instanceof SyncInfoModelElement) {
				SyncInfoModelElement sime = (SyncInfoModelElement) o;
				return sime.getSyncInfo().getRemote();
			}
			return null;
		}

		@Override
		public int hashCode(Object element) {
			Object r = getContributedResourceOrResourceVariant(element);
			if (r != null)
				return r.hashCode();
			return element.hashCode();
		}

		@Override
		public boolean equals(Object a, Object b) {
			// no need to check for null, CustomeHashtable cannot contain null keys
			if (a instanceof IResource || a instanceof IResourceVariant) {
				b = getContributedResourceOrResourceVariant(b);
			} else if (b instanceof IResource || b instanceof IResourceVariant) {
				a = getContributedResourceOrResourceVariant(a);
				return b.equals(a); // a may be null
			}
			return a.equals(b);
		}
	};

	public void setLinkingEnabled(boolean enabled) {
		fLinkingEnabled= enabled;
		IDialogSettings dialogSettings = getDialogSettings();
		dialogSettings.put(KEY_LINK_WITH_EDITOR, fLinkingEnabled);

		IWorkbenchPage page= getSite().getPage();
		if (enabled) {
			page.addPartListener(fLinkWithEditorListener);

			IEditorPart editor = page.getActiveEditor();
			if (editor != null)
				editorActivated(editor);
		} else {
			page.removePartListener(fLinkWithEditorListener);
		}
		fOpenAndLinkWithEditorHelper.setLinkWithEditor(enabled);

	}

	private void restoreLinkingEnabled() {
		fLinkingEnabled = getDialogSettings().getBoolean(KEY_LINK_WITH_EDITOR);
	}

	/**
	 * Links to editor (if option enabled)
	 * @param selection the selection
	 */
	private void linkToEditor(ISelection selection) {
		Object obj = getSingleElement(selection);
		if (obj != null) {
			IEditorPart part = isOpenInEditor(obj);
			if (part != null) {
				IWorkbenchPage page= getSite().getPage();
				page.bringToTop(part);
			}
		}
	}

	/**
	 * An editor has been activated. Set the selection in the Sync View
	 * to be the editor's input, if linking is enabled.
	 * @param editor the activated editor
	 */
	private void editorActivated(IEditorPart editor) {
		if (!isLinkingEnabled())
			return;

		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput == null)
			return;
		Object input= getInputFromEditor(editorInput);
		if (input == null)
			return;
		if (!inputIsSelected(editorInput))
			showInput(input);
		else
			getViewer().getTree().showSelection();
	}

	boolean showInput(Object input) {
		Object element = input;
		if (element != null) {
			IElementComparer previousComparer = getViewer().getComparer();
			getViewer().setComparer(COMPARER);
			try {
				ISelection newSelection = new StructuredSelection(element);
				if (getViewer().getSelection().equals(newSelection)) {
					getViewer().reveal(element);
				} else {
					getViewer().setSelection(newSelection, true);

					while (element != null && getViewer().getSelection().isEmpty()) {
						// Try to select parent in case element is filtered
						element = getParent(element);
						if (element != null) {
							newSelection = new StructuredSelection(element);
							getViewer().setSelection(newSelection, true);
						} else {
							// Failed to find parent to select
							return false;
						}
					}
				}
				return true;
			} finally {
				getViewer().setComparer(previousComparer);
			}
		}
		return false;
	}

	/**
	 * Returns the element's parent.
	 * @param element the element
	 *
	 * @return the parent or <code>null</code> if there's no parent
	 */
	private Object getParent(Object element) {
		if (element instanceof IResource)
			return ((IResource)element).getParent();
		return null;
	}

	private TreeViewer getViewer() {
		IPage currentPage = getCurrentPage();
		if (currentPage instanceof ISynchronizePage) {
			return (TreeViewer) ((ISynchronizePage)currentPage).getViewer();
		}
		/*
		 * We should never get here. fLinkWithEditorListener is removed when no
		 * participants are available and the method should not be called
		 * afterwards. See participantsRemoved method for the listener's
		 * removal.
		 */
		Assert.isTrue(false);
		return null;
	}

	private boolean inputIsSelected(IEditorInput input) {
		IStructuredSelection selection= getViewer().getStructuredSelection();
		if (selection.size() != 1)
			return false;
		IEditorInput selectionAsInput= getEditorInput(selection.getFirstElement());
		return input.equals(selectionAsInput);
	}

	private static IEditorInput getEditorInput(Object input) {
		IResource[] resources = Utils.getContributedResources(new Object[] { input });
		if (resources.length > 0)
			input = resources[0];
		if (input instanceof IFile)
			return new FileEditorInput((IFile) input);
		return null;
	}

	private Object getInputFromEditor(IEditorInput editorInput) {
		Object input= editorInput.getAdapter(IFile.class);
		if (input == null && editorInput instanceof FileRevisionEditorInput) {
			IFileRevision fileRevision = ((FileRevisionEditorInput)editorInput).getFileRevision();
			if (fileRevision instanceof ResourceVariantFileRevision)
				return ((ResourceVariantFileRevision) fileRevision).getVariant();
		}
		if (input == null && editorInput instanceof IStorageEditorInput) {
			try {
				input= ((IStorageEditorInput) editorInput).getStorage();
			} catch (CoreException e) {
				// ignore
			}
		}
		return input;
	}

	// copy-pasted from org.eclipse.jdt.internal.ui.javaeditor.EditorUtility and modified

	private static IEditorPart isOpenInEditor(Object inputElement) {
		IEditorInput input = getEditorInput(inputElement);
		if (input != null) {
			IWorkbenchPage p = TeamUIPlugin.getActivePage();
			if (p != null) {
				IEditorPart editor = p.findEditor(input);
				if (editor == null) {
					IEditorReference[] er = p.getEditorReferences();
					for (IEditorReference e : er) {
						if (e.getId().equals("org.eclipse.compare.CompareEditor") && matches(e, input)) { //$NON-NLS-1$
							editor = e.getEditor(false);
						}
					}
				}
				return editor;
			}
		}
		return null;
	}

	private static boolean matches(IEditorReference editorRef,
			IEditorInput input) {
		if (input instanceof FileEditorInput) {
			IFile file = ((FileEditorInput) input).getFile();

			CompareEditorInput cei = (CompareEditorInput) ((EditorPart) editorRef
					.getPart(false)).getEditorInput();
			Object compareResult = cei.getCompareResult();

			if (compareResult instanceof IAdaptable) {
				IResource r = ((IAdaptable) compareResult).getAdapter(IResource.class);
				if (r != null)
					return file.equals(r);
			}
			if (compareResult instanceof ICompareInput) {
				ICompareInput compareInput = (ICompareInput) compareResult;
				ITypedElement left = compareInput.getLeft();
				if (left instanceof ResourceNode)
					if (file.equals(((ResourceNode) left).getResource()))
						return true;
				ITypedElement right = compareInput.getRight();
				if (right instanceof ResourceNode)
					if (file.equals(((ResourceNode) right).getResource()))
						return true;
			}
		}
		return false;
	}

	private static IEditorPart openInEditor(IFile file, boolean activate) throws PartInitException {
		if (file == null)
			throwPartInitException(TeamUIMessages.SynchronizeView_fileMustNotBeNull);

		IWorkbenchPage p = TeamUIPlugin.getActivePage();
		if (p == null)
			throwPartInitException(TeamUIMessages.SynchronizeView_noActiveWorkbenchPage);

		IEditorPart editorPart = IDE.openEditor(p, file, activate);
		return editorPart;
	}

	private static void throwPartInitException(String message) throws PartInitException {
		IStatus status = new Status(IStatus.ERROR, TeamUIPlugin.ID, IStatus.OK, message, null);
		throw new PartInitException(status);
	}

	// copy-pasted from org.eclipse.jdt.internal.ui.util.SelectionUtil and modified

	/**
	 * Returns the selected element if the selection consists of a single
	 * element only.
	 *
	 * @param s the selection
	 * @return the selected first element or null
	 */
	private static Object getSingleElement(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection = (IStructuredSelection) s;
		if (selection.size() != 1)
			return null;

		return selection.getFirstElement();
	}

	@Override
	public boolean show(ShowInContext context) {
		Object selection = getSingleElement(context.getSelection());
		if (selection != null) {
			// If can show the selection, do it.
			// Otherwise, fall through and attempt to show the input
			if (showInput(selection))
				return true;
		}
		Object input = context.getInput();
		if (input != null) {
			if (input instanceof IEditorInput) {
				return showInput(getInputFromEditor((IEditorInput) input));
			}
			return showInput(input);
		}
		return false;
	}

	public IAction getPastePatchAction() {
		return fPastePatchAction;
	}
}