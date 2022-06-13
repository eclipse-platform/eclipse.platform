/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.services.about;

import java.io.PrintWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.e4.core.services.about.AboutSections;
import org.eclipse.e4.core.services.about.ISystemInformation;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(service = { ISystemInformation.class }, property = { AboutSections.SECTION + '=' + AboutSections.SECTION_INSTALLED_FEATURES })
public class InstalledFeatures implements ISystemInformation {

	private final List<IBundleGroupProvider> providers = new ArrayList<>();

	@Override
	public void append(PrintWriter writer) {
		LinkedList<IBundleGroup> groups = new LinkedList<>();
		providers.forEach(p -> Arrays.stream(p.getBundleGroups()).forEach(groups::add));
		groups.stream().sorted(createComparator()).forEach(i -> writer
				.println(String.format(AboutMessages.featuresInfoFormat, i.getIdentifier(), i.getVersion(), i.getName())));
	}

	private Comparator<IBundleGroup> createComparator() {
		final Collator collator = Collator.getInstance(Locale.getDefault());
		return (IBundleGroup o1, IBundleGroup o2) -> {
			String id1 = o1.getIdentifier();
			String id2 = o2.getIdentifier();
			if (!id1.equals(id2)) {
				return collator.compare(id1, id2);
			}
			return collator.compare(o1.getName(), o2.getName());
		};
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE)
	public void bindBundleProvider(IBundleGroupProvider provider) {
		providers.add(provider);
	}

	public void unbindBundleProvider(IBundleGroupProvider provider) {
		providers.remove(provider);
	}
}
