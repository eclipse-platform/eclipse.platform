package org.eclipse.ant.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
/**
 * A page to set the preferences for the classpath
 */
public class AntClasspathPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
		
	protected JarsPage jarsPage;
	
/**
 * Create the console page.
 */
public AntClasspathPreferencePage() {
	setDescription(Policy.bind("preferences.description.classpath"));
	IPreferenceStore store = AntUIPlugin.getPlugin().getPreferenceStore();
	setPreferenceStore(store);
}


/**
 * @see IWorkbenchPreferencePage#init
 */
public void init(IWorkbench workbench) {
}

protected Control createContents(Composite parent) {
//	fSWTWidget= parent;
//	
//	PixelConverter converter= new PixelConverter(parent);
	
	Composite composite= new Composite(parent, SWT.NONE);	
	
	GridLayout layout= new GridLayout();
	layout.marginWidth= 0;
	layout.numColumns= 1;		
	composite.setLayout(layout);

	TabFolder folder= new TabFolder(composite, SWT.NONE);
//	folder.setLayout(new TabFolderLayout());	
	folder.setLayoutData(new GridData(GridData.FILL_BOTH));
	folder.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			tabChanged(e.item);
		}	
	});
	
//	ImageRegistry imageRegistry= JavaPlugin.getDefault().getImageRegistry();
//	
	TabItem item;

//	jarsPage = new JarsPage();
	item = new TabItem(folder, SWT.NONE);
	item.setText(Policy.bind("foo"));
//	item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT));
//	item.setData(jarsPage);
//	item.setControl(jarsPage.getControl());
	
//	IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();	
//	Image projectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
//	
//	fProjectsPage= new ProjectsWorkbookPage(fClassPathList);		
//	item= new TabItem(folder, SWT.NONE);
//	item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.projects")); //$NON-NLS-1$
//	item.setImage(projectImage);
//	item.setData(fProjectsPage);
//	item.setControl(fProjectsPage.getControl(folder));
//	
//	fLibrariesPage= new LibrariesWorkbookPage(fWorkspaceRoot, fClassPathList);		
//	item= new TabItem(folder, SWT.NONE);
//	item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.libraries")); //$NON-NLS-1$
//	item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_LIBRARY));
//	item.setData(fLibrariesPage);
//	item.setControl(fLibrariesPage.getControl(folder));
//	
//	// a non shared image
//	Image cpoImage= JavaPluginImages.DESC_TOOL_CLASSPATH_ORDER.createImage();
//	composite.addDisposeListener(new ImageDisposer(cpoImage));	
//	
//	ClasspathOrderingWorkbookPage ordpage= new ClasspathOrderingWorkbookPage(fClassPathList);		
//	item= new TabItem(folder, SWT.NONE);
//	item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.order")); //$NON-NLS-1$
//	item.setImage(cpoImage);
//	item.setData(ordpage);
//	item.setControl(ordpage.getControl(folder));
//			
//	if (fCurrJProject != null) {
//		fSourceContainerPage.init(fCurrJProject);
//		fLibrariesPage.init(fCurrJProject);
//		fProjectsPage.init(fCurrJProject);
//	}		
//					
//	Composite editorcomp= new Composite(composite, SWT.NONE);	
//
//	DialogField[] editors= new DialogField[] { fBuildPathDialogField };
//	LayoutUtil.doDefaultLayout(editorcomp, editors, true, 0, 0);
//	
//	int maxFieldWidth= converter.convertWidthInCharsToPixels(40);
//	LayoutUtil.setWidthHint(fBuildPathDialogField.getTextControl(null), maxFieldWidth);
//	LayoutUtil.setHorizontalGrabbing(fBuildPathDialogField.getTextControl(null));
//
//	editorcomp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//	
//	if (fIsNewProject) {
//		folder.setSelection(0);
//		fCurrPage= fSourceContainerPage;
//	} else {
//		folder.setSelection(3);
//		fCurrPage= ordpage;
//		fClassPathList.selectFirstElement();
//	}
//
//	WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.BUILD_PATH_BLOCK);				
	return composite;
}

private void tabChanged(Widget widget) {
//	if (widget instanceof TabItem) {
//		BuildPathBasePage newPage = (BuildPathBasePage) ((TabItem) widget).getData();
//		if (fCurrPage != null) {
//			List selection= fCurrPage.getSelection();
//			if (!selection.isEmpty()) {
//				newPage.setSelection(selection);
//			}
//		}
//		fCurrPage= newPage;
//	}
}
}
