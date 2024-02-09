

FAQ How can I be notified when the workspace is being saved?
============================================================

  

  

If your plug-in maintains a model based on the workspace, you will want to save your model to disk whenever the workspace is saved. This ensures that your model will be in sync with the workspace every time the platform starts up. Your plug-in can take part in the workspace save process by registering an ISaveParticipant. It is a common mistake to try to perform saving directly from your Plugin.shutdown method, but at this point it is too late to make changes to the workspace. The workspace is saved before any plug-ins start to shut down, so any changes made to files, markers, and other workspace state from your plug-in's shutdown method will be lost.

  
The three kinds of save events are full workspace saves, snapshots, and project saves. Projects cannot be saved explicitly, but they are saved automatically when they are closed. Snapshots must be fast, saving only essential information. Full saves can take longer, and they must ensure that all information that will be needed in future sessions is persisted.

  
You must register your save participant at the start of each session. When you register your participant, you receive a resource delta describing all the changes that occurred since the last save you participated in. This allows your model to catch up with any changes that happened before your plug-in started up. This delta is exactly like the resource deltas provided to a resource change listener. After processing this delta, you can be sure that your model is perfectly in sync with the workspace contents.

After the initial registration, your save participant will be notified each time the workspace is saved.

  

See Also:
---------

[FAQ\_How\_and\_when\_do\_I\_save\_the\_workspace?](./FAQ_How_and_when_do_I_save_the_workspace.md "FAQ How and when do I save the workspace?")

Go to **Platform Plug-in Developer Guide > Programmer's Guide** \> Resources overview > Workspace save participation

