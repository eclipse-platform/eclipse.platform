/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.ide.dialogs.EncodingFieldEditor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Repository preference page for setting the encoding of the server
 */
public class RepositoryEncodingPropertyPage extends PropertyPage implements IPropertyChangeListener {
	
	private static final int LABEL_WIDTH_HINT = 400;
	
	private EncodingFieldEditor encoding;
	private ICVSRepositoryLocation location;

	private boolean valueChanged;
	
	public class OSGIPreferenceStore implements IPreferenceStore {
		private Preferences preferences, defaults;
		private boolean dirty;
		
		/**
		 * Create a wrapper for the given OSGI preferences node
		 * @param preferences an OSGI preferences node
		 */
		public OSGIPreferenceStore(Preferences preferences, Preferences defaults) {
			this.preferences = preferences;
			this.defaults = defaults;
		}

		@Override
		public void addPropertyChangeListener(IPropertyChangeListener listener) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void removePropertyChangeListener(IPropertyChangeListener listener) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean contains(String name) {
			try {
				String[] keys = preferences.keys();
				for (String string : keys) {
					if (string.equals(name)) {
						return true;
					}
				}
				return false;
			} catch (BackingStoreException e) {
				CVSUIPlugin.log(new CVSStatus(IStatus.ERROR, CVSUIMessages.internal, e)); 
				return false;
			}
		}


		@Override
		public boolean getBoolean(String name) {
			return preferences.getBoolean(name, getDefaultBoolean(name));
		}

		@Override
		public boolean getDefaultBoolean(String name) {
			if (defaults != null) {
				return defaults.getBoolean(name, false);
			}
			return false;
		}

		@Override
		public double getDefaultDouble(String name) {
			if (defaults != null) {
				return defaults.getDouble(name, 0);
			}
			return 0;
		}

		@Override
		public float getDefaultFloat(String name) {
			if (defaults != null) {
				return defaults.getFloat(name, 0);
			}
			return 0;
		}

		@Override
		public int getDefaultInt(String name) {
			if (defaults != null) {
				return defaults.getInt(name, 0);
			}
			return 0;
		}

		@Override
		public long getDefaultLong(String name) {
			if (defaults != null) {
				return defaults.getLong(name, 0);
			}
			return 0;
		}

		@Override
		public String getDefaultString(String name) {
			if (defaults != null) {
				return defaults.get(name, null);
			}
			return null;
		}

		@Override
		public double getDouble(String name) {
			return preferences.getDouble(name, getDefaultDouble(name));
		}

		@Override
		public float getFloat(String name) {
			return preferences.getFloat(name, getDefaultFloat(name));
		}

		@Override
		public int getInt(String name) {
			return preferences.getInt(name, getDefaultInt(name));
		}

		@Override
		public long getLong(String name) {
			return preferences.getLong(name, getDefaultLong(name));
		}

		@Override
		public String getString(String name) {
			return preferences.get(name, getDefaultString(name));
		}

		@Override
		public boolean isDefault(String name) {
			return !contains(name);
		}

		@Override
		public boolean needsSaving() {
			return dirty;
		}

		@Override
		public void putValue(String name, String value) {
			preferences.put(name, value);
			dirty = true;
		}

		@Override
		public void setDefault(String name, double value) {
			// Defaults cannot be set this way
		}

		@Override
		public void setDefault(String name, float value) {
			// Defaults cannot be set this way
		}

		@Override
		public void setDefault(String name, int value) {
			// Defaults cannot be set this way
		}

		@Override
		public void setDefault(String name, long value) {
			// Defaults cannot be set this way
		}

		@Override
		public void setDefault(String name, String defaultObject) {
			// Defaults cannot be set this way
		}

		@Override
		public void setDefault(String name, boolean value) {
			// Defaults cannot be set this way
		}

		@Override
		public void setToDefault(String name) {
			preferences.remove(name);
			dirty = true;
		}

		@Override
		public void setValue(String name, double value) {
			preferences.putDouble(name, value);
			dirty = true;
		}

		@Override
		public void setValue(String name, float value) {
			preferences.putFloat(name, value);
			dirty = true;
		}

		@Override
		public void setValue(String name, int value) {
			preferences.putInt(name, value);
			dirty = true;
		}

		@Override
		public void setValue(String name, long value) {
			preferences.putLong(name, value);
			dirty = true;
		}

		@Override
		public void setValue(String name, String value) {
			putValue(name, value);
		}

		@Override
		public void setValue(String name, boolean value) {
			preferences.putBoolean(name, value);
			dirty = true;
		}
	}
	
	@Override
	protected Control createContents(Composite parent) {
		initialize();

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		
		Label label = createWrappingLabel(composite, CVSUIMessages.RepositoryEncodingPropertyPage_2, 1); 
		
		encoding = new EncodingFieldEditor(CVSRepositoryLocation.PREF_SERVER_ENCODING, CVSUIMessages.RepositoryEncodingPropertyPage_3, composite); 
		encoding.setPage(this);
		encoding.setPreferenceStore(getLocationPreferenceStore());
		encoding.load();
		encoding.setPropertyChangeListener(this);
		
		Link pageLink = new Link(composite,  SWT.LEFT | SWT.WRAP);
		pageLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				PreferenceDialog dialog = PreferencesUtil
						.createPreferenceDialogOn(
								getShell(),
								"org.eclipse.ui.preferencePages.Workspace", null, null); //$NON-NLS-1$
				dialog.open();

			}
		});
		
		pageLink.setLayoutData(label.getLayoutData());	
		pageLink.setText(CVSUIMessages.RepositoryEncodingPropertyPage_4);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.REPOSITORY_ENCODING_PROPERTY_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}
	
	private IPreferenceStore getLocationPreferenceStore() {
		return new OSGIPreferenceStore(
			((CVSRepositoryLocation)location).getPreferences(),
			CVSRepositoryLocation.getDefaultPreferences());
	}

	private void initialize() {
		location = null;
		IAdaptable element = getElement();
		if (element instanceof ICVSRepositoryLocation) {
			location = (ICVSRepositoryLocation)element;
		} else {
			Object adapter = element.getAdapter(ICVSRepositoryLocation.class);
			if (adapter instanceof ICVSRepositoryLocation) {
				location = (ICVSRepositoryLocation)adapter;
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == FieldEditor.IS_VALID) {
			setValid(((Boolean)event.getNewValue()).booleanValue());
			return;
		} else if (event.getProperty() == FieldEditor.VALUE) {
			valueChanged = true;
			return;
		}
	}
	
	@Override
	public boolean performOk() {
		if (!valueChanged) {
			// See bug 137073
			// return true;
		}
		if (!KnownRepositories.getInstance().isKnownRepository(location.getLocation(false))) {
			// The location may have been replaced by the main properties page
			MessageDialog.openInformation(getShell(), CVSUIMessages.RepositoryEncodingPropertyPage_0, NLS.bind(CVSUIMessages.RepositoryEncodingPropertyPage_1, new String[] { location.getLocation(true) })); // 
			return true;
		}
		encoding.store();
		try {
			((CVSRepositoryLocation)location).getPreferences().flush();
		} catch (BackingStoreException e) {
			// Log and ignore
			CVSUIPlugin.log(new CVSStatus(IStatus.ERROR, CVSUIMessages.internal, e)); 
		}
		return true;
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		encoding.loadDefault();
	}
	
	private Label createWrappingLabel(Composite parent, String text, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		label.setFont(parent.getFont());
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}
}
