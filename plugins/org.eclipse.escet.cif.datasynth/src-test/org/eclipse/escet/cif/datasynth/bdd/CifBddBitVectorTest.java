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

package org.eclipse.escet.cif.datasynth.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;

/** {@link CifBddBitVector} unit tests. */
@SuppressWarnings("javadoc")
public class CifBddBitVectorTest {
    /** The BDD factory to use. */
    private final BDDFactory factory = BDDFactory.init("java", 100, 100);

    @Test
    public void testCreateNegative() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.create(factory, -1));
    }

    @Test
    public void testCreate() {
        for (int i = 0; i < 64; i++) {
            CifBddBitVector vector = CifBddBitVector.create(factory, i);
            for (int j = 0; j < i; j++) {
                assertEquals(factory.zero(), vector.getBit(j));
            }
        }
    }

    @Test
    public void testCreateBitsNegative() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.create(factory, -1));
    }

    @Test
    public void testCreateBits() {
        for (boolean v: new boolean[] {true, false}) {
            BDD b = v ? factory.one() : factory.zero();
            for (int i = 0; i < 64; i++) {
                CifBddBitVector vector = CifBddBitVector.createBits(factory, i, v);
                for (int j = 0; j < i; j++) {
                    assertEquals(b, vector.getBit(j));
                }
            }
        }
    }

    @Test
    public void testCreateIntNegative() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.create(factory, -1));
    }

    @Test
    public void testCreateInt() {
        for (int i = 0; i < 64; i++) {
            CifBddBitVector vector = CifBddBitVector.createInt(factory, i);
            assertEquals(i, vector.getLong());
        }
        for (int i = Integer.MAX_VALUE; i > Integer.MAX_VALUE - 64; i--) {
            CifBddBitVector vector = CifBddBitVector.createInt(factory, i);
            assertEquals(i, vector.getInt());
        }
    }

    @Test
    public void testCreateIntWithLenNegativeLen() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.createInt(factory, -1, 1));
    }

    @Test
    public void testCreateIntWithLenNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.createInt(factory, 1, -1));
    }

    @Test
    public void testCreateIntWithLen1() {
        for (int l = 0; l < 10; l++) {
            for (int i = 0; i < 64; i++) {
                CifBddBitVector vector = CifBddBitVector.createInt(factory, l, i);
                if (i < vector.countInt()) {
                    assertEquals(i, vector.getLong());
                }
            }
        }
    }

    @Test
    public void testCreateIntWithLen2() {
        for (int l = 0; l < 10; l++) {
            for (int i = 0; i < 64; i++) {
                CifBddBitVector vector1 = CifBddBitVector.createInt(factory, l, i);
                CifBddBitVector vector2 = CifBddBitVector.createInt(factory, 10, i);
                vector2.resize(l);
                assertEquals(factory.one(), vector1.equalTo(vector2));
            }
        }
    }

    @Test
    public void testCreateDomain() {
        BDDDomain domain = factory.extDomain(2 * 2 * 2);
        int length = factory.varNum();
        assertEquals(3, length);
        CifBddBitVector vector = CifBddBitVector.createDomain(domain);
        assertEquals(length, vector.length());
        for (int i = 0; i < length; i++) {
            assertEquals(factory.ithVar(i), vector.getBit(i));
        }
    }

    @Test
    public void testCopy() {
        // Create.
        CifBddBitVector vector1 = CifBddBitVector.createInt(factory, 5);
        assertEquals(3, vector1.length());
        assertEquals(factory.one(), vector1.getBit(0));
        assertEquals(factory.zero(), vector1.getBit(1));
        assertEquals(factory.one(), vector1.getBit(2));

        // Copy.
        CifBddBitVector vector2 = vector1.copy();
        assertEquals(3, vector2.length());
        assertEquals(factory.one(), vector2.getBit(0));
        assertEquals(factory.zero(), vector2.getBit(1));
        assertEquals(factory.one(), vector2.getBit(2));

        // Not the same objects, but real copies.
        assertNotSame(vector1, vector2);
        assertNotSame(vector1.getBit(0), vector2.getBit(0));
        assertNotSame(vector1.getBit(1), vector2.getBit(1));
        assertNotSame(vector1.getBit(2), vector2.getBit(2));

        // Freeing the original doesn't free the copy.
        vector1.free();
        assertEquals(3, vector2.length());
        assertEquals(factory.one(), vector2.getBit(0));
        assertEquals(factory.zero(), vector2.getBit(1));
        assertEquals(factory.one(), vector2.getBit(2));
    }

    @Test
    public void testReplaceBy() {
        int num1 = 0b01011;
        int num2 = 0b11101;
        CifBddBitVector vector1 = CifBddBitVector.createInt(factory, num1);
        CifBddBitVector vector2 = CifBddBitVector.createInt(factory, num2);
        assertEquals(num1, vector1.getInt());
        assertEquals(num2, vector2.getInt());
        vector1.replaceBy(vector2);
        assertEquals(num2, vector1.getInt());
    }

    @Test
    public void testLength() {
        for (int i = 0; i < 64; i++) {
            CifBddBitVector vector = CifBddBitVector.create(factory, i);
            assertEquals(i, vector.length());
        }
    }

    @Test
    public void testCountInt() {
        assertEquals(1, CifBddBitVector.create(factory, 0).countInt());
        assertEquals(2, CifBddBitVector.create(factory, 1).countInt());
        assertEquals(4, CifBddBitVector.create(factory, 2).countInt());
        assertEquals(8, CifBddBitVector.create(factory, 3).countInt());
        assertEquals(1073741824, CifBddBitVector.create(factory, 30).countInt());

        for (int i = 0; i < 31; i++) {
            CifBddBitVector vector = CifBddBitVector.create(factory, i);
            assertEquals(1 << i, vector.countInt());
        }
    }

    @Test
    public void testCountLong() {
        assertEquals(1, CifBddBitVector.create(factory, 0).countLong());
        assertEquals(2, CifBddBitVector.create(factory, 1).countLong());
        assertEquals(4, CifBddBitVector.create(factory, 2).countLong());
        assertEquals(8, CifBddBitVector.create(factory, 3).countLong());
        assertEquals(4611686018427387904L, CifBddBitVector.create(factory, 62).countLong());

        for (int i = 0; i < 63; i++) {
            CifBddBitVector vector = CifBddBitVector.create(factory, i);
            assertEquals(1L << i, vector.countLong());
        }
    }

    @Test
    public void testGetBit() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertEquals(factory.zero(), vector.getBit(0));
    }

    @Test
    public void testGetNegative() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> vector.getBit(-1));
    }

    @Test
    public void testGetTooLarge() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> vector.getBit(1));
    }

    @Test
    public void testGetInt() {
        // Constant values.
        CifBddBitVector vector = CifBddBitVector.create(factory, 4);
        int cnt = vector.countInt();
        for (int i = 0; i < cnt; i++) {
            vector.setInt(i);
            assertEquals(i, vector.getInt());
        }

        // Large constant values.
        vector = CifBddBitVector.create(factory, 31);
        assertEquals(Integer.MAX_VALUE, vector.countLong() - 1);
        vector.setInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, vector.getInt());

        // Non-constant value.
        vector.setDomain(factory.extDomain(2 * 2));
        assertEquals(-1, vector.getInt());
    }

    @Test
    public void testGetLong() {
        // Constant values.
        CifBddBitVector vector = CifBddBitVector.create(factory, 4);
        int cnt = vector.countInt();
        for (int i = 0; i < cnt; i++) {
            vector.setInt(i);
            assertEquals(i, vector.getLong());
        }

        // Non-constant value.
        vector.setDomain(factory.extDomain(2 * 2));
        assertEquals(-1, vector.getInt());
    }

    @Test
    public void testSetBit() {
        int cnt = 4;
        CifBddBitVector vector = CifBddBitVector.create(factory, cnt);
        int value = 0;
        for (int i = 0; i < cnt; i++) {
            vector.setBit(i, factory.one());
            value += (1 << i);
            assertEquals(value, vector.getInt());
        }
        for (int i = 0; i < cnt; i++) {
            vector.setBit(i, factory.zero());
            value -= (1 << i);
            assertEquals(value, vector.getInt());
        }
    }

    @Test
    public void testSetBitNegative() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> vector.setBit(-1, factory.zero()));
    }

    @Test
    public void testSetBitTooLarge() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> vector.setBit(1, factory.zero()));
    }

    @Test
    public void testSetBits() {
        int cnt = 31;
        CifBddBitVector vector = CifBddBitVector.create(factory, cnt);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.zero(), vector.getBit(i));
        }
        vector.setBits(true);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.one(), vector.getBit(i));
        }
        vector.setBits(false);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.zero(), vector.getBit(i));
        }
        vector.setBits(false);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.zero(), vector.getBit(i));
        }
        vector.setBits(true);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.one(), vector.getBit(i));
        }
        vector.setBits(true);
        for (int i = 0; i < cnt; i++) {
            assertEquals(factory.one(), vector.getBit(i));
        }
    }

    @Test
    public void testCreateSetBits() {
        int cnt = 31;
        CifBddBitVector vector1 = CifBddBitVector.create(factory, cnt);
        CifBddBitVector vector2 = CifBddBitVector.createBits(factory, cnt, false);
        for (int i = 0; i < cnt; i++) {
            assertEquals(vector1.getBit(i), vector2.getBit(i));
        }
        vector1.setBits(true);
        vector2 = CifBddBitVector.createBits(factory, cnt, true);
        for (int i = 0; i < cnt; i++) {
            assertEquals(vector1.getBit(i), vector2.getBit(i));
        }
        vector1.setBits(false);
        vector2 = CifBddBitVector.createBits(factory, cnt, false);
        for (int i = 0; i < cnt; i++) {
            assertEquals(vector1.getBit(i), vector2.getBit(i));
        }
    }

    @Test
    public void testSetIntNegative() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.setInt(-1));
    }

    @Test
    public void testSetIntTooLarge() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 2);
        assertThrows(IllegalArgumentException.class, () -> vector.setInt(4));
    }

    @Test
    public void testSetInt() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 3);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));

        vector.setInt(1);
        assertEquals(factory.one(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));

        vector.setInt(2);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.one(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));

        vector.setInt(3);
        assertEquals(factory.one(), vector.getBit(0));
        assertEquals(factory.one(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));

        vector.setInt(4);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.one(), vector.getBit(2));

        vector.setInt(5);
        assertEquals(factory.one(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.one(), vector.getBit(2));

        vector.setInt(6);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.one(), vector.getBit(1));
        assertEquals(factory.one(), vector.getBit(2));

        vector.setInt(7);
        assertEquals(factory.one(), vector.getBit(0));
        assertEquals(factory.one(), vector.getBit(1));
        assertEquals(factory.one(), vector.getBit(2));

        vector.setInt(0);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));
    }

    @Test
    public void testCreateSetInt() {
        for (int i = 0; i < 32; i++) {
            int length = BddUtils.getMinimumBits(i);
            CifBddBitVector vector1 = CifBddBitVector.create(factory, length);
            vector1.setInt(i);
            CifBddBitVector vector2 = CifBddBitVector.createInt(factory, i);
            assertEquals(length, vector1.length());
            assertEquals(length, vector2.length());
            for (int j = 0; j < length; j++) {
                assertEquals(vector1.getBit(j), vector2.getBit(j));
            }
        }
    }

    @Test
    public void testSetDomain() {
        BDDDomain domain = factory.extDomain(2 * 2 * 2);
        assertEquals(3, factory.varNum());
        BDD var0 = factory.ithVar(0);
        BDD var1 = factory.ithVar(1);
        BDD var2 = factory.ithVar(2);

        // Equal sizes.
        CifBddBitVector vector = CifBddBitVector.create(factory, 3);
        vector.setDomain(domain);
        assertEquals(var0, vector.getBit(0));
        assertEquals(var1, vector.getBit(1));
        assertEquals(var2, vector.getBit(2));

        // Larger bit vector.
        vector = CifBddBitVector.create(factory, 5);
        vector.setDomain(domain);
        assertEquals(var0, vector.getBit(0));
        assertEquals(var1, vector.getBit(1));
        assertEquals(var2, vector.getBit(2));
        assertEquals(factory.zero(), vector.getBit(3));
        assertEquals(factory.zero(), vector.getBit(4));

        // Smaller bit vector.
        vector = CifBddBitVector.create(factory, 2);
        vector.setDomain(domain);
        assertEquals(var0, vector.getBit(0));
        assertEquals(var1, vector.getBit(1));
    }

    @Test
    public void testCreateSetDomain() {
        BDDDomain domain = factory.extDomain(2 * 2 * 2);
        int length = factory.varNum();
        assertEquals(3, length);
        CifBddBitVector vector1 = CifBddBitVector.create(factory, length);
        vector1.setDomain(domain);
        CifBddBitVector vector2 = CifBddBitVector.createDomain(domain);
        assertEquals(length, vector1.length());
        assertEquals(length, vector2.length());
        for (int i = 0; i < length; i++) {
            assertEquals(factory.ithVar(i), vector1.getBit(i));
            assertEquals(factory.ithVar(i), vector2.getBit(i));
        }
    }

    @Test
    public void testResizeNegative() {
        assertThrows(IllegalArgumentException.class, () -> CifBddBitVector.create(factory, 0).resize(-1));
    }

    @Test
    public void testResize() {
        CifBddBitVector vector1 = CifBddBitVector.createInt(factory, 1234);
        int length = vector1.length();
        for (int i = 0; i < 60; i++) {
            CifBddBitVector vector2 = CifBddBitVector.createInt(factory, 1234);
            vector2.resize(i);
            assertEquals(i, vector2.length());

            if (i >= length) {
                assertEquals(1234, vector2.getLong());
            }
            int min = Math.min(length, i);
            for (int j = 0; j < min; j++) {
                assertEquals(vector1.getBit(j), vector2.getBit(j));
            }
            for (int j = min; j < i; j++) {
                assertEquals(factory.zero(), vector2.getBit(j));
            }
        }
    }

    @Test
    public void testAdd() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                CifBddBitVectorAndCarry vc = vector1.add(vector2);

                int actual = vc.vector.getInt();
                if (vc.carry.isOne()) {
                    actual += cnt;
                }

                int expected = i + j;
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testSubtract1() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                CifBddBitVectorAndCarry vc = vector1.subtract(vector2);

                int actual = vc.vector.getInt();
                if (vc.carry.isOne()) {
                    actual -= cnt;
                }

                int expected = i - j;
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testSubtract2() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 2);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 2);
        vector1.setInt(2);
        vector2.setInt(3);
        CifBddBitVectorAndCarry vc = vector1.subtract(vector2);
        assertEquals(3, vc.vector.getInt());
        assertEquals(true, vc.carry.isOne());
    }

    @Test
    public void testDivZero() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.div(0));
    }

    @Test
    public void testDivNegative() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.div(-1));
    }

    @Test
    public void testDivTooLarge() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> vector.div(2));
    }

    @Test
    public void testDiv() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 5);
        int cnt = vector.countInt();
        for (int i = 0; i < cnt; i++) {
            vector.setInt(i);
            for (int j = 1; j < cnt; j++) {
                int actual = vector.div(j).getInt();
                int expected = i / j;
                assertEquals(expected, actual, i + " / " + j);
            }
        }
    }

    @Test
    public void testModZero() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.mod(0));
    }

    @Test
    public void testModNegative() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.mod(-1));
    }

    @Test
    public void testModTooLarge() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> vector.mod(2));
    }

    @Test
    public void testModHighestNonFalse() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalStateException.class, () -> vector.mod(1));
    }

    @Test
    public void testIncMod() {
        int bits = 5;
        int value = 0;
        CifBddBitVector vector1 = CifBddBitVector.createInt(factory, bits, value);
        CifBddBitVector one = CifBddBitVector.createInt(factory, bits, 1);
        int cnt = 1 << bits;
        for (int i = 0; i < 10 * cnt; i++) {
            // Check identical results for integer value and vector.
            assertEquals(value, vector1.getInt());

            // Perform single inc/mod operation on integer values.
            value = (value + 1) % cnt;

            // Perform single inc/mod operation on bit vectors.
            CifBddBitVectorAndCarry tmp1 = vector1.add(one);
            CifBddBitVector tmp2 = tmp1.vector;
            tmp2.resize(bits + 1);
            tmp2 = tmp2.mod(cnt);
            tmp2.resize(bits);
            vector1 = tmp2;
        }
    }

    @Test
    public void testMod() {
        int bits = 5;
        CifBddBitVector vector = CifBddBitVector.create(factory, bits + 1);
        int cnt = vector.countInt();
        int cntDiv2 = 1 << bits;
        for (int i = 0; i < cntDiv2; i++) {
            vector.setInt(i);
            for (int j = 1; j < cnt; j++) {
                int actual = vector.mod(j).getInt();
                int expected = i % j;
                assertEquals(expected, actual, i + " % " + j);
            }
        }
    }

    @Test
    public void testShiftLeftNegative() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.shiftLeft(-1, factory.zero()));
    }

    @Test
    public void testShiftLeft() {
        int val0 = 0b110101;

        int val1t = 0b101011;
        int val2t = 0b010111;
        int val3t = 0b101111;
        int val4t = 0b011111;
        int val5t = 0b111111;
        int val6t = 0b111111;

        int val1f = 0b101010;
        int val2f = 0b010100;
        int val3f = 0b101000;
        int val4f = 0b010000;
        int val5f = 0b100000;
        int val6f = 0b000000;

        CifBddBitVector vector = CifBddBitVector.createInt(factory, val0);

        assertEquals(val0, vector.shiftLeft(0, factory.one()).getInt());
        assertEquals(val1t, vector.shiftLeft(1, factory.one()).getInt());
        assertEquals(val2t, vector.shiftLeft(2, factory.one()).getInt());
        assertEquals(val3t, vector.shiftLeft(3, factory.one()).getInt());
        assertEquals(val4t, vector.shiftLeft(4, factory.one()).getInt());
        assertEquals(val5t, vector.shiftLeft(5, factory.one()).getInt());
        assertEquals(val6t, vector.shiftLeft(6, factory.one()).getInt());
        assertEquals(val6t, vector.shiftLeft(7, factory.one()).getInt());

        assertEquals(val0, vector.shiftLeft(0, factory.zero()).getInt());
        assertEquals(val1f, vector.shiftLeft(1, factory.zero()).getInt());
        assertEquals(val2f, vector.shiftLeft(2, factory.zero()).getInt());
        assertEquals(val3f, vector.shiftLeft(3, factory.zero()).getInt());
        assertEquals(val4f, vector.shiftLeft(4, factory.zero()).getInt());
        assertEquals(val5f, vector.shiftLeft(5, factory.zero()).getInt());
        assertEquals(val6f, vector.shiftLeft(6, factory.zero()).getInt());
        assertEquals(val6f, vector.shiftLeft(7, factory.zero()).getInt());
    }

    @Test
    public void testShiftRightNegative() {
        CifBddBitVector vector = CifBddBitVector.createInt(factory, 1);
        assertThrows(IllegalArgumentException.class, () -> vector.shiftRight(-1, factory.zero()));
    }

    @Test
    public void testShiftRight() {
        int val0 = 0b110101;

        int val1t = 0b111010;
        int val2t = 0b111101;
        int val3t = 0b111110;
        int val4t = 0b111111;
        int val5t = 0b111111;
        int val6t = 0b111111;

        int val1f = 0b011010;
        int val2f = 0b001101;
        int val3f = 0b000110;
        int val4f = 0b000011;
        int val5f = 0b000001;
        int val6f = 0b000000;

        CifBddBitVector vector = CifBddBitVector.createInt(factory, val0);

        assertEquals(val0, vector.shiftRight(0, factory.one()).getInt());
        assertEquals(val1t, vector.shiftRight(1, factory.one()).getInt());
        assertEquals(val2t, vector.shiftRight(2, factory.one()).getInt());
        assertEquals(val3t, vector.shiftRight(3, factory.one()).getInt());
        assertEquals(val4t, vector.shiftRight(4, factory.one()).getInt());
        assertEquals(val5t, vector.shiftRight(5, factory.one()).getInt());
        assertEquals(val6t, vector.shiftRight(6, factory.one()).getInt());
        assertEquals(val6t, vector.shiftRight(7, factory.one()).getInt());

        assertEquals(val0, vector.shiftRight(0, factory.zero()).getInt());
        assertEquals(val1f, vector.shiftRight(1, factory.zero()).getInt());
        assertEquals(val2f, vector.shiftRight(2, factory.zero()).getInt());
        assertEquals(val3f, vector.shiftRight(3, factory.zero()).getInt());
        assertEquals(val4f, vector.shiftRight(4, factory.zero()).getInt());
        assertEquals(val5f, vector.shiftRight(5, factory.zero()).getInt());
        assertEquals(val6f, vector.shiftRight(6, factory.zero()).getInt());
        assertEquals(val6f, vector.shiftRight(7, factory.zero()).getInt());
    }

    @Test
    public void testIfThenElse() {
        CifBddBitVector vector1 = CifBddBitVector.createInt(factory, 3, 3);
        CifBddBitVector vector2 = CifBddBitVector.createInt(factory, 3, 7);
        CifBddBitVector vector3 = vector1.ifThenElse(vector2, factory.one());
        CifBddBitVector vector4 = vector1.ifThenElse(vector2, factory.zero());
        BDD test1 = vector3.equalTo(vector1);
        BDD test2 = vector4.equalTo(vector2);
        assertTrue(test1.isOne());
        assertTrue(test2.isOne());
    }

    @Test
    public void testLessThan() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i < j, vector1.lessThan(vector2).isOne());
            }
        }
    }

    @Test
    public void testLessOrEqual() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i <= j, vector1.lessOrEqual(vector2).isOne());
            }
        }
    }

    @Test
    public void testGreaterThan() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i > j, vector1.greaterThan(vector2).isOne());
            }
        }
    }

    @Test
    public void testGreaterOrEqual() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i >= j, vector1.greaterOrEqual(vector2).isOne());
            }
        }
    }

    @Test
    public void testEqualTo() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i == j, vector1.equalTo(vector2).isOne());
            }
        }
    }

    @Test
    public void testUnequalTo() {
        CifBddBitVector vector1 = CifBddBitVector.create(factory, 4);
        CifBddBitVector vector2 = CifBddBitVector.create(factory, 4);
        int cnt = vector1.countInt();
        for (int i = 0; i < cnt; i++) {
            vector1.setInt(i);
            for (int j = 0; j < cnt; j++) {
                vector2.setInt(j);
                assertEquals(i != j, vector1.unequalTo(vector2).isOne());
            }
        }
    }

    @Test
    public void testFree() {
        CifBddBitVector vector = CifBddBitVector.create(factory, 3);
        assertEquals(factory.zero(), vector.getBit(0));
        assertEquals(factory.zero(), vector.getBit(1));
        assertEquals(factory.zero(), vector.getBit(2));
        vector.free();
    }
}
