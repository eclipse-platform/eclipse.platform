/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.webapp.servlet.EclipseConnector;
import org.eclipse.help.internal.webapp.servlet.ExtraFilters;
import org.eclipse.help.internal.webapp.servlet.PrioritizedFilter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ua.tests.doc.internal.dialogs.SelectTocDialog;
import org.eclipse.ua.tests.doc.internal.linkchecker.AddScriptFilter;
import org.eclipse.ua.tests.doc.internal.linkchecker.OnLoadFilter;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class LoadTocAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	private static ArrayList<String> topicList;
	private String firstHref;
	private static List<String> errors = new ArrayList<>();
	private String lastPage;

	private class MonitorThread extends Thread {
		String lastHref;
		int timesSame = 0;
		boolean isComplete = false;
		@Override
		public void run() {
			while (!isComplete) {
				if (lastHref == lastPage) {
					timesSame++;
				} else {
					lastHref = lastPage;
					timesSame = 0;
				}
				if (topicList == null) {
					isComplete = true;
				} else if (timesSame >= 10 ) {
					errors.add("Time out on page " + lastPage);
					isComplete = true;
					showErrors();
				} else  {
					try {
						sleep(500);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	public static void showErrors() {
		if (errors == null) return;
		if (errors.isEmpty()) {
			reportStatus("Testing complete, no errors found");
		} else {
			reportStatus("Testing complete, errors found");
		}
		for (Iterator<String> iter = errors.iterator(); iter.hasNext();) {
			String errorMessage = iter.next();
			reportStatus(errorMessage);
		}
		errors = null;
		topicList = null;
	}

	private static void reportStatus(String errorMessage) {
		//HelpPlugin.logWarning(errorMessage);
		System.out.println(errorMessage);
	}

	private class NotFoundCallout implements EclipseConnector.INotFoundCallout {
		@Override
		public void notFound(String url) {
			if (errors != null) {
				errors.add("Error opening " + lastPage + "\n   cannot load " + url);
			}
		}
	}

	private class LinkProvider implements Iterator<String> {
		private List<String> links;
		int lastLink = -1;

		public LinkProvider(List<String> links) {
			this.links = links;
		}

		@Override
		public boolean hasNext() {
			if (topicList != null && lastLink < links.size() && links.size() > 0) {
				return true;
			}
			EclipseConnector.setNotFoundCallout(null);
			showErrors();
			return false;
		}

		@Override
		public String next() {
			if (lastLink >= 0 && lastLink < links.size()) {
				lastPage = links.get(lastLink);
				//System.out.println("Last page is " + lastPage);
			}
			lastLink++;
			if (lastLink < links.size()) {
				String currentPage = links.get(lastLink);
				//System.out.println("Current page is " + currentPage);
				return currentPage;
			} else if (lastLink == links.size()) {
				String currentPage = links.get(lastLink - 1);
				//System.out.println("Current page is " + currentPage);
				return currentPage;
			}
			return null;
		}

		@Override
		public void remove() {
		}

	}

	/**
	 * The constructor.
	 */
	public LoadTocAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		showErrors();
		SelectTocDialog dlg = new SelectTocDialog(window.getShell());
		dlg.open();
		if (dlg.getReturnCode() == Window.CANCEL) {
			return;
		}
		int testKind = dlg.getTestKind();
		PrioritizedFilter[] filters = new PrioritizedFilter[] {
				new PrioritizedFilter(new OnLoadFilter(testKind), 1),
				new PrioritizedFilter(new AddScriptFilter(), 2)};
		ExtraFilters.setFilters(filters);
		Toc[] tocsToCheck = dlg.getTocsToCheck();
		if (testKind == SelectTocDialog.PAGES_EXIST) {
			new CheckTocAction().checkTocFilesExist(tocsToCheck);
			return;
		}

		firstHref = null;
		topicList = new ArrayList<>();
		for (Toc toc : tocsToCheck) {
			reportStatus("Test level = " + testKind + " testing " + toc.getTocContribution().getId());
			ITopic[] topics = toc.getTopics();
			addTopics(topics);
		}
		lastPage = "No pages read";
		LinkProvider linkProvider = new LinkProvider(topicList);
		OnLoadFilter.setLinkProvider(linkProvider);
		EclipseConnector.setNotFoundCallout(new NotFoundCallout());
		errors = new ArrayList<>();
		if (linkProvider.hasNext()) {
			firstHref = linkProvider.next();
			PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(firstHref);
			new MonitorThread().start();
		} else {
			reportStatus("No pages to check");
		}
	}

	private void addTopics(ITopic[] topics) {
		for (ITopic nextTopic : topics) {
			addTopic(nextTopic);
		}
	}

	private void addTopic(ITopic nextTopic) {
		String href = nextTopic.getHref();
		if (href != null && !isFiltered(href)) {
			if (firstHref == null) {
				firstHref = href;
			}
			topicList.add(href);
		}
		addTopics(nextTopic.getSubtopics());
	}

	private boolean isFiltered(String href) {
		if (!href.startsWith("/")) return true;
		//if (href.startsWith("/org.eclipse.pde.doc.user/reference")) return true;
		//if (href.startsWith("/org.eclipse.platform.doc.isv/reference")) return true;
		//if (href.startsWith("/org.eclipse.platform.doc.isv/samples")) return true;
		//if (href.startsWith("/org.eclipse.jdt.doc.isv/reference")) return true;
		return false;
	}

	/**
	 * Selection in the workbench has been changed. We
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	@Override
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}