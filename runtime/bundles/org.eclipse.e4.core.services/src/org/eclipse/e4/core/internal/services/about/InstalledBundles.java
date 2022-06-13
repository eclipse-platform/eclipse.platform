/*******************************************************************************
 *  Copyright (c) 2019, 2020 ArSysOp and others.
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.e4.core.services.about.AboutSections;
import org.eclipse.e4.core.services.about.ISystemInformation;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

@Component(service = { ISystemInformation.class }, property = { AboutSections.SECTION + '=' + AboutSections.SECTION_INSTALLED_BUNDLES })
public class InstalledBundles implements ISystemInformation {

	@Override
	public void append(PrintWriter writer) {
		Bundle[] bundles = FrameworkUtil.getBundle(InstalledBundles.class).getBundleContext().getBundles();
		Map<String, String> headers = Arrays.stream(bundles).collect(
				Collectors.toMap(this::identify, this::name));
		Arrays.stream(bundles).sorted(createComparator(headers)).forEach(b -> writeBundleInfo(writer, b));
	}

	String identify(Bundle bundle) {
		return bundle.getSymbolicName() + '_' + bundle.getVersion();
	}

	private String name(Bundle bundle) {
		String name = bundle.getHeaders(null).get(Constants.BUNDLE_NAME);
		// Bug 567113: do not return null names because the used Collectors.toMap does
		// not accept null values for whatever reason.
		// And empty string is better for the used purpose anyway.
		return name != null ? name : ""; //$NON-NLS-1$
	}

	private void writeBundleInfo(PrintWriter writer, Bundle bundle) {
		String id = bundle.getSymbolicName();
		String version = bundle.getVersion().toString();
		String name = name(bundle);
		String state = getStateName(bundle.getState());
		writer.println(String.format(AboutMessages.bundleInfoFormat, id, version, name, state));
	}

	private Comparator<Bundle> createComparator(Map<String, String> names) {
		final Collator collator = Collator.getInstance(Locale.getDefault());
		return (Bundle o1, Bundle o2) -> {
			String id1 = identify(o1);
			String id2 = identify(o2);
			if (!id1.equals(id2)) {
				return collator.compare(id1, id2);
			}
			return collator.compare(names.get(id1), names.get(id2));
		};
	}

	private String getStateName(int state) {
		switch (state) {
		case Bundle.INSTALLED:
			return AboutMessages.bundleStateInstalled;
		case Bundle.RESOLVED:
			return AboutMessages.bundleStateResolved;
		case Bundle.STARTING:
			return AboutMessages.bundleStateStarting;
		case Bundle.STOPPING:
			return AboutMessages.bundleStateStopping;
		case Bundle.UNINSTALLED:
			return AboutMessages.bundleStateUninstalled;
		case Bundle.ACTIVE:
			return AboutMessages.bundleStateActive;
		default:
			return AboutMessages.bundleStateUnknown;
		}
	}
}
