package org.eclipse.update.internal.ui.search;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.EnvironmentUtil;

public class SearchObject extends NamedModelObject {
	public static final String P_REFRESH = "p_refresh"; //$NON-NLS-1$
	public static final String P_CATEGORY = "p_category"; //$NON-NLS-1$

	private static final String KEY_NAME = "Search.name"; //$NON-NLS-1$
	private static final String KEY_BEGIN = "Search.begin"; //$NON-NLS-1$
	private static final String KEY_MY_COMPUTER = "Search.myComputer"; //$NON-NLS-1$
	private static final String KEY_CONTACTING = "Search.contacting"; //$NON-NLS-1$
	private static final String KEY_CHECKING = "Search.checking"; //$NON-NLS-1$
	protected static final String S_MY_COMPUTER = "searchMyComputer"; //$NON-NLS-1$
	protected static final String S_BOOKMARKS = "searchBookmarks"; //$NON-NLS-1$
	protected static final String S_DISCOVERY = "searchDiscovery"; //$NON-NLS-1$
	protected static final String S_FILTER = "searchFilter"; //$NON-NLS-1$
	protected static final String S_DRIVES = "searchDrives"; //$NON-NLS-1$

	private Vector result = new Vector();
	private boolean searchInProgress;
	private BackgroundProgressMonitor backgroundProgress;
	private BackgroundThread searchThread;
	private SiteBookmark[] myComputerSites = null;
	private String categoryId;
	private boolean categoryFixed;
	private Hashtable settings = new Hashtable();
	private boolean persistent = true;
	private boolean instantSearch = false;

	class SearchAdapter extends MonitorAdapter {
		public void done() {
			searchInProgress = false;
		}
	}

	public SearchObject() {
		this("", null, false); //$NON-NLS-1$
	}
	public SearchObject(String name, SearchCategoryDescriptor descriptor) {
		this(name, descriptor, false);
	}
	public SearchObject(
		String name,
		SearchCategoryDescriptor descriptor,
		boolean categoryFixed) {
		super(name);
		this.categoryId = descriptor.getId();
		this.categoryFixed = categoryFixed;
		backgroundProgress = new BackgroundProgressMonitor();
		backgroundProgress.addProgressMonitor(new SearchAdapter());
	}

	public boolean isCategoryFixed() {
		return categoryFixed;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean value) {
		this.persistent = value;
	}

	public boolean isInstantSearch() {
		return instantSearch;
	}

	public void setInstantSearch(boolean value) {
		this.instantSearch = value;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String id) {
		if (categoryId != null && !categoryId.equals(id)) {
			settings.clear();
		}
		this.categoryId = id;
		notifyObjectChanged(P_CATEGORY);
	}
	
	public void setDisplay(Display display) {
		backgroundProgress.setDisplay(display);
	}
	
	public Hashtable getSettings() {
		return settings;
	}

	public boolean getSearchMyComputer() {
		return getBooleanValue(S_MY_COMPUTER);
	}

	public void setSearchMyComputer(boolean value) {
		settings.put(S_MY_COMPUTER, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setSearchBookmarks(boolean value) {
		setBooleanValue(S_BOOKMARKS, value);
	}
	public boolean getSearchBookmarks() {
		return getBooleanValue(S_BOOKMARKS);
	}

	public void setSearchDiscovery(boolean value) {
		setBooleanValue(S_DISCOVERY, value);
	}
	public boolean getSearchDiscovery() {
		return getBooleanValue(S_DISCOVERY);
	}
	public String getDriveSettings() {
		return (String)settings.get(S_DRIVES);
	}
	public void setDriveSettings(String drives) {
		settings.put(S_DRIVES, drives);
	}
	
	public void setFilterEnvironment(boolean value) {
		setBooleanValue(S_FILTER, !value);
	}
	public boolean getFilterEnvironment() {
		return !getBooleanValue(S_FILTER);
	}

	private boolean getBooleanValue(String key) {
		String value = (String) settings.get(key);
		if (value != null && value.equals("true")) //$NON-NLS-1$
			return true;
		return false;
	}
	private void setBooleanValue(String key, boolean value) {
		settings.put(key, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		return result.toArray();
	}

	public boolean hasChildren() {
		return result.size() > 0;
	}

	public void attachProgressMonitor(IProgressMonitor monitor) {
		backgroundProgress.addProgressMonitor(monitor);
	}
	public void detachProgressMonitor(IProgressMonitor monitor) {
		backgroundProgress.removeProgressMonitor(monitor);
	}

	public void startSearch(Display display, ISearchQuery[] queries)
		throws InvocationTargetException, InterruptedException {
		if (searchInProgress)
			return;
		backgroundProgress.setDisplay(display);
		IRunnableWithProgress operation = getSearchOperation(display, queries);
		searchThread =
			new BackgroundThread(operation, backgroundProgress, display);
		searchInProgress = true;
		searchThread.start();
		Throwable throwable = searchThread.getThrowable();
		if (throwable != null) {
			UpdateUIPlugin.logException(throwable);
			if (throwable instanceof InvocationTargetException) {
				throw (InvocationTargetException) throwable;
			} else if (throwable instanceof InterruptedException) {
				throw (InterruptedException) throwable;
			} else if (throwable instanceof OperationCanceledException) {
				// See 1GAN3L5: ITPUI:WIN2000 - ModalContext converts OperationCancelException into InvocationTargetException
				throw new InterruptedException(throwable.getMessage());
			} else {
				throw new InvocationTargetException(throwable);
			}
		}
	}

	public boolean isSearchInProgress() {
		return searchInProgress;
	}

	public void stopSearch() {
		if (!searchInProgress || searchThread == null)
			return;
		backgroundProgress.setCanceled(true);
	}

	public IRunnableWithProgress getSearchOperation(final Display display, final ISearchQuery[] queries) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doSearch(display, queries, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					monitor.done();
				}
			}
		};
	}

	private void doSearch(Display display, ISearchQuery[] queries, IProgressMonitor monitor)
		throws CoreException {
		result.clear();
		asyncFireObjectChanged(display, this, P_REFRESH);

		ArrayList candidates = new ArrayList();

		monitor.beginTask(
			UpdateUIPlugin.getResourceString(KEY_BEGIN),
			IProgressMonitor.UNKNOWN);

		if (getSearchMyComputer()) {
			monitor.setTaskName(
				UpdateUIPlugin.getResourceString(KEY_MY_COMPUTER));
			initializeMyComputerSites(monitor);
		}
		computeSearchSources(candidates);
		int ntasks = queries.length * (1 + candidates.size());

		monitor.beginTask(
			UpdateUIPlugin.getResourceString(KEY_BEGIN),
			ntasks);
		
		ArrayList statusList = new ArrayList();	
							
		for (int i = 0; i < queries.length; i++) {
			ISearchQuery query = queries[i];
			ISiteAdapter site = query.getSearchSite();
			if (site != null) {
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				UpdateUIPlugin.getResourceString(KEY_CHECKING);
				IStatus status = searchOneSite(display, site, query, subMonitor);
				if (status!=null)
					statusList.add(status);
				if (monitor.isCanceled())
					break;
			}
			for (int j = 0; j < candidates.size(); j++) {
				if (monitor.isCanceled()) {
					break;
				}
				Object source = candidates.get(j);
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				IStatus status = searchOneSite(display, (ISiteAdapter) source, query, subMonitor);
				if (status!=null)
					statusList.add(status);
				monitor.worked(1);
			}
			if (monitor.isCanceled())
				break;
		}
		searchInProgress = false;
		monitor.done();
		asyncFireObjectChanged(display, this, P_REFRESH);
		if (statusList.size()>0) {
			IStatus [] children = (IStatus[])statusList.toArray(new IStatus[statusList.size()]);
			MultiStatus multiStatus = new MultiStatus(UpdateUIPlugin.getPluginId(),
				ISite.SITE_ACCESS_EXCEPTION,
				children,
				UpdateUIPlugin.getResourceString("Search.networkProblems"), //$NON-NLS-1$
				null);
			throw new CoreException(multiStatus);
		}
	}

	public void computeSearchSources(ArrayList sources) {
		addMyComputerSites(sources);
		addDiscoverySites(sources);
		addBookmarks(sources);
	}

	private IStatus searchOneSite(
		Display display,
		ISiteAdapter siteAdapter,
		ISearchQuery query,
		SubProgressMonitor monitor)
		throws CoreException {
		String pattern = UpdateUIPlugin.getResourceString(KEY_CONTACTING);
		String text =
			UpdateUIPlugin.getFormattedMessage(pattern, siteAdapter.getLabel());
		monitor.subTask(text);
		URL siteURL = siteAdapter.getURL();

		ISite site;
		try {
			site = SiteManager.getSite(siteURL);
		}
		catch (CoreException e) {
			// Test the exception. If the exception is
			// due to the site connection problems,
			// allow the search to move on to 
			// the next site. Otherwise,
			// rethrow the exception, causing the search
			// to terminate.
			IStatus status = e.getStatus();
			if (status==null || status.getCode()!=ISite.SITE_ACCESS_EXCEPTION)
				throw e;
			monitor.worked(1);
			return status;
		}
		monitor.getWrappedProgressMonitor().subTask(UpdateUIPlugin.getResourceString(KEY_CHECKING));

		monitor.beginTask("", 2); //$NON-NLS-1$
	
		IFeature [] matches = query.getMatchingFeatures(site, new SubProgressMonitor(monitor, 1));

		for (int i=0; i<matches.length; i++) {
			if (monitor.isCanceled()) return null;
			if (getFilterEnvironment() && !isValidEnvironment(matches[i]))
				continue;
			// bingo - add this
			SearchResultSite searchSite = findResultSite(site);
			if (searchSite == null) {
				searchSite = new SearchResultSite(this, siteAdapter.getLabel(), site);
				result.add(searchSite);
				asyncFireObjectAdded(display, this, searchSite);
			}
			SimpleFeatureAdapter featureAdapter = new SimpleFeatureAdapter(matches[i]);
			searchSite.addCandidate(featureAdapter);
			asyncFireObjectAdded(display, searchSite, featureAdapter);
		}
		monitor.worked(1);
		return null;
	}
	
	private SearchResultSite findResultSite(ISite site) {
		for (int i=0; i<result.size(); i++) {
			SearchResultSite resultSite = (SearchResultSite)result.get(i);
			if (resultSite.getSite().equals(site)) return resultSite;
		}
		return null;
	}
	
	private boolean isValidEnvironment(IFeature candidate) {
		return EnvironmentUtil.isValidEnvironment(candidate);
	}

	private void asyncFireObjectAdded(Display display, final Object parent, final Object child) {
		final UpdateModel model = getModel();
		display.asyncExec(new Runnable() {
			public void run() {
				model.fireObjectsAdded(parent, new Object[] { child });
			}
		});
	}

	private void asyncFireObjectChanged(Display display, final Object obj, final String property) {
		final UpdateModel model = getModel();
		display.asyncExec(new Runnable() {
			public void run() {
				model.fireObjectChanged(obj, property);
			}
		});
	}

	private void initializeMyComputerSites(IProgressMonitor monitor) {
		Vector sites = new Vector();
		MyComputer myComputer = new MyComputer();
		MyComputerSearchSettings settings = new MyComputerSearchSettings(this);
		myComputer.collectSites(sites, settings, monitor);
		if (sites.size() > 0) {
			myComputerSites = (SiteBookmark[]) sites.toArray(new SiteBookmark[sites.size()]);
		} else
			myComputerSites = null;
	}

	private void addMyComputerSites(ArrayList result) {
		if (myComputerSites != null && getSearchMyComputer()) {
			for (int i = 0; i < myComputerSites.length; i++) {
				result.add(myComputerSites[i]);
			}
		}
	}
	private void addBookmarks(ArrayList result) {
		if (getSearchBookmarks() == false)
			return;
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		SiteBookmark[] bookmarks = model.getBookmarkLeafs();
		for (int i = 0; i < bookmarks.length; i++) {
			SiteBookmark bookmark = bookmarks[i];
			result.add(bookmark);
		}
	}
	private void addDiscoverySites(ArrayList result) {
		if (getSearchDiscovery() == false)
			return;
		DiscoveryFolder dfolder = new DiscoveryFolder();
		Object [] children = dfolder.getChildren(dfolder);
		for (int i=0; i<children.length; i++) {
			SiteBookmark bookmark = (SiteBookmark)children[i];
			result.add(bookmark);
		}
	}
}