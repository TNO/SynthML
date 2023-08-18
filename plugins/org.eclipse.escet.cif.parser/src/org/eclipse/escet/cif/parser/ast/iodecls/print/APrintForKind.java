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

package org.eclipse.escet.cif.parser.ast.iodecls.print;

/** {@link APrintFor} filter kind. */
public enum APrintForKind {
    /** 'event' filter. */
    EVENT,

    /** 'time' filter. */
    TIME,

    /** Name filter. */
    NAME,

    /** 'initial' filter. */
    INITIAL,

    /** 'final' filter. */
    FINAL;
}
