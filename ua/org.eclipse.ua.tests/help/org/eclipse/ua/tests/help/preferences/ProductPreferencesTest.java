/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.preferences;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

import org.eclipse.help.internal.util.ProductPreferences;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/*
 * Tests the products preferences utility
 */
public class ProductPreferencesTest {

	// [items], [expectedOrder], [primaryOrdering], [secondaryOrdering1], [secondaryOrdering2], ...
	private static final String[][] GET_ORDERED_LIST_DATA = {

		// one item, no specified ordering
		{ "a", "a", "", },

		// just one item to order, not found
		{ "a", "a", "b,c,d", "e,f,g" },

		// just one item to order
		{ "a", "a", "a", "a" },

		// several items
		{ "a,b,c", "a,b,c", "a", "b,c" },

		// extra element not ordered
		{ "a,b,c", "b,c,a", "b,c", "d,e,f", "g,h,i" },

		// longer test
		{ "1,2,3,4,5", "1,2,3,4,5", "2,3", "1,2", "4,5", "3,4" },

		// same, but items shuffled
		{ "four,two,five,one,three", "one,two,three,four,five", "two,three", "one,two", "four,five", "three,four" },

		// would fail if only used one secondary ordering
		{ "seven,six,five,four,three,two,one", "one,two,three,four,five,six,seven", "one,two", "two,three,four", "one,two,six", "three,four,five", "five,six", "four,five", "six,seven" },

		// slightly overlapping
		{ "5,4,6,3,7,2,8,1,9", "9,8,7,6,5,4,3,2,1", "3,2,1", "9,8,7", "7,6,5", "5,4,3" },

		// primary is subset
		{ "5,4,6,3,7,2,8,1,9", "9,8,7,6,5,4,3,2,1", "9,7,5,3,1", "9,8,7,6,5,4,3,2,1" },

		// complex test
		{ "4,7,2,8,1,5,9,3,6", "1,2,3,4,5,6,7,8,9", "2,4,6,8", "1,3,5,7,9", "1,2", "3,4", "5,6", "7,8" },

		// conflicts; primary wins
		{ "1,2,3", "2,3,1", "2,3,1", "1,2,3", "1,3,2", "2,1,3", "2,3,1", "3,1,2", "3,2,1" },

		// one conflict; primary wins
		{ "3,2,1", "1,3,2", "1,3", "1,2" },

		// variation of previous
		{ "3,2,1", "1,2,3", "1,3", "1,2,3" },

		// primary wants one way but everyone else wants other way; primary wins
		{ "2,1,3", "3,1,2", "3,1,2", "3,2,1", "3,2,1" },

	};

	// [inputFile in data/help/preferences/], [key1=value1], [key2=value2], ...
	private static final String[][] GET_PROPERTIES_FILE_DATA = {

		// file with a single property
		{ "propertiesSingle.txt", "myKey=myValue" },

		// file with many properties
		{ "propertiesMultiple.txt", "key1=value1", "key2=value2", "this.is.another/key=this.is.another.value" }
	};

	// [key to find], [possible resulting values], [primaryProperties], [secondaryProperties1], [secondaryProperties2], ...
	private static final String[][] GET_VALUE_DATA = {

		// key doesn't exist
		{ "keyDoesNotExist", "", "values1.txt", "values1.txt" },

		// only exists in primary properties
		{ "key1", "value1", "values1.txt", "values1.txt" },

		// exists only in two secondary properties
		{ "key2", "value2,value3", "values1.txt", "values1.txt", "values2.txt", "values3.txt" },

		// exists both primary and secondary; primary should take precedence
		{ "key3", "value3", "values1.txt", "values1.txt", "values2.txt", "values3.txt" }
	};

	// [input], [token1], [token2], ...
	private static final String[][] TOKENIZE_DATA = {

		// tolerate null; return no tokens
		{ null },

		// no delimiters
		{ "Test", "Test" },

		// simple test
		{ "one, two, three", "one", "two", "three" },

		// same one repeated
		{ "repeat, repeat, repeat", "repeat", "repeat", "repeat" },

		// just commas, no whitespace
		{ "a,b,c", "a", "b", "c" },

		// semicolons
		{ "semi;colon;separator", "semi", "colon", "separator" },

		// various whitespace
		{ "all\nkinds\rof\t whitespace", "all", "kinds", "of", "whitespace" },

		// mixed commas and semicolons
		{ "many;,;,;,separators,;,;,;,test", "many", "separators", "test" },

		// all mixed
		{ "mixed; space, and  separators", "mixed", "space", "and", "separators" }
	};

	@Test
	public void testGetOrderedList() {
		for (String[] data : GET_ORDERED_LIST_DATA) {
			List<String> items = ProductPreferences.tokenize(data[0]);
			List<String> expectedOrder = ProductPreferences.tokenize(data[1]);
			List<String> primaryOrdering = ProductPreferences.tokenize(data[2]);
			List<List<String>> secondaryOrderings = IntStream.range(0, data.length - 3)
					.mapToObj(i -> ProductPreferences.tokenize(data[i + 3])).collect(toList());

			List<String> actualOrder = ProductPreferences.getOrderedList(items, primaryOrdering, secondaryOrderings, null);
			assertThat(actualOrder).containsExactlyElementsOf(expectedOrder);
		}
	}

	@Test
	public void testGetPropertiesFile() {
		for (String[] data : GET_PROPERTIES_FILE_DATA) {
			String path = "data/help/preferences/" + data[0];
			Properties properties = ProductPreferences
					.loadPropertiesFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(), path);

			assertThat(properties).as("result of loading a properties file").isNotNull();
			assertThat(data).hasSize(properties.size() + 1);

			for (int j=1;j<data.length;++j) {
				StringTokenizer tok = new StringTokenizer(data[j], "=");
				String key = tok.nextToken();
				String expectedValue = tok.nextToken();
				String actualValue = properties.getProperty(key);
				assertThat(actualValue).as("one of the properties files' keys did not match the expected value: file="
						+ path + ", key=" + key).isEqualTo(expectedValue);
			}
		}
	}

	@Test
	public void testGetValue() {
		for (String[] data : GET_VALUE_DATA) {
			String key = data[0];
			Set<String> allowableValues = new HashSet<>(ProductPreferences.tokenize(data[1]));
			Properties primary = ProductPreferences.loadPropertiesFile(
					FrameworkUtil.getBundle(getClass()).getSymbolicName(), "data/help/preferences/" + data[2]);
			Properties[] secondary = new Properties[data.length - 3];
			for (int j=0;j<secondary.length;++j) {
				secondary[j] = ProductPreferences.loadPropertiesFile(
						FrameworkUtil.getBundle(getClass()).getSymbolicName(), "data/help/preferences/" + data[j + 3]);
			}

			String value = ProductPreferences.getValue(key, primary, secondary);
			if (allowableValues.isEmpty()) {
				assertThat(value).as("value for key: " + key).isNull();
			}
			else {
				assertThat(allowableValues).contains(value);
			}
		}
	}

	@Test
	public void testTokenize() {
		for (String[] data : TOKENIZE_DATA) {
			String input = data[0];
			List<String> output = ProductPreferences.tokenize(input);

			assertThat(output).as("tokenized output for: " + input).isNotNull() //
					.as("check number of tokens").hasSize(data.length - 1);
			for (int j=0;j<output.size();++j) {
				assertThat(output.get(j)).as("one of the tokens did not match the expected result")
						.isEqualTo(data[j + 1]);
			}
		}
	}
}
