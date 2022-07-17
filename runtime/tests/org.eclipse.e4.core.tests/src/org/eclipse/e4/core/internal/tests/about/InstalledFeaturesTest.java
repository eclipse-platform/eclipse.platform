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
package org.eclipse.e4.core.internal.tests.about;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.e4.core.internal.services.about.InstalledFeatures;
import org.junit.Test;

@SuppressWarnings("restriction")
public class InstalledFeaturesTest {

	@Test
	public void testInstalledFeatures() throws IOException {
		InstalledFeatures service = new InstalledFeatures();
		int amount = 3;
		IBundleGroupProvider provider = BundleGroupFactory.provider("bgp",
				IntStream
						.range(0, amount).mapToObj(i -> BundleGroupFactory.identify("id" + i, "1." + i + ".0")
								.describe("name" + i, "description" + i, "providerName" + i))
						.collect(Collectors.toList()));
		service.bindBundleProvider(provider);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintWriter pw = new PrintWriter(baos)) {
			service.append(pw);
			pw.flush();
			String printed = new String(baos.toByteArray(), Charset.defaultCharset());
			IntStream.range(0, amount).mapToObj(i -> "id" + i + " (1." + i + ".0) \"name" + i + "\"")
					.forEach(s -> assertTrue(printed.contains(s)));
		}
	}

}
