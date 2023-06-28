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
 *     vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *     Andrey Loskutov <loskutov@gmx.de> - Bug 466789
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.AbstractConsole;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * Page book console view.
 *
 * @since 3.0
 */
public class ConsoleView extends PageBookView implements IConsoleView, IConsoleListener, IPropertyChangeListener, IPartListener2 {

	/**
	 * Whether this console is pinned.
	 */
	private boolean fPinned = false;

	/**
	 * Stack of consoles in MRU order
	 */
	private List<IConsole> fStack = new ArrayList<>();

	/**
	 * The console being displayed, or <code>null</code> if none
	 */
	private IConsole fActiveConsole = null;

	/**
	 * Map of consoles to dummy console parts (used to close pages)
	 */
	private Map<IConsole, ConsoleWorkbenchPart> fConsoleToPart;

	/**
	 * Map of consoles to array of page participants
	 */
	private Map<IConsole, ListenerList<IConsolePageParticipant>> fConsoleToPageParticipants;

	/**
	 * Map of parts to consoles
	 */
	private Map<ConsoleWorkbenchPart, IConsole> fPartToConsole;

	/**
	 * Whether this view is active
	 */
	private boolean fActive = false;

	/**
	 * 'In Console View' context
	 */
	private IContextActivation fActivatedContext;

	// actions
	private PinConsoleAction fPinAction = null;
	private ConsoleDropDownAction fDisplayConsoleAction = null;

	private OpenConsoleAction fOpenConsoleAction = null;

	private boolean fScrollLock;
	private boolean fWordWrap;

	private boolean isAvailable() {
		return getPageBook() != null && !getPageBook().isDisposed();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Object source = event.getSource();
		if (source instanceof IConsole && event.getProperty().equals(IBasicPropertyConstants.P_TEXT)) {
			if (source.equals(getConsole())) {
				updateTitle();
			}
		}

	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		super.partClosed(part);
		fPinAction.update();
	}

	@Override
	public IConsole getConsole() {
		return fActiveConsole;
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		// don't show the page when pinned, unless this is the first console to be added
		// or its the default page
		if (fActiveConsole != null && pageRec.page != getDefaultPage() && fPinned && fConsoleToPart.size() > 1) {
			IConsole console = fPartToConsole.get(pageRec.part);
			if (!fStack.contains(console)) {
				fStack.add(console);
			}
			return;
		}

		IConsole recConsole = fPartToConsole.get(pageRec.part);
		if (recConsole!=null && recConsole.equals(fActiveConsole)) {
			return;
		}

		super.showPageRec(pageRec);

		if (fActiveConsole != recConsole) {
			if (fActive && fActiveConsole != null) {
				deactivateParticipants(fActiveConsole);
			}
			if (recConsole != null) {
				activateParticipants(recConsole);
			}
		}
		fActiveConsole = recConsole;
		// bring active console on top of stack
		if (fActiveConsole != null && !fStack.isEmpty() && !fActiveConsole.equals(fStack.get(0))) {
			fStack.remove(fActiveConsole);
			fStack.add(0, fActiveConsole);
		}
		updateTitle();
		updateHelp();
		// update console actions
		if (fPinAction != null) {
			fPinAction.update();
		}
		IPage page = getCurrentPage();
		if (page instanceof IOConsolePage) {
			((IOConsolePage) page).setWordWrap(fWordWrap);
		}
		/*
		 * Bug 268608: cannot invoke find/replace after opening console
		 *
		 * Global actions of TextConsolePage must be updated here,
		 * but they are only updated on a selection change.
		 */
		if (page instanceof TextConsolePage) {
			TextConsoleViewer viewer = ((TextConsolePage) page).getViewer();
			viewer.setSelection(viewer.getSelection());
		}
	}

	/**
	 * Activates the participants for the given console, if any.
	 *
	 * @param console the console
	 */
	private void activateParticipants(IConsole console) {
		// activate
		if (console != null && fActive) {
			final ListenerList<IConsolePageParticipant> listeners = getParticipants(console);
			if (listeners != null) {
				for (IConsolePageParticipant iConsolePageParticipant : listeners) {
					final IConsolePageParticipant participant = iConsolePageParticipant;
					SafeRunner.run(new ISafeRunnable() {
						@Override
						public void run() throws Exception {
							participant.activated();
						}
						@Override
						public void handleException(Throwable exception) {
							ConsolePlugin.log(exception);
							listeners.remove(participant);
						}
					});
				}
			}
		}
	}

	/**
	 * Returns a stack of consoles in the view in MRU order.
	 *
	 * @return a stack of consoles in the view in MRU order
	 */
	protected List<IConsole> getConsoleStack() {
		return fStack;
	}

	/**
	 * Updates the view title based on the active console
	 */
	protected void updateTitle() {
		IConsole console = getConsole();
		if (console == null) {
			setContentDescription(ConsoleMessages.ConsoleView_0);
		} else {
			String newName = console.getName();
			String oldName = getContentDescription();
			if (newName!=null && !(newName.equals(oldName))) {
				setContentDescription(console.getName());
			}
		}
	}

	protected void updateHelp() {
		IConsole console = getConsole();
		String helpContextId = null;
		if (console instanceof AbstractConsole) {
			AbstractConsole abs = (AbstractConsole) console;
			helpContextId = abs.getHelpContextId();
		}
		if (helpContextId == null) {
			helpContextId = IConsoleHelpContextIds.CONSOLE_VIEW;
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getPageBook().getParent(), helpContextId);
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IConsole console = fPartToConsole.get(part);

		// dispose page participants
		ListenerList<IConsolePageParticipant> listeners = fConsoleToPageParticipants.remove(console);
		if (listeners != null) {
			for (IConsolePageParticipant iConsolePageParticipant : listeners) {
				final IConsolePageParticipant participant = iConsolePageParticipant;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						participant.dispose();
					}
					@Override
					public void handleException(Throwable exception) {
						ConsolePlugin.log(exception);
					}
				});
			}
		}

		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();
		console.removePropertyChangeListener(this);

		// empty cross-reference cache
		fPartToConsole.remove(part);
		fConsoleToPart.remove(console);
		if (fPartToConsole.isEmpty()) {
			fActiveConsole = null;
		}

		// update console actions
		fPinAction.update();
	}

	/**
	 * Returns the page participants registered for the given console, or
	 * <code>null</code>
	 *
	 * @param console the console
	 * @return registered page participants or <code>null</code>
	 */
	private ListenerList<IConsolePageParticipant> getParticipants(IConsole console) {
		return fConsoleToPageParticipants.get(console);
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart dummyPart) {
		ConsoleWorkbenchPart part = (ConsoleWorkbenchPart)dummyPart;
		final IConsole console = part.getConsole();
		final IPageBookViewPage page = console.createPage(this);
		initPage(page);
		page.createControl(getPageBook());
		console.addPropertyChangeListener(this);

		// initialize page participants
		IConsolePageParticipant[] consoleParticipants = ((ConsoleManager)getConsoleManager()).getPageParticipants(console);
		final ListenerList<IConsolePageParticipant> participants = new ListenerList<>();
		for (IConsolePageParticipant consoleParticipant : consoleParticipants) {
			participants.add(consoleParticipant);
		}
		fConsoleToPageParticipants.put(console, participants);
		for (IConsolePageParticipant iConsolePageParticipant : participants) {
			final IConsolePageParticipant participant = iConsolePageParticipant;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					participant.init(page, console);
				}
				@Override
				public void handleException(Throwable exception) {
					ConsolePlugin.log(exception);
					participants.remove(participant);
				}
			});
		}

		PageRec rec = new PageRec(dummyPart, page);
		return rec;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof ConsoleWorkbenchPart;
	}

	@Override
	public void dispose() {
		IViewSite site = getViewSite();
		if(site != null) {
			site.getPage().removePartListener((IPartListener2)this);
		}
		super.dispose();
		ConsoleManager consoleManager = (ConsoleManager) ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.removeConsoleListener(this);
		consoleManager.unregisterConsoleView(this);
		if (fDisplayConsoleAction != null) {
			fDisplayConsoleAction.dispose();
			fDisplayConsoleAction = null;
		}
		if (fOpenConsoleAction != null) {
			fOpenConsoleAction.dispose();
			fOpenConsoleAction = null;
		}
	}

	/**
	 * Returns the console manager.
	 *
	 * @return the console manager
	 */
	private IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		page.createControl(getPageBook());
		initPage(page);
		return page;
	}

	@Override
	public void consolesAdded(final IConsole[] consoles) {
		if (isAvailable()) {
			Runnable r = () -> {
				for (IConsole console : consoles) {
					if (isAvailable()) {
						// ensure it's still registered since this is done asynchronously
						IConsole[] allConsoles = getConsoleManager().getConsoles();
						for (IConsole registered : allConsoles) {
							if (registered.equals(console)) {
								ConsoleWorkbenchPart part = new ConsoleWorkbenchPart(console, getSite());
								fConsoleToPart.put(console, part);
								fPartToConsole.put(part, console);
								partActivated(part);
								break;
							}
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	@Override
	public void consolesRemoved(final IConsole[] consoles) {
		if (isAvailable()) {
			Runnable r = () -> {
				for (IConsole console : consoles) {
					if (isAvailable()) {
						fStack.remove(console);
						ConsoleWorkbenchPart part = fConsoleToPart.get(console);
						if (part != null) {
							partClosed(part);
						}
						if (getConsole() == null) {
							IConsole[] available = getConsoleManager().getConsoles();
							if (available.length > 0) {
								display(available[available.length - 1]);
							}
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	/**
	 * Constructs a console view
	 */
	public ConsoleView() {
		super();
		fConsoleToPart = new HashMap<>();
		fPartToConsole = new HashMap<>();
		fConsoleToPageParticipants = new HashMap<>();

		ConsoleManager consoleManager = (ConsoleManager) ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.registerConsoleView(this);
	}

	protected void createActions() {
		fPinAction = new PinConsoleAction(this);
		fDisplayConsoleAction = new ConsoleDropDownAction(this);
		ConsoleFactoryExtension[] extensions = ((ConsoleManager)ConsolePlugin.getDefault().getConsoleManager()).getConsoleFactoryExtensions();
		if (extensions.length > 0) {
			fOpenConsoleAction = new OpenConsoleAction();
		}
	}

	protected void configureToolBar(IToolBarManager mgr) {
		mgr.add(new Separator(IConsoleConstants.LAUNCH_GROUP));
		mgr.add(new Separator(IConsoleConstants.OUTPUT_GROUP));
		mgr.add(new Separator("fixedGroup")); //$NON-NLS-1$
		mgr.add(fPinAction);
		mgr.add(fDisplayConsoleAction);
		if (fOpenConsoleAction != null) {
			mgr.add(fOpenConsoleAction);
			if (mgr instanceof ToolBarManager) {
				ToolBarManager tbm= (ToolBarManager) mgr;
				final ToolBar tb= tbm.getControl();
				tb.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						ToolItem ti= tb.getItem(new Point(e.x, e.y));
						if (ti != null) {
							if (ti.getData() instanceof ActionContributionItem) {
								ActionContributionItem actionContributionItem= (ActionContributionItem) ti.getData();
								IAction action= actionContributionItem.getAction();
								if (action == fOpenConsoleAction) {
									Event event= new Event();
									event.widget= ti;
									event.x= e.x;
									event.y= e.y;
									action.runWithEvent(event);
								}
							}
						}
					}
				});
			}
		}
	}

	@Override
	public void display(IConsole console) {
		if (fPinned && fActiveConsole != null) {
			return;
		}
		if (console.equals(fActiveConsole)) {
			return;
		}
		ConsoleWorkbenchPart part = fConsoleToPart.get(console);
		if (part != null) {
			partActivated(part);
			// Workaround for bug 345435: call activated for this to force PageBookView to
			// activate the new pages context
			if (fActive) {
				partActivated(this);
			}
		}
	}

	@Override
	public void setPinned(boolean pin) {
		fPinned = pin;
		if (fPinAction != null) {
			fPinAction.update();
		}
	}

	@Override
	public boolean isPinned() {
		return fPinned;
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return null;
	}

	/**
	 * Registers the given runnable with the display associated with this view's
	 * control, if any.
	 *
	 * @param r the runnable
	 * @see org.eclipse.swt.widgets.Display#asyncExec(java.lang.Runnable)
	 */
	public void asyncExec(Runnable r) {
		if (isAvailable()) {
			getPageBook().getDisplay().asyncExec(r);
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
		createActions();
		IToolBarManager tbm= getViewSite().getActionBars().getToolBarManager();
		configureToolBar(tbm);
		updateForExistingConsoles();
		getViewSite().getActionBars().updateActionBars();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IConsoleHelpContextIds.CONSOLE_VIEW);
		getViewSite().getPage().addPartListener((IPartListener2)this);
		initPageSwitcher();
	}

	/**
	 * Initialize the PageSwitcher.
	 */
	private void initPageSwitcher() {
		new PageSwitcher(this) {
			@Override
			public void activatePage(Object page) {
				ShowConsoleAction.showConsole((IConsole) page, ConsoleView.this);
			}

			@Override
			public ImageDescriptor getImageDescriptor(Object page) {
				return ((IConsole) page).getImageDescriptor();
			}

			@Override
			public String getName(Object page) {
				return ((IConsole) page).getName();
			}

			@Override
			public Object[] getPages() {
				return getConsoleManager().getConsoles();
			}

			@Override
			public int getCurrentPageIndex() {
				IConsole currentConsole= getConsole();
				IConsole[] consoles= getConsoleManager().getConsoles();
				for (int i= 0; i < consoles.length; i++) {
					if (consoles[i].equals(currentConsole)) {
						return i;
					}
				}
				return super.getCurrentPageIndex();
			}
		};
	}

	/**
	 * Initialize for existing consoles
	 */
	private void updateForExistingConsoles() {
		IConsoleManager manager = getConsoleManager();
		// create pages for consoles
		IConsole[] consoles = manager.getConsoles();
		consolesAdded(consoles);
		// add as a listener
		manager.addConsoleListener(this);
	}

	@Override
	public void warnOfContentChange(IConsole console) {
		IWorkbenchPart part = fConsoleToPart.get(console);
		if (part != null) {
			IWorkbenchSiteProgressService service = part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
			if (service != null) {
				service.warnOfContentChange();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> key) {
		Object adpater = super.getAdapter(key);
		if (adpater == null) {
			IConsole console = getConsole();
			if (console != null) {
				ListenerList<IConsolePageParticipant> listeners = getParticipants(console);
				// an adapter can be asked for before the console participants are created
				if (listeners != null) {
					for (IConsolePageParticipant iConsolePageParticipant : listeners) {
						IConsolePageParticipant participant = iConsolePageParticipant;
						adpater = participant.getAdapter(key);
						if (adpater != null) {
							return (T) adpater;
						}
					}
				}
			}
		}
		return (T) adpater;
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			fActive = true;
			IContextService contextService = getSite().getService(IContextService.class);
			if(contextService != null) {
				fActivatedContext = contextService.activateContext(IConsoleConstants.ID_CONSOLE_VIEW);
				activateParticipants(fActiveConsole);
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			fActive = false;
			IContextService contextService = getSite().getService(IContextService.class);
			if(contextService != null) {
				contextService.deactivateContext(fActivatedContext);
				deactivateParticipants(fActiveConsole);
			}
		}
	}

	/**
	 * Returns if the specified part reference is to this view part (if the part
	 * reference is the console view or not)
	 *
	 * @param partRef the workbench part reference
	 * @return true if the specified part reference is the console view
	 */
	protected boolean isThisPart(IWorkbenchPartReference partRef) {
		if (partRef instanceof IViewReference) {
			IViewReference viewRef = (IViewReference) partRef;
			if (getViewSite() != null && viewRef.getId().equals(getViewSite().getId())) {
				String secId = viewRef.getSecondaryId();
				String mySec = null;
				if (getSite() instanceof IViewSite) {
					mySec = ((IViewSite)getSite()).getSecondaryId();
				}
				if (mySec == null) {
					return secId == null;
				}
				return mySec.equals(secId);
			}
		}
		return false;
	}

	/**
	 * Deactivates participants for the given console, if any.
	 *
	 * @param console console to deactivate
	 */
	private void deactivateParticipants(IConsole console) {
		// deactivate
		if (console != null) {
			final ListenerList<IConsolePageParticipant> listeners = getParticipants(console);
			if (listeners != null) {
				for (IConsolePageParticipant iConsolePageParticipant : listeners) {
					final IConsolePageParticipant participant = iConsolePageParticipant;
					SafeRunner.run(new ISafeRunnable() {
						@Override
						public void run() throws Exception {
							participant.deactivated();
						}
						@Override
						public void handleException(Throwable exception) {
							ConsolePlugin.log(exception);
							listeners.remove(participant);
						}
					});
				}
			}
		}
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
	public void setScrollLock(boolean scrollLock) {
		fScrollLock = scrollLock;

		IPage page = getCurrentPage();
		if (page instanceof IOConsolePage) {
			((IOConsolePage)page).setAutoScroll(!scrollLock);
		}
	}

	@Override
	public boolean getScrollLock() {
		return fScrollLock;
	}

	@Override
	public void setWordWrap(boolean wordWrap) {
		fWordWrap = wordWrap;

		IWorkbenchPart part = getSite().getPart();
		if (part instanceof PageBookView) {
			Control control = ((PageBookView) part).getCurrentPage().getControl();
			if (control instanceof StyledText) {
				((StyledText) control).setWordWrap(wordWrap);
			}
		}
	}

	@Override
	public boolean getWordWrap() {
		return fWordWrap;
	}

	@Override
	public void pin(IConsole console) {
		if (console == null) {
			setPinned(false);
		} else {
			if (isPinned()) {
				setPinned(false);
			}
			display(console);
			setPinned(true);
		}
	}

	@Override
	public void setAutoScrollLock(boolean scrollLock) {
		IPage page = getCurrentPage();
		if (page instanceof IOConsolePage) {
			((IOConsolePage) page).setAutoScroll(!scrollLock);
		}

	}

	@Override
	public boolean getAutoScrollLock() {
		IPage page = getCurrentPage();
		if (page instanceof IOConsolePage) {
			return !((IOConsolePage) page).isAutoScroll();
		}
		return fScrollLock;
	}
}
