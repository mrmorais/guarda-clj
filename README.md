# Clj Guarda

Clojure implementation of a very simple hash keeper. It's a git-like application that stores associated MD5 hashes to identify file tampering.

## Installation

To build from the source code you'll need to use Leiningen. Download from https://leiningen.org/.

Also you can download .jar executable from the project's Github releases section.

## Usage

Run the executable as .jar

    $ java -jar guarda-0.1.0-standalone.jar [args]

Or run with `lein` (being on the project root path)

    $ lein run

## Options

The usage modes of Guarda and parameters definition must be setted as program args:

- **method**: defines which hash type is going to be used between:
  - `-hash`: simple hash algorithm (MD5)
  - `-hmac`: hash with a key. With that method the key must follow the -hmac statement as a parameter setting.

- **option**: defines which mode Guarda is running it could be three diferent ones:
  - `-i`: The **init** mode. It reads a path three of a desired directory and stores a initial, ground truthly hashes.
  - `-t`: The **tracking** mode. It tracks the directory recalculating hashes and highlighting tampered, included or deleted files.
  - `-x`: The **exterminator** mode. It removes the Guarda current structure.

- **path**: defines the root directory that is going to be analysed.
  - Usage pattern: `-path /home/user` (relative path is supported)

- **output**: defines the output report filename that will be generated after executing the tracking mode.

## Examples

...

### Bugs

...


## License

Copyright © 2019 Guarda

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
