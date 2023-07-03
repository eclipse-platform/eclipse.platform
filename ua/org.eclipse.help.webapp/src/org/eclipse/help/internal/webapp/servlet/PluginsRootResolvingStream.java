/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.search.HTMLDocParser;

/**
 * This class replaces PLUGINS_ROOT with a relative path to eliminate redirects.
 * It also performs preprocessing to add child links at runtime.
 */
public class PluginsRootResolvingStream extends OutputStream {

	protected OutputStream out;

	private int state = INITIAL_STATE;
	private int charsMatched = 0;
	private int lastKeywordMatch = 0;
	private static final int  INITIAL_STATE = 0;
	private static final int  IN_TAG = 1;
	private static final int  IN_QUOTE = 2;
	private static final int  IN_QUOTE_NOT_PLUGINS_ROOT = 3;
	private static final int  MAY_BE_INCLUDE = 4;
	private static final int  IN_METATAG = 5;
	private static final String PLUGINS_ROOT = "PLUGINS_ROOT/"; //$NON-NLS-1$
	private static final String INSERT_CHILD_LINKS = "<!--INSERT_CHILD_LINKS-->"; //$NON-NLS-1$
	private static final String INSERT_CHILD_LINK_STYLE = "<!--INSERT_CHILD_LINK_STYLE-->"; //$NON-NLS-1$
	private final String[] keywords = { INSERT_CHILD_LINKS, INSERT_CHILD_LINK_STYLE };
	private boolean[] possibleKeywordMatches;
	private String pathPrefix;
	private StringBuilder tag;
	private ByteArrayOutputStream metaTagBuffer;
	private boolean tagRead;
	private HttpServletRequest req;
	private String charset;

	public PluginsRootResolvingStream(OutputStream out, HttpServletRequest req, String prefix) {
		this.out = out;
		this.pathPrefix = prefix;
		this.req = req;
	}

	@Override
	public void write(int b) throws IOException {
		switch(state) {
		case INITIAL_STATE:
			if (b == '<') {
				state = IN_TAG;
				charsMatched = 0;
				tag = new StringBuilder();
				tagRead = false;
			} else {
				out.write(b);
			}
			break;
		case IN_TAG:
			if (charsMatched == 0) {
				if (b == '!') {
					state = MAY_BE_INCLUDE;
					possibleKeywordMatches = new boolean[keywords.length];
					for (int i = 0; i < possibleKeywordMatches.length; i++) {
						possibleKeywordMatches[i] = true;
					}
					charsMatched = 2; // Chars matched in "<!--INCLUDE"
					lastKeywordMatch = 0;
					break;
				} else {
					out.write('<');
				}
			}
			if (b == '>') {
				state = INITIAL_STATE;
			} else if (b == '"') {
				state = IN_QUOTE;
				charsMatched = 0;
			} else {
				charsMatched++;
				if (!tagRead) {
					if (b >= 0 && b < 128 && tag.length() < 20) {
						// ASCII
						char c = (char)b;
						if (Character.isLetter(c)) {
							tag.append(c);
						} else if (Character.isWhitespace(c)) {
							tagRead = true;
							if (tag.toString().equalsIgnoreCase("meta")) { //$NON-NLS-1$
								state = IN_METATAG;
								metaTagBuffer = new ByteArrayOutputStream(7);
								metaTagBuffer.write("<meta ".getBytes()); //$NON-NLS-1$
							}
						} else  {
							tag.append(c);
						}
					}
				}
			}
			out.write(b);
			break;
		case IN_QUOTE_NOT_PLUGINS_ROOT:
			if (b == '>') {
				state = INITIAL_STATE;
			} else if (b == '"') {
				state = IN_TAG;
				charsMatched = 1;
			}
			out.write(b);
			break;
		case IN_QUOTE:
			// In a quote which may start with PLUGINS_ROOT
			if (b == PLUGINS_ROOT.charAt(charsMatched)) {
				charsMatched++;
				if (charsMatched == PLUGINS_ROOT.length()) {
					out.write(pathPrefix.getBytes());
					state = IN_QUOTE_NOT_PLUGINS_ROOT;
				}
			} else {
				// We just discovered that this is not "PLUGINS_ROOT/
				// flush out the characters
				state = IN_QUOTE_NOT_PLUGINS_ROOT;
				flushPluginsRootCharacters();
				out.write(b);
			}
			break;
		case MAY_BE_INCLUDE:
			// Compare against all possible keywords
			boolean canStillMatch = false;
			int perfectMatch = -1;
			for (int i = 0; i < keywords.length; i++) {
				if (possibleKeywordMatches[i]) {
					if (keywords[i].charAt(charsMatched) == b) {
						canStillMatch = true;
						lastKeywordMatch = i;
						if (keywords[i].length() == charsMatched + 1) {
							perfectMatch = i;
						}
					} else {
						possibleKeywordMatches[i] = false;
					}
				}
			}
			if (perfectMatch != -1) {
				insertBasedOnKeyword(perfectMatch);
				state=INITIAL_STATE;
			} else if (canStillMatch) {
				charsMatched++;
			} else {
				state = INITIAL_STATE;
				flushKeywordCharacters();
				out.write(b);
			}
			break;
		case IN_METATAG:
			out.write(b);
			metaTagBuffer.write(b);
			if (b=='>') {
				parseMetaTag(metaTagBuffer);
				metaTagBuffer = null;
				state = INITIAL_STATE;
			}
			break;
		default:
			out.write(b);
		}
	}

	private void parseMetaTag(ByteArrayOutputStream buffer) {
		try (ByteArrayInputStream is = new ByteArrayInputStream(buffer.toByteArray())) {
			String value = HTMLDocParser.getCharsetFromHTML(is);
			if (value != null) {
				this.charset = value;
			}
		} catch (IOException e) {
		}
	}

	protected void insertBasedOnKeyword(int index) throws IOException {
		if (index == 0 ) {
			ChildLinkInserter inserter = new ChildLinkInserter(req, out);
			inserter.addContents(getCharset());
		} else {
			ChildLinkInserter inserter = new ChildLinkInserter(req, out);
			inserter.addStyle();
		}
	}

	private void flushPluginsRootCharacters() throws IOException {
		out.write(PLUGINS_ROOT.substring(0, charsMatched).getBytes(StandardCharsets.UTF_8));
	}

	private void flushKeywordCharacters() throws IOException {
		String matchingCharacters = keywords[lastKeywordMatch].substring(0, charsMatched);
		out.write(matchingCharacters.getBytes(StandardCharsets.UTF_8));
	}


	@Override
	public void close() throws IOException {
		if (state == IN_QUOTE) {
			flushPluginsRootCharacters();
		} else if (state == MAY_BE_INCLUDE) {
			flushKeywordCharacters();
		}
		out.close();
		super.close();
	}

	public String getCharset() {
		return charset;
	}
}