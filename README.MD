Rip Minecraft assets from mojang for archiving purposes. You will still need to buy an account to run the game of course. this program is for archiving purposes only. You can rip from .minecraft

Commands
- rip dir/version.json/assetsIndex.json outputDir --mcDir=value 
- rip assetsIndex.json & minecraft.jar & outputDir
- checkMojang --skipSnaps --mcDir=value
- checkDisk --skipSnaps --skipOldMajors --mcDir=value
- checkOmni
- checkOld --mcDir=value --skipSnaps
- recomputeHashes
- verify
- help

Notes:
- the `rip` command only requires the `minecraft.jar` if you only input the assetsIndex without the `-s` flag.
- the `rip` command requires user input in the `dir` variant when runnning the command without the `-s` flag
