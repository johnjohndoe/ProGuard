/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.evaluation.value;

import proguard.classfile.ClassConstants;

/**
 * This class represents a partially evaluated float value.
 *
 * @author Eric Lafortune
 */
public class FloatValue extends Category1Value
{
    /**
     * Returns the specific float value, if applicable.
     */
    public float value()
    {
        return 0f;
    }


    // Basic binary methods.

    /**
     * Returns the generalization of this FloatValue and the given other
     * FloatValue.
     */
    public FloatValue generalize(FloatValue other)
    {
        return this;
    }


    /**
     * Returns the sum of this FloatValue and the given FloatValue.
     */
    public FloatValue add(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the difference of this FloatValue and the given FloatValue.
     */
    public FloatValue subtract(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the difference of the given FloatValue and this FloatValue.
     */
    public FloatValue subtractFrom(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the product of this FloatValue and the given FloatValue.
     */
    public FloatValue multiply(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the quotient of this FloatValue and the given FloatValue.
     */
    public FloatValue divide(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the quotient of the given FloatValue and this FloatValue.
     */
    public FloatValue divideOf(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the remainder of this FloatValue divided by the given FloatValue.
     */
    public FloatValue remainder(FloatValue other)
    {
        return this;
    }

    /**
     * Returns the remainder of the given FloatValue divided by this FloatValue.
     */
    public FloatValue remainderOf(FloatValue other)
    {
        return this;
    }

    /**
     * Returns an IntegerValue with value -1, 0, or 1, if this FloatValue is
     * less than, equal to, or greater than the given FloatValue, respectively.
     */
    public IntegerValue compare(FloatValue other, ValueFactory valueFactory)
    {
        return valueFactory.createIntegerValue();
    }


    // Derived binary methods.

    /**
     * Returns an IntegerValue with value 1, 0, or -1, if this FloatValue is
     * less than, equal to, or greater than the given FloatValue, respectively.
     */
    public final IntegerValue compareReverse(FloatValue other, ValueFactory valueFactory)
    {
        return compare(other, valueFactory).negate();
    }


    // Basic unary methods.

    /**
     * Returns the negated value of this FloatValue.
     */
    public FloatValue negate()
    {
        return this;
    }

    /**
     * Converts this FloatValue to an IntegerValue.
     */
    public IntegerValue convertToInteger(ValueFactory valueFactory)
    {
        return valueFactory.createIntegerValue();
    }

    /**
     * Converts this FloatValue to a LongValue.
     */
    public LongValue convertToLong(ValueFactory valueFactory)
    {
        return valueFactory.createLongValue();
    }

    /**
     * Converts this FloatValue to a DoubleValue.
     */
    public DoubleValue convertToDouble(ValueFactory valueFactory)
    {
        return valueFactory.createDoubleValue();
    }


    // Similar binary methods, but this time with more specific arguments.

    /**
     * Returns the generalization of this FloatValue and the given other
     * SpecificFloatValue.
     */
    public FloatValue generalize(SpecificFloatValue other)
    {
        return this;
    }


    /**
     * Returns the sum of this FloatValue and the given SpecificFloatValue.
     */
    public FloatValue add(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the difference of this FloatValue and the given SpecificFloatValue.
     */
    public FloatValue subtract(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the difference of the given SpecificFloatValue and this FloatValue.
     */
    public FloatValue subtractFrom(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the product of this FloatValue and the given SpecificFloatValue.
     */
    public FloatValue multiply(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the quotient of this FloatValue and the given SpecificFloatValue.
     */
    public FloatValue divide(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the quotient of the given SpecificFloatValue and this
     * FloatValue.
     */
    public FloatValue divideOf(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the remainder of this FloatValue divided by the given
     * SpecificFloatValue.
     */
    public FloatValue remainder(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns the remainder of the given SpecificFloatValue and this
     * FloatValue.
     */
    public FloatValue remainderOf(SpecificFloatValue other)
    {
        return this;
    }

    /**
     * Returns an IntegerValue with value -1, 0, or 1, if this FloatValue is
     * less than, equal to, or greater than the given SpecificFloatValue,
     * respectively.
     */
    public IntegerValue compare(SpecificFloatValue other, ValueFactory valueFactory)
    {
        return valueFactory.createIntegerValue();
    }


    // Derived binary methods.

    /**
     * Returns an IntegerValue with value 1, 0, or -1, if this FloatValue is
     * less than, equal to, or greater than the given SpecificFloatValue,
     * respectively.
     */
    public final IntegerValue compareReverse(SpecificFloatValue other, ValueFactory valueFactory)
    {
        return compare(other, valueFactory).negate();
    }


    // Implementations for Value.

    public final FloatValue floatValue()
    {
        return this;
    }

    public final Value generalize(Value other)
    {
        return this.generalize(other.floatValue());
    }

    public final int computationalType()
    {
        return TYPE_FLOAT;
    }

    public final String internalType()
    {
        return String.valueOf(ClassConstants.INTERNAL_TYPE_FLOAT);
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        return object != null &&
               this.getClass() == object.getClass();
    }


    public int hashCode()
    {
        return this.getClass().hashCode();
    }


    public String toString()
    {
        return "f";
    }
}
