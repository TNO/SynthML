//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.varorder.metrics;

/** Variable order metric kind. */
public enum VarOrderMetricKind {
    /** Total span metric. */
    TOTAL_SPAN,

    /** Weighted Event Span (WES) metric. */
    WES;

    /**
     * Create an instance of the metric for this metric kind.
     *
     * @return The metric.
     */
    public VarOrderMetric create() {
        switch (this) {
            case TOTAL_SPAN:
                return new TotalSpanMetric();
            case WES:
                return new WesMetric();
        }
        throw new RuntimeException("Unknown metric: " + this);
    }
}
