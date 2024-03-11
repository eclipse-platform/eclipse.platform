

FAQ Where can I find that elusive .log file?
============================================

Whenever it encounters a problem that does not warrant launching a dialog, Eclipse saves a report in the workspace log file. The log file can be looked at in four alternative ways.

*   **Window > Show View > PDE Runtime > Error Log**. This gives you a view with the contents of the .log file.

*   **Help > About Eclipse Platform > Configuration Details**. This prints out a great number of details about the environment and also concatenates the .log file. Great for including in a bug report.

*   Locate the file yourself, see workspace/.metadata/.log or eclipse/configuration/*.log

*   Start Eclipse using -consoleLog. This will print the messages that normally go to the .log file in the enclosing shell/command window.

When the Java VM suffers a hard crash, it produces a separate logging file named something like hs\_err\_pid_XXXXX_.log. These files are also helpful for diagnosing problems.

See Also:
---------

*   [FAQ How do I use the platform logging facility?](./FAQ_How_do_I_use_the_platform_logging_facility.md "FAQ How do I use the platform logging facility?")

