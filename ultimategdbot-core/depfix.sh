#!/bin/bash

# $1 = path to directory containing the dependencies

for file in `ls $1 | grep "\-[0-9][^\.]*\.jar$"`
do
	matched=`echo $file | grep -o "\-[0-9][^\.]*\.jar$"`
	mv $1/$file $1/${file/$matched/.jar} 
done
