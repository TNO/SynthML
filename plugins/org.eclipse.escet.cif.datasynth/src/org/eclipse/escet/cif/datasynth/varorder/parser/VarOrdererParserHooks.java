//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.datasynth.varorder.parser;

import static org.eclipse.escet.common.java.Lists.list;

import java.util.Collections;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererListOrderersArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererMultiInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererNumberArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererOrdererArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererSingleInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererStringArg;
import org.eclipse.escet.setext.runtime.Parser;
import org.eclipse.escet.setext.runtime.Token;

/**
 * Call back hook methods for:
 * <ul>
 * <li>{@link VarOrdererParser}</li>
 * </ul>
 */
public final class VarOrdererParserHooks implements VarOrdererParser.Hooks {
    @Override
    public void setParser(Parser<?> parser) {
        // No need to store the parser.
    }

    @Override // VarOrdererSeq : VarOrderer;
    public List<VarOrdererInstance> parseVarOrdererSeq1(VarOrdererInstance v1) {
        return list(v1);
    }

    @Override // VarOrdererSeq : VarOrdererSeq ARROWTK VarOrderer;
    public List<VarOrdererInstance> parseVarOrdererSeq2(List<VarOrdererInstance> l1, VarOrdererInstance v3) {
        l1.add(v3);
        return l1;
    }

    @Override // VarOrdererList : VarOrderer;
    public List<VarOrdererInstance> parseVarOrdererList1(VarOrdererInstance v1) {
        return list(v1);
    }

    @Override // VarOrdererList : VarOrdererList COMMATK VarOrderer;
    public List<VarOrdererInstance> parseVarOrdererList2(List<VarOrdererInstance> l1, VarOrdererInstance v3) {
        l1.add(v3);
        return l1;
    }

    @Override // VarOrderer : @IDENTIFIERTK;
    public VarOrdererInstance parseVarOrderer1(Token t1) {
        return new VarOrdererSingleInstance(t1, Collections.emptyList(), false);
    }

    @Override // VarOrderer : @IDENTIFIERTK PAROPENTK PARCLOSETK;
    public VarOrdererInstance parseVarOrderer2(Token t1) {
        return new VarOrdererSingleInstance(t1, Collections.emptyList(), true);
    }

    @Override // VarOrderer : @IDENTIFIERTK PAROPENTK VarOrdererArgs OptComma PARCLOSETK;
    public VarOrdererInstance parseVarOrderer3(Token t1, List<VarOrdererArg> l3, Token t4) {
        return new VarOrdererSingleInstance(t1, l3, true);
    }

    @Override // VarOrderer : @PAROPENTK VarOrdererSeq PARCLOSETK;
    public VarOrdererInstance parseVarOrderer4(Token t1, List<VarOrdererInstance> l2) {
        return new VarOrdererMultiInstance(t1.position, l2);
    }

    @Override // VarOrdererArgs : VarOrdererArg;
    public List<VarOrdererArg> parseVarOrdererArgs1(VarOrdererArg v1) {
        return list(v1);
    }

    @Override // VarOrdererArgs : VarOrdererArgs COMMATK VarOrdererArg;
    public List<VarOrdererArg> parseVarOrdererArgs2(List<VarOrdererArg> l1, VarOrdererArg v3) {
        l1.add(v3);
        return l1;
    }

    @Override // VarOrdererArg : @IDENTIFIERTK EQUALTK @NUMBERTK;
    public VarOrdererArg parseVarOrdererArg1(Token t1, Token t3) {
        return new VarOrdererNumberArg(t1, t3);
    }

    @Override // VarOrdererArg : @IDENTIFIERTK EQUALTK @STRINGTK;
    public VarOrdererArg parseVarOrdererArg2(Token t1, Token t3) {
        return new VarOrdererStringArg(t1, t3);
    }

    @Override // VarOrdererArg : @IDENTIFIERTK EQUALTK VarOrderer;
    public VarOrdererArg parseVarOrdererArg3(Token t1, VarOrdererInstance v3) {
        return new VarOrdererOrdererArg(t1, v3);
    }

    @Override // VarOrdererArg : @IDENTIFIERTK EQUALTK SQOPENTK VarOrdererList SQCLOSETK;
    public VarOrdererArg parseVarOrdererArg4(Token t1, List<VarOrdererInstance> l4) {
        return new VarOrdererListOrderersArg(t1, l4);
    }

    @Override // OptComma : ;
    public Token parseOptComma1() {
        return null;
    }

    @Override // OptComma : @COMMATK;
    public Token parseOptComma2(Token t1) {
        return t1;
    }
}
