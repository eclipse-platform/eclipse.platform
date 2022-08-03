/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.navigator.SaveablesProvider;

/**
 * Provider used by the Common Navigator framework to link saveables to
 * model elements.
 */
public class ModelSaveablesProvider extends SaveablesProvider {

	private List<Saveable> saveables = new ArrayList<>();

	@Override
	public Object[] getElements(Saveable saveable) {
		if (saveable instanceof ModelSaveable) {
			ModelSaveable ms = (ModelSaveable) saveable;
			return new Object[] { ms.getModelObject() };
		}
		return new Object[0];
	}

	@Override
	public Saveable getSaveable(Object element) {
		for (Object element2 : saveables) {
			ModelSaveable saveable = (ModelSaveable) element2;
			if (saveable.getModelObject().equals(element))
				return saveable;
		}
		return null;
	}

	@Override
	public Saveable[] getSaveables() {
		return saveables.toArray(new Saveable[saveables.size()]);
	}

	public void makeDirty(ModelObjectDefinitionFile mo) {
		Saveable saveable = getSaveable(mo);
		if (saveable == null) {
			saveable = new ModelSaveable(this, mo);
			saveables.add(saveable);
			fireSaveablesOpened(new Saveable[] { saveable });
		}
		((ModelSaveable)saveable).makeDirty();
		fireSaveablesDirtyChanged(new Saveable[] { saveable });
	}

	public void saved(ModelSaveable saveable) {
		fireSaveablesDirtyChanged(new Saveable[] { saveable });
	}

}
