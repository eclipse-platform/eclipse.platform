

Internationalization
====================

This page is a hub for resources relating to preparing Eclipse for use in other languages and locales (also known by the abbreviation [i18n](s://en.wikipedia.org/wiki/I18n)). 
This work involves translating strings into other languages (often called NLS for Natural Language Support), handling bi-directional text (BIDI), and preparing content such as dates and times in a format appropriate for a given locale.

Tools Used
----------

Some Eclipse plug-ins use ICU4J APIs when working with locale-specific content.

Most Eclipse plug-ins use a special [Eclipse message bundle](http://www.eclipse.org/eclipse/platform-core/documents/3.1/message_bundles.html) mechanism for working with translated strings. This mechanism uses traditional Java message.properties files, but without using String-based keys. This has much better memory usage characteristics than traditional approaches.

Bidi
----

For Bidirectional locales, like Arabic and Hebrew, some new Bidi-specific APIs were added to Eclipse 3.2 that inject directional markers into strings with implicit left-to-right meaning (such as file paths and URLs) in order to render them properly when the text is mixed. This was a necessary due to an apparent bug in rendering these strings containing mixed text on Windows. The problem appears more frequently on Windows platforms (vs Linux), but has also been found to occur on Linux in certain cases. See the Unicode [Bidirectional algorithm](http://www.unicode.org/reports/tr9/) for the specifics on how strings are normally rendered in bidirectional locales.

When to externalize strings
---------------------------

Generally speaking, there are two possible audiences for a message string in your code: Eclipse plug-in programmers or end users. Messages directed at end users should be translated. Messages for the developers of the plug-in in question should not be translated. This is because the plug-in programmer likely does not understand the language being used by any given end user. If the user reports a problem and provides data such as their error log, the programmer needs to be able to understand that information. Similarly, the programmer may be running tests on different locales where they don't understand the end user strings, but need to understand error and tracing information being produced while running in that locale. Since all plug-in documentation and the code itself is written in English, it is assumed that the plug-in developers are able to understand English programming messages.

The following are specific examples of strings that **should** be translated:

*   Messages appearing in the GUI (editors, views, dialogs, etc).
*   Messages explicitly returned by programmatic API (IStatus messages, messages in checked exceptions, etc)
*   Messages printed to the console by command line tools directed at end users
*   Messages in Ant tasks

The following are specific examples of strings that should **not** be translated:

*   Messages that only ever go to the log
*   Debugging trace messages (such as those managed by the Equinox DebugOptions, and DebugTrace API), or other strings printed to stderr
*   Messages in unchecked exceptions (RuntimeException or Error)


