Gluck Compiler
==============

This project was an assignment of the Compilers course at [ITBA] [1] in 2008. See copyright notes at the bottom.

It features a [**compiler**] [2] of a programming language called Gluck. It is based in Java libraries, namely JFlex and BYACC/J, among others.

Refer to the documentation for further information on the compiler features and usage.

  [1]: http://www.itba.edu.ar
  [2]: http://en.wikipedia.org/wiki/Compiler

Program usage
-------------

Note: Build `dcc.jar` using the Apache Ant buildfile. It requires the `lib` directory at the same level. Also note that the source code must include the `system` directory at the default location for the `io` and `Util` classes to be compiled.

    java -jar dcc.jar [options] [filename]

Options:

    -S for grammar analysis only
    -D for grammar analysis and showing dependencies (-r for recursive search)
    -T for showing the symbol table
    -E for lexical, syntactic and semantic parsing
    -I for generating intermediate code (-o for including optimizations)
    -A for generating Jasmin-compatible assembly-code
    -C for compiling input source code into a .class file

Copyright
---------

    Copyright (c) 2008
     - Rafael Martín Bigio <rbigio@itba.edu.ar>
     - Santiago Andrés Coffey <scoffey@itba.edu.ar>
     - Andrés Santiago Gregoire <agregoir@itba.edu.ar>

    The following additional provisions apply to third party software
    included as part of this product:
     - BYACC/J: Public domain software. See <http://byaccj.sourceforge.net/>
     - JFlex: Licensed under the GNU GPL. See <http://www.jflex.de/>
     - Asm 3.1: Licensed under the BSD license. See <http://asm.ow2.org/>
     - Jasmin: Copyright (c) 2004 Jonathan Meyer et al.
       Licensed under the GNU GPL. See <http://jasmin.sourceforge.net/>

    Gluck Compiler is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gluck Compiler is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gluck Compiler.  If not, see <http://www.gnu.org/licenses/>.

