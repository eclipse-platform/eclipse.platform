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
package org.eclipse.help.internal.toc;

import java.util.Comparator;

import org.eclipse.help.AbstractTocProvider;


public class TocProviderComparator implements Comparator<AbstractTocProvider> {

	@Override
	public int compare(AbstractTocProvider provider1, AbstractTocProvider provider2)
	{
		if(provider1.getPriority()<provider2.getPriority())
			return -1;
		else if(provider1.getPriority()>provider2.getPriority())
			return 1;
		else
			return 0;
	}
}
