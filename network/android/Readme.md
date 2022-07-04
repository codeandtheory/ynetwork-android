# Network - Android

- Module responsible for android specific networking codebase.
- The Network engine and cache engine is implemented using OkHttp and DiskCache (equivalent to
  OkHttp cache).
- This is implementation specific to the android platform for multi-platform module for which
  documentation could be found in [Core Module's documentation](../core/Readme.md)

## AndroidNetworkEngine

- The `AndroidNetworkEngine` is a wrapper around the `OkHttp` network library.
- The OkHttp's instance inside the engine is initiated in `#init` function with the help
  of `NetworkEngineConfiguration`.

## AndroidCacheEngine

- The `AndroidCacheEngine` is a wrapper around Disk LRU-Cache. This cache writes the data into a file
  directory allowing the data to be persistent across app launch.
- Since OkHttp doesn't expose it's Disk LRU cache to the developers, the copy of Disk LRU cache has
  been made to allow more control over how the cache should work.
- This copy of this Disk LRU cache is inspiration from the Google's earlier implementation of Disk
  Cache in Android SDK, which was then moved by Google into OkHttp library.

## Documentation:

https://www.notion.so/ymedialabs/Accelerator-Networking-Module-Documentation-f9d14919cd2a45c0943af4205a2489ec