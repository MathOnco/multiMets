#!/usr/bin/env bash
[ -d ../classes ] && rm -r ../classes
[ -d ../results ] && rm -r ../results

mkdir ../classes
javac Main.java -d ../classes
cd ../classes

indS=19;#simulation index
tx=2;#0=none/grow,1=cell-cycle dependent MTD, 2=cell-cycle independent MTD, 3=cell-cycle dependent AT, 4=cell-cycle independent AT
mov=1;#0=movie off,1=movie on

parameters=$(head -n +$indS '../source/arrayVars0.txt' | tail -1)

java Main ${parameters} $tx $indS $mov
