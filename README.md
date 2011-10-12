# lein-checkouts

This lein plugin performs one of the lein tasks (clean, deps, compile, jar, uberjar, install) on the current project and makes sure that the dependency projects 
which linked in the "checkouts" folder are built as well.

## Install

You can install this plugin via: 

    $ lein plugin install lein-checkouts "1.0.0"

Include it as a dev-dependency in your project.clj is another option:

    :dev-dependencies [[lein-checkouts "1.0.0"]]

## Usage

When using leiningen you can use so called "checkout dependencies" by creating a "checkouts" folder that contains symbolic links 
on other projects you want to use without having to build them.

This plugin enables you to build and install all of those dependency projects for a release of your project (e.g. jar, uberjar).

Depending on the specified task different actions are performed on the dependency projects.

- clean, deps, install: The task is executed for the project and its dependency projects.
- jar, uberjar: All dependency projects are "install"ed and the given task is executed on the current project.

## License

Copyright (C) 2011 Gunnar Völkel

Distributed under the Eclipse Public License, the same as Clojure.
