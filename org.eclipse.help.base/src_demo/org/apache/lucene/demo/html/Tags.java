package org.apache.lucene.demo.html;

/**
 * Copyright 2004, 2015 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public final class Tags {

  /**
   * contains all tags for which whitespaces have to be inserted for proper tokenization
   */
  public static final Set<String> WS_ELEMS = Collections.synchronizedSet(new HashSet<>());

  static{
    WS_ELEMS.add("<hr"); //$NON-NLS-1$
    WS_ELEMS.add("<hr/");  // note that "<hr />" does not need to be listed explicitly //$NON-NLS-1$
    WS_ELEMS.add("<br"); //$NON-NLS-1$
    WS_ELEMS.add("<br/"); //$NON-NLS-1$
    WS_ELEMS.add("<p"); //$NON-NLS-1$
    WS_ELEMS.add("</p"); //$NON-NLS-1$
    WS_ELEMS.add("<div"); //$NON-NLS-1$
    WS_ELEMS.add("</div"); //$NON-NLS-1$
    WS_ELEMS.add("<td"); //$NON-NLS-1$
    WS_ELEMS.add("</td"); //$NON-NLS-1$
    WS_ELEMS.add("<li"); //$NON-NLS-1$
    WS_ELEMS.add("</li"); //$NON-NLS-1$
    WS_ELEMS.add("<q"); //$NON-NLS-1$
    WS_ELEMS.add("</q"); //$NON-NLS-1$
    WS_ELEMS.add("<blockquote"); //$NON-NLS-1$
    WS_ELEMS.add("</blockquote"); //$NON-NLS-1$
    WS_ELEMS.add("<dt"); //$NON-NLS-1$
    WS_ELEMS.add("</dt"); //$NON-NLS-1$
    WS_ELEMS.add("<h1"); //$NON-NLS-1$
    WS_ELEMS.add("</h1"); //$NON-NLS-1$
    WS_ELEMS.add("<h2"); //$NON-NLS-1$
    WS_ELEMS.add("</h2"); //$NON-NLS-1$
    WS_ELEMS.add("<h3"); //$NON-NLS-1$
    WS_ELEMS.add("</h3"); //$NON-NLS-1$
    WS_ELEMS.add("<h4"); //$NON-NLS-1$
    WS_ELEMS.add("</h4"); //$NON-NLS-1$
    WS_ELEMS.add("<h5"); //$NON-NLS-1$
    WS_ELEMS.add("</h5"); //$NON-NLS-1$
    WS_ELEMS.add("<h6"); //$NON-NLS-1$
    WS_ELEMS.add("</h6"); //$NON-NLS-1$
  }
}
