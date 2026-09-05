Democracy (vendored, Apache License 2.0)
========================================

Source:   https://github.com/Hydr46605/Democracy
Tag:      v0.7.0
Commit:   07c05066d45db5da4dcdde479072a4d42482efbc
License:  Apache License 2.0 (see LICENSE in this directory)

Lifes vendors the following release jars because Democracy is a private
repository and does not publish Maven artifacts yet:

| Jar                            | SHA-256                                                          | Bytes  | Used at          |
|--------------------------------|------------------------------------------------------------------|--------|------------------|
| democracy-annotations-0.7.0    | c45a013251b68f79b12a1e38b5c6277366ad902856eacee0c7f73beffe757143 | 4234   | compile only     |
| democracy-processor-0.7.0      | 0a0dec7629f44111817f351a27cb132642221b71c71bbab1d1312ba7ca58fdbe | 25830  | compile only     |
| democracy-api-0.7.0            | fc8e065397c8feba5711b35c287b085326c6b072da9507bd7158287ec52e5fa3 | 19842  | bundled in jar   |
| democracy-core-0.7.0           | 3b487b817f33d82e324de138d7a415d8c07f81124aa3a2f6c44baf45d977bc8b | 32638  | bundled in jar   |
| democracy-platform-paper-0.7.0 | be6c7813ac977cb824d4a33502bf864be7e1825e13a1f8b71e16bf0ab809fed4 | 8266   | bundled in jar   |
| democracy-testkit-0.7.0        | bc7dd4c6f1bfa3efc7f8f4d5bc3286bf445244c53bf3b549c70f01671c3f4353 | 3616   | tests only       |

Reproducing the jars:

    git clone https://github.com/Hydr46605/Democracy democracy-src
    git -C democracy-src checkout 07c05066d45db5da4dcdde479072a4d42482efbc
    (cd democracy-src && ./gradlew clean jar)
    # then compare every SHA-256 above against democracy-src/*/build/libs/*.jar

Upgrading:

1. Pick the new Democracy release tag and rebuild in a clean checkout.
2. Replace the jars in this directory and update this file
   (tag, commit, table, checksums).
3. Re-verify the Lifes test suite; the generated-command surface must
   still compile without source changes.
