/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import static org.eclipse.jface.util.Util.isValid;

import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

public class HelpView extends ViewPart implements IPartListener2, ISelectionChangedListener, IPageChangedListener {
	private FormToolkit toolkit;

	private String firstPageId;

	private ReusableHelpPart reusableHelpPart;

	private IWorkbenchPart monitoredPart;

	private boolean visible;

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.getColors().initializeSectionToolBarColors();
		reusableHelpPart.createControl(parent, toolkit);
		reusableHelpPart.setDefaultContextHelpText(Messages.HelpView_defaultText);
		reusableHelpPart.showPage(getFirstPage());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.help.ui.helpView"); //$NON-NLS-1$
		IWorkbenchWindow window = getSite().getPage().getWorkbenchWindow();
		if (window == null)
			return;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return;
		IWorkbenchPartReference aref = page.getActivePartReference();
		if (aref != null)
			handlePartActivation(aref);
	}

	@Override
	public void dispose() {
		IWorkbenchWindow window = getSite().getPage().getWorkbenchWindow();
		IPartService service = window.getPartService();
		if (monitoredPart != null) {
			uninstallSelectionListener(monitoredPart);
			uninstallPageListener(monitoredPart);
		}
		service.removePartListener(this);
		if (reusableHelpPart != null) {
			reusableHelpPart.dispose();
			reusableHelpPart = null;
		}
		if (toolkit != null) {
			toolkit.dispose();
			toolkit = null;
		}
		super.dispose();
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		if (memento!=null)
			this.firstPageId = memento.getString("pageId"); //$NON-NLS-1$
		init(site);
		reusableHelpPart = new ReusableHelpPart(site.getWorkbenchWindow(),
				getHelpPartStyle());
		IActionBars actionBars = site.getActionBars();
		reusableHelpPart.init(actionBars, actionBars.getToolBarManager(),
				actionBars.getStatusLineManager(), actionBars.getMenuManager(), memento);
		IWorkbenchWindow window = site.getPage().getWorkbenchWindow();
		IPartService service = window.getPartService();
		service.addPartListener(this);
	}

	@Override
	public void saveState(IMemento memento) {
		if (reusableHelpPart!=null && memento!=null) {
			String pageId = reusableHelpPart.getCurrentPageId();
			if (pageId!=null)
				memento.putString("pageId", pageId); //$NON-NLS-1$
			reusableHelpPart.saveState(memento);
		}
	}

	private void handlePartActivation(IWorkbenchPartReference ref) {
		if (reusableHelpPart == null)
			return;
		if (!visible || !reusableHelpPart.isMonitoringContextHelp())
			return;
		if (isThisPart(ref))
			return;
		IWorkbenchPart part = ref.getPart(false);
		Display display = part.getSite().getShell().getDisplay();
		Control c = display.getFocusControl();
		if (c != null && c.isVisible() && !c.isDisposed()) {
			IContextProvider provider = part.getAdapter(IContextProvider.class);
			if (provider != null) {
				reusableHelpPart.update(provider, null, part, c, false);
				if ((provider.getContextChangeMask() & IContextProvider.SELECTION) != 0) {
					// context help changes with selections
					installSelectionListener(part);
				}
			} else
				reusableHelpPart.update(part, c);
			if (part instanceof IPageChangeProvider)
				installPageListener(part);
		} else {
			if (HelpPlugin.DEBUG_CONTEXT) {
				if (c == null) {
					System.out.println("Context: focus control is null " ); //$NON-NLS-1$
				}
				if (!c.isVisible()) {
					System.out.println("Context: focus control not visible " ); //$NON-NLS-1$
				}
			}
		}
	}

	private void installPageListener(IWorkbenchPart part) {
		if (part instanceof IPageChangeProvider)
			((IPageChangeProvider)part).addPageChangedListener(this);
		monitoredPart = part;
	}

	private void uninstallPageListener(IWorkbenchPart part) {
		if (part instanceof IPageChangeProvider)
			((IPageChangeProvider)part).removePageChangedListener(this);
		monitoredPart = null;
	}

	private void installSelectionListener(IWorkbenchPart part) {
		ISelectionProvider provider = part.getSite().getSelectionProvider();
		if (provider instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) provider)
				.addPostSelectionChangedListener(this);
		} else if (provider != null) {
			provider.addSelectionChangedListener(this);
		}
		monitoredPart = part;
	}

	private void uninstallSelectionListener(IWorkbenchPart part) {
		ISelectionProvider provider = part.getSite().getSelectionProvider();
		if (provider instanceof IPostSelectionProvider)
			((IPostSelectionProvider) provider)
					.removePostSelectionChangedListener(this);
		else if (provider != null)
			provider.removeSelectionChangedListener(this);
		monitoredPart = null;
	}

	private boolean isThisPart(IWorkbenchPartReference ref) {
		IWorkbenchPart part = ref.getPart(false);
		return part != null && part.equals(this);
	}

	private void updateActivePart() {
		if (reusableHelpPart == null || !reusableHelpPart.isMonitoringContextHelp() || monitoredPart == null) {
			return;
		}
		Control c = monitoredPart.getSite().getShell().getDisplay().getFocusControl();

		if (isValid(c) && visible) {
			IContextProvider provider = monitoredPart.getAdapter(IContextProvider.class);
			if (provider != null)
				reusableHelpPart.update(provider, null, monitoredPart, c, false);
			else
				reusableHelpPart.update(monitoredPart, c);
		}
	}

	private void handlePartDeactivation(IWorkbenchPartReference ref) {
		IWorkbenchPart part = ref.getPart(false);
		if (monitoredPart != null && part != null && part.equals(monitoredPart)) {
			uninstallSelectionListener(part);
			uninstallPageListener(part);
		}
	}

	@Override
	public void partActivated(final IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			visible = true;
			hook(true, partRef);
			selectionChanged(null);
		} else {
			if (HelpPlugin.DEBUG_CONTEXT) {
				System.out.println("Help View: activation of " + partRef.getPartName() + " part"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			handlePartActivation(partRef);
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			visible = true;
			hook(true, partRef);
			selectionChanged(null);
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		handlePartDeactivation(partRef);
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		handlePartDeactivation(partRef);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			visible = false;
			hook(false, partRef);
		}
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			visible = true;
			hook(true, partRef);
			selectionChanged(null);
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		if (isThisPart(partRef)) {
			visible = true;
			hook(true, partRef);
			selectionChanged(null);
		}
	}

	private void hook(boolean doHook, IWorkbenchPartReference partref) {
		if (doHook) {
			IWorkbenchWindow window = partref.getPage().getWorkbenchWindow();
			IPartService service = window.getPartService();
			IWorkbenchPartReference aref = service.getActivePartReference();
			if (aref != null)
				handlePartActivation(aref);
		} else {
			if (monitoredPart != null) {
				uninstallSelectionListener(monitoredPart);
				uninstallPageListener(monitoredPart);
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (!visible)
			return;
		getSite().getShell().getDisplay().asyncExec(this::updateActivePart);
	}

	protected String getFirstPage() {
		if (firstPageId!=null)
			return firstPageId;
		return IHelpUIConstants.HV_CONTEXT_HELP_PAGE;
	}

	public void displayContext(IContext context, IWorkbenchPart part,
			Control control) {
		if (reusableHelpPart != null) {
			/*
			 * If the context help has no description text and exactly one
			 * topic, go straight to the topic and skip context help.
			 */
			IHelpResource[] topics = context.getRelatedTopics();
			if (context.getText() != null || topics.length != 1) {
				// Ensure that context help is currently showing
				reusableHelpPart.showPage(IHelpUIConstants.HV_CONTEXT_HELP_PAGE);
				// check if there is a dynamic version
				IContextProvider provider = null;
				if (part!=null)
					provider = part.getAdapter(IContextProvider.class);

				reusableHelpPart.update(provider, context, part, control, true);
			}
			else {
				reusableHelpPart.showURL(topics[0].getHref());
			}
		}
	}

	protected int getHelpPartStyle() {
		return ReusableHelpPart.getDefaultStyle();
	}

	@Override
	public void setFocus() {
		if (reusableHelpPart != null)
			reusableHelpPart.setFocus();
	}

	public void startSearch(String phrase) {
		if (reusableHelpPart != null)
			reusableHelpPart.startSearch(phrase);
	}

	public void showIndex() {
		if (reusableHelpPart != null)
			reusableHelpPart.showPage(IHelpUIConstants.HV_INDEX_PAGE, true);
	}

	public void showHelp(String href) {
		if (reusableHelpPart != null)
			reusableHelpPart.showURL(href);
	}

	public void showDynamicHelp(IWorkbenchPart part, Control c) {
		if (reusableHelpPart != null)
			reusableHelpPart.showDynamicHelp(part, c);
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if (!visible)
			return;
		updateActivePart();
	}
}
