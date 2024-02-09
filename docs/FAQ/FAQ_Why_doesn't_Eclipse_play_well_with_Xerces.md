

FAQ Why doesn't Eclipse play well with Xerces?
==============================================

Many plug-ins in the Eclipse Platform require an XML parser for reading and storing various data in XML format. In particular, the platform uses an XML parser to read each plug-in's plugin.xml file during start-up. Because the JDK before 1.4 did not provide an XML parser, Eclipse used to ship with a plug-in containing a Xerces parser. Xerces is one of the two XML parser implementations maintained by the Apache Project.

  
The first problems began to appear when users tried to start Eclipse using a JDK that also contained an implementation of Xerces. Prior to JDK 1.4, it was common practice to throw a copy of Xerces or Xalan into the JDK's ext directory so that it could be used by all applications. Thus, two copies of Xerces were available when Eclipse was starting up, one in the ext directory and one in the org.apache.xerces plug-in. Because libraries provided by the JDK always appear at the beginning of the runtime classpath, the one in the JDK is always found first. If this copy of Xerces was slightly different from the one the platform expected, various linkage errors or ClassCastExceptions occurred on start-up, often preventing Eclipse from starting up at all. The workaround in this case was pretty straightforward: Omit the ext directory from the classpath when starting Eclipse:

      eclipse -vmargs -Djava.ext.dirs=

  
The situation became worse with JDK 1.4, which added a specification for XML called Java API for XML Processing (JAXP). The exact parser implementation was left unspecified, so each JDK was free to choose its own, as long as it was compliant with the interfaces defined by JAXP. Sun decided to use the Apache Crimson parser, and IBM went with the Apache Xerces parser. Worst of all, because these packages were now part of the standard JDK libraries, there was no easy workaround as there has been for the ext problem. The JDK people, realizing that they had messed up by bundling such widely used packages in their JDKs, thereby breaking any application that used slightly different versions of those packages, plan to prefix the implementation package names with a unique prefix to prevent these collisions in the future. However, it was too late to fix this problem for JDK 1.4. The bottom line: You cannot use Eclipse 2.1 or earlier with an IBM 1.4 VM.

  
For Eclipse 3.0, the problem was solved by tossing out the Xerces plug-in and simply using JAXP for any XML processing. Now at most one Xerces is on the classpath, so there cannot be any collisions. Of course, this means that you need at least JDK 1.4 to run Eclipse 3.0.

Update March 29/2006: WTP ships its own **org.apache.xerces** plugin because it needs to use the schema validation capability from Xerces. The Sun JRE is one of the JREs that WTP supports. It comes with the Crimson XML parser, which does not offer the schema validation capability that WTP is looking for. As a result, WTP needs to package Xerces explicitly.

