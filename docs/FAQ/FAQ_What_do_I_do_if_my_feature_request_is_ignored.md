

FAQ What do I do if my feature request is ignored?
==================================================

Don't be put off by little or no initial response to a feature request logged in Bugzilla. Sometimes, the committers are focusing on feature deadlines or other priorities, but rest assured that everything logged to Bugzilla is at least read by one or more committers within a week. If your request gets no response, you can do some things to help speed up the process.

First, make sure that your request is well described and motivated. Eclipse Bugzilla gets many feature requests, so you need to "sell" your request to the committers and convince them why your request is important. If you are requesting a feature, demonstrate why it is useful to not only you, but also others. If you are requesting new API, you need to describe your main use cases and describe why they cannot be implemented using existing API. Keep in mind that as stewards of their individual plug-ins, committers have an obligation to prevent feature bloat. API is typically added to a plug-in only if it can be demonstrated to be broadly useful to a large number of downstream consumers of that plug-in. If every little requested feature were added without rigorous scrutiny, every plug-in in the platform today would be at least twice as big, every menu twice as long, and the whole platform much slower in execution. And the maintenance would absorb more time and thus prevent committers from fixing other bugs.

Once you have convinced the committers that your request is valid, keep in mind that there is no guarantee it will be implemented for you. They still have only limited time and are focused primarily on major features in the development plan. However, at this point, you are welcome to implement the feature yourself and attach a patch of your implementation to the bug report. Committers will then review your patch and either apply it or reject it. If they reject it, they will describe their technical or other reasons and will often work with you to get it into suitable shape for releasing. Don't forget: In open source, as in much of the world, there is no such thing as a free lunch. The Eclipse committers will implement your feature only if they believe that it is truly useful to the users of their component. If they suspect that you are simply asking them to do your work for you, you'll be out of luck.

See Also:
---------

*   [FAQ How do I report a bug in Eclipse?](./FAQ_How_do_I_report_a_bug_in_Eclipse.md "FAQ How do I report a bug in Eclipse?")

