/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.data;

import java.util.ArrayList;

import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

public class PerformWhen implements IExecutableItem  {
	private String condition;
	private ArrayList<AbstractExecutable> executables;
	private AbstractExecutable selectedExecutable;

	/**
	 * Constructor for PerformWhen.
	 */
	public PerformWhen() {
		super();
	}

	public PerformWhen(String condition) {
		super();
		this.condition = condition;
	}

	/**
	 * Returns the condition.
	 * @return String
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * Sets the condition.
	 * @param newCondition The new condition to set
	 */
	public void setCondition(String newCondition) {
		this.condition = newCondition;
	}

	/**
	 * @return Returns the executables.
	 */
	public ArrayList<AbstractExecutable> getExecutables() {
		return executables;
	}

	/**
	 * @param executable the AbstractExecutable to add.
	 */
	public void addExecutable(AbstractExecutable executable) {
		if(executables == null) {
			executables = new ArrayList<>();
		}
		executables.add(executable);
	}


	/**
	 * This method always returns <code>null</code>, it is only here aid in parsing.
	 * @return Returns the executables.
	 */
	@Override
	public AbstractExecutable getExecutable() {
		return null;
	}

	/**
	 * Delegate to the addAbstractExecutable metod.
	 * @param executable the AbstractExecutable to add.
	 */
	@Override
	public void setExecutable(AbstractExecutable executable) {
		addExecutable(executable);
	}

	public AbstractExecutable getSelectedExecutable() {
		return selectedExecutable;
	}

	public void setSelectedExecutable(CheatSheetManager csm) {
		String conditionValue = csm.getVariableData(condition);

		for (AbstractExecutable executable : executables) {
			if(executable.getWhen() != null && executable.getWhen().equals(conditionValue)) {
				selectedExecutable = executable;
				break;
			}
		}
	}

}
