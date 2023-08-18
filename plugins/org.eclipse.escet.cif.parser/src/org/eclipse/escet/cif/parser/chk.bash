#!/usr/bin/env bash

################################################################################
# Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the terms
# of the MIT License which is available at https://opensource.org/licenses/MIT
#
# SPDX-License-Identifier: MIT
#################################################################################

# "    public ..." or "    @Override"

set -e

SCRIPT=`readlink -f $0`
SCRIPTPATH=`dirname $SCRIPT`
cd $SCRIPTPATH

HOOKS_BASE=CifParserHooks
HOOKS_SKELETON_PATH=../../../../../../src-gen/org/eclipse/escet/cif/parser

grep -Pzo "(\n    public [^{]+{|\n    @Override( // [^;]+;)?)" $HOOKS_BASE.java \
    | sed '/^$/d' \
    | tr --delete '\n' \
    | sed -e 's@\x0@\n@g' \
    | sed -e 's@              // @ @g' \
    | sed -e 's@    {@ {@g' \
    | sed -e 's@     \+@ @g' \
    | dos2unix \
    > $HOOKS_BASE.java.tmp

grep "^    [@p][Ou]" $HOOKS_SKELETON_PATH/$HOOKS_BASE.skeleton \
    | dos2unix \
    > $HOOKS_BASE.skeleton.tmp

diff -u $HOOKS_BASE.skeleton.tmp $HOOKS_BASE.java.tmp
echo "No differences found!"

rm $HOOKS_BASE.skeleton.tmp $HOOKS_BASE.java.tmp
