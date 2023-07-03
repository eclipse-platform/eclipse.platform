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
 *     Alex Blewitt - Bug 474070
 *******************************************************************************/
package org.eclipse.help.ui;

import java.util.Hashtable;

import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.views.EngineDescriptor;
import org.eclipse.help.ui.internal.views.ScopeSet;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Clients that contribute search scope root page to the search engine
 * definition must extend this class and implement
 * <code>createScopeContents</code> method. The page will come preset with the
 * engine name, image and description, as well as the master switch that turns
 * the engine on or off. When the engine master switch is set to false, all the
 * children in the client composite will be disabled.
 *
 * @since 3.1
 */
public abstract class RootScopePage extends PreferencePage implements
		ISearchScopePage {
	private IEngineDescriptor ed;

	private String scopeSetName;

	private Button masterButton;

	private Text labelText;

	private Text descText;

	private Hashtable<Control, Boolean> disabledStates = new Hashtable<>();

	private Label spacer;

	private Composite contentContainer;

	/**
	 * The default constructor.
	 */
	public RootScopePage() {
	}

	@Override
	public void init(IEngineDescriptor ed, String scopeSetName) {
		this.ed = ed;
		this.scopeSetName = scopeSetName;
	}

	/**
	 * Creates the initial contents of the page and allocates the area for the
	 * clients. Classes that extend this class should implement
	 * <code>createScopeContents(Composite)</code> instead.
	 *
	 * @param parent
	 *            the page parent
	 * @return the page client control
	 */
	@Override
	protected final Control createContents(Composite parent) {
		initializeDefaults(getPreferenceStore());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
		 "org.eclipse.help.ui.searchScope"); //$NON-NLS-1$
		contentContainer = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		GridData gd;
		//if (ed.isUserDefined())
		layout.numColumns = 2;
		contentContainer.setLayout(layout);
		if (isInPreferenceDialog()) {
			masterButton = new Button(contentContainer, SWT.CHECK);
			masterButton.setText(Messages.RootScopePage_masterButton);
			gd = new GridData();
			gd.horizontalSpan = 2;// ed.isUserDefined() ? 2 : 1;
			masterButton.setLayoutData(gd);

			spacer = new Label(contentContainer, SWT.NULL);
			gd = new GridData();
			gd.horizontalSpan = 2;// ed.isUserDefined() ? 2 : 1;
			spacer.setLayoutData(gd);
			boolean masterValue = getPreferenceStore().getBoolean(ScopeSet.getMasterKey(ed.getId()));
			masterButton.setSelection(masterValue);
			masterButton.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					masterValueChanged(masterButton.getSelection());
				}
			});

			Label label = new Label(contentContainer, SWT.NULL);
			label.setText(Messages.RootScopePage_name);
			labelText = new Text(contentContainer, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 200;
			labelText.setLayoutData(gd);
			labelText.setEditable(ed.isUserDefined());
			label = new Label(contentContainer, SWT.NULL);
			label.setText(Messages.RootScopePage_desc);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gd);
			descText = new Text(contentContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			descText.setEditable(ed.isUserDefined());
			gd.widthHint = 200;
			gd.heightHint = 48;
			descText.setLayoutData(gd);
		}

		int ccol = createScopeContents(contentContainer);
		// adjust number of columns if needed
		if (ccol > layout.numColumns && isInPreferenceDialog()) {
			layout.numColumns = ccol;
			gd = (GridData) masterButton.getLayoutData();
			gd.horizontalSpan = layout.numColumns;
			gd = (GridData) spacer.getLayoutData();
			gd.horizontalSpan = layout.numColumns;

			gd = (GridData) labelText.getLayoutData();
			gd.horizontalSpan = layout.numColumns - 1;
			gd = (GridData) descText.getLayoutData();
			gd.horizontalSpan = layout.numColumns - 1;

		}
		updateControls(true);
		return contentContainer;
	}

	/**
	 * Called when the value of the master switch has changed. The default
	 * implementation disables the scope contents control when the master switch
	 * is off. Subclass can override this behaviour.
	 *
	 * @param value
	 *            <code>true</code> if the master switch is on,
	 *            <code>false</code> otherwise.
	 */

	protected void masterValueChanged(boolean value) {
		updateEnableState(value);
	}

	private void updateEnableState(boolean enabled) {
		Control[] children = contentContainer.getChildren();

		boolean first = disabledStates.isEmpty();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child == masterButton)
				continue;
			if (!enabled) {
				disabledStates.put(child, Boolean.valueOf(child.isEnabled()));
				child.setEnabled(false);
			} else {
				Boolean savedState = disabledStates.get(child);
				if (!first)
					child.setEnabled(savedState != null ? savedState
							.booleanValue() : true);
			}
		}
	}

	/**
	 * Returns the scope set name passed to the page during initialization.
	 *
	 * @return the name of the current scope set
	 */

	protected String getScopeSetName() {
		return scopeSetName;
	}

	/**
	 * Returns the descriptor of the engine associated with this page.
	 *
	 * @return the engine descriptor
	 */

	protected IEngineDescriptor getEngineDescriptor() {
		return ed;
	}

	/**
	 * Tests whether the search engine has been selected to participate in the
	 * search.
	 *
	 * @return <code>true</code> if the search engine is enabled, <code>false</code>
	 *         otherwise.
	 */

	protected boolean isEngineEnabled() {
		if (!isInPreferenceDialog()) {
			return true;
		}
		return masterButton.getSelection();
	}

	/**
	 * Stores the value of the master switch in the preference store. Subclasses
	 * may override but must call 'super'.
	 *
	 * @return <code>true</code> if the wizard can be closed,
	 *         <code>false</code> otherwise.
	 */

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(ScopeSet.getMasterKey(ed.getId()),
				isEngineEnabled());
		if (labelText != null) {
			ed.setLabel(labelText.getText());
			ed.setDescription(descText.getText());
		}
		return true;
	}

	/**
	 * Sets the value of the master switch to the initial value from the
	 * extension. Subclasses may override but must call 'super'.
	 */
	@Override
	protected void performDefaults() {
		getPreferenceStore().setToDefault(ScopeSet.getMasterKey(ed.getId()));
		updateControls(false);
		super.performDefaults();
	}

	private void updateControls(boolean first) {
		if (isInPreferenceDialog()) {
			boolean value = getPreferenceStore().getBoolean(ScopeSet.getMasterKey(ed.getId()));
			boolean cvalue = masterButton.getSelection();
			if (value != cvalue) {
				masterButton.setSelection(value);
				masterValueChanged(value);
			} else if (first) {
				masterValueChanged(value);
			}
			labelText.setText(ed.getLabel());
			descText.setText(ed.getDescription());
		}
	}

	private boolean isInPreferenceDialog() {
		return getContainer() != null;
	}

	/**
	 * Initializes default values of the store to be used when the user presses
	 * 'Defaults' button. Subclasses may override but must call 'super'.
	 *
	 * @param store
	 *            the preference store
	 */

	protected void initializeDefaults(IPreferenceStore store) {
		Boolean value = (Boolean) ed.getParameters().get(
				EngineDescriptor.P_MASTER);
		store.setDefault(ScopeSet.getMasterKey(ed.getId()), value
				.booleanValue());
	}

	/**
	 * Abstract method that subclasses must implement in order to provide root
	 * page content. The parent uses <code>GridLayout</code> to position and
	 * size the widgets. Widgets created in this method should use
	 * <code>GridData</code> to configure the way they fit in the overall
	 * page.
	 * <p>
	 * The common widgets created by this page will set number of columns they
	 * need for themselves only. Clients that implement this method should
	 * return the required number of columns so that the root page widgets can
	 * be adjusted if more columns are needed than initially set.
	 *
	 * @param parent
	 *            the page parent
	 * @return number of columns required by the client content
	 */
	protected abstract int createScopeContents(Composite parent);
}
