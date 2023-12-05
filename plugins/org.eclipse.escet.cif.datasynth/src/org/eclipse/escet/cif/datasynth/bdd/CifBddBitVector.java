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

import java.util.Arrays;

import org.eclipse.escet.common.java.Assert;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;

/** BDD bit vector. */
public class CifBddBitVector {
    /** The BDD factory to use. */
    private BDDFactory factory;

    /** The BDDs for each of the bits of the bit vector. The lowest bit is at index zero. */
    private BDD[] bits;

    /**
     * Constructor for the {@link CifBddBitVector} class.
     *
     * @param factory The BDD factory to use.
     * @param length The number of bits of the bit vector.
     * @throws IllegalArgumentException If the length is negative.
     */
    private CifBddBitVector(BDDFactory factory, int length) {
        // Precondition check.
        if (length < 0) {
            throw new IllegalArgumentException("Length is negative.");
        }

        // Create.
        this.factory = factory;
        bits = new BDD[length];
    }

    /**
     * Creates a {@link CifBddBitVector}. Initializes the bits of the bit vector to 'false'.
     *
     * @param factory The BDD factory to use.
     * @param length The number of bits of the bit vector.
     * @return The created bit vector.
     * @throws IllegalArgumentException If the length is negative.
     */
    public static CifBddBitVector create(BDDFactory factory, int length) {
        return createBits(factory, length, false);
    }

    /**
     * Creates a {@link CifBddBitVector}. Initializes the bits of the bit vector to the given boolean value.
     *
     * @param factory The BDD factory to use.
     * @param length The number of bits of the bit vector.
     * @param value The value to use for each bit.
     * @return The created bit vector.
     * @throws IllegalArgumentException If the length is negative.
     */
    public static CifBddBitVector createBits(BDDFactory factory, int length, boolean value) {
        // Create.
        CifBddBitVector vector = new CifBddBitVector(factory, length);

        // Initialize.
        for (int i = 0; i < vector.bits.length; i++) {
            vector.bits[i] = value ? factory.one() : factory.zero();
        }

        // Return.
        return vector;
    }

    /**
     * Creates a {@link CifBddBitVector}. Initializes the bits of the bit vector to the given integer value. Uses an as
     * small as possible bit vector to represent the integer value.
     *
     * @param factory The BDD factory to use.
     * @param value The integer value to represent using a bit vector.
     * @return The created bit vector.
     * @throws IllegalArgumentException If the value is negative.
     */
    public static CifBddBitVector createInt(BDDFactory factory, int value) {
        // Precondition check.
        if (value < 0) {
            throw new IllegalArgumentException("Value is negative.");
        }

        // Calculate number of bits needed.
        int length = 0;
        int v = value;
        while (v > 0) {
            length++;
            v = v >> 1;
        }

        // Create.
        CifBddBitVector vector = new CifBddBitVector(factory, length);

        // Initialize.
        for (int i = 0; i < vector.bits.length; i++) {
            vector.bits[i] = ((value & 0x1) != 0) ? factory.one() : factory.zero();
            value >>= 1;
        }
        Assert.check(value == 0);

        // Return.
        return vector;
    }

    /**
     * Creates a {@link CifBddBitVector}. Initializes the bits of the bit vector to the given integer value. If the
     * requested length is larger than the number of bits needed, the remaining/highest bits are set to 'false'. If the
     * requested length is smaller than the number of bits needed, the highest bits are dropped.
     *
     * @param factory The BDD factory to use.
     * @param length The number of bits of the bit vector.
     * @param value The integer value to represent using a bit vector.
     * @return The created bit vector.
     * @throws IllegalArgumentException If the length is negative.
     * @throws IllegalArgumentException If the value is negative.
     */
    public static CifBddBitVector createInt(BDDFactory factory, int length, int value) {
        // Precondition check.
        if (length < 0) {
            throw new IllegalArgumentException("Length is negative.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value is negative.");
        }

        // Create.
        CifBddBitVector vector = new CifBddBitVector(factory, length);

        // Initialize.
        for (int i = 0; i < vector.bits.length; i++) {
            vector.bits[i] = ((value & 0x1) != 0) ? factory.one() : factory.zero();
            value >>= 1;
        }

        // Return.
        return vector;
    }

    /**
     * Creates a {@link CifBddBitVector}. Initializes the bits of the bit vector to the variables of the given domain.
     * The length of the bit vector is the number of variables in the given domain.
     *
     * @param domain The domain to use.
     * @return The created bit vector.
     */
    public static CifBddBitVector createDomain(BDDDomain domain) {
        // Create.
        CifBddBitVector vector = new CifBddBitVector(domain.getFactory(), domain.varNum());

        // Initialize.
        int[] vars = domain.vars();
        for (int i = 0; i < vars.length; i++) {
            vector.bits[i] = vector.factory.ithVar(vars[i]);
        }

        // Return.
        return vector;
    }

    /**
     * Creates a copy of this bit vector. A new instance of the bit vector is created, that has the same length. Each
     * bit is {@link BDD#id copied} to the new bit vector.
     *
     * @return The copy.
     */
    public CifBddBitVector copy() {
        CifBddBitVector vector = new CifBddBitVector(factory, bits.length);
        for (int i = 0; i < bits.length; i++) {
            vector.bits[i] = bits[i].id();
        }
        return vector;
    }

    /**
     * Modifies this bit vector to represent the given other bit vector. This bit vector is first {@link #free freed},
     * then the content of the other bit vector is moved to this bit vector. The other bit vector is essentially
     * {@link #free freed}, and can no longer be used.
     *
     * <p>
     * This bit vector and the given other bit vector don't need to have the same length.
     * </p>
     *
     * @param other The other bit vector.
     */
    public void replaceBy(CifBddBitVector other) {
        free();
        this.factory = other.factory;
        this.bits = other.bits;
        other.factory = null;
        other.bits = null;
    }

    /**
     * Returns the length of the bit vector, in number of bits.
     *
     * @return The length of the bit vector, in number of bits.
     */
    public int length() {
        return bits.length;
    }

    /**
     * Returns the value count, the number of values that can be represented by the bit vector.
     *
     * @return The value count.
     * @throws IllegalStateException If the bit vector has more than 30 bits.
     */
    public int countInt() {
        // For 31 bits, the count becomes -2147483648 rather than 2147483648.
        if (bits.length > 30) {
            throw new IllegalStateException("More than 30 bits in vector.");
        }

        // A zero length bit vector always stores value zero.
        return 1 << bits.length;
    }

    /**
     * Returns the value count, the number of values that can be represented by the bit vector.
     *
     * @return The value count.
     * @throws IllegalStateException If the bit vector has more than 62 bits.
     */
    public long countLong() {
        // For 63 bits, the count becomes -9223372036854775808 rather than
        // 9223372036854775808.
        if (bits.length > 62) {
            throw new IllegalStateException("More than 62 bits in vector.");
        }

        // A zero length bit vector always stores value zero.
        return 1L << bits.length;
    }

    /**
     * Returns the BDD for the bit with the given index. The lowest bit is at index zero.
     *
     * @param index The 0-based index of the bit.
     * @return The BDD for the bit with the given index.
     * @throws IndexOutOfBoundsException If the index is negative, or greater than or equal to {@link #length()}.
     */
    public BDD getBit(int index) {
        return bits[index];
    }

    /**
     * Returns the value represented by the bit vector, if it is a constant bit vector, or {@code -1} otherwise.
     *
     * @return The value represented by the bit vector, or {@code -1}.
     * @throws IllegalStateException If the bit vector has more than 31 bits.
     */
    public int getInt() {
        // For 32 bits, the value can overflow.
        if (bits.length > 31) {
            throw new IllegalStateException("More than 31 bits in vector.");
        }

        // Get value.
        int value = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            if (bits[i].isOne()) {
                value = (value << 1) | 1;
            } else if (bits[i].isZero()) {
                value = (value << 1);
            } else {
                return -1;
            }
        }
        return value;
    }

    /**
     * Returns the value represented by the bit vector, if it is a constant bit vector, or {@code -1} otherwise.
     *
     * @return The value represented by the bit vector, or {@code -1}.
     * @throws IllegalStateException If the bit vector has more than 63 bits.
     */
    public long getLong() {
        // For 64 bits, the value can overflow.
        if (bits.length > 63) {
            throw new IllegalStateException("More than 63 bits in vector.");
        }

        // Get value.
        long value = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            if (bits[i].isOne()) {
                value = (value << 1) | 1;
            } else if (bits[i].isZero()) {
                value = (value << 1);
            } else {
                return -1;
            }
        }
        return value;
    }

    /**
     * Updates the bit vector, setting the bit with the given index to a given BDD. The previous BDD stored at the bit
     * is first {@link BDD#free freed}.
     *
     * @param idx The 0-based index of the bit to set.
     * @param bdd The BDD to use as new value for the given bit.
     * @throws IndexOutOfBoundsException If the index is negative, or greater than or equal to {@link #length()}.
     */
    public void setBit(int idx, BDD bdd) {
        bits[idx].free();
        bits[idx] = bdd;
    }

    /**
     * Updates the bit vector, setting each bit to the given value. The BDDs that were previously stored, are first
     * {@link BDD#free freed}.
     *
     * @param value The value to set for each bit.
     */
    public void setBits(boolean value) {
        for (int i = 0; i < bits.length; i++) {
            bits[i].free();
            bits[i] = value ? factory.one() : factory.zero();
        }
    }

    /**
     * Updates the bit vector to represent the given value. The BDDs that were previously stored, are first
     * {@link BDD#free freed}.
     *
     * @param value The value to which to set the bit vector.
     * @throws IllegalArgumentException If the value is negative.
     * @throws IllegalArgumentException If the value is doesn't fit within the bit vector.
     */
    public void setInt(int value) {
        // Precondition check.
        if (value < 0) {
            throw new IllegalArgumentException("Value is negative.");
        }

        // Set value.
        for (int i = 0; i < bits.length; i++) {
            bits[i].free();
            bits[i] = ((value & 0x1) != 0) ? factory.one() : factory.zero();
            value >>= 1;
        }

        // Make sure the value fits (precondition).
        if (value > 0) {
            throw new IllegalArgumentException("Value doesn't fit.");
        }
    }

    /**
     * Updates the bit vector to represent the given BDD domain. If the domain is larger than the bit vector, only part
     * of the domain is used. If the domain is smaller than the bit vector, the higher bits of the bit vector are set to
     * 'false'. The BDDs that were previously stored, are first {@link BDD#free freed}.
     *
     * @param domain The domain to which to set the bit vector.
     */
    public void setDomain(BDDDomain domain) {
        int[] vars = domain.vars();
        int cnt = Math.min(vars.length, bits.length);
        for (int i = 0; i < cnt; i++) {
            bits[i].free();
            bits[i] = factory.ithVar(vars[i]);
        }
        for (int i = cnt; i < bits.length; i++) {
            bits[i].free();
            bits[i] = factory.zero();
        }
    }

    /**
     * Resizes the bit vector to have the given length. If the new length is larger than the current length, the
     * additional (most significant) bits are set the 'false'. If the new length is smaller than the current length, the
     * most significant bits are dropped. The BDDs for dropped bits, are {@link BDD#free freed}.
     *
     * @param length The new length of the bit vector.
     * @throws IllegalArgumentException If the new length is negative.
     */
    public void resize(int length) {
        // Optimization.
        if (length == bits.length) {
            return;
        }

        // Precondition check.
        if (length < 0) {
            throw new IllegalArgumentException("Length is negative.");
        }

        // Resize.
        BDD[] newBits = new BDD[length];
        int min = Math.min(bits.length, length);
        System.arraycopy(bits, 0, newBits, 0, min);

        // If new length is larger, set additional bits to 'false'.
        for (int i = min; i < length; i++) {
            newBits[i] = factory.zero();
        }

        // If new length is smaller, free dropped bits.
        for (int i = min; i < bits.length; i++) {
            bits[i].free();
        }

        // Replace the bits.
        bits = newBits;
    }

    /**
     * Adds the given bit vector to this bit vector. This operation returns a new bit vector and carry. The bit vectors
     * on which the operation is performed are not modified or {@link #free freed}.
     *
     * @param other The bit vector to add to this bit vector.
     * @return The result.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public CifBddBitVectorAndCarry add(CifBddBitVector other) {
        // Precondition check.
        if (this.bits.length != other.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        CifBddBitVector rslt = new CifBddBitVector(factory, bits.length);
        BDD carry = factory.zero();
        for (int i = 0; i < bits.length; i++) {
            // rslt[i] = this[i] ^ other[i] ^ carry
            rslt.bits[i] = this.bits[i].xor(other.bits[i]).xorWith(carry.id());

            // carry = (this[i] & other[i]) | (carry & (this[i] | other[i]))
            carry = this.bits[i].and(other.bits[i]).orWith(carry.andWith(this.bits[i].or(other.bits[i])));
        }
        return new CifBddBitVectorAndCarry(rslt, carry);
    }

    /**
     * Subtracts the given bit vector from this bit vector. This operation returns a new bit vector and carry. The bit
     * vectors on which the operation is performed are not modified or {@link #free freed}.
     *
     * @param other The bit vector to subtract from this bit vector.
     * @return The result.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public CifBddBitVectorAndCarry subtract(CifBddBitVector other) {
        // Precondition check.
        if (this.bits.length != other.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        CifBddBitVector rslt = new CifBddBitVector(factory, bits.length);
        BDD carry = factory.zero();
        for (int i = 0; i < bits.length; i++) {
            // rslt[i] = this[i] ^ other[i] ^ carry
            rslt.bits[i] = this.bits[i].xor(other.bits[i]).xorWith(carry.id());

            // carry = (this[n] & other[n] & carry) | (!this[n] & (other[n] | carry))
            BDD tmp1 = other.bits[i].or(carry);
            BDD tmp2 = this.bits[i].apply(tmp1, BDDFactory.less);
            tmp1.free();
            carry = this.bits[i].and(other.bits[i]).andWith(carry).orWith(tmp2);
        }
        return new CifBddBitVectorAndCarry(rslt, carry);
    }

    /**
     * Computes the quotient of dividing this vector (the dividend) by the given value (the divisor). This operation
     * returns a new bit vector. The bit vector on which the operation is performed is not modified or {@link #free
     * freed}.
     *
     * @param value The value by which to divide this bit vector.
     * @return The quotient.
     * @throws IllegalArgumentException If the divisor is not positive.
     * @throws IllegalArgumentException If the divisor doesn't fit within this bit vector.
     */
    public CifBddBitVector div(int value) {
        return divmod(value, true);
    }

    /**
     * Computes the modulus of dividing this vector (the dividend) by the given value (the divisor). This operation
     * returns a new bit vector. The bit vector on which the operation is performed is not modified or {@link #free
     * freed}.
     *
     * @param value The value by which to divide this bit vector.
     * @return The modulus.
     * @throws IllegalArgumentException If the divisor is not positive.
     * @throws IllegalArgumentException If the divisor doesn't fit within this bit vector.
     * @throws IllegalStateException If the highest bit of this vector is not 'false'.
     */
    public CifBddBitVector mod(int value) {
        return divmod(value, false);
    }

    /**
     * Computes the quotient or modulus of dividing this vector (the dividend) by the given value (the divisor). This
     * operation returns a new bit vector. The bit vector on which the operation is performed is not modified or
     * {@link #free freed}.
     *
     * @param value The value by which to divide this bit vector.
     * @param isDiv Whether to compute and return the quotient ({@code true}) or modulus ({@code false}).
     * @return The quotient or modulus.
     * @throws IllegalArgumentException If the divisor is not positive.
     * @throws IllegalArgumentException If the divisor doesn't fit within this bit vector.
     * @throws IllegalStateException If the highest bit of this vector is not 'false', and a modulus is computed.
     */
    public CifBddBitVector divmod(int value, boolean isDiv) {
        // Precondition check. Remaining checks performed as part of the
        // computation.
        if (value <= 0) {
            throw new IllegalArgumentException("Divisor is not positive.");
        }
        if (!isDiv && (bits.length == 0 || !bits[bits.length - 1].isZero())) {
            throw new IllegalStateException("Highest bit is not false.");
        }

        // Compute results.
        CifBddBitVector divisor = create(factory, bits.length);
        try {
            divisor.setInt(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Divisor doesn't fit.");
        }
        CifBddBitVector tmp = createBits(factory, bits.length, false);
        CifBddBitVector tmpRemainder = tmp.shiftLeft(1, bits[bits.length - 1]);
        CifBddBitVector result = shiftLeft(1, factory.zero());

        divModRecursive(divisor, tmpRemainder, result, bits.length);

        divisor.free();
        tmp.free();

        // Return requested result.
        if (isDiv) {
            tmpRemainder.free();
            return result;
        } else {
            CifBddBitVector remainder = tmpRemainder.shiftRight(1, factory.zero());
            tmpRemainder.free();
            result.free();
            return remainder;
        }
    }

    /**
     * Computes the quotient and modulus/remainder of dividing a bit vector (the dividend) by the given other bit vector
     * (the divisor).
     *
     * @param divisor The divisor bit vector. Is not modified.
     * @param remainder The modulus/remainder bit vector, as computed so far. Is modified in-place.
     * @param result The result/quotient bit vector, as computed so far. Is modified in-place.
     * @param step The number of steps to perform.
     */
    private void divModRecursive(CifBddBitVector divisor, CifBddBitVector remainder, CifBddBitVector result, int step) {
        int divLen = divisor.bits.length;
        BDD isSmaller = divisor.lessOrEqual(remainder);
        CifBddBitVector newResult = result.shiftLeft(1, isSmaller);
        CifBddBitVector sub = create(factory, divLen);

        for (int i = 0; i < divLen; i++) {
            sub.bits[i] = isSmaller.ite(divisor.bits[i], factory.zero());
        }

        CifBddBitVectorAndCarry tmp = remainder.subtract(sub);
        CifBddBitVector newRemainder = tmp.vector.shiftLeft(1, result.bits[divLen - 1]);

        if (step > 1) {
            divModRecursive(divisor, newRemainder, newResult, step - 1);
        }

        tmp.vector.free();
        tmp.carry.free();
        sub.free();
        isSmaller.free();

        result.replaceBy(newResult);
        remainder.replaceBy(newRemainder);
    }

    /**
     * Computes the bit vector resulting from shifting this bit vector {@code amount} bits to the left. The given
     * {@code carry} is shifted in. This operation returns a new bit vector. The bit vector on which the operation is
     * performed is not modified or {@link #free freed}.
     *
     * @param amount The amount of bits to shift.
     * @param carry The carry to shift in. Only copies are used.
     * @return The shifted bit vector.
     * @throws IllegalArgumentException If the shift amount is negative.
     */
    public CifBddBitVector shiftLeft(int amount, BDD carry) {
        // Precondition check.
        if (amount < 0) {
            throw new IllegalArgumentException("Amount is negative.");
        }

        // Compute result.
        CifBddBitVector result = new CifBddBitVector(factory, bits.length);

        int min = Math.min(bits.length, amount);
        for (int i = 0; i < min; i++) {
            result.bits[i] = carry.id();
        }
        for (int i = min; i < bits.length; i++) {
            result.bits[i] = bits[i - amount].id();
        }

        return result;
    }

    /**
     * Computes the bit vector resulting from shifting this bit vector {@code amount} bits to the right. The given
     * {@code carry} is shifted in. This operation returns a new bit vector. The bit vector on which the operation is
     * performed is not modified or {@link #free freed}.
     *
     * @param amount The amount of bits to shift.
     * @param carry The carry to shift in. Only copies are used.
     * @return The shifted bit vector.
     * @throws IllegalArgumentException If the shift amount is negative.
     */
    public CifBddBitVector shiftRight(int amount, BDD carry) {
        // Precondition check.
        if (amount < 0) {
            throw new IllegalArgumentException("Amount is negative.");
        }

        // Compute result.
        CifBddBitVector result = new CifBddBitVector(factory, bits.length);

        int max = Math.max(0, bits.length - amount);
        for (int i = max; i < bits.length; i++) {
            result.bits[i] = carry.id();
        }
        for (int i = 0; i < max; i++) {
            result.bits[i] = bits[i + amount].id();
        }

        return result;
    }

    /**
     * Computes an if-then-else with this bit vector as the 'then' value, and a given 'if' condition and 'else' value.
     * This operation returns a new bit vector. The bit vectors and condition on which the operation is performed are
     * not modified or {@link #free freed}.
     *
     * @param elseVector The 'else' bit vector.
     * @param condition The 'if' condition.
     * @return The result.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public CifBddBitVector ifThenElse(CifBddBitVector elseVector, BDD condition) {
        // Precondition check.
        if (this.bits.length != elseVector.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        CifBddBitVector rslt = new CifBddBitVector(factory, bits.length);
        for (int i = 0; i < bits.length; i++) {
            rslt.bits[i] = condition.ite(this.getBit(i), elseVector.getBit(i));
        }
        return rslt;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be strictly less than the given bit
     * vector. This operation returns a new BDD. The bit vectors on which the operation is performed are not modified or
     * {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be strictly less than the given bit
     *     vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD lessThan(CifBddBitVector other) {
        // Precondition check.
        if (this.bits.length != other.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        BDD rslt = factory.zero();
        for (int i = 0; i < bits.length; i++) {
            // rslt = (!this[i] & other[i]) | biimp(this[i], other[i]) & rslt
            BDD lt = this.bits[i].apply(other.bits[i], BDDFactory.less);
            BDD eq = this.bits[i].biimp(other.bits[i]);
            rslt = lt.orWith(eq.andWith(rslt));
        }
        return rslt;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be less than or equal to the given
     * bit vector. This operation returns a new BDD. The bit vectors on which the operation is performed are not
     * modified or {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be less than or equal to the given
     *     bit vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD lessOrEqual(CifBddBitVector other) {
        // Precondition check.
        if (this.bits.length != other.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        BDD rslt = factory.one();
        for (int i = 0; i < bits.length; i++) {
            // rslt = (!this[i] & other[i]) | biimp(this[i], other[i]) & rslt
            BDD lt = this.bits[i].apply(other.bits[i], BDDFactory.less);
            BDD eq = this.bits[i].biimp(other.bits[i]);
            rslt = lt.orWith(eq.andWith(rslt));
        }
        return rslt;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be strictly greater than the given
     * bit vector. This operation returns a new BDD. The bit vectors on which the operation is performed are not
     * modified or {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be strictly greater than the given
     *     bit vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD greaterThan(CifBddBitVector other) {
        BDD le = lessOrEqual(other);
        BDD gt = le.not();
        le.free();
        return gt;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be greater than or equal to the
     * given bit vector. This operation returns a new BDD. The bit vectors on which the operation is performed are not
     * modified or {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be greater than or equal to the
     *     given bit vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD greaterOrEqual(CifBddBitVector other) {
        BDD lt = lessThan(other);
        BDD ge = lt.not();
        lt.free();
        return ge;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be equal to the given bit vector.
     * This operation returns a new BDD. The bit vectors on which the operation is performed are not modified or
     * {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be equal to the given bit vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD equalTo(CifBddBitVector other) {
        // Precondition check.
        if (this.bits.length != other.bits.length) {
            throw new IllegalArgumentException("Different lengths.");
        }

        // Compute result.
        BDD eq = factory.one();
        for (int i = 0; i < bits.length; i++) {
            BDD bit = this.bits[i].biimp(other.bits[i]);
            eq = eq.andWith(bit);
        }
        return eq;
    }

    /**
     * Returns a BDD indicating the conditions that must hold for this bit vector to be unequal to the given bit vector.
     * This operation returns a new BDD. The bit vectors on which the operation is performed are not modified or
     * {@link #free freed}.
     *
     * @param other The bit vector to compare against.
     * @return A BDD indicating the conditions that must hold for this bit vector to be unequal to the given bit vector.
     * @throws IllegalArgumentException If this bit vector and the given bit vector have a different length.
     */
    public BDD unequalTo(CifBddBitVector other) {
        BDD eq = this.equalTo(other);
        BDD uneq = eq.not();
        eq.free();
        return uneq;
    }

    /**
     * Frees the bit vector, {@link BDD#free freeing} the BDDs representing the bits. The bit vector should not be used
     * after calling this method.
     */
    public void free() {
        for (int i = 0; i < bits.length; i++) {
            bits[i].free();
        }
        factory = null;
        bits = null;
    }

    @Override
    public String toString() {
        if (bits == null) {
            return "freed";
        }
        return Arrays.toString(bits);
    }
}
