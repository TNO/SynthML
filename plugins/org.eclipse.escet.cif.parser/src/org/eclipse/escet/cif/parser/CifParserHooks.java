//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.parser;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.AAlgParameter;
import org.eclipse.escet.cif.parser.ast.ACompDecl;
import org.eclipse.escet.cif.parser.ast.ACompDefDecl;
import org.eclipse.escet.cif.parser.ast.ACompInstDecl;
import org.eclipse.escet.cif.parser.ast.AComponentParameter;
import org.eclipse.escet.cif.parser.ast.ADecl;
import org.eclipse.escet.cif.parser.ast.AEquation;
import org.eclipse.escet.cif.parser.ast.AEquationDecl;
import org.eclipse.escet.cif.parser.ast.AEventParameter;
import org.eclipse.escet.cif.parser.ast.AEventParameterPart;
import org.eclipse.escet.cif.parser.ast.AFuncDecl;
import org.eclipse.escet.cif.parser.ast.AGroupBody;
import org.eclipse.escet.cif.parser.ast.AImport;
import org.eclipse.escet.cif.parser.ast.AImportDecl;
import org.eclipse.escet.cif.parser.ast.AInitialDecl;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.AInvariantDecl;
import org.eclipse.escet.cif.parser.ast.ALocationParameter;
import org.eclipse.escet.cif.parser.ast.AMarkedDecl;
import org.eclipse.escet.cif.parser.ast.ANamespaceDecl;
import org.eclipse.escet.cif.parser.ast.AParameter;
import org.eclipse.escet.cif.parser.ast.ASpecification;
import org.eclipse.escet.cif.parser.ast.annotations.AAnnotation;
import org.eclipse.escet.cif.parser.ast.annotations.AAnnotationArgument;
import org.eclipse.escet.cif.parser.ast.automata.AAlphabetDecl;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AAutomatonBody;
import org.eclipse.escet.cif.parser.ast.automata.ACoreEdge;
import org.eclipse.escet.cif.parser.ast.automata.AEdgeEvent;
import org.eclipse.escet.cif.parser.ast.automata.AEdgeEvent.Direction;
import org.eclipse.escet.cif.parser.ast.automata.AEdgeLocationElement;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AEquationLocationElement;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AInitialLocationElement;
import org.eclipse.escet.cif.parser.ast.automata.AInvariantLocationElement;
import org.eclipse.escet.cif.parser.ast.automata.ALocation;
import org.eclipse.escet.cif.parser.ast.automata.ALocationElement;
import org.eclipse.escet.cif.parser.ast.automata.AMarkedLocationElement;
import org.eclipse.escet.cif.parser.ast.automata.AMonitorDecl;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUrgentLocationElement;
import org.eclipse.escet.cif.parser.ast.declarations.AAlgVariable;
import org.eclipse.escet.cif.parser.ast.declarations.AAlgVariableDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AConstDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AConstant;
import org.eclipse.escet.cif.parser.ast.declarations.AContVariable;
import org.eclipse.escet.cif.parser.ast.declarations.AContVariableDecl;
import org.eclipse.escet.cif.parser.ast.declarations.ADiscVariable;
import org.eclipse.escet.cif.parser.ast.declarations.ADiscVariableDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AEnumDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AEventDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AInputVariableDecl;
import org.eclipse.escet.cif.parser.ast.declarations.ATypeDef;
import org.eclipse.escet.cif.parser.ast.declarations.ATypeDefDecl;
import org.eclipse.escet.cif.parser.ast.declarations.AVariableValue;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ACastExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ADictExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ADictPair;
import org.eclipse.escet.cif.parser.ast.expressions.AElifExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AEmptySetDictExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AFuncCallExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AListExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AProjectionExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ARealExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AReceivedExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ASelfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ASetExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ASliceExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AStdLibFunctionExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AStringExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ASwitchCase;
import org.eclipse.escet.cif.parser.ast.expressions.ASwitchExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ATauExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ATimeExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ATupleExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.escet.cif.parser.ast.functions.AAssignFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.ABreakFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AContinueFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AElifFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AElseFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AExternalFuncBody;
import org.eclipse.escet.cif.parser.ast.functions.AFuncBody;
import org.eclipse.escet.cif.parser.ast.functions.AFuncParam;
import org.eclipse.escet.cif.parser.ast.functions.AFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AIfFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AInternalFuncBody;
import org.eclipse.escet.cif.parser.ast.functions.AReturnFuncStatement;
import org.eclipse.escet.cif.parser.ast.functions.AWhileFuncStatement;
import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrint;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrintFile;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrintFor;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrintForKind;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrintTxt;
import org.eclipse.escet.cif.parser.ast.iodecls.print.APrintWhen;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgCopy;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgFile;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgIn;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgInEvent;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgInEventIf;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgInEventIfEntry;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgInEventSingle;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgMove;
import org.eclipse.escet.cif.parser.ast.iodecls.svg.ASvgOut;
import org.eclipse.escet.cif.parser.ast.tokens.AEventParamFlag;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.cif.parser.ast.tokens.AStringToken;
import org.eclipse.escet.cif.parser.ast.types.ABoolType;
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.cif.parser.ast.types.ADictType;
import org.eclipse.escet.cif.parser.ast.types.ADistType;
import org.eclipse.escet.cif.parser.ast.types.AField;
import org.eclipse.escet.cif.parser.ast.types.AFuncType;
import org.eclipse.escet.cif.parser.ast.types.AIntType;
import org.eclipse.escet.cif.parser.ast.types.AListType;
import org.eclipse.escet.cif.parser.ast.types.ANamedType;
import org.eclipse.escet.cif.parser.ast.types.ARange;
import org.eclipse.escet.cif.parser.ast.types.ARealType;
import org.eclipse.escet.cif.parser.ast.types.ASetType;
import org.eclipse.escet.cif.parser.ast.types.AStringType;
import org.eclipse.escet.cif.parser.ast.types.ATupleType;
import org.eclipse.escet.cif.parser.ast.types.AVoidType;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.Parser;
import org.eclipse.escet.setext.runtime.Token;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;

/**
 * Call back hook methods for:
 * <ul>
 * <li>{@link CifParser}</li>
 * </ul>
 */
public final class CifParserHooks implements CifParser.Hooks {
    /** The parser that owns the call back hooks. */
    private Parser<?> parser;

    @Override
    public void setParser(Parser<?> parser) {
        this.parser = parser;
    }

    @Override // SupKind : @PLANTKW;
    public Token parseSupKind1(Token t1) {
        return t1;
    }

    @Override // SupKind : @REQUIREMENTKW;
    public Token parseSupKind2(Token t1) {
        return t1;
    }

    @Override // SupKind : @SUPERVISORKW;
    public Token parseSupKind3(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ACOSHKW;
    public Token parseStdLibFunction01(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ACOSKW;
    public Token parseStdLibFunction02(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ASINHKW;
    public Token parseStdLibFunction03(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ASINKW;
    public Token parseStdLibFunction04(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ATANHKW;
    public Token parseStdLibFunction05(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ATANKW;
    public Token parseStdLibFunction06(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @COSHKW;
    public Token parseStdLibFunction07(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @COSKW;
    public Token parseStdLibFunction08(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SINHKW;
    public Token parseStdLibFunction09(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SINKW;
    public Token parseStdLibFunction10(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @TANHKW;
    public Token parseStdLibFunction11(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @TANKW;
    public Token parseStdLibFunction12(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ABSKW;
    public Token parseStdLibFunction13(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @CBRTKW;
    public Token parseStdLibFunction14(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @CEILKW;
    public Token parseStdLibFunction15(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @DELKW;
    public Token parseStdLibFunction16(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @EMPTYKW;
    public Token parseStdLibFunction17(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @EXPKW;
    public Token parseStdLibFunction18(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @FLOORKW;
    public Token parseStdLibFunction19(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @FMTKW;
    public Token parseStdLibFunction20(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @LNKW;
    public Token parseStdLibFunction21(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @LOGKW;
    public Token parseStdLibFunction22(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @MAXKW;
    public Token parseStdLibFunction23(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @MINKW;
    public Token parseStdLibFunction24(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @POPKW;
    public Token parseStdLibFunction25(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @POWKW;
    public Token parseStdLibFunction26(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ROUNDKW;
    public Token parseStdLibFunction27(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SCALEKW;
    public Token parseStdLibFunction28(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SIGNKW;
    public Token parseStdLibFunction29(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SIZEKW;
    public Token parseStdLibFunction30(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @SQRTKW;
    public Token parseStdLibFunction31(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @BERNOULLIKW;
    public Token parseStdLibFunction32(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @BETAKW;
    public Token parseStdLibFunction33(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @BINOMIALKW;
    public Token parseStdLibFunction34(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @CONSTANTKW;
    public Token parseStdLibFunction35(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @ERLANGKW;
    public Token parseStdLibFunction36(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @EXPONENTIALKW;
    public Token parseStdLibFunction37(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @GAMMAKW;
    public Token parseStdLibFunction38(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @GEOMETRICKW;
    public Token parseStdLibFunction39(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @LOGNORMALKW;
    public Token parseStdLibFunction40(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @NORMALKW;
    public Token parseStdLibFunction41(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @POISSONKW;
    public Token parseStdLibFunction42(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @RANDOMKW;
    public Token parseStdLibFunction43(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @TRIANGLEKW;
    public Token parseStdLibFunction44(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @UNIFORMKW;
    public Token parseStdLibFunction45(Token t1) {
        return t1;
    }

    @Override // StdLibFunction : @WEIBULLKW;
    public Token parseStdLibFunction46(Token t1) {
        return t1;
    }

    @Override // Specification : GroupBody;
    public ASpecification parseSpecification1(AGroupBody a1) {
        String src = parser.getSource();
        String loc = parser.getLocation();
        return new ASpecification(a1, TextPosition.createDummy(loc, src));
    }

    @Override // GroupBody : OptGroupDecls;
    public AGroupBody parseGroupBody1(List<ADecl> l1) {
        return new AGroupBody(l1);
    }

    @Override // AutomatonBody : OptAutDecls Locations OptIoDecls;
    public AAutomatonBody parseAutomatonBody1(List<ADecl> l1, List<ALocation> l2, List<AIoDecl> l3) {
        l1.addAll(l3);
        return new AAutomatonBody(l1, l2);
    }

    @Override // OptGroupDecls : ;
    public List<ADecl> parseOptGroupDecls1() {
        return list();
    }

    @Override // OptGroupDecls : OptGroupDecls GroupDecl;
    public List<ADecl> parseOptGroupDecls2(List<ADecl> l1, ADecl a2) {
        l1.add(a2);
        return l1;
    }

    @Override // GroupDecl : Decl;
    public ADecl parseGroupDecl01(ADecl a1) {
        return a1;
    }

    @Override // GroupDecl : @IMPORTKW Imports SEMICOLTK;
    public ADecl parseGroupDecl02(Token t1, List<AImport> l2) {
        return new AImportDecl(l2, t1.position);
    }

    @Override // GroupDecl : @NAMESPACEKW @IDENTIFIERTK SEMICOLTK;
    public ADecl parseGroupDecl03(Token t1, Token t2) {
        return new ANamespaceDecl(new AName(t2.text, t2.position), t1.position);
    }

    @Override // GroupDecl : @NAMESPACEKW @RELATIVENAMETK SEMICOLTK;
    public ADecl parseGroupDecl04(Token t1, Token t2) {
        return new ANamespaceDecl(new AName(t2.text, t2.position), t1.position);
    }

    @Override // GroupDecl : @FUNCKW Types Identifier FuncParams COLONTK FuncBody;
    public ADecl parseGroupDecl05(Token t1, List<ACifType> l2, AIdentifier a3, List<AFuncParam> l4, AFuncBody a6) {
        if (a6 instanceof AInternalFuncBody) {
            parser.addFoldRange(t1.position, ((AInternalFuncBody)a6).endPos);
        }
        return new AFuncDecl(a3, l2, l4, a6, a3.position);
    }

    @Override // GroupDecl : Identifier COLONTK Name CompInstArgs @SEMICOLTK;
    public ADecl parseGroupDecl06(AIdentifier a1, AName a3, List<AExpression> l4, Token t5) {
        parser.addFoldRange(a1.position, t5.position);
        return new ACompInstDecl(a1, a3, l4, a1.position);
    }

    @Override // GroupDecl : @GROUPKW DEFKW Identifier CompDefParms COLONTK GroupBody @ENDKW;
    public ADecl parseGroupDecl07(Token t1, AIdentifier a3, List<AParameter> l4, AGroupBody a6, Token t7) {
        parser.addFoldRange(t1, t7);
        return new ACompDefDecl(null, a3, l4, a6, a3.position);
    }

    @Override // GroupDecl : OptSupKind @AUTOMATONKW DEFKW Identifier CompDefParms COLONTK AutomatonBody @ENDKW;
    public ADecl parseGroupDecl08(Token t1, Token t2, AIdentifier a4, List<AParameter> l5, AAutomatonBody a7,
            Token t8)
    {
        Token firstToken = (t1 != null) ? t1 : t2;
        parser.addFoldRange(firstToken, t8);
        return new ACompDefDecl(t1, a4, l5, a7, a4.position);
    }

    @Override // GroupDecl : SupKind DEFKW Identifier CompDefParms COLONTK AutomatonBody @ENDKW;
    public ADecl parseGroupDecl09(Token t1, AIdentifier a3, List<AParameter> l4, AAutomatonBody a6, Token t7) {
        parser.addFoldRange(t1, t7);
        return new ACompDefDecl(t1, a3, l4, a6, a3.position);
    }

    @Override // GroupDecl : @GROUPKW Identifier COLONTK GroupBody @ENDKW;
    public ADecl parseGroupDecl10(Token t1, AIdentifier a2, AGroupBody a4, Token t5) {
        parser.addFoldRange(t1, t5);
        return new ACompDecl(null, a2, a4, a2.position);
    }

    @Override // GroupDecl : OptSupKind @AUTOMATONKW Identifier COLONTK AutomatonBody @ENDKW;
    public ADecl parseGroupDecl11(Token t1, Token t2, AIdentifier a3, AAutomatonBody a5, Token t6) {
        Token firstToken = (t1 != null) ? t1 : t2;
        parser.addFoldRange(firstToken, t6);
        return new ACompDecl(t1, a3, a5, a3.position);
    }

    @Override // GroupDecl : SupKind Identifier COLONTK AutomatonBody @ENDKW;
    public ADecl parseGroupDecl12(Token t1, AIdentifier a2, AAutomatonBody a4, Token t5) {
        parser.addFoldRange(t1, t5);
        return new ACompDecl(t1, a2, a4, a2.position);
    }

    @Override // OptAutDecls : ;
    public List<ADecl> parseOptAutDecls1() {
        return list();
    }

    @Override // OptAutDecls : OptAutDecls AutDecl;
    public List<ADecl> parseOptAutDecls2(List<ADecl> l1, ADecl a2) {
        l1.add(a2);
        return l1;
    }

    @Override // AutDecl : Decl;
    public ADecl parseAutDecl1(ADecl a1) {
        return a1;
    }

    @Override // AutDecl : @ALPHABETKW Events SEMICOLTK;
    public ADecl parseAutDecl2(Token t1, List<AName> l2) {
        return new AAlphabetDecl(l2, t1.position);
    }

    @Override // AutDecl : @ALPHABETKW SEMICOLTK;
    public ADecl parseAutDecl3(Token t1) {
        return new AAlphabetDecl(null, t1.position);
    }

    @Override // AutDecl : @MONITORKW Events SEMICOLTK;
    public ADecl parseAutDecl4(Token t1, List<AName> l2) {
        return new AMonitorDecl(l2, t1.position);
    }

    @Override // AutDecl : @MONITORKW SEMICOLTK;
    public ADecl parseAutDecl5(Token t1) {
        return new AMonitorDecl(list(), t1.position);
    }

    @Override // AutDecl : OptAnnos @DISCKW Type DiscDecls SEMICOLTK;
    public ADecl parseAutDecl6(List<AAnnotation> l1, Token t2, ACifType a3, List<ADiscVariable> l4) {
        return new ADiscVariableDecl(l1, a3, l4, t2.position);
    }

    @Override // Decl : @TYPEKW TypeDefs SEMICOLTK;
    public ADecl parseDecl01(Token t1, List<ATypeDef> l2) {
        return new ATypeDefDecl(l2, t1.position);
    }

    @Override // Decl : ENUMKW Identifier EQTK Identifiers SEMICOLTK;
    public ADecl parseDecl02(AIdentifier a2, List<AIdentifier> l4) {
        return new AEnumDecl(a2.id, l4, a2.position);
    }

    @Override // Decl : OptControllability @EVENTKW Identifiers SEMICOLTK;
    public ADecl parseDecl03(Token t1, Token t2, List<AIdentifier> l3) {
        return new AEventDecl(t1, l3, null, t2.position);
    }

    @Override // Decl : OptControllability @EVENTKW EventType Identifiers SEMICOLTK;
    public ADecl parseDecl04(Token t1, Token t2, ACifType a3, List<AIdentifier> l4) {
        return new AEventDecl(t1, l4, a3, t2.position);
    }

    @Override // Decl : Controllability Identifiers SEMICOLTK;
    public ADecl parseDecl05(Token t1, List<AIdentifier> l2) {
        return new AEventDecl(t1, l2, null, t1.position);
    }

    @Override // Decl : Controllability EventType Identifiers SEMICOLTK;
    public ADecl parseDecl06(Token t1, ACifType a2, List<AIdentifier> l3) {
        return new AEventDecl(t1, l3, a2, t1.position);
    }

    @Override // Decl : @CONSTKW Type ConstantDefs SEMICOLTK;
    public ADecl parseDecl07(Token t1, ACifType a2, List<AConstant> l3) {
        return new AConstDecl(a2, l3, t1.position);
    }

    @Override // Decl : @ALGKW Type AlgVarsDefs SEMICOLTK;
    public ADecl parseDecl08(Token t1, ACifType a2, List<AAlgVariable> l3) {
        return new AAlgVariableDecl(a2, l3, t1.position);
    }

    @Override // Decl : OptAnnos @INPUTKW Type Identifiers SEMICOLTK;
    public ADecl parseDecl09(List<AAnnotation> l1, Token t2, ACifType a3, List<AIdentifier> l4) {
        return new AInputVariableDecl(l1, a3, l4, t2.position);
    }

    @Override // Decl : @CONTKW ContDecls SEMICOLTK;
    public ADecl parseDecl10(Token t1, List<AContVariable> l2) {
        return new AContVariableDecl(l2, t1.position);
    }

    @Override // Decl : @EQUATIONKW Equations SEMICOLTK;
    public ADecl parseDecl11(Token t1, List<AEquation> l2) {
        AEquationDecl rslt = new AEquationDecl(l2, t1.position);
        for (AEquation eqn: l2) {
            // Set parent of the equation.
            eqn.parent = rslt;
        }
        return rslt;
    }

    @Override // Decl : @INITIALKW Expressions SEMICOLTK;
    public ADecl parseDecl12(Token t1, List<AExpression> l2) {
        return new AInitialDecl(l2, t1.position);
    }

    @Override // Decl : InvariantDecls;
    public ADecl parseDecl13(AInvariantDecl a1) {
        return a1;
    }

    @Override // Decl : @MARKEDKW Expressions SEMICOLTK;
    public ADecl parseDecl14(Token t1, List<AExpression> l2) {
        return new AMarkedDecl(l2, t1.position);
    }

    @Override // Decl : IoDecl;
    public ADecl parseDecl15(AIoDecl a1) {
        return a1;
    }

    @Override // Identifier : @IDENTIFIERTK;
    public AIdentifier parseIdentifier1(Token t1) {
        return new AIdentifier(t1.text, t1.position);
    }

    @Override // Imports : StringToken;
    public List<AImport> parseImports1(AStringToken a1) {
        return list(new AImport(a1, a1.position));
    }

    @Override // Imports : Imports COMMATK StringToken;
    public List<AImport> parseImports2(List<AImport> l1, AStringToken a3) {
        l1.add(new AImport(a3, a3.position));
        return l1;
    }

    @Override // StringToken : @STRINGTK;
    public AStringToken parseStringToken1(Token t1) {
        return new AStringToken(t1.text, t1.position);
    }

    @Override // TypeDefs : Identifier EQTK Type;
    public List<ATypeDef> parseTypeDefs1(AIdentifier a1, ACifType a3) {
        return list(new ATypeDef(a3, a1, a1.position));
    }

    @Override // TypeDefs : TypeDefs COMMATK Identifier EQTK Type;
    public List<ATypeDef> parseTypeDefs2(List<ATypeDef> l1, AIdentifier a3, ACifType a5) {
        l1.add(new ATypeDef(a5, a3, a3.position));
        return l1;
    }

    @Override // ConstantDefs : Identifier EQTK Expression;
    public List<AConstant> parseConstantDefs1(AIdentifier a1, AExpression a3) {
        return list(new AConstant(a1, a3, a1.position));
    }

    @Override // ConstantDefs : ConstantDefs COMMATK Identifier EQTK Expression;
    public List<AConstant> parseConstantDefs2(List<AConstant> l1, AIdentifier a3, AExpression a5) {
        l1.add(new AConstant(a3, a5, a3.position));
        return l1;
    }

    @Override // AlgVarsDefs : Identifier;
    public List<AAlgVariable> parseAlgVarsDefs1(AIdentifier a1) {
        return list(new AAlgVariable(a1, null, a1.position));
    }

    @Override // AlgVarsDefs : Identifier EQTK Expression;
    public List<AAlgVariable> parseAlgVarsDefs2(AIdentifier a1, AExpression a3) {
        return list(new AAlgVariable(a1, a3, a1.position));
    }

    @Override // AlgVarsDefs : AlgVarsDefs COMMATK Identifier;
    public List<AAlgVariable> parseAlgVarsDefs3(List<AAlgVariable> l1, AIdentifier a3) {
        l1.add(new AAlgVariable(a3, null, a3.position));
        return l1;
    }

    @Override // AlgVarsDefs : AlgVarsDefs COMMATK Identifier EQTK Expression;
    public List<AAlgVariable> parseAlgVarsDefs4(List<AAlgVariable> l1, AIdentifier a3, AExpression a5) {
        l1.add(new AAlgVariable(a3, a5, a3.position));
        return l1;
    }

    @Override // FuncParams : PAROPENTK PARCLOSETK;
    public List<AFuncParam> parseFuncParams1() {
        return list();
    }

    @Override // FuncParams : PAROPENTK FuncParamDecls PARCLOSETK;
    public List<AFuncParam> parseFuncParams2(List<AFuncParam> l2) {
        return l2;
    }

    @Override // FuncParamDecls : Type Identifiers;
    public List<AFuncParam> parseFuncParamDecls1(ACifType a1, List<AIdentifier> l2) {
        return list(new AFuncParam(a1, l2));
    }

    @Override // FuncParamDecls : FuncParamDecls SEMICOLTK Type Identifiers;
    public List<AFuncParam> parseFuncParamDecls2(List<AFuncParam> l1, ACifType a3, List<AIdentifier> l4) {
        l1.add(new AFuncParam(a3, l4));
        return l1;
    }

    @Override // FuncBody : FuncVarDecls FuncStatements @ENDKW;
    public AFuncBody parseFuncBody1(List<ADiscVariableDecl> l1, List<AFuncStatement> l2, Token t3) {
        return new AInternalFuncBody(l1, l2, t3.position);
    }

    @Override // FuncBody : StringToken SEMICOLTK;
    public AFuncBody parseFuncBody2(AStringToken a1) {
        return new AExternalFuncBody(a1.txt, a1.position);
    }

    @Override // FuncVarDecls : ;
    public List<ADiscVariableDecl> parseFuncVarDecls1() {
        return list();
    }

    @Override // FuncVarDecls : FuncVarDecls Type FuncVarDecl SEMICOLTK;
    public List<ADiscVariableDecl> parseFuncVarDecls2(List<ADiscVariableDecl> l1, ACifType a2, List<ADiscVariable> l3) {
        l1.add(new ADiscVariableDecl(listc(0), a2, l3, null));
        return l1;
    }

    @Override // FuncVarDecl : Identifier;
    public List<ADiscVariable> parseFuncVarDecl1(AIdentifier a1) {
        return list(new ADiscVariable(a1, null, a1.position));
    }

    @Override // FuncVarDecl : Identifier EQTK Expression;
    public List<ADiscVariable> parseFuncVarDecl2(AIdentifier a1, AExpression a3) {
        AVariableValue value = new AVariableValue(list(a3), a3.position);
        return list(new ADiscVariable(a1, value, a1.position));
    }

    @Override // FuncVarDecl : FuncVarDecl COMMATK Identifier;
    public List<ADiscVariable> parseFuncVarDecl3(List<ADiscVariable> l1, AIdentifier a3) {
        l1.add(new ADiscVariable(a3, null, a3.position));
        return l1;
    }

    @Override // FuncVarDecl : FuncVarDecl COMMATK Identifier EQTK Expression;
    public List<ADiscVariable> parseFuncVarDecl4(List<ADiscVariable> l1, AIdentifier a3, AExpression a5) {
        AVariableValue value = new AVariableValue(list(a5), a5.position);
        l1.add(new ADiscVariable(a3, value, a3.position));
        return l1;
    }

    @Override // FuncStatements : FuncStatement;
    public List<AFuncStatement> parseFuncStatements1(AFuncStatement a1) {
        return list(a1);
    }

    @Override // FuncStatements : FuncStatements FuncStatement;
    public List<AFuncStatement> parseFuncStatements2(List<AFuncStatement> l1, AFuncStatement a2) {
        l1.add(a2);
        return l1;
    }

    @Override // FuncStatement : Addressables @BECOMESTK Expressions SEMICOLTK;
    public AFuncStatement parseFuncStatement1(List<AExpression> l1, Token t2, List<AExpression> l3) {
        return new AAssignFuncStatement(l1, l3, t2.position);
    }

    @Override // FuncStatement : @IFKW Expressions COLONTK FuncStatements OptElifFuncStats OptElseFuncStat ENDKW;
    public AFuncStatement parseFuncStatement2(Token t1, List<AExpression> l2, List<AFuncStatement> l4,
            List<AElifFuncStatement> l5, AElseFuncStatement a6)
    {
        return new AIfFuncStatement(l2, l4, l5, a6, t1.position);
    }

    @Override // FuncStatement : @WHILEKW Expressions COLONTK FuncStatements ENDKW;
    public AFuncStatement parseFuncStatement3(Token t1, List<AExpression> l2, List<AFuncStatement> l4) {
        return new AWhileFuncStatement(l2, l4, t1.position);
    }

    @Override // FuncStatement : @BREAKKW SEMICOLTK;
    public AFuncStatement parseFuncStatement4(Token t1) {
        return new ABreakFuncStatement(t1.position);
    }

    @Override // FuncStatement : @CONTINUEKW SEMICOLTK;
    public AFuncStatement parseFuncStatement5(Token t1) {
        return new AContinueFuncStatement(t1.position);
    }

    @Override // FuncStatement : @RETURNKW Expressions SEMICOLTK;
    public AFuncStatement parseFuncStatement6(Token t1, List<AExpression> l2) {
        return new AReturnFuncStatement(l2, t1.position);
    }

    @Override // OptElifFuncStats : ;
    public List<AElifFuncStatement> parseOptElifFuncStats1() {
        return list();
    }

    @Override // OptElifFuncStats : OptElifFuncStats @ELIFKW Expressions COLONTK FuncStatements;
    public List<AElifFuncStatement> parseOptElifFuncStats2(List<AElifFuncStatement> l1, Token t2, List<AExpression> l3,
            List<AFuncStatement> l5)
    {
        l1.add(new AElifFuncStatement(l3, l5, t2.position));
        return l1;
    }

    @Override // OptElseFuncStat : ;
    public AElseFuncStatement parseOptElseFuncStat1() {
        return null;
    }

    @Override // OptElseFuncStat : @ELSEKW FuncStatements;
    public AElseFuncStatement parseOptElseFuncStat2(Token t1, List<AFuncStatement> l2) {
        return new AElseFuncStatement(l2, t1.position);
    }

    @Override // Events : Name;
    public List<AName> parseEvents1(AName a1) {
        return list(a1);
    }

    @Override // Events : Events COMMATK Name;
    public List<AName> parseEvents2(List<AName> l1, AName a3) {
        l1.add(a3);
        return l1;
    }

    @Override // CoreEdge : EdgeEvents OptEdgeGuard OptEdgeUrgent OptEdgeUpdate;
    public ACoreEdge parseCoreEdge1(List<AEdgeEvent> l1, List<AExpression> l2, TextPosition t3, List<AUpdate> l4) {
        return new ACoreEdge(l1, l2, t3, l4);
    }

    @Override // CoreEdge : WHENKW Expressions OptEdgeUrgent OptEdgeUpdate;
    public ACoreEdge parseCoreEdge2(List<AExpression> l2, TextPosition t3, List<AUpdate> l4) {
        return new ACoreEdge(list(), l2, t3, l4);
    }

    @Override // CoreEdge : @NOWKW OptEdgeUpdate;
    public ACoreEdge parseCoreEdge3(Token t1, List<AUpdate> l2) {
        return new ACoreEdge(list(), list(), t1.position, l2);
    }

    @Override // CoreEdge : DOKW Updates;
    public ACoreEdge parseCoreEdge4(List<AUpdate> l2) {
        return new ACoreEdge(list(), list(), null, l2);
    }

    @Override // OptEdgeGuard : ;
    public List<AExpression> parseOptEdgeGuard1() {
        return list();
    }

    @Override // OptEdgeGuard : WHENKW Expressions;
    public List<AExpression> parseOptEdgeGuard2(List<AExpression> l2) {
        return l2;
    }

    @Override // OptEdgeUrgent : ;
    public TextPosition parseOptEdgeUrgent1() {
        return null;
    }

    @Override // OptEdgeUrgent : @NOWKW;
    public TextPosition parseOptEdgeUrgent2(Token t1) {
        return t1.position;
    }

    @Override // OptEdgeUpdate : ;
    public List<AUpdate> parseOptEdgeUpdate1() {
        return list();
    }

    @Override // OptEdgeUpdate : DOKW Updates;
    public List<AUpdate> parseOptEdgeUpdate2(List<AUpdate> l2) {
        return l2;
    }

    @Override // EdgeEvents : EdgeEvent;
    public List<AEdgeEvent> parseEdgeEvents1(AEdgeEvent a1) {
        return list(a1);
    }

    @Override // EdgeEvents : EdgeEvents COMMATK EdgeEvent;
    public List<AEdgeEvent> parseEdgeEvents2(List<AEdgeEvent> l1, AEdgeEvent a3) {
        l1.add(a3);
        return l1;
    }

    @Override // EdgeEvent : @TAUKW;
    public AEdgeEvent parseEdgeEvent1(Token t1) {
        AExpression eventRef = new ATauExpression(t1.position);
        return new AEdgeEvent(Direction.NONE, eventRef, null, t1.position);
    }

    @Override // EdgeEvent : Name;
    public AEdgeEvent parseEdgeEvent2(AName a1) {
        AExpression eventRef = new ANameExpression(a1, false, a1.position);
        return new AEdgeEvent(Direction.NONE, eventRef, null, a1.position);
    }

    @Override // EdgeEvent : Name @EXCLAMATIONTK;
    public AEdgeEvent parseEdgeEvent3(AName a1, Token t2) {
        AExpression eventRef = new ANameExpression(a1, false, a1.position);
        return new AEdgeEvent(Direction.SEND, eventRef, null, t2.position);
    }

    @Override // EdgeEvent : Name @EXCLAMATIONTK Expression;
    public AEdgeEvent parseEdgeEvent4(AName a1, Token t2, AExpression a3) {
        AExpression eventRef = new ANameExpression(a1, false, a1.position);
        return new AEdgeEvent(Direction.SEND, eventRef, a3, t2.position);
    }

    @Override // EdgeEvent : Name @QUESTIONTK;
    public AEdgeEvent parseEdgeEvent5(AName a1, Token t2) {
        AExpression eventRef = new ANameExpression(a1, false, a1.position);
        return new AEdgeEvent(Direction.RECEIVE, eventRef, null, t2.position);
    }

    @Override // Locations : Location;
    public List<ALocation> parseLocations1(ALocation a1) {
        return list(a1);
    }

    @Override // Locations : Locations Location;
    public List<ALocation> parseLocations2(List<ALocation> l1, ALocation a2) {
        l1.add(a2);
        return l1;
    }

    @Override // Location : OptAnnos @LOCATIONKW SEMICOLTK;
    public ALocation parseLocation1(List<AAnnotation> l1, Token t2) {
        return new ALocation(l1, null, null, t2.position);
    }

    @Override // Location : OptAnnos @LOCATIONKW Identifier SEMICOLTK;
    public ALocation parseLocation2(List<AAnnotation> l1, Token t2, AIdentifier a3) {
        return new ALocation(l1, a3, null, t2.position);
    }

    @Override // Location : OptAnnos @LOCATIONKW COLONTK LocationElements;
    public ALocation parseLocation3(List<AAnnotation> l1, Token t2, List<ALocationElement> l4) {
        ALocation loc = new ALocation(l1, null, l4, t2.position);

        for (ALocationElement lelem: l4) {
            if (!(lelem instanceof AEquationLocationElement)) {
                continue;
            }
            for (AEquation eqn: ((AEquationLocationElement)lelem).equations) {
                // Set parent of the equation.
                eqn.parent = loc;
            }
        }

        return loc;
    }

    @Override // Location : OptAnnos @LOCATIONKW Identifier COLONTK LocationElements;
    public ALocation parseLocation4(List<AAnnotation> l1, Token t2, AIdentifier a3, List<ALocationElement> l5) {
        ALocation loc = new ALocation(l1, a3, l5, t2.position);

        for (ALocationElement lelem: l5) {
            if (!(lelem instanceof AEquationLocationElement)) {
                continue;
            }
            for (AEquation eqn: ((AEquationLocationElement)lelem).equations) {
                // Set parent of the equation.
                eqn.parent = loc;
            }
        }

        return loc;
    }

    @Override // LocationElements : LocationElement;
    public List<ALocationElement> parseLocationElements1(ALocationElement a1) {
        return list(a1);
    }

    @Override // LocationElements : LocationElements LocationElement;
    public List<ALocationElement> parseLocationElements2(List<ALocationElement> l1, ALocationElement a2) {
        l1.add(a2);
        return l1;
    }

    @Override // LocationElement : @INITIALKW SEMICOLTK;
    public ALocationElement parseLocationElement1(Token t1) {
        return new AInitialLocationElement(null, t1.position);
    }

    @Override // LocationElement : @INITIALKW Expressions SEMICOLTK;
    public ALocationElement parseLocationElement2(Token t1, List<AExpression> l2) {
        return new AInitialLocationElement(l2, t1.position);
    }

    @Override // LocationElement : InvariantDecls;
    public ALocationElement parseLocationElement3(AInvariantDecl a1) {
        return new AInvariantLocationElement(a1);
    }

    @Override // LocationElement : @EQUATIONKW Equations SEMICOLTK;
    public ALocationElement parseLocationElement4(Token t1, List<AEquation> l2) {
        // Parent set in callback for 'Location' non-terminal.
        return new AEquationLocationElement(l2, t1.position);
    }

    @Override // LocationElement : @MARKEDKW SEMICOLTK;
    public ALocationElement parseLocationElement5(Token t1) {
        return new AMarkedLocationElement(null, t1.position);
    }

    @Override // LocationElement : @MARKEDKW Expressions SEMICOLTK;
    public ALocationElement parseLocationElement6(Token t1, List<AExpression> l2) {
        return new AMarkedLocationElement(l2, t1.position);
    }

    @Override // LocationElement : @URGENTKW SEMICOLTK;
    public ALocationElement parseLocationElement7(Token t1) {
        return new AUrgentLocationElement(t1.position);
    }

    @Override // LocationElement : @EDGEKW CoreEdge SEMICOLTK;
    public ALocationElement parseLocationElement8(Token t1, ACoreEdge a2) {
        return new AEdgeLocationElement(a2, null, t1.position);
    }

    @Override // LocationElement : @EDGEKW CoreEdge GOTOKW Identifier SEMICOLTK;
    public ALocationElement parseLocationElement9(Token t1, ACoreEdge a2, AIdentifier a4) {
        return new AEdgeLocationElement(a2, a4, t1.position);
    }

    @Override // CompInstArgs : PAROPENTK PARCLOSETK;
    public List<AExpression> parseCompInstArgs1() {
        return list();
    }

    @Override // CompInstArgs : PAROPENTK Expressions PARCLOSETK;
    public List<AExpression> parseCompInstArgs2(List<AExpression> l2) {
        return l2;
    }

    @Override // CompDefParms : PAROPENTK PARCLOSETK;
    public List<AParameter> parseCompDefParms1() {
        return list();
    }

    @Override // CompDefParms : PAROPENTK CompDefDecls PARCLOSETK;
    public List<AParameter> parseCompDefParms2(List<AParameter> l2) {
        return l2;
    }

    @Override // CompDefDecls : CompDefDeclaration;
    public List<AParameter> parseCompDefDecls1(AParameter a1) {
        return list(a1);
    }

    @Override // CompDefDecls : CompDefDecls SEMICOLTK CompDefDeclaration;
    public List<AParameter> parseCompDefDecls2(List<AParameter> l1, AParameter a3) {
        l1.add(a3);
        return l1;
    }

    @Override // CompDefDeclaration : OptControllability @EVENTKW EventParamIds;
    public AParameter parseCompDefDeclaration1(Token t1, Token t2, List<AEventParameterPart> l3) {
        return new AEventParameter(t1, l3, null, t2.position);
    }

    @Override // CompDefDeclaration : OptControllability @EVENTKW EventType EventParamIds;
    public AParameter parseCompDefDeclaration2(Token t1, Token t2, ACifType a3, List<AEventParameterPart> l4) {
        return new AEventParameter(t1, l4, a3, t2.position);
    }

    @Override // CompDefDeclaration : Controllability EventParamIds;
    public AParameter parseCompDefDeclaration3(Token t1, List<AEventParameterPart> l2) {
        return new AEventParameter(t1, l2, null, t1.position);
    }

    @Override // CompDefDeclaration : Controllability EventType EventParamIds;
    public AParameter parseCompDefDeclaration4(Token t1, ACifType a2, List<AEventParameterPart> l3) {
        return new AEventParameter(t1, l3, a2, t1.position);
    }

    @Override // CompDefDeclaration : Name Identifiers;
    public AParameter parseCompDefDeclaration5(AName a1, List<AIdentifier> l2) {
        return new AComponentParameter(a1, l2, a1.position);
    }

    @Override // CompDefDeclaration : @LOCATIONKW Identifiers;
    public AParameter parseCompDefDeclaration6(Token t1, List<AIdentifier> l2) {
        return new ALocationParameter(l2, t1.position);
    }

    @Override // CompDefDeclaration : @ALGKW Type Identifiers;
    public AParameter parseCompDefDeclaration7(Token t1, ACifType a2, List<AIdentifier> l3) {
        return new AAlgParameter(a2, l3, t1.position);
    }

    @Override // EventParamIds : EventParamId;
    public List<AEventParameterPart> parseEventParamIds1(AEventParameterPart a1) {
        return list(a1);
    }

    @Override // EventParamIds : EventParamIds COMMATK EventParamId;
    public List<AEventParameterPart> parseEventParamIds2(List<AEventParameterPart> l1, AEventParameterPart a3) {
        l1.add(a3);
        return l1;
    }

    @Override // EventParamId : Identifier OptEventParamFlags;
    public AEventParameterPart parseEventParamId1(AIdentifier a1, List<AEventParamFlag> l2) {
        return new AEventParameterPart(a1, l2);
    }

    @Override // OptEventParamFlags : ;
    public List<AEventParamFlag> parseOptEventParamFlags1() {
        return list();
    }

    @Override // OptEventParamFlags : OptEventParamFlags EventParamFlag;
    public List<AEventParamFlag> parseOptEventParamFlags2(List<AEventParamFlag> l1, AEventParamFlag a2) {
        l1.add(a2);
        return l1;
    }

    @Override // EventParamFlag : @EXCLAMATIONTK;
    public AEventParamFlag parseEventParamFlag1(Token t1) {
        return new AEventParamFlag(t1.text, t1.position);
    }

    @Override // EventParamFlag : @QUESTIONTK;
    public AEventParamFlag parseEventParamFlag2(Token t1) {
        return new AEventParamFlag(t1.text, t1.position);
    }

    @Override // EventParamFlag : @TILDETK;
    public AEventParamFlag parseEventParamFlag3(Token t1) {
        return new AEventParamFlag(t1.text, t1.position);
    }

    @Override // DiscDecls : DiscDecl;
    public List<ADiscVariable> parseDiscDecls1(ADiscVariable a1) {
        return list(a1);
    }

    @Override // DiscDecls : DiscDecls COMMATK DiscDecl;
    public List<ADiscVariable> parseDiscDecls2(List<ADiscVariable> l1, ADiscVariable a3) {
        l1.add(a3);
        return l1;
    }

    @Override // DiscDecl : Identifier;
    public ADiscVariable parseDiscDecl1(AIdentifier a1) {
        return new ADiscVariable(a1, null, a1.position);
    }

    @Override // DiscDecl : Identifier INKW @ANYKW;
    public ADiscVariable parseDiscDecl2(AIdentifier a1, Token t3) {
        return new ADiscVariable(a1, new AVariableValue(null, t3.position), a1.position);
    }

    @Override // DiscDecl : Identifier EQTK Expression;
    public ADiscVariable parseDiscDecl3(AIdentifier a1, AExpression a3) {
        return new ADiscVariable(a1, new AVariableValue(list(a3), a1.position), a1.position);
    }

    @Override // DiscDecl : Identifier INKW CUROPENTK Expressions CURCLOSETK;
    public ADiscVariable parseDiscDecl4(AIdentifier a1, List<AExpression> l4) {
        return new ADiscVariable(a1, new AVariableValue(l4, a1.position), a1.position);
    }

    @Override // ContDecls : ContDecl;
    public List<AContVariable> parseContDecls1(AContVariable a1) {
        return list(a1);
    }

    @Override // ContDecls : ContDecls COMMATK ContDecl;
    public List<AContVariable> parseContDecls2(List<AContVariable> l1, AContVariable a3) {
        l1.add(a3);
        return l1;
    }

    @Override // ContDecl : Identifier OptDerivative;
    public AContVariable parseContDecl1(AIdentifier a1, AExpression a2) {
        return new AContVariable(a1, null, a2, a1.position);
    }

    @Override // ContDecl : Identifier EQTK Expression OptDerivative;
    public AContVariable parseContDecl2(AIdentifier a1, AExpression a3, AExpression a4) {
        return new AContVariable(a1, a3, a4, a1.position);
    }

    @Override // OptDerivative : ;
    public AExpression parseOptDerivative1() {
        return null;
    }

    @Override // OptDerivative : DERKW Expression;
    public AExpression parseOptDerivative2(AExpression a2) {
        return a2;
    }

    @Override // Equations : Equation;
    public List<AEquation> parseEquations1(AEquation a1) {
        return list(a1);
    }

    @Override // Equations : Equations COMMATK Equation;
    public List<AEquation> parseEquations2(List<AEquation> l1, AEquation a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Equation : Identifier APOSTROPHETK @EQTK Expression;
    public AEquation parseEquation1(AIdentifier a1, Token t3, AExpression a4) {
        return new AEquation(a1, true, a4, t3.position);
    }

    @Override // Equation : Identifier @EQTK Expression;
    public AEquation parseEquation2(AIdentifier a1, Token t2, AExpression a3) {
        return new AEquation(a1, false, a3, t2.position);
    }

    @Override // InvariantDecls : OptSupKind @INVARIANTKW Invariants SEMICOLTK;
    public AInvariantDecl parseInvariantDecls1(Token t1, Token t2, List<AInvariant> l3) {
        return new AInvariantDecl(t1, l3, t2.position);
    }

    @Override // InvariantDecls : SupKind Invariants SEMICOLTK;
    public AInvariantDecl parseInvariantDecls2(Token t1, List<AInvariant> l2) {
        return new AInvariantDecl(t1, l2, t1.position);
    }

    @Override // Invariants : Invariant;
    public List<AInvariant> parseInvariants1(AInvariant a1) {
        return list(a1);
    }

    @Override // Invariants : Invariants COMMATK Invariant;
    public List<AInvariant> parseInvariants2(List<AInvariant> l1, AInvariant a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Invariant : Expression;
    public AInvariant parseInvariant1(AExpression a1) {
        return new AInvariant(null, a1, null, null);
    }

    @Override // Invariant : Identifier COLONTK Expression;
    public AInvariant parseInvariant2(AIdentifier a1, AExpression a3) {
        return new AInvariant(a1, a3, null, null);
    }

    @Override // Invariant : Name @NEEDSKW Expression;
    public AInvariant parseInvariant3(AName a1, Token t2, AExpression a3) {
        return new AInvariant(null, a3, t2, list(a1));
    }

    @Override // Invariant : Identifier COLONTK Name @NEEDSKW Expression;
    public AInvariant parseInvariant4(AIdentifier a1, AName a3, Token t4, AExpression a5) {
        return new AInvariant(a1, a5, t4, list(a3));
    }

    @Override // Invariant : NonEmptySetExpression @NEEDSKW Expression;
    public AInvariant parseInvariant5(ASetExpression a1, Token t2, AExpression a3) {
        Assert.check(!a1.elements.isEmpty());
        List<AName> events = listc(a1.elements.size());
        for (AExpression elem: a1.elements) {
            if (elem instanceof ANameExpression) {
                events.add(((ANameExpression)elem).name);
            } else {
                String msg = "Event name or reference expected.";
                throw new CustomSyntaxException(msg, elem.position);
            }
        }
        return new AInvariant(null, a3, t2, events);
    }

    @Override // Invariant : Expression @DISABLESKW Name;
    public AInvariant parseInvariant6(AExpression a1, Token t2, AName a3) {
        return new AInvariant(null, a1, t2, list(a3));
    }

    @Override // Invariant : Identifier COLONTK Expression @DISABLESKW Name;
    public AInvariant parseInvariant7(AIdentifier a1, AExpression a3, Token t4, AName a5) {
        return new AInvariant(a1, a3, t4, list(a5));
    }

    @Override // Invariant : Expression @DISABLESKW NamesSet;
    public AInvariant parseInvariant8(AExpression a1, Token t2, List<AName> l3) {
        return new AInvariant(null, a1, t2, l3);
    }

    @Override // NamesSet : CUROPENTK Names CURCLOSETK;
    public List<AName> parseNamesSet1(List<AName> l2) {
        return l2;
    }

    @Override // Names : Name;
    public List<AName> parseNames1(AName a1) {
        return list(a1);
    }

    @Override // Names : Names COMMATK Name;
    public List<AName> parseNames2(List<AName> l1, AName a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Updates : Update;
    public List<AUpdate> parseUpdates1(AUpdate a1) {
        return list(a1);
    }

    @Override // Updates : Updates COMMATK Update;
    public List<AUpdate> parseUpdates2(List<AUpdate> l1, AUpdate a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Update : Addressable @BECOMESTK Expression;
    public AUpdate parseUpdate1(AExpression a1, Token t2, AExpression a3) {
        return new AAssignmentUpdate(a1, a3, t2.position);
    }

    @Override // Update : @IFKW Expressions COLONTK Updates OptElifUpdates OptElseUpdate ENDKW;
    public AUpdate parseUpdate2(Token t1, List<AExpression> l2, List<AUpdate> l4, List<AElifUpdate> l5,
            List<AUpdate> l6)
    {
        return new AIfUpdate(l2, l4, l5, l6, t1.position);
    }

    @Override // Addressables : Addressable;
    public List<AExpression> parseAddressables1(AExpression a1) {
        return list(a1);
    }

    @Override // Addressables : Addressables COMMATK Addressable;
    public List<AExpression> parseAddressables2(List<AExpression> l1, AExpression a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Addressable : Name;
    public AExpression parseAddressable1(AName a1) {
        return new ANameExpression(new AName(a1.name, a1.position), false, a1.position);
    }

    @Override // Addressable : Name Projections;
    public AExpression parseAddressable2(AName a1, List<AProjectionExpression> l2) {
        AExpression refExpr = new ANameExpression(new AName(a1.name, a1.position), false, a1.position);
        AProjectionExpression rslt = null;
        for (AProjectionExpression proj: l2) {
            if (rslt == null) {
                rslt = new AProjectionExpression(refExpr, proj.index, proj.position);
            } else {
                rslt = new AProjectionExpression(rslt, proj.index, proj.position);
            }
        }
        return rslt;
    }

    @Override // Addressable : @PAROPENTK Addressable COMMATK Addressables PARCLOSETK;
    public AExpression parseAddressable3(Token t1, AExpression a2, List<AExpression> l4) {
        l4.add(0, a2);
        return new ATupleExpression(l4, t1.position);
    }

    @Override // Projections : Projection;
    public List<AProjectionExpression> parseProjections1(AProjectionExpression a1) {
        return list(a1);
    }

    @Override // Projections : Projections Projection;
    public List<AProjectionExpression> parseProjections2(List<AProjectionExpression> l1, AProjectionExpression a2) {
        l1.add(a2);
        return l1;
    }

    @Override // Projection : @SQOPENTK Expression SQCLOSETK;
    public AProjectionExpression parseProjection1(Token t1, AExpression a2) {
        return new AProjectionExpression(null, a2, t1.position);
    }

    @Override // OptElifUpdates : ;
    public List<AElifUpdate> parseOptElifUpdates1() {
        return list();
    }

    @Override // OptElifUpdates : OptElifUpdates @ELIFKW Expressions COLONTK Updates;
    public List<AElifUpdate> parseOptElifUpdates2(List<AElifUpdate> l1, Token t2, List<AExpression> l3,
            List<AUpdate> l5)
    {
        l1.add(new AElifUpdate(l3, l5, t2.position));
        return l1;
    }

    @Override // OptElseUpdate : ;
    public List<AUpdate> parseOptElseUpdate1() {
        return list();
    }

    @Override // OptElseUpdate : ELSEKW Updates;
    public List<AUpdate> parseOptElseUpdate2(List<AUpdate> l2) {
        return l2;
    }

    @Override // Identifiers : Identifier;
    public List<AIdentifier> parseIdentifiers1(AIdentifier a1) {
        return list(a1);
    }

    @Override // Identifiers : Identifiers COMMATK Identifier;
    public List<AIdentifier> parseIdentifiers2(List<AIdentifier> l1, AIdentifier a3) {
        l1.add(a3);
        return l1;
    }

    @Override // OptSupKind : ;
    public Token parseOptSupKind1() {
        return null;
    }

    @Override // OptSupKind : SupKind;
    public Token parseOptSupKind2(Token t1) {
        return t1;
    }

    @Override // OptControllability : ;
    public Token parseOptControllability1() {
        return null;
    }

    @Override // OptControllability : Controllability;
    public Token parseOptControllability2(Token t1) {
        return t1;
    }

    @Override // Controllability : @CONTROLLABLEKW;
    public Token parseControllability1(Token t1) {
        return t1;
    }

    @Override // Controllability : @UNCONTROLLABLEKW;
    public Token parseControllability2(Token t1) {
        return t1;
    }

    @Override // OptIoDecls : ;
    public List<AIoDecl> parseOptIoDecls1() {
        return list();
    }

    @Override // OptIoDecls : OptIoDecls IoDecl;
    public List<AIoDecl> parseOptIoDecls2(List<AIoDecl> l1, AIoDecl a2) {
        l1.add(a2);
        return l1;
    }

    @Override // IoDecl : SvgFile;
    public AIoDecl parseIoDecl1(ASvgFile a1) {
        return a1;
    }

    @Override // IoDecl : SvgCopy;
    public AIoDecl parseIoDecl2(ASvgCopy a1) {
        return a1;
    }

    @Override // IoDecl : SvgMove;
    public AIoDecl parseIoDecl3(ASvgMove a1) {
        return a1;
    }

    @Override // IoDecl : SvgOut;
    public AIoDecl parseIoDecl4(ASvgOut a1) {
        return a1;
    }

    @Override // IoDecl : SvgIn;
    public AIoDecl parseIoDecl5(ASvgIn a1) {
        return a1;
    }

    @Override // IoDecl : PrintFile;
    public AIoDecl parseIoDecl6(APrintFile a1) {
        return a1;
    }

    @Override // IoDecl : Print;
    public AIoDecl parseIoDecl7(APrint a1) {
        return a1;
    }

    @Override // SvgFile : @SVGFILEKW StringToken SEMICOLTK;
    public ASvgFile parseSvgFile1(Token t1, AStringToken a2) {
        return new ASvgFile(a2, t1.position);
    }

    @Override // OptSvgFile : ;
    public ASvgFile parseOptSvgFile1() {
        return null;
    }

    @Override // OptSvgFile : @FILEKW StringToken;
    public ASvgFile parseOptSvgFile2(Token t1, AStringToken a2) {
        return new ASvgFile(a2, t1.position);
    }

    @Override // SvgCopy : @SVGCOPYKW IDKW Expression OptSvgCopyPre OptSvgCopyPost OptSvgFile @SEMICOLTK;
    public ASvgCopy parseSvgCopy1(Token t1, AExpression a3, AExpression a4, AExpression a5, ASvgFile a6, Token t7) {
        parser.addFoldRange(t1.position, t7.position);
        return new ASvgCopy(a3, a4, a5, a6, t1.position);
    }

    @Override // OptSvgCopyPre : ;
    public AExpression parseOptSvgCopyPre1() {
        return null;
    }

    @Override // OptSvgCopyPre : PREKW Expression;
    public AExpression parseOptSvgCopyPre2(AExpression a2) {
        return a2;
    }

    @Override // OptSvgCopyPost : ;
    public AExpression parseOptSvgCopyPost1() {
        return null;
    }

    @Override // OptSvgCopyPost : POSTKW Expression;
    public AExpression parseOptSvgCopyPost2(AExpression a2) {
        return a2;
    }

    @Override // SvgMove : @SVGMOVEKW IDKW Expression TOKW Expression COMMATK Expression OptSvgFile @SEMICOLTK;
    public ASvgMove parseSvgMove1(Token t1, AExpression a3, AExpression a5, AExpression a7, ASvgFile a8, Token t9) {
        parser.addFoldRange(t1.position, t9.position);
        return new ASvgMove(a3, a5, a7, a8, t1.position);
    }

    @Override // SvgOut : @SVGOUTKW IDKW Expression SvgAttr VALUEKW Expression OptSvgFile @SEMICOLTK;
    public ASvgOut parseSvgOut1(Token t1, AExpression a3, AStringToken a4, AExpression a6, ASvgFile a7, Token t8) {
        parser.addFoldRange(t1.position, t8.position);
        AStringToken svgAttr = a4.txt.isEmpty() ? null : a4;
        TextPosition svgTextPos = a4.txt.isEmpty() ? a4.position : null;
        return new ASvgOut(a3, svgAttr, svgTextPos, a6, a7, t1.position);
    }

    @Override // SvgAttr : ATTRKW StringToken;
    public AStringToken parseSvgAttr1(AStringToken a2) {
        return a2;
    }

    @Override // SvgAttr : @TEXTKW;
    public AStringToken parseSvgAttr2(Token t1) {
        // Dummy empty string, to be processed by the enclosing parser rule.
        return new AStringToken("\"\"", t1.position);
    }

    @Override // SvgIn : @SVGINKW IDKW Expression EVENTKW SvgInEvent OptSvgFile @SEMICOLTK;
    public ASvgIn parseSvgIn1(Token t1, AExpression a3, ASvgInEvent a5, ASvgFile a6, Token t7) {
        parser.addFoldRange(t1.position, t7.position);
        return new ASvgIn(a3, a5, list(), a6, t1.position);
    }

    @Override // SvgIn : @SVGINKW IDKW Expression DOKW Updates OptSvgFile @SEMICOLTK;
    public ASvgIn parseSvgIn2(Token t1, AExpression a3, List<AUpdate> l5, ASvgFile a6, Token t7) {
        parser.addFoldRange(t1.position, t7.position);
        return new ASvgIn(a3, null, l5, a6, t1.position);
    }

    @Override // SvgInEvent : Name;
    public ASvgInEvent parseSvgInEvent1(AName a1) {
        return new ASvgInEventSingle(a1, a1.position);
    }

    @Override // SvgInEvent : @IFKW Expression @COLONTK Name OptSvgInEventElifs @ELSEKW Name ENDKW;
    public ASvgInEvent parseSvgInEvent2(Token t1, AExpression a2, Token t3, AName a4, List<ASvgInEventIfEntry> l5,
            Token t6, AName a7)
    {
        l5.add(0, new ASvgInEventIfEntry(a2, a4, t3.position));
        l5.add(new ASvgInEventIfEntry(null, a7, t6.position));
        return new ASvgInEventIf(l5, t1.position);
    }

    @Override // SvgInEvent : @IFKW Expression @COLONTK Name SvgInEventElifs ENDKW;
    public ASvgInEvent parseSvgInEvent3(Token t1, AExpression a2, Token t3, AName a4, List<ASvgInEventIfEntry> l5) {
        l5.add(0, new ASvgInEventIfEntry(a2, a4, t3.position));
        return new ASvgInEventIf(l5, t1.position);
    }

    @Override // OptSvgInEventElifs : ;
    public List<ASvgInEventIfEntry> parseOptSvgInEventElifs1() {
        return list();
    }

    @Override // OptSvgInEventElifs : SvgInEventElifs;
    public List<ASvgInEventIfEntry> parseOptSvgInEventElifs2(List<ASvgInEventIfEntry> l1) {
        return l1;
    }

    @Override // SvgInEventElifs : @ELIFKW Expression COLONTK Name;
    public List<ASvgInEventIfEntry> parseSvgInEventElifs1(Token t1, AExpression a2, AName a4) {
        return list(new ASvgInEventIfEntry(a2, a4, t1.position));
    }

    @Override // SvgInEventElifs : SvgInEventElifs @ELIFKW Expression COLONTK Name;
    public List<ASvgInEventIfEntry> parseSvgInEventElifs2(List<ASvgInEventIfEntry> l1, Token t2, AExpression a3,
            AName a5)
    {
        l1.add(new ASvgInEventIfEntry(a3, a5, t2.position));
        return l1;
    }

    @Override // PrintFile : @PRINTFILEKW StringToken SEMICOLTK;
    public APrintFile parsePrintFile1(Token t1, AStringToken a2) {
        return new APrintFile(a2, t1.position);
    }

    @Override // Print : @PRINTKW PrintTxt OptPrintFors OptPrintWhen OptPrintFile @SEMICOLTK;
    public APrint parsePrint1(Token t1, APrintTxt a2, List<APrintFor> l3, APrintWhen a4, APrintFile a5, Token t6) {
        parser.addFoldRange(t1.position, t6.position);
        return new APrint(a2, l3, a4, a5, t1.position);
    }

    @Override // PrintTxt : Expression;
    public APrintTxt parsePrintTxt1(AExpression a1) {
        return new APrintTxt(null, a1);
    }

    @Override // PrintTxt : PREKW Expression;
    public APrintTxt parsePrintTxt2(AExpression a2) {
        return new APrintTxt(a2, null);
    }

    @Override // PrintTxt : POSTKW Expression;
    public APrintTxt parsePrintTxt3(AExpression a2) {
        return new APrintTxt(null, a2);
    }

    @Override // PrintTxt : PREKW Expression POSTKW Expression;
    public APrintTxt parsePrintTxt4(AExpression a2, AExpression a4) {
        return new APrintTxt(a2, a4);
    }

    @Override // OptPrintFors : ;
    public List<APrintFor> parseOptPrintFors1() {
        return list();
    }

    @Override // OptPrintFors : FORKW PrintFors;
    public List<APrintFor> parseOptPrintFors2(List<APrintFor> l2) {
        return l2;
    }

    @Override // PrintFors : PrintFor;
    public List<APrintFor> parsePrintFors1(APrintFor a1) {
        return list(a1);
    }

    @Override // PrintFors : PrintFors COMMATK PrintFor;
    public List<APrintFor> parsePrintFors2(List<APrintFor> l1, APrintFor a3) {
        l1.add(a3);
        return l1;
    }

    @Override // PrintFor : @EVENTKW;
    public APrintFor parsePrintFor1(Token t1) {
        return new APrintFor(APrintForKind.EVENT, null, t1.position);
    }

    @Override // PrintFor : @TIMEKW;
    public APrintFor parsePrintFor2(Token t1) {
        return new APrintFor(APrintForKind.TIME, null, t1.position);
    }

    @Override // PrintFor : Name;
    public APrintFor parsePrintFor3(AName a1) {
        return new APrintFor(APrintForKind.NAME, a1.name, a1.position);
    }

    @Override // PrintFor : @INITIALKW;
    public APrintFor parsePrintFor4(Token t1) {
        return new APrintFor(APrintForKind.INITIAL, null, t1.position);
    }

    @Override // PrintFor : @FINALKW;
    public APrintFor parsePrintFor5(Token t1) {
        return new APrintFor(APrintForKind.FINAL, null, t1.position);
    }

    @Override // OptPrintWhen : ;
    public APrintWhen parseOptPrintWhen1() {
        return null;
    }

    @Override // OptPrintWhen : WHENKW Expression;
    public APrintWhen parseOptPrintWhen2(AExpression a2) {
        return new APrintWhen(null, a2);
    }

    @Override // OptPrintWhen : WHENKW PREKW Expression;
    public APrintWhen parseOptPrintWhen3(AExpression a3) {
        return new APrintWhen(a3, null);
    }

    @Override // OptPrintWhen : WHENKW POSTKW Expression;
    public APrintWhen parseOptPrintWhen4(AExpression a3) {
        return new APrintWhen(null, a3);
    }

    @Override // OptPrintWhen : WHENKW PREKW Expression POSTKW Expression;
    public APrintWhen parseOptPrintWhen5(AExpression a3, AExpression a5) {
        return new APrintWhen(a3, a5);
    }

    @Override // OptPrintFile : ;
    public APrintFile parseOptPrintFile1() {
        return null;
    }

    @Override // OptPrintFile : FILEKW StringToken;
    public APrintFile parseOptPrintFile2(AStringToken a2) {
        return new APrintFile(a2, a2.position);
    }

    @Override // Types : Type;
    public List<ACifType> parseTypes1(ACifType a1) {
        return list(a1);
    }

    @Override // Types : Types COMMATK Type;
    public List<ACifType> parseTypes2(List<ACifType> l1, ACifType a3) {
        l1.add(a3);
        return l1;
    }

    @Override // EventType : @VOIDKW;
    public ACifType parseEventType1(Token t1) {
        return new AVoidType(t1.position);
    }

    @Override // EventType : Type;
    public ACifType parseEventType2(ACifType a1) {
        return a1;
    }

    @Override // Type : @BOOLKW;
    public ACifType parseType01(Token t1) {
        return new ABoolType(t1.position);
    }

    @Override // Type : @INTKW;
    public ACifType parseType02(Token t1) {
        return new AIntType(null, t1.position);
    }

    @Override // Type : @INTKW SQOPENTK Expression DOTDOTTK Expression SQCLOSETK;
    public ACifType parseType03(Token t1, AExpression a3, AExpression a5) {
        return new AIntType(new ARange(a3, a5, t1.position), t1.position);
    }

    @Override // Type : @REALKW;
    public ACifType parseType04(Token t1) {
        return new ARealType(t1.position);
    }

    @Override // Type : @STRINGKW;
    public ACifType parseType05(Token t1) {
        return new AStringType(t1.position);
    }

    @Override // Type : @LISTKW Type;
    public ACifType parseType06(Token t1, ACifType a2) {
        return new AListType(a2, null, t1.position);
    }

    @Override // Type : @LISTKW SQOPENTK Expression SQCLOSETK Type;
    public ACifType parseType07(Token t1, AExpression a3, ACifType a5) {
        return new AListType(a5, new ARange(a3, null, t1.position), t1.position);
    }

    @Override // Type : @LISTKW SQOPENTK Expression DOTDOTTK Expression SQCLOSETK Type;
    public ACifType parseType08(Token t1, AExpression a3, AExpression a5, ACifType a7) {
        return new AListType(a7, new ARange(a3, a5, t1.position), t1.position);
    }

    @Override // Type : @SETKW Type;
    public ACifType parseType09(Token t1, ACifType a2) {
        return new ASetType(a2, t1.position);
    }

    @Override // Type : @DICTKW PAROPENTK Type COLONTK Type PARCLOSETK;
    public ACifType parseType10(Token t1, ACifType a3, ACifType a5) {
        return new ADictType(a3, a5, t1.position);
    }

    @Override // Type : @TUPLEKW PAROPENTK Fields PARCLOSETK;
    public ACifType parseType11(Token t1, List<AField> l3) {
        return new ATupleType(l3, t1.position);
    }

    @Override // Type : @FUNCKW Type PAROPENTK PARCLOSETK;
    public ACifType parseType12(Token t1, ACifType a2) {
        return new AFuncType(a2, list(), t1.position);
    }

    @Override // Type : @FUNCKW Type PAROPENTK Types PARCLOSETK;
    public ACifType parseType13(Token t1, ACifType a2, List<ACifType> l4) {
        return new AFuncType(a2, l4, t1.position);
    }

    @Override // Type : @DISTKW Type;
    public ACifType parseType14(Token t1, ACifType a2) {
        return new ADistType(a2, t1.position);
    }

    @Override // Type : Name;
    public ACifType parseType15(AName a1) {
        return new ANamedType(a1, a1.position);
    }

    @Override // Fields : Field;
    public List<AField> parseFields1(AField a1) {
        return list(a1);
    }

    @Override // Fields : Fields SEMICOLTK Field;
    public List<AField> parseFields2(List<AField> l1, AField a3) {
        l1.add(a3);
        return l1;
    }

    @Override // Field : Type Identifiers;
    public AField parseField1(ACifType a1, List<AIdentifier> l2) {
        return new AField(l2, a1);
    }

    @Override // Expressions : Expression;
    public List<AExpression> parseExpressions1(AExpression a1) {
        return list(a1);
    }

    @Override // Expressions : Expressions COMMATK Expression;
    public List<AExpression> parseExpressions2(List<AExpression> l1, AExpression a3) {
        l1.add(a3);
        return l1;
    }

    @Override // OptExpression : ;
    public AExpression parseOptExpression1() {
        return null;
    }

    @Override // OptExpression : Expression;
    public AExpression parseOptExpression2(AExpression a1) {
        return a1;
    }

    @Override // Expression : OrExpression;
    public AExpression parseExpression1(AExpression a1) {
        return a1;
    }

    @Override // Expression : OrExpression @IMPLIESTK OrExpression;
    public AExpression parseExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // Expression : OrExpression @EQUIVALENCETK OrExpression;
    public AExpression parseExpression3(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // OrExpression : AndExpression;
    public AExpression parseOrExpression1(AExpression a1) {
        return a1;
    }

    @Override // OrExpression : OrExpression @ORKW AndExpression;
    public AExpression parseOrExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // AndExpression : CompareExpression;
    public AExpression parseAndExpression1(AExpression a1) {
        return a1;
    }

    @Override // AndExpression : AndExpression @ANDKW CompareExpression;
    public AExpression parseAndExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : AddExpression;
    public AExpression parseCompareExpression1(AExpression a1) {
        return a1;
    }

    @Override // CompareExpression : CompareExpression @LTTK AddExpression;
    public AExpression parseCompareExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @LETK AddExpression;
    public AExpression parseCompareExpression3(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @EQTK AddExpression;
    public AExpression parseCompareExpression4(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @NETK AddExpression;
    public AExpression parseCompareExpression5(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @GETK AddExpression;
    public AExpression parseCompareExpression6(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @GTTK AddExpression;
    public AExpression parseCompareExpression7(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @INKW AddExpression;
    public AExpression parseCompareExpression8(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // CompareExpression : CompareExpression @SUBKW AddExpression;
    public AExpression parseCompareExpression9(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // AddExpression : MulExpression;
    public AExpression parseAddExpression1(AExpression a1) {
        return a1;
    }

    @Override // AddExpression : AddExpression @PLUSTK MulExpression;
    public AExpression parseAddExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // AddExpression : AddExpression @MINUSTK MulExpression;
    public AExpression parseAddExpression3(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // MulExpression : UnaryExpression;
    public AExpression parseMulExpression1(AExpression a1) {
        return a1;
    }

    @Override // MulExpression : MulExpression @ASTERISKTK UnaryExpression;
    public AExpression parseMulExpression2(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // MulExpression : MulExpression @SLASHTK UnaryExpression;
    public AExpression parseMulExpression3(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // MulExpression : MulExpression @DIVKW UnaryExpression;
    public AExpression parseMulExpression4(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // MulExpression : MulExpression @MODKW UnaryExpression;
    public AExpression parseMulExpression5(AExpression a1, Token t2, AExpression a3) {
        return new ABinaryExpression(t2.text, a1, a3, t2.position);
    }

    @Override // UnaryExpression : FuncExpression;
    public AExpression parseUnaryExpression1(AExpression a1) {
        return a1;
    }

    @Override // UnaryExpression : @MINUSTK UnaryExpression;
    public AExpression parseUnaryExpression2(Token t1, AExpression a2) {
        return new AUnaryExpression(t1.text, a2, t1.position);
    }

    @Override // UnaryExpression : @PLUSTK UnaryExpression;
    public AExpression parseUnaryExpression3(Token t1, AExpression a2) {
        return new AUnaryExpression(t1.text, a2, t1.position);
    }

    @Override // UnaryExpression : @NOTKW UnaryExpression;
    public AExpression parseUnaryExpression4(Token t1, AExpression a2) {
        return new AUnaryExpression(t1.text, a2, t1.position);
    }

    @Override // UnaryExpression : @SAMPLEKW FuncExpression;
    public AExpression parseUnaryExpression5(Token t1, AExpression a2) {
        return new AUnaryExpression(t1.text, a2, t1.position);
    }

    @Override // FuncExpression : ExpressionFactor;
    public AExpression parseFuncExpression1(AExpression a1) {
        return a1;
    }

    @Override // FuncExpression : FuncExpression @SQOPENTK Expression SQCLOSETK;
    public AExpression parseFuncExpression2(AExpression a1, Token t2, AExpression a3) {
        return new AProjectionExpression(a1, a3, t2.position);
    }

    @Override // FuncExpression : FuncExpression @SQOPENTK OptExpression COLONTK OptExpression SQCLOSETK;
    public AExpression parseFuncExpression3(AExpression a1, Token t2, AExpression a3, AExpression a5) {
        return new ASliceExpression(a1, a3, a5, t2.position);
    }

    @Override // FuncExpression : FuncExpression @PAROPENTK PARCLOSETK;
    public AExpression parseFuncExpression4(AExpression a1, Token t2) {
        return new AFuncCallExpression(a1, null, t2.position);
    }

    @Override // FuncExpression : FuncExpression @PAROPENTK Expressions PARCLOSETK;
    public AExpression parseFuncExpression5(AExpression a1, Token t2, List<AExpression> l3) {
        return new AFuncCallExpression(a1, l3, t2.position);
    }

    @Override // FuncExpression : StdLibFunction @PAROPENTK PARCLOSETK;
    public AExpression parseFuncExpression6(Token t1, Token t2) {
        return new AFuncCallExpression(new AStdLibFunctionExpression(t1.text, t1.position), null, t2.position);
    }

    @Override // FuncExpression : StdLibFunction @PAROPENTK Expressions PARCLOSETK;
    public AExpression parseFuncExpression7(Token t1, Token t2, List<AExpression> l3) {
        return new AFuncCallExpression(new AStdLibFunctionExpression(t1.text, t1.position), l3, t2.position);
    }

    @Override // ExpressionFactor : @TRUEKW;
    public AExpression parseExpressionFactor01(Token t1) {
        return new ABoolExpression(true, t1.position);
    }

    @Override // ExpressionFactor : @FALSEKW;
    public AExpression parseExpressionFactor02(Token t1) {
        return new ABoolExpression(false, t1.position);
    }

    @Override // ExpressionFactor : @NUMBERTK;
    public AExpression parseExpressionFactor03(Token t1) {
        return new AIntExpression(t1.text, t1.position);
    }

    @Override // ExpressionFactor : @REALTK;
    public AExpression parseExpressionFactor04(Token t1) {
        return new ARealExpression(t1.text, t1.position);
    }

    @Override // ExpressionFactor : StringToken;
    public AExpression parseExpressionFactor05(AStringToken a1) {
        return new AStringExpression(a1.txt, a1.position);
    }

    @Override // ExpressionFactor : @TIMEKW;
    public AExpression parseExpressionFactor06(Token t1) {
        return new ATimeExpression(t1.position);
    }

    @Override // ExpressionFactor : @SQOPENTK SQCLOSETK;
    public AExpression parseExpressionFactor07(Token t1) {
        return new AListExpression(list(), t1.position);
    }

    @Override // ExpressionFactor : @SQOPENTK Expressions SQCLOSETK;
    public AExpression parseExpressionFactor08(Token t1, List<AExpression> l2) {
        return new AListExpression(l2, t1.position);
    }

    @Override // ExpressionFactor : @CUROPENTK CURCLOSETK;
    public AExpression parseExpressionFactor09(Token t1) {
        return new AEmptySetDictExpression(t1.position);
    }

    @Override // ExpressionFactor : NonEmptySetExpression;
    public AExpression parseExpressionFactor10(ASetExpression a1) {
        return a1;
    }

    @Override // ExpressionFactor : @CUROPENTK DictPairs CURCLOSETK;
    public AExpression parseExpressionFactor11(Token t1, List<ADictPair> l2) {
        return new ADictExpression(l2, t1.position);
    }

    @Override // ExpressionFactor : @PAROPENTK Expression COMMATK Expressions PARCLOSETK;
    public AExpression parseExpressionFactor12(Token t1, AExpression a2, List<AExpression> l4) {
        l4.add(0, a2);
        return new ATupleExpression(l4, t1.position);
    }

    @Override // ExpressionFactor : @LTTK Type GTTK ExpressionFactor;
    public AExpression parseExpressionFactor13(Token t1, ACifType a2, AExpression a4) {
        return new ACastExpression(a4, a2, t1.position);
    }

    @Override // ExpressionFactor : @IFKW Expressions COLONTK Expression OptElifExprs ELSEKW Expression ENDKW;
    public AExpression parseExpressionFactor14(Token t1, List<AExpression> l2, AExpression a4, List<AElifExpression> l5,
            AExpression a7)
    {
        return new AIfExpression(l2, a4, l5, a7, t1.position);
    }

    @Override // ExpressionFactor : @SWITCHKW Expression COLONTK SwitchBody ENDKW;
    public AExpression parseExpressionFactor15(Token t1, AExpression a2, List<ASwitchCase> l4) {
        return new ASwitchExpression(a2, l4, t1.position);
    }

    @Override // ExpressionFactor : PAROPENTK Expression PARCLOSETK;
    public AExpression parseExpressionFactor16(AExpression a2) {
        return a2;
    }

    @Override // ExpressionFactor : Name;
    public AExpression parseExpressionFactor17(AName a1) {
        return new ANameExpression(a1, false, a1.position);
    }

    @Override // ExpressionFactor : Name @APOSTROPHETK;
    public AExpression parseExpressionFactor18(AName a1, Token t2) {
        return new ANameExpression(a1, true, TextPosition.mergePositions(a1.position, t2.position));
    }

    @Override // ExpressionFactor : @QUESTIONTK;
    public AExpression parseExpressionFactor19(Token t1) {
        return new AReceivedExpression(t1.position);
    }

    @Override // ExpressionFactor : @SELFKW;
    public AExpression parseExpressionFactor20(Token t1) {
        return new ASelfExpression(t1.position);
    }

    @Override // NonEmptySetExpression : @CUROPENTK Expressions CURCLOSETK;
    public ASetExpression parseNonEmptySetExpression1(Token t1, List<AExpression> l2) {
        return new ASetExpression(l2, t1.position);
    }

    @Override // DictPairs : Expression @COLONTK Expression;
    public List<ADictPair> parseDictPairs1(AExpression a1, Token t2, AExpression a3) {
        return list(new ADictPair(a1, a3, t2.position));
    }

    @Override // DictPairs : DictPairs COMMATK Expression @COLONTK Expression;
    public List<ADictPair> parseDictPairs2(List<ADictPair> l1, AExpression a3, Token t4, AExpression a5) {
        l1.add(new ADictPair(a3, a5, t4.position));
        return l1;
    }

    @Override // OptElifExprs : ;
    public List<AElifExpression> parseOptElifExprs1() {
        return list();
    }

    @Override // OptElifExprs : OptElifExprs @ELIFKW Expressions COLONTK Expression;
    public List<AElifExpression> parseOptElifExprs2(List<AElifExpression> l1, Token t2, List<AExpression> l3,
            AExpression a5)
    {
        l1.add(new AElifExpression(l3, a5, t2.position));
        return l1;
    }

    @Override // SwitchBody : SwitchCases;
    public List<ASwitchCase> parseSwitchBody1(List<ASwitchCase> l1) {
        return l1;
    }

    @Override // SwitchBody : SwitchCases @ELSEKW Expression;
    public List<ASwitchCase> parseSwitchBody2(List<ASwitchCase> l1, Token t2, AExpression a3) {
        l1.add(new ASwitchCase(null, a3, t2.position));
        return l1;
    }

    @Override // SwitchBody : @ELSEKW Expression;
    public List<ASwitchCase> parseSwitchBody3(Token t1, AExpression a2) {
        return list(new ASwitchCase(null, a2, t1.position));
    }

    @Override // SwitchCases : @CASEKW Expression COLONTK Expression;
    public List<ASwitchCase> parseSwitchCases1(Token t1, AExpression a2, AExpression a4) {
        return list(new ASwitchCase(a2, a4, t1.position));
    }

    @Override // SwitchCases : SwitchCases @CASEKW Expression COLONTK Expression;
    public List<ASwitchCase> parseSwitchCases2(List<ASwitchCase> l1, Token t2, AExpression a3, AExpression a5) {
        l1.add(new ASwitchCase(a3, a5, t2.position));
        return l1;
    }

    @Override // Name : Identifier;
    public AName parseName1(AIdentifier a1) {
        return new AName(a1.id, a1.position);
    }

    @Override // Name : @RELATIVENAMETK;
    public AName parseName2(Token t1) {
        return new AName(t1.text, t1.position);
    }

    @Override // Name : @ABSOLUTENAMETK;
    public AName parseName3(Token t1) {
        return new AName(t1.text, t1.position);
    }

    @Override // Name : @ROOTNAMETK;
    public AName parseName4(Token t1) {
        return new AName(t1.text, t1.position);
    }

    @Override // OptAnnos : ;
    public List<AAnnotation> parseOptAnnos1() {
        return list();
    }

    @Override // OptAnnos : OptAnnos Annotation;
    public List<AAnnotation> parseOptAnnos2(List<AAnnotation> l1, AAnnotation a2) {
        l1.add(a2);
        return l1;
    }

    @Override // Annotation : @ANNOTATIONNAMETK;
    public AAnnotation parseAnnotation1(Token t1) {
        return new AAnnotation(t1, list());
    }

    @Override // Annotation : @ANNOTATIONNAMETK PAROPENTK PARCLOSETK;
    public AAnnotation parseAnnotation2(Token t1) {
        return new AAnnotation(t1, list());
    }

    @Override // Annotation : @ANNOTATIONNAMETK PAROPENTK AnnotationArgs OptComma PARCLOSETK;
    public AAnnotation parseAnnotation3(Token t1, List<AAnnotationArgument> l3, Token t4) {
        return new AAnnotation(t1, l3);
    }

    @Override // AnnotationArgs : AnnotationArg;
    public List<AAnnotationArgument> parseAnnotationArgs1(AAnnotationArgument a1) {
        return list(a1);
    }

    @Override // AnnotationArgs : AnnotationArgs COMMATK AnnotationArg;
    public List<AAnnotationArgument> parseAnnotationArgs2(List<AAnnotationArgument> l1, AAnnotationArgument a3) {
        l1.add(a3);
        return l1;
    }

    @Override // AnnotationArg : @IDENTIFIERTK EQTK Expression;
    public AAnnotationArgument parseAnnotationArg1(Token t1, AExpression a3) {
        return new AAnnotationArgument(t1, a3);
    }

    @Override // AnnotationArg : @RELATIVENAMETK EQTK Expression;
    public AAnnotationArgument parseAnnotationArg2(Token t1, AExpression a3) {
        return new AAnnotationArgument(t1, a3);
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
