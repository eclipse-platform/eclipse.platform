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

import java.util.*;

public class Entities {
	static final Hashtable<String, String> decoder = new Hashtable<>(300);
  static final String[]  encoder = new String[0x100];

  static final String decode(String entity) {
    if (entity.charAt(entity.length()-1) == ';')  // remove trailing semicolon
      entity = entity.substring(0, entity.length()-1);
    if (entity.charAt(1) == '#') {
      int start = 2;
      int radix = 10;
      if (entity.charAt(2) == 'X' || entity.charAt(2) == 'x') {
	start++;
	radix = 16;
      }
      char c =
	(char)Integer.parseInt(entity.substring(start), radix);
      return Character.toString(c);
    } else {
      String s = decoder.get(entity);
      if (s != null)
	return s;
      else return ""; //$NON-NLS-1$
    }
  }

  public static final String encode(String s) {
    int length = s.length();
    StringBuilder buffer = new StringBuilder(length * 2);
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      int j = c;
      if (j < 0x100 && encoder[j] != null) {
	buffer.append(encoder[j]);		  // have a named encoding
	buffer.append(';');
      } else if (j < 0x80) {
	buffer.append(c);			  // use ASCII value
      } else {
	buffer.append("&#");			  // use numeric encoding //$NON-NLS-1$
	buffer.append((int)c);
	buffer.append(';');
      }
    }
    return buffer.toString();
  }

  static final void add(String entity, int value) {
    decoder.put(entity, (Character.valueOf((char)value)).toString());
    if (value < 0x100)
      encoder[value] = entity;
  }

  static {
    add("&nbsp",   160); //$NON-NLS-1$
    add("&iexcl",  161); //$NON-NLS-1$
    add("&cent",   162); //$NON-NLS-1$
    add("&pound",  163); //$NON-NLS-1$
    add("&curren", 164); //$NON-NLS-1$
    add("&yen",    165); //$NON-NLS-1$
    add("&brvbar", 166); //$NON-NLS-1$
    add("&sect",   167); //$NON-NLS-1$
    add("&uml",    168); //$NON-NLS-1$
    add("&copy",   169); //$NON-NLS-1$
    add("&ordf",   170); //$NON-NLS-1$
    add("&laquo",  171); //$NON-NLS-1$
    add("&not",    172); //$NON-NLS-1$
    add("&shy",    173); //$NON-NLS-1$
    add("&reg",    174); //$NON-NLS-1$
    add("&macr",   175); //$NON-NLS-1$
    add("&deg",    176); //$NON-NLS-1$
    add("&plusmn", 177); //$NON-NLS-1$
    add("&sup2",   178); //$NON-NLS-1$
    add("&sup3",   179); //$NON-NLS-1$
    add("&acute",  180); //$NON-NLS-1$
    add("&micro",  181); //$NON-NLS-1$
    add("&para",   182); //$NON-NLS-1$
    add("&middot", 183); //$NON-NLS-1$
    add("&cedil",  184); //$NON-NLS-1$
    add("&sup1",   185); //$NON-NLS-1$
    add("&ordm",   186); //$NON-NLS-1$
    add("&raquo",  187); //$NON-NLS-1$
    add("&frac14", 188); //$NON-NLS-1$
    add("&frac12", 189); //$NON-NLS-1$
    add("&frac34", 190); //$NON-NLS-1$
    add("&iquest", 191); //$NON-NLS-1$
    add("&Agrave", 192); //$NON-NLS-1$
    add("&Aacute", 193); //$NON-NLS-1$
    add("&Acirc",  194); //$NON-NLS-1$
    add("&Atilde", 195); //$NON-NLS-1$
    add("&Auml",   196); //$NON-NLS-1$
    add("&Aring",  197); //$NON-NLS-1$
    add("&AElig",  198); //$NON-NLS-1$
    add("&Ccedil", 199); //$NON-NLS-1$
    add("&Egrave", 200); //$NON-NLS-1$
    add("&Eacute", 201); //$NON-NLS-1$
    add("&Ecirc",  202); //$NON-NLS-1$
    add("&Euml",   203); //$NON-NLS-1$
    add("&Igrave", 204); //$NON-NLS-1$
    add("&Iacute", 205); //$NON-NLS-1$
    add("&Icirc",  206); //$NON-NLS-1$
    add("&Iuml",   207); //$NON-NLS-1$
    add("&ETH",    208); //$NON-NLS-1$
    add("&Ntilde", 209); //$NON-NLS-1$
    add("&Ograve", 210); //$NON-NLS-1$
    add("&Oacute", 211); //$NON-NLS-1$
    add("&Ocirc",  212); //$NON-NLS-1$
    add("&Otilde", 213); //$NON-NLS-1$
    add("&Ouml",   214); //$NON-NLS-1$
    add("&times",  215); //$NON-NLS-1$
    add("&Oslash", 216); //$NON-NLS-1$
    add("&Ugrave", 217); //$NON-NLS-1$
    add("&Uacute", 218); //$NON-NLS-1$
    add("&Ucirc",  219); //$NON-NLS-1$
    add("&Uuml",   220); //$NON-NLS-1$
    add("&Yacute", 221); //$NON-NLS-1$
    add("&THORN",  222); //$NON-NLS-1$
    add("&szlig",  223); //$NON-NLS-1$
    add("&agrave", 224); //$NON-NLS-1$
    add("&aacute", 225); //$NON-NLS-1$
    add("&acirc",  226); //$NON-NLS-1$
    add("&atilde", 227); //$NON-NLS-1$
    add("&auml",   228); //$NON-NLS-1$
    add("&aring",  229); //$NON-NLS-1$
    add("&aelig",  230); //$NON-NLS-1$
    add("&ccedil", 231); //$NON-NLS-1$
    add("&egrave", 232); //$NON-NLS-1$
    add("&eacute", 233); //$NON-NLS-1$
    add("&ecirc",  234); //$NON-NLS-1$
    add("&euml",   235); //$NON-NLS-1$
    add("&igrave", 236); //$NON-NLS-1$
    add("&iacute", 237); //$NON-NLS-1$
    add("&icirc",  238); //$NON-NLS-1$
    add("&iuml",   239); //$NON-NLS-1$
    add("&eth",    240); //$NON-NLS-1$
    add("&ntilde", 241); //$NON-NLS-1$
    add("&ograve", 242); //$NON-NLS-1$
    add("&oacute", 243); //$NON-NLS-1$
    add("&ocirc",  244); //$NON-NLS-1$
    add("&otilde", 245); //$NON-NLS-1$
    add("&ouml",   246); //$NON-NLS-1$
    add("&divide", 247); //$NON-NLS-1$
    add("&oslash", 248); //$NON-NLS-1$
    add("&ugrave", 249); //$NON-NLS-1$
    add("&uacute", 250); //$NON-NLS-1$
    add("&ucirc",  251); //$NON-NLS-1$
    add("&uuml",   252); //$NON-NLS-1$
    add("&yacute", 253); //$NON-NLS-1$
    add("&thorn",  254); //$NON-NLS-1$
    add("&yuml",   255); //$NON-NLS-1$
    add("&fnof",   402); //$NON-NLS-1$
    add("&Alpha",  913); //$NON-NLS-1$
    add("&Beta",   914); //$NON-NLS-1$
    add("&Gamma",  915); //$NON-NLS-1$
    add("&Delta",  916); //$NON-NLS-1$
    add("&Epsilon",917); //$NON-NLS-1$
    add("&Zeta",   918); //$NON-NLS-1$
    add("&Eta",    919); //$NON-NLS-1$
    add("&Theta",  920); //$NON-NLS-1$
    add("&Iota",   921); //$NON-NLS-1$
    add("&Kappa",  922); //$NON-NLS-1$
    add("&Lambda", 923); //$NON-NLS-1$
    add("&Mu",     924); //$NON-NLS-1$
    add("&Nu",     925); //$NON-NLS-1$
    add("&Xi",     926); //$NON-NLS-1$
    add("&Omicron",927); //$NON-NLS-1$
    add("&Pi",     928); //$NON-NLS-1$
    add("&Rho",    929); //$NON-NLS-1$
    add("&Sigma",  931); //$NON-NLS-1$
    add("&Tau",    932); //$NON-NLS-1$
    add("&Upsilon",933); //$NON-NLS-1$
    add("&Phi",    934); //$NON-NLS-1$
    add("&Chi",    935); //$NON-NLS-1$
    add("&Psi",    936); //$NON-NLS-1$
    add("&Omega",  937); //$NON-NLS-1$
    add("&alpha",  945); //$NON-NLS-1$
    add("&beta",   946); //$NON-NLS-1$
    add("&gamma",  947); //$NON-NLS-1$
    add("&delta",  948); //$NON-NLS-1$
    add("&epsilon",949); //$NON-NLS-1$
    add("&zeta",   950); //$NON-NLS-1$
    add("&eta",    951); //$NON-NLS-1$
    add("&theta",  952); //$NON-NLS-1$
    add("&iota",   953); //$NON-NLS-1$
    add("&kappa",  954); //$NON-NLS-1$
    add("&lambda", 955); //$NON-NLS-1$
    add("&mu",     956); //$NON-NLS-1$
    add("&nu",     957); //$NON-NLS-1$
    add("&xi",     958); //$NON-NLS-1$
    add("&omicron",959); //$NON-NLS-1$
    add("&pi",     960); //$NON-NLS-1$
    add("&rho",    961); //$NON-NLS-1$
    add("&sigmaf", 962); //$NON-NLS-1$
    add("&sigma",  963); //$NON-NLS-1$
    add("&tau",    964); //$NON-NLS-1$
    add("&upsilon",965); //$NON-NLS-1$
    add("&phi",    966); //$NON-NLS-1$
    add("&chi",    967); //$NON-NLS-1$
    add("&psi",    968); //$NON-NLS-1$
    add("&omega",  969); //$NON-NLS-1$
    add("&thetasym",977); //$NON-NLS-1$
    add("&upsih",  978); //$NON-NLS-1$
    add("&piv",    982); //$NON-NLS-1$
    add("&bull",   8226); //$NON-NLS-1$
    add("&hellip", 8230); //$NON-NLS-1$
    add("&prime",  8242); //$NON-NLS-1$
    add("&Prime",  8243); //$NON-NLS-1$
    add("&oline",  8254); //$NON-NLS-1$
    add("&frasl",  8260); //$NON-NLS-1$
    add("&weierp", 8472); //$NON-NLS-1$
    add("&image",  8465); //$NON-NLS-1$
    add("&real",   8476); //$NON-NLS-1$
    add("&trade",  8482); //$NON-NLS-1$
    add("&alefsym",8501); //$NON-NLS-1$
    add("&larr",   8592); //$NON-NLS-1$
    add("&uarr",   8593); //$NON-NLS-1$
    add("&rarr",   8594); //$NON-NLS-1$
    add("&darr",   8595); //$NON-NLS-1$
    add("&harr",   8596); //$NON-NLS-1$
    add("&crarr",  8629); //$NON-NLS-1$
    add("&lArr",   8656); //$NON-NLS-1$
    add("&uArr",   8657); //$NON-NLS-1$
    add("&rArr",   8658); //$NON-NLS-1$
    add("&dArr",   8659); //$NON-NLS-1$
    add("&hArr",   8660); //$NON-NLS-1$
    add("&forall", 8704); //$NON-NLS-1$
    add("&part",   8706); //$NON-NLS-1$
    add("&exist",  8707); //$NON-NLS-1$
    add("&empty",  8709); //$NON-NLS-1$
    add("&nabla",  8711); //$NON-NLS-1$
    add("&isin",   8712); //$NON-NLS-1$
    add("&notin",  8713); //$NON-NLS-1$
    add("&ni",     8715); //$NON-NLS-1$
    add("&prod",   8719); //$NON-NLS-1$
    add("&sum",    8721); //$NON-NLS-1$
    add("&minus",  8722); //$NON-NLS-1$
    add("&lowast", 8727); //$NON-NLS-1$
    add("&radic",  8730); //$NON-NLS-1$
    add("&prop",   8733); //$NON-NLS-1$
    add("&infin",  8734); //$NON-NLS-1$
    add("&ang",    8736); //$NON-NLS-1$
    add("&and",    8743); //$NON-NLS-1$
    add("&or",     8744); //$NON-NLS-1$
    add("&cap",    8745); //$NON-NLS-1$
    add("&cup",    8746); //$NON-NLS-1$
    add("&int",    8747); //$NON-NLS-1$
    add("&there4", 8756); //$NON-NLS-1$
    add("&sim",    8764); //$NON-NLS-1$
    add("&cong",   8773); //$NON-NLS-1$
    add("&asymp",  8776); //$NON-NLS-1$
    add("&ne",     8800); //$NON-NLS-1$
    add("&equiv",  8801); //$NON-NLS-1$
    add("&le",     8804); //$NON-NLS-1$
    add("&ge",     8805); //$NON-NLS-1$
    add("&sub",    8834); //$NON-NLS-1$
    add("&sup",    8835); //$NON-NLS-1$
    add("&nsub",   8836); //$NON-NLS-1$
    add("&sube",   8838); //$NON-NLS-1$
    add("&supe",   8839); //$NON-NLS-1$
    add("&oplus",  8853); //$NON-NLS-1$
    add("&otimes", 8855); //$NON-NLS-1$
    add("&perp",   8869); //$NON-NLS-1$
    add("&sdot",   8901); //$NON-NLS-1$
    add("&lceil",  8968); //$NON-NLS-1$
    add("&rceil",  8969); //$NON-NLS-1$
    add("&lfloor", 8970); //$NON-NLS-1$
    add("&rfloor", 8971); //$NON-NLS-1$
    add("&lang",   9001); //$NON-NLS-1$
    add("&rang",   9002); //$NON-NLS-1$
    add("&loz",    9674); //$NON-NLS-1$
    add("&spades", 9824); //$NON-NLS-1$
    add("&clubs",  9827); //$NON-NLS-1$
    add("&hearts", 9829); //$NON-NLS-1$
    add("&diams",  9830); //$NON-NLS-1$
    add("&quot",   34); //$NON-NLS-1$
    add("&amp",    38); //$NON-NLS-1$
    add("&lt",     60); //$NON-NLS-1$
    add("&gt",     62); //$NON-NLS-1$
    add("&OElig",  338); //$NON-NLS-1$
    add("&oelig",  339); //$NON-NLS-1$
    add("&Scaron", 352); //$NON-NLS-1$
    add("&scaron", 353); //$NON-NLS-1$
    add("&Yuml",   376); //$NON-NLS-1$
    add("&circ",   710); //$NON-NLS-1$
    add("&tilde",  732); //$NON-NLS-1$
    add("&ensp",   8194); //$NON-NLS-1$
    add("&emsp",   8195); //$NON-NLS-1$
    add("&thinsp", 8201); //$NON-NLS-1$
    add("&zwnj",   8204); //$NON-NLS-1$
    add("&zwj",    8205); //$NON-NLS-1$
    add("&lrm",    8206); //$NON-NLS-1$
    add("&rlm",    8207); //$NON-NLS-1$
    add("&ndash",  8211); //$NON-NLS-1$
    add("&mdash",  8212); //$NON-NLS-1$
    add("&lsquo",  8216); //$NON-NLS-1$
    add("&rsquo",  8217); //$NON-NLS-1$
    add("&sbquo",  8218); //$NON-NLS-1$
    add("&ldquo",  8220); //$NON-NLS-1$
    add("&rdquo",  8221); //$NON-NLS-1$
    add("&bdquo",  8222); //$NON-NLS-1$
    add("&dagger", 8224); //$NON-NLS-1$
    add("&Dagger", 8225); //$NON-NLS-1$
    add("&permil", 8240); //$NON-NLS-1$
    add("&lsaquo", 8249); //$NON-NLS-1$
    add("&rsaquo", 8250); //$NON-NLS-1$
    add("&euro",   8364); //$NON-NLS-1$

  }
}
