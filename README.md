Updated to 1.21.4
Fixed some integer overflow errors and issues with unload and load tasks not finishing properly
Added ability to specify how many items to unload or load.

Also fixed an issue with the gui search through npc's for items to trade not working as before it stopped at first page of a trade gui. Will be working on fix for multiple npc's soon.
/load and /unload work as normal fully loading and unloading all inventory containers on a player craft.
you can specify /load all and /unload all to do the same thing
you can also specify a specific amount of items to unload, ie /unload 500 to unload 500 items. and /load 500 to load 500 items for instance. 


everything else works the same, give an npc the trader trait, select a trade menu and then give the trader npc the cargo trait. 
when you want to unload or load items move your craft within range and do the /unload or /load commands while holding one of the item you want to buy or sell


















Release is for CargoRevamped branch using the free version of dtlTraders not main branch.
Will not be releasing a build for the main branch as it requires the premium version of dtlTraders. if you want to use this plugin you will need to buy the premium version yourself, and replace the dtlTraders.jar with dtlTradersPlus.jar in the libs folder before compiling.
Yes i know it's a hassle, it would have been easier if dtlTraders had a public repository but unfortunately it doesn't.
