Rip Minecraft assets from mojang for archiving purposes. You will still need to buy an account to run the game of course. this program is for archiving purposes only. You can rip from .minecraft

Commands
- rip dir/version.json/assetsIndex.json outputDir `--mcDir=value -s -a`
- rip assetsIndex.json & minecraft.jar & outputDir `-s -a`
- checkAll `-c` (WARNING: this could dl up to 21GB+ worth of data if none of the other commands were ran)
- checkMojang `--skipSnaps --mcDir=value`
- checkDisk `--skipSnaps --mcDir=value`
- checkOmni
- checkOld `--mcDir=value --skipSnaps -c`
- recomputeHashes
- verify `--info`
- help

Details:
- `rip -a` flag specifies to rip all assets from the `minecraft.jar` that are not in the assets index including unessary files like `.png, .nbt` files
- `rip -s` skips the `minecraft.jar` extraction part when your using a `assetsIndex.json` instead of a `client.json` file for ripping. this will lead to broken resource packs in vanilla minecraft due to it missing the `.mcmeta` files
- `checkMojang` rips all assets obtainable from checking mojang's master json files from https://launchermeta.mojang.com/mc/game/version_manifest.json and https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
- `checkDisk` checks all jsons files stored at `mcripped/jsons/*` the purpose of this command is to recheck all jsons stored on the disk even custom ones no longer obtainable
- `checkOmni` checks omni archive that has mc versions that the current launcher does not
- `checkOld` checks files of older minecraft sites such as s3.amazonaws.com all minecraft domains and uses legacy json apis. the major json is found at http://s3.amazonaws.com/Minecraft.Download/versions/versions.json . Also goes through a list of all snapshots from beta 1.8 - 1.16.4 and some 1.17
- `verify` verifies the integraty of your current archive downloads and deletes invalid files without the `--info` flag. As checking the hash integraty of every download would slow down the process and mojang's files don't always read the hash the json/url provides
- `recomputeHashes` use this command after the verify command to recompute your index.hash and reset your machine learned data
-  the `-c` flag resets global learned data and forces it to be re-checked(aka downloaded)
- `--mcDir=path` sets the dir of the minecraft
- `--skipSnaps` skips snapshots

Links / API notes:
```
Json Apis:
- New Major V2: https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
- New Major V1: https://launchermeta.mojang.com/mc/game/version_manifest.json

https://s3.amazonaws.com/Minecraft.Download #1.6-? got replaced by json api
- Old Major: http://s3.amazonaws.com/Minecraft.Download/versions/versions.json
- Old Assets Index: https://s3.amazonaws.com/Minecraft.Download/indexes/<assetsIndexId>.json
- Old Minor Json: https://s3.amazonaws.com/Minecraft.Download/versions/<versions>/<version>.json
- Old Version: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/<version>.jar
- Old Server: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/minecraft_server.<version>.jar
- Old Server Exe: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/minecraft_server.<version>.exe

https://assets.minecraft.net
- http://assets.minecraft.net/<version>.replaceAll("\\.","_").replaceAll("~", "_")/minecraft.jar
- http://assets.minecraft.net/<version>.replaceAll("\\.","_").replaceAll("~", "_")/minecraft_server.jar
- http://assets.minecraft.net/<version>.replaceAll("\\.","_").replaceAll("~", "_")/Minecraft_Server.exe

https://minecraft.net/resources #classic-a1.1.2
- gives a comma seperated value file with path,filesize,timestamp
- vanilla client would re-download if the filesize has changed

Amazon Aws indexes:
- http://assets.minecraft.net uses the same index as regular amazonaws however it's currently down so it's embedded into this project now
- http://s3.amazonaws.com/MinecraftResources #a1.1.2_01-1.5.2
- Old: http://s3.amazonaws.com/MinecraftDownload #old launcher alpha-1.5.2 used this
- http://s3.amazonaws.com/Minecraft.Resources #1.6-? got replaced with current json api
```
