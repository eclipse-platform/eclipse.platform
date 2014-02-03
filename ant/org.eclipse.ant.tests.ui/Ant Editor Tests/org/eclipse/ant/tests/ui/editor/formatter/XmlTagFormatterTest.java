/*******************************************************************************
 * Copyright (c) 2004, 2005 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *     IBM Corporation - Bug 84342
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import java.util.List;

import org.eclipse.ant.internal.ui.editor.formatter.XmlTagFormatter;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

public class XmlTagFormatterTest extends AbstractAntUITest {

    // TODO This test is too low level and too tightly coupled to internals.

    /* ---------------- Test Fixture ---------------- */
    // In case anyone wonders why many people recommend against testing
    // privates, I produce this example...
    private static class InnerClassFactory extends XmlTagFormatter {

        public static class ParseException extends XmlTagFormatter.ParseException {

			private static final long serialVersionUID = 1L;

			public ParseException(String message) {
                super(message);
            }
        }

        public static class Tag extends XmlTagFormatter.Tag {
        }

        public static class TagFormatter extends XmlTagFormatter.TagFormatter {

            public boolean lineRequiresWrap(String line, int lineWidth,
                    int tabWidth) {
                return super.lineRequiresWrap(line, lineWidth, tabWidth);
            }

            public int tabExpandedLineWidth(String line, int tabWidth) {
                return super.tabExpandedLineWidth(line, tabWidth);
            }

            public String wrapTag(Tag tag, FormattingPreferences prefs,
                    String indent, String lineDelimiter) {
                return super.wrapTag(tag, prefs, indent, lineDelimiter);
            }
        }

        public static class TagParser extends XmlTagFormatter.TagParser {

            public String getElementName(String tagText)
                    throws XmlTagFormatter.ParseException {
                return super.getElementName(tagText);
            }

            public List getAttibutes(String elementText)
                    throws XmlTagFormatter.ParseException {
                return super.getAttibutes(elementText);
            }
        }

        public static Tag createTag() {
            return new Tag();
        }

        public static TagFormatter createTagFormatter() {
            return new TagFormatter();
        }

        public static TagParser createTagParser() {
            return new TagParser();
        }

        public static void validateAttributePair(Object attributePair, String attribute, String value) {
            XmlTagFormatter.AttributePair pair = (XmlTagFormatter.AttributePair) attributePair;
            assertEquals(attribute, pair.getAttribute());
            assertEquals(value, pair.getValue());
        }
    }

    public XmlTagFormatterTest(String name) {
        super(name);
    }

    private FormattingPreferences getPreferences(final boolean wrapLongTags,
            final boolean alignCloseChar, final int maxLineWidth) {

        return new FormattingPreferences() {

            public boolean alignElementCloseChar() {
                return alignCloseChar;
            }

            public boolean wrapLongTags() {
                return wrapLongTags;
            }

            public int getMaximumLineWidth() {
                return maxLineWidth;
            }
        };
    }

    private void simpleTest(String source, String target,
            FormattingPreferences prefs, String indent, String lineDelimiter) throws Exception {
    	
        String result = XmlTagFormatter.format(source, prefs, indent, lineDelimiter);
        assertEquals(target, result);
    }

    /* ---------------- Test Methods ---------------- */

    public void testParserGetElementName() throws Exception {

        InnerClassFactory.TagParser tagParser = InnerClassFactory
                .createTagParser();

        String elementFixture1 = "<myElement attribute1=\"value1\" attribute2=\"value2\" />"; //$NON-NLS-1$
        assertEquals("myElement", tagParser.getElementName(elementFixture1)); //$NON-NLS-1$

        String elementFixture2 = "<myElement\t\nattribute1=\"value1\" attribute2=\"value2\" />"; //$NON-NLS-1$
        assertEquals("myElement", tagParser.getElementName(elementFixture2)); //$NON-NLS-1$

        assertEquals("x", tagParser.getElementName("<x/>")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("x", tagParser.getElementName("<x>")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("x:y", tagParser.getElementName("<x:y/>")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("x:y", tagParser.getElementName("<x:y abc/>")); //$NON-NLS-1$ //$NON-NLS-2$

        Exception e1 = null;
        try {
            tagParser.getElementName("<>"); //$NON-NLS-1$
        } catch (Exception e) {
            e1 = e;
        }
        assertNotNull(e1);
        assertTrue(e1.getClass().isAssignableFrom(
                InnerClassFactory.ParseException.class));

        Exception e2 = null;
        try {
            tagParser.getElementName("<>"); //$NON-NLS-1$
        } catch (Exception e) {
            e2 = e;
        }
        assertNotNull(e2);
        assertTrue(e2.getClass().isAssignableFrom(
                InnerClassFactory.ParseException.class));

    }

    public void testParserGetAttributes() throws Exception {
        InnerClassFactory.TagParser tagParser = InnerClassFactory
                .createTagParser();

        List attributePairs;

        // test normal situation
        attributePairs = tagParser
                .getAttibutes("<myElement attribute1=\"value1\" attribute2=\"value2\" />"); //$NON-NLS-1$

        assertEquals(2, attributePairs.size());
        InnerClassFactory.validateAttributePair(attributePairs.get(0),
                "attribute1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        InnerClassFactory.validateAttributePair(attributePairs.get(1),
                "attribute2", "value2"); //$NON-NLS-1$ //$NON-NLS-2$

        
        // test with extra whitespace and funny quotes
        attributePairs = tagParser
                .getAttibutes("<myElement \nattribute1 =  'value1\"'\nattribute2\t=\"value2'\" />"); //$NON-NLS-1$

        assertEquals(2, attributePairs.size());
        InnerClassFactory.validateAttributePair(attributePairs.get(0),
                "attribute1", "value1\""); //$NON-NLS-1$ //$NON-NLS-2$
        InnerClassFactory.validateAttributePair(attributePairs.get(1),
                "attribute2", "value2'"); //$NON-NLS-1$ //$NON-NLS-2$

// TODO attributes which contain whitespace should throw a parse error
//       
//        // test parse errors - whitespace in attribute name
//        Exception e1 = null;
//        try {
//            attributePairs = tagParser
//                    .getAttibutes("<myElement at tribute1 = \"value1\" attribute2=\"value2\" />");
//        } catch (Exception e) {
//            e1 = e;
//        }
//        assertNotNull(e1);
//        assertTrue(e1.getClass().isAssignableFrom(
//                InnerClassFactory.ParseException.class));
        
        
        // test parse errors - equals in the wrong place
        Exception e2 = null;
        try {
            attributePairs = tagParser
                    .getAttibutes("<myElement attribute1=\"value1\" = attribute2=\"value2\" />"); //$NON-NLS-1$
        } catch (Exception e) {
            e2 = e;
        }
        assertNotNull(e2);
        assertTrue(e2.getClass().isAssignableFrom(
                InnerClassFactory.ParseException.class));
        
        
        // test parse errors - quotes in the wrong place
        Exception e3 = null;
        try {
            attributePairs = tagParser
                    .getAttibutes("<myElement attribute1=\"\"value1\"  attribute2=\"value2\" />"); //$NON-NLS-1$
        } catch (Exception e) {
            e3 = e;
        }
        assertNotNull(e3);
        assertTrue(e3.getClass().isAssignableFrom(InnerClassFactory.ParseException.class));
    }

    public void testFormat01() throws Exception {
    	String lineSep= System.getProperty("line.separator");
        String indent = "\t"; //$NON-NLS-1$
        String source = "<target name=\"myTargetName\" depends=\"a,b,c,d,e,f,g\" description=\"This is a very long element which ought to be wrapped.\">"; //$NON-NLS-1$
        String target = "<target name=\"myTargetName\"" + lineSep //$NON-NLS-1$
                + indent
                + "        depends=\"a,b,c,d,e,f,g\"" + lineSep //$NON-NLS-1$
                + indent
                + "        description=\"This is a very long element which ought to be wrapped.\">"; //$NON-NLS-1$

        simpleTest(source, target, getPreferences(true, false, 60), indent, lineSep);
    }

    public void testFormat02() throws Exception {
    	String lineSep= System.getProperty("line.separator");
        String indent = "\t"; //$NON-NLS-1$
        String source = "<target name=\"myTargetName\" depends=\"a,b,c,d,e,f,g\" description=\"This is a very long element which ought to be wrapped.\">"; //$NON-NLS-1$
        String target = "<target name=\"myTargetName\"" + lineSep //$NON-NLS-1$
                + indent
                + "        depends=\"a,b,c,d,e,f,g\"" + lineSep //$NON-NLS-1$
                + indent
                + "        description=\"This is a very long element which ought to be wrapped.\"" + lineSep //$NON-NLS-1$
                + indent + ">"; //$NON-NLS-1$

        simpleTest(source, target, getPreferences(true, true, 60), indent, lineSep);
    }
    
    public void testBug73411() throws Exception {
    	String lineSep= System.getProperty("line.separator");
        String indent = "\t"; //$NON-NLS-1$
        String source = "<target name='myTargetName' depends=\"a,b,c,d,e,f,g\" description=\'This is a very long element which ought to be \"wrapped\".'>"; //$NON-NLS-1$
        String target = "<target name='myTargetName'" + lineSep //$NON-NLS-1$
                + indent
                + "        depends=\"a,b,c,d,e,f,g\"" + lineSep //$NON-NLS-1$
                + indent
                + "        description='This is a very long element which ought to be \"wrapped\".'" + lineSep //$NON-NLS-1$
                + indent + ">"; //$NON-NLS-1$

        simpleTest(source, target, getPreferences(true, true, 60), indent, lineSep);
    }

    public void testLineRequiresWrap() throws Exception {

        InnerClassFactory.TagFormatter tagFormatter = InnerClassFactory.createTagFormatter();

        boolean shouldWrap = tagFormatter.lineRequiresWrap(
                        "\t\t  <myElement attribute1=\"value1\" attribute2=\"value2\" />", //$NON-NLS-1$
                        70, 8);
        boolean shouldNotWrap = tagFormatter.lineRequiresWrap(
                        "\t\t <myElement attribute1=\"value1\" attribute2=\"value2\" />", //$NON-NLS-1$
                        70, 8);
        assertTrue(shouldWrap);
        assertTrue(!shouldNotWrap);

    }

    public void testTabExpandedLineWidth() throws Exception {

        InnerClassFactory.TagFormatter tagFormatter = InnerClassFactory.createTagFormatter();

        assertEquals(20, tagFormatter.tabExpandedLineWidth("\t  1234567890", 8)); //$NON-NLS-1$
        assertEquals(10, tagFormatter.tabExpandedLineWidth("1234567890", 8)); //$NON-NLS-1$
        assertEquals(19, tagFormatter.tabExpandedLineWidth("\t1\t2	34567890", 3)); //$NON-NLS-1$
    }

    public void testTabToStringAndMinimumLength() throws Exception {
        InnerClassFactory.Tag tag = InnerClassFactory.createTag();

        tag.setElementName("myElement"); //$NON-NLS-1$
        tag.setClosed(false);
        assertEquals("<myElement>", tag.toString()); //$NON-NLS-1$
        assertEquals(tag.toString().length(), tag.minimumLength());

        tag.setClosed(true);
        assertEquals("<myElement />", tag.toString()); //$NON-NLS-1$
        assertEquals(tag.toString().length(), tag.minimumLength());

        tag.addAttribute("attribute1", "value1", '"'); //$NON-NLS-1$ //$NON-NLS-2$
        tag.addAttribute("attribute2", "value2", '"'); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(
                "<myElement attribute1=\"value1\" attribute2=\"value2\" />", //$NON-NLS-1$
                tag.toString());
        assertEquals(tag.toString().length(), tag.minimumLength());

        tag.setClosed(false);
        assertEquals("<myElement attribute1=\"value1\" attribute2=\"value2\">", //$NON-NLS-1$
                tag.toString());
        assertEquals(tag.toString().length(), tag.minimumLength());
    }

    public void testWrapTag() throws Exception {

        InnerClassFactory.Tag tag = InnerClassFactory.createTag();

        InnerClassFactory.TagFormatter tagFormatter = InnerClassFactory
                .createTagFormatter();

        FormattingPreferences dontAlignCloseChar = new FormattingPreferences() {

            public boolean alignElementCloseChar() {
                return false;
            }
        };
        FormattingPreferences doAlignCloseChar = new FormattingPreferences() {

            public boolean alignElementCloseChar() {
                return true;
            }
        };

        tag.setElementName("myElement"); //$NON-NLS-1$
        tag.addAttribute("attribute1", "value1", '"'); //$NON-NLS-1$ //$NON-NLS-2$
        tag.addAttribute("attribute2", "value2", '"'); //$NON-NLS-1$ //$NON-NLS-2$

        tag.setClosed(true);

        String lineSep= System.getProperty("line.separator");
        assertEquals("<myElement attribute1=\"value1\"" + lineSep //$NON-NLS-1$
                + "\t\t             attribute2=\"value2\" />", tagFormatter //$NON-NLS-1$
                .wrapTag(tag, dontAlignCloseChar, "\t\t  ", lineSep)); //$NON-NLS-1$

        assertEquals("<myElement attribute1=\"value1\"" + lineSep //$NON-NLS-1$
                + "\t\t             attribute2=\"value2\"" + lineSep + "\t\t  />", //$NON-NLS-1$
                tagFormatter.wrapTag(tag, doAlignCloseChar, "\t\t  ", lineSep)); //$NON-NLS-1$

        tag.setClosed(false);

        assertEquals("<myElement attribute1=\"value1\"" + lineSep //$NON-NLS-1$
                + "\t\t             attribute2=\"value2\">", tagFormatter //$NON-NLS-1$
                .wrapTag(tag, dontAlignCloseChar, "\t\t  ", lineSep)); //$NON-NLS-1$

        assertEquals("<myElement attribute1=\"value1\"" + lineSep //$NON-NLS-1$
                + "\t\t             attribute2=\"value2\"" + lineSep + "\t\t  >", //$NON-NLS-1$
                tagFormatter.wrapTag(tag, doAlignCloseChar, "\t\t  ", lineSep)); //$NON-NLS-1$

    }

    public void testBug63558() throws Exception {
		
		// Ordinarily the double space after the element name would be repaired
		// but if the formatter is working correctly these examples will be
		// considered malformed and will be passed through untouched.
    	 String lineSep= System.getProperty("line.separator");
		String source1 = "<echo  file=\"foo\">" + lineSep + "&lt;html>&lt;body>&lt;pre>" //$NON-NLS-1$
				+ "${compilelog}&lt;/pre>&lt;/body>&lt;/html>"; //$NON-NLS-1$
		FormattingPreferences prefs = getPreferences(true, false, 60); //$NON-NLS-1$
		simpleTest(source1, source1, prefs, "\t", lineSep);
	
		String source2 = "<echo  file=\"foo\"/bar/baz></echo>"; //$NON-NLS-1$		
		simpleTest(source2, source2, prefs, "\t", lineSep);
	}
}
