package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.PageBook;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.model.UpdateModel;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.search.*;
import org.eclipse.update.internal.ui.views.SearchResultView;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.update.ui.forms.internal.engine.FormEngine;

public class SearchForm extends UpdateWebForm {
	private static final String KEY_TITLE = "SearchPage.title";
	private static final String KEY_LAST_SEARCH = "SearchPage.lastSearch";
	private static final String KEY_NO_LAST_SEARCH = "SearchPage.noLastSearch";
	private static final String KEY_SEARCH_NOW = "SearchPage.searchNow";
	private static final String KEY_CANCEL = "SearchPage.cancel";
	private static final String KEY_DESC = "SearchPage.desc";
	private static final String KEY_QUERY = "SearchPage.query.label";
	private static final String KEY_CATEGORY = "SearchPage.query.category";
	private static final String KEY_OPTIONS = "SearchPage.options.label";
	private static final String KEY_MY_COMPUTER_CHECK =
		"SearchPage.options.myComputerCheck";
	private static final String KEY_MY_COMPUTER_TITLE =
		"SearchPage.options.myComputerSettings.title";
	private static final String KEY_MY_COMPUTER_MORE =
		"SearchPage.options.myComputerSettings";
	private static final String KEY_FULL_MODE_CHECK =
		"SearchPage.options.fullModeCheck";
	private static final String KEY_BOOKMARK_CHECK =
		"SearchPage.options.bookmarkCheck";
	private static final String KEY_DISCOVERY_CHECK =
		"SearchPage.options.discoveryCheck";
	private static final String KEY_FILTER_CHECK =
		"SearchPage.options.filterCheck";
	private static final String UPDATES_IMAGE_ID = "updates";
	private static final String SETTINGS_SECTION = "SearchForm";
	private static final String S_FULL_MODE = "fullMode";

	private FormEngine descLabel;
	private Label infoLabel;
	private ExpandableGroup queryGroup;
	private ExpandableGroup optionsGroup;
	private CCombo categoryCombo;
	private Button myComputerCheck;
	private Button discoveryCheck;
	private Button bookmarkCheck;
	private Button filterCheck;
	private Button myComputerSettings;
	//private Button fullModeCheck;
	private Button searchButton;
	private PageBook pagebook;
	private SearchMonitor monitor;
	//private UpdateSearchProgressMonitor statusMonitor;
	//private SearchResultSection searchResultSection;
	private IDialogSettings settings;
	private SearchObject searchObject;
	private ArrayList categories = new ArrayList();
	private Hashtable descTable = new Hashtable();
	private ISearchCategory currentCategory;

	abstract class OptionsGroup extends ExpandableGroup {
		protected SelectableFormLabel createTextLabel(
			Composite parent,
			FormWidgetFactory factory) {
			SelectableFormLabel label = super.createTextLabel(parent, factory);
			label.setFont(JFaceResources.getBannerFont());
			return label;
		}
		public void expanded() {
			getControl().getParent().layout();
			reflow();
			updateSize();
		}
		public void collapsed() {
			getControl().getParent().layout();
			reflow();
			updateSize();
		}
	}

	class SearchMonitor extends ProgressMonitorPart {
		public SearchMonitor(Composite parent) {
			super(parent, null);
			setBackground(factory.getBackgroundColor());
			fLabel.setBackground(factory.getBackgroundColor());
			fProgressIndicator.setBackground(factory.getBackgroundColor());
		}
		public void done() {
			super.done();
			updateButtonText();
			searchButton.setEnabled(true);
			Date date = new Date();
			String pattern = UpdateUIPlugin.getResourceString(KEY_LAST_SEARCH);
			String text =
				UpdateUIPlugin.getFormattedMessage(
					pattern,
					Utilities.format(date));
			infoLabel.setText(text);
			infoLabel.getParent().layout();
			reflow(true);
			searchObject.detachProgressMonitor(this);
			//			if (statusMonitor != null) {
			//				searchObject.detachProgressMonitor(statusMonitor);
			//				statusMonitor = null;
			//			}
			enableOptions(true);
			activateSearchResultSelection();
		}
		
		private void activateSearchResultSelection() {
			SearchResultView sview = (SearchResultView)UpdateUIPlugin.getActivePage().findView(UpdatePerspective.ID_SEARCH_RESULTS);
			if (sview!=null)
				sview.setSelectionActive(true);
		}
	}

	public SearchForm(UpdateFormPage page) {
		super(page);
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		IDialogSettings master =
			UpdateUIPlugin.getDefault().getDialogSettings();
		settings = master.getSection(SETTINGS_SECTION);
		if (settings == null)
			settings = master.addNewSection(SETTINGS_SECTION);
	}

	public void dispose() {
		if (searchObject != null) {
			detachFrom(searchObject);
		}
		/*
		if (searchResultSection != null)
			searchResultSection.dispose();
		*/
		super.dispose();
	}

	private void detachFrom(SearchObject obj) {
		obj.detachProgressMonitor(monitor);
		//		if (statusMonitor != null)
		//			obj.detachProgressMonitor(statusMonitor);
	}

	public void initialize(Object modelObject) {
		updateHeadingText(null);
		super.initialize(modelObject);
	}

	private void updateHeadingText(SearchObject obj) {
		String title = UpdateUIPlugin.getResourceString(KEY_TITLE);
		if (obj != null)
			title += " - " + obj.getName();
		setHeadingText(title);
	}

	protected void createContents(Composite parent) {
		HTMLTableLayout layout = new HTMLTableLayout();
		parent.setLayout(layout);
		layout.leftMargin = 5;
		layout.rightMargin = 5;
		layout.topMargin = 0;
		layout.bottomMargin = 2;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 10;
		layout.numColumns = 2;

		FormWidgetFactory factory = getFactory();
		TableData td;

		descLabel = factory.createFormEngine(parent);
		descLabel.load(UpdateUIPlugin.getResourceString(KEY_DESC), true, true);
		td = new TableData();
		td.colspan = 2;
		descLabel.setLayoutData(td);

		queryGroup = new OptionsGroup() {
			public void fillExpansion(
				Composite expansion,
				FormWidgetFactory factory) {
				fillQueryGroup(expansion, factory);
			}
		};
		Composite optionContainer = factory.createComposite(parent);
		layout = new HTMLTableLayout();
		layout.numColumns = 2;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		layout.topMargin = 0;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 0;
		optionContainer.setLayout(layout);
		td = new TableData();
		td.colspan = 2;
		optionContainer.setLayoutData(td);

		queryGroup.setText(UpdateUIPlugin.getResourceString(KEY_QUERY));
		queryGroup.createControl(optionContainer, factory);
		setFocusControl(queryGroup.getControl());

		optionsGroup = new OptionsGroup() {
			public void fillExpansion(
				Composite expansion,
				FormWidgetFactory factory) {
				GridLayout layout = new GridLayout();
				layout.numColumns = 2;
				expansion.setLayout(layout);

				myComputerCheck =
					factory.createButton(expansion, null, SWT.CHECK);
				myComputerCheck.setText(
					UpdateUIPlugin.getResourceString(KEY_MY_COMPUTER_CHECK));
				myComputerCheck.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						myComputerSettings.setEnabled(
							myComputerCheck.getSelection());
						searchObject.setSearchMyComputer(
							myComputerCheck.getSelection());
					}
				});

				myComputerSettings =
					factory.createButton(expansion, null, SWT.PUSH);
				myComputerSettings.setText(
					UpdateUIPlugin.getResourceString(KEY_MY_COMPUTER_MORE));
				myComputerSettings
					.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						BusyIndicator
							.showWhile(
								myComputerSettings.getDisplay(),
								new Runnable() {
							public void run() {
								MyComputerSearchDialog sd =
									new MyComputerSearchDialog(
										myComputerSettings.getShell(),
										searchObject);
								sd.create();
								sd.getShell().setText(
									UpdateUIPlugin.getResourceString(
										KEY_MY_COMPUTER_TITLE));
								sd.open();
							}
						});
					}
				});
				GridData gd;
				discoveryCheck =
					factory.createButton(expansion, null, SWT.CHECK);
				discoveryCheck.setText(
					UpdateUIPlugin.getResourceString(KEY_DISCOVERY_CHECK));
				discoveryCheck.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						searchObject.setSearchDiscovery(
							discoveryCheck.getSelection());
					}
				});
				gd = new GridData();
				gd.horizontalSpan = 2;
				discoveryCheck.setLayoutData(gd);

				bookmarkCheck =
					factory.createButton(expansion, null, SWT.CHECK);
				bookmarkCheck.setText(
					UpdateUIPlugin.getResourceString(KEY_BOOKMARK_CHECK));
				bookmarkCheck.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						searchObject.setSearchBookmarks(
							bookmarkCheck.getSelection());
					}
				});
				gd = new GridData();
				gd.horizontalSpan = 2;
				bookmarkCheck.setLayoutData(gd);

				filterCheck = factory.createButton(expansion, null, SWT.CHECK);
				filterCheck.setText(
					UpdateUIPlugin.getResourceString(KEY_FILTER_CHECK));
				filterCheck.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						searchObject.setFilterEnvironment(
							filterCheck.getSelection());
					}
				});
				gd = new GridData();
				gd.horizontalSpan = 2;
				filterCheck.setLayoutData(gd);
			}
		};
		optionsGroup.setText(UpdateUIPlugin.getResourceString(KEY_OPTIONS));
		optionsGroup.createControl(optionContainer, factory);

/*
		fullModeCheck = factory.createButton(parent, null, SWT.CHECK);
		fullModeCheck.setText(
			UpdateUIPlugin.getResourceString(KEY_FULL_MODE_CHECK));
		fullModeCheck.setSelection(settings.getBoolean(S_FULL_MODE));
		fullModeCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleMode(fullModeCheck.getSelection());
			}
		});
		td = new TableData();
		td.colspan = 2;
		fullModeCheck.setLayoutData(td);
*/

		Composite sep = factory.createCompositeSeparator(parent);
		td = new TableData();
		td.align = TableData.FILL;
		td.heightHint = 1;
		td.colspan = 2;
		sep.setBackground(factory.getColor(factory.COLOR_BORDER));
		sep.setLayoutData(td);

		Composite searchContainer = factory.createComposite(parent);
		GridLayout glayout = new GridLayout();
		glayout.numColumns = 2;
		searchContainer.setLayout(glayout);
		td = new TableData();
		td.align = TableData.FILL;
		td.colspan = 2;
		searchContainer.setLayoutData(td);
		GridData gd;

		infoLabel = factory.createLabel(searchContainer, null);
		infoLabel.setText(UpdateUIPlugin.getResourceString(KEY_NO_LAST_SEARCH));
		gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
		infoLabel.setLayoutData(gd);

		searchButton =
			factory.createButton(
				searchContainer,
				UpdateUIPlugin.getResourceString(KEY_SEARCH_NOW),
				SWT.PUSH);
		searchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				performSearch();
			}
		});
		gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
		gd.horizontalIndent = 10;
		searchButton.setLayoutData(gd);

		monitor = new SearchMonitor(parent);
		td = new TableData();
		td.align = TableData.FILL;
		td.colspan = 2;
		monitor.setLayoutData(td);
		if (searchObject != null && searchObject.isSearchInProgress()) {
			// sync up with the search
			catchUp();
		}
		/*
		searchResultSection =
			new SearchResultSection((UpdateFormPage) getPage());
		Control control = searchResultSection.createControl(parent, factory);
		td = new TableData();
		td.align = TableData.FILL;
		td.colspan = 2;
		td.grabHorizontal = true;
		control.setLayoutData(td);
		searchResultSection.setFullMode(settings.getBoolean(S_FULL_MODE));
		*/
		WorkbenchHelp.setHelp(parent, "org.eclipse.update.ui.SearchForm");
	}

	private void fillQueryGroup(
		Composite container,
		FormWidgetFactory factory) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		container.setLayout(layout);
		factory.createLabel(
			container,
			UpdateUIPlugin.getResourceString(KEY_CATEGORY));
		categoryCombo = new CCombo(container, SWT.READ_ONLY | SWT.FLAT);
		categoryCombo.setBackground(factory.getBackgroundColor());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		categoryCombo.setLayoutData(gd);

		pagebook = new PageBook(container, SWT.NULL);
		pagebook.setBackground(factory.getBackgroundColor());
		gd =
			new GridData(
				GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		pagebook.setLayoutData(gd);
		SearchCategoryDescriptor[] descriptors =
			SearchCategoryRegistryReader.getDefault().getCategoryDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			ISearchCategory category = descriptors[i].createCategory();
			if (category != null) {
				categories.add(category);
				descTable.put(category, descriptors[i]);
				categoryCombo.add(descriptors[i].getName());
				category.createControl(pagebook, factory);
			}
		}
		categoryCombo.pack();
		categoryCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = categoryCombo.getSelectionIndex();
				ISearchCategory category =
					(ISearchCategory) categories.get(index);
				switchTo(category);
				searchObject.setCategoryId(category.getId());
			}
		});
		categoryCombo.select(0);
		factory.paintBordersFor(container);
	}

	private void switchTo(ISearchCategory category) {
		pagebook.showPage(category.getControl());
		currentCategory = category;
		SearchCategoryDescriptor desc =
			(SearchCategoryDescriptor) descTable.get(category);
		descLabel.load(desc.getDescription(), true, true);
		reflow();
		updateSize();
	}

	private void selectCategory(SearchObject obj) {
		for (int i = 0; i < categories.size(); i++) {
			ISearchCategory category = (ISearchCategory) categories.get(i);
			if (category.getId().equals(obj.getCategoryId())) {
				categoryCombo.select(i);
				switchTo(category);
				category.load(obj.getSettings(), !obj.isCategoryFixed());
				/*
				searchResultSection.setSearchString(
					category.getCurrentSearch());
				*/
				categoryCombo.setEnabled(!obj.isCategoryFixed());
				break;
			}
		}
	}

	private void reflow(boolean searchFinished) {
		/*
		if (searchFinished)
			searchResultSection.searchFinished();
		else
			searchResultSection.reflow();
		*/
		descLabel.getParent().layout(true);
		((Composite) getControl()).layout(true);
		updateSize();
	}

	private void reflow() {
		reflow(false);
	}

/*
	private void toggleMode(final boolean fullMode) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				searchResultSection.setFullMode(fullMode);
				descLabel.getParent().layout(true);
				((Composite) getControl()).layout(true);
				updateSize();
			}
		});
		settings.put(S_FULL_MODE, fullMode);
	}
*/

	private void performSearch() {
		if (searchObject != null) {
			if (searchObject.isSearchInProgress()) {
				stopSearch();
			} else {
				if (!startSearch())
					return;
			}
			updateButtonText();
		}
	}

	private boolean startSearch() {
		try {
			if (!searchObject.isCategoryFixed()) {
				currentCategory.store(searchObject.getSettings());
				if (!searchObject
					.getCategoryId()
					.equals(currentCategory.getId()))
					searchObject.setCategoryId(currentCategory.getId());
			}
			searchObject.attachProgressMonitor(monitor);
			//attachStatusLineMonitor();
			enableOptions(false);
			hookSearchView();
			searchObject.startSearch(getControl().getDisplay(), getQueries());
			
			//searchResultSection.setSearchObject(searchObject);
			//searchResultSection.searchStarted();
		} catch (InvocationTargetException e) {
			UpdateUIPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			UpdateUIPlugin.logException(e);
			return false;
		}
		return true;
	}

	private void hookSearchView() {
		try {
			String viewId = UpdatePerspective.ID_SEARCH_RESULTS;
			IWorkbenchPage page = UpdateUIPlugin.getActivePage();
			SearchResultView sview = (SearchResultView)page.findView(viewId);

			if (sview == null) {
				sview = (SearchResultView)page.showView(viewId);
				page.showView(UpdatePerspective.ID_DETAILS);
			}
			else {
				sview.setSelectionActive(false);
				page.bringToTop(sview);
			}
			sview.setCurrentSearch(searchObject);
		} catch (PartInitException e) {
			UpdateUIPlugin.logException(e);
		}
	}

	private ISearchQuery[] getQueries() {
		int index = categoryCombo.getSelectionIndex();
		ISearchCategory category = (ISearchCategory) categories.get(index);
		//searchResultSection.setSearchString(category.getCurrentSearch());
		return category.getQueries();
	}

	private void catchUp() {
		searchObject.attachProgressMonitor(monitor);
		//attachStatusLineMonitor();
		enableOptions(false);
		updateButtonText();
	}

	//	private void attachStatusLineMonitor() {
	//		if (statusMonitor != null)
	//			return;
	//		IViewSite vsite = getPage().getView().getViewSite();
	//		IStatusLineManager manager = vsite.getActionBars().getStatusLineManager();
	//		manager = getRootManager(manager);
	//
	//		statusMonitor = new UpdateSearchProgressMonitor(manager);
	//		searchObject.attachProgressMonitor(statusMonitor);
	//	}

	private IStatusLineManager getRootManager(IStatusLineManager manager) {
		IContributionManager parent = manager;

		while (parent instanceof SubStatusLineManager) {
			IContributionManager newParent =
				((SubStatusLineManager) parent).getParent();
			if (newParent == null)
				break;
			parent = newParent;
		}
		return (IStatusLineManager) parent;
	}

	private void stopSearch() {
		searchButton.setEnabled(false);
		searchObject.stopSearch();
	}

	private void enableOptions(boolean enable) {
		myComputerCheck.setEnabled(enable);
		myComputerSettings.setEnabled(enable);
		discoveryCheck.setEnabled(enable);
		bookmarkCheck.setEnabled(enable);
		filterCheck.setEnabled(enable);
		if (currentCategory.getControl()!=null)
			currentCategory.getControl().setEnabled(enable);
	}

	private void updateButtonText() {
		boolean inSearch =
			searchObject != null && searchObject.isSearchInProgress();
		if (inSearch)
			searchButton.setText(UpdateUIPlugin.getResourceString(KEY_CANCEL));
		else
			searchButton.setText(
				UpdateUIPlugin.getResourceString(KEY_SEARCH_NOW));
		searchButton.getParent().layout(true);
	}

	private void updateScopeSettings(SearchObject sobj) {
		myComputerCheck.setSelection(sobj.getSearchMyComputer());
		myComputerSettings.setEnabled(sobj.getSearchMyComputer());
		discoveryCheck.setSelection(sobj.getSearchDiscovery());
		bookmarkCheck.setSelection(sobj.getSearchBookmarks());
		filterCheck.setSelection(sobj.getFilterEnvironment());
	}

	public void expandTo(Object obj) {
		if (obj instanceof SearchObject) {
			inputChanged((SearchObject) obj);
		}
	}
	private void inputChanged(final SearchObject obj) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				if (searchObject != null) {
					if (searchObject == obj)
						return;
					if (currentCategory != null) {
						if (!searchObject.isCategoryFixed())
							currentCategory.store(searchObject.getSettings());
						searchObject.setCategoryId(currentCategory.getId());
					}
					detachFrom(searchObject);
				}
				searchObject = obj;
				updateHeadingText(searchObject);
				selectCategory(obj);
				updateScopeSettings(obj);
				//searchResultSection.setSearchObject(searchObject);
				if (searchObject.isSearchInProgress()) {
					// sync up with the search
					catchUp();
				} else if (searchObject.isInstantSearch()) {
					searchObject.setInstantSearch(false);
					getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							performSearch();
						}
					});
				}
			}
		});
	}

	public void objectChanged(Object object, String property) {
		if (object.equals(searchObject)) {
			if (NamedModelObject.P_NAME.equals(property))
				updateHeadingText(searchObject);
		}
	}
}