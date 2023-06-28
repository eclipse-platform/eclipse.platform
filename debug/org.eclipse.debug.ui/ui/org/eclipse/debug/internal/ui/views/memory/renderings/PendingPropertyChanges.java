/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

/**
 * Stores changes in properties when the rendering is hidden.
 * These data will be used to sync up the rendering when it becomes visible again.
 *
 */
public class PendingPropertyChanges
{
	BigInteger fTopVisibleAddress;
	BigInteger fSelectedAddress;
	BigInteger fPageStartAddress;
	int fColumnSize = -1;
	int fRowSize = -1;
	int fPageSize = -1;

	public int getColumnSize() {
		return fColumnSize;
	}

	public void setColumnSize(int columnSize) {
		fColumnSize = columnSize;
	}

	public BigInteger getPageStartAddress() {
		return fPageStartAddress;
	}

	public void setPageStartAddress(BigInteger pageStartAddress) {
		fPageStartAddress = pageStartAddress;
	}

	public int getRowSize() {
		return fRowSize;
	}

	public void setRowSize(int rowSize) {
		fRowSize = rowSize;
	}

	public BigInteger getSelectedAddress() {
		return fSelectedAddress;
	}

	public void setSelectedAddress(BigInteger selectedAddress) {
		fSelectedAddress = selectedAddress;
	}

	public BigInteger getTopVisibleAddress() {
		return fTopVisibleAddress;
	}

	public void setTopVisibleAddress(BigInteger topVisibleAddress) {
		fTopVisibleAddress = topVisibleAddress;
	}

	public void setPageSize(int pageSize)
	{
		fPageSize = pageSize;
	}

	public int getPageSize()
	{
		return fPageSize;
	}

}