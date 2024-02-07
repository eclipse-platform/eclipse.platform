Coding Conventions
==================

Oracle has established coding standards that are generally considered reasonable, as evidenced by their widespread adoption by other Java-based development efforts. One of the goals is to make the Eclipse Platform blend in with the Java platform. This goal is furthered by our following suit.

[Oracle's Code Conventions for the Java Programming Language](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html) covers filenames, file organization, indentation, comments, declarations, statements, white space, naming conventions, and programming practices. 
All code written for the Eclipse Platform should follow these conventions except as noted below. 
We deviate only in places where our needs differ from Oracle's; when we do deviate, we explain why. (The section numbers shown below are Oracle's.)

*   Section 3.1.1 Beginning Comments

The Eclipse project has specific guidelines for copyright notices to appear at the beginning of source files. See the [Eclipse Project Charter](http://www.eclipse.org/eclipse/eclipse-charter.php) for details.

*   Section 4 Indentation

We indent with tabs (4 spaces wide), since mixed indents are a mess.

*   Section 9 Naming Conventions

The Eclipse project has more specific naming conventions. See Eclipse Project [Naming Conventions](/Naming_Conventions "Naming Conventions") for details.

Modifiers should be ordered as specified in the JLS and summarized in [AST#newModifiers(int)](http://help.eclipse.org/neon/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/AST.html#newModifiers-int-):

    public protected private
    abstract default static final
    synchronized native strictfp transient volatile
    

For Javadoc conventions, see Oracle's [How to Write Doc Comments for the Javadoc Tool](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html) and [Eclipse/API_Central](/https://github.com/eclipse-platform/eclipse.platform/blob/api-central/docs/API_Central.md).

For Eclipse projects, there is no policy that requires a specific coding format or style, though the Eclipse style in the formatter and cleanup is preferred. Project teams typically determine their own styles and then commit the appropriate files. One way is to commit files on a per-project basis, the other is to have a central set of files that should be imported by each committer. See [Eclipse Incubation Mailing List, 20-June-2016](https://dev.eclipse.org/mhonarc/lists/incubation/msg00141.html).

It is possible to configure the Eclipse formatter to skip certain sections, see [How to turn off the Eclipse code formatter for certain sections of Java code?](https://stackoverflow.com/questions/1820908/how-to-turn-off-the-eclipse-code-formatter-for-certain-sections-of-java-code).



