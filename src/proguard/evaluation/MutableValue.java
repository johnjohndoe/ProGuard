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
package proguard.evaluation;

import proguard.evaluation.value.*;

/**
 * This Value is a mutable wrapper for other Value instances.
 * Its generalization method affects the contained Value as a side-effect.
 */
class MutableValue extends Category1Value
{
    private Value containedValue;


    /**
     * Generalizes the contained value with the given value.
     * @param otherContainedValue the other value.
     */
    public void generalizeContainedValue(Value otherContainedValue)
    {
        MutableValue lastMutableValue  = lastMutableValue();
        Value        lastContainedValue= lastMutableValue.containedValue;

        lastMutableValue.containedValue =
            lastContainedValue == null ? otherContainedValue :
                                         otherContainedValue.generalize(lastContainedValue);
    }


    /**
     * Sets the contained value.
     */
    public void setContainedValue(Value containedValue)
    {
        lastMutableValue().containedValue = containedValue;
    }


    /**
     * Returns the contained value.
     */
    public Value getContainedValue()
    {
        return lastMutableValue().containedValue;
    }


    // Implementations for Value.

    public Value generalize(Value other)
    {
        MutableValue otherMutableValue = (MutableValue)other;

        MutableValue thisLastMutableValue  = this.lastMutableValue();
        MutableValue otherLastMutableValue = otherMutableValue.lastMutableValue();

        Value thisLastContainedValue  = thisLastMutableValue.containedValue;
        Value otherLastContainedValue = otherLastMutableValue.containedValue;

        if (thisLastMutableValue != otherLastMutableValue)
        {
            otherLastMutableValue.containedValue = thisLastMutableValue;
        }

        thisLastMutableValue.containedValue =
            thisLastContainedValue  == null ? otherLastContainedValue :
            otherLastContainedValue == null ? thisLastContainedValue  :
                                              thisLastContainedValue.generalize(otherLastContainedValue);
        return thisLastMutableValue;
    }


    public int computationalType()
    {
        return 0;
    }

    public final String internalType()
    {
        return null;
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        if (object == null ||
            this.getClass() != object.getClass())
        {
            return false;
        }

        MutableValue other = (MutableValue)object;
        Value thisContainedValue  = this.getContainedValue();
        Value otherContainedValue = other.getContainedValue();
        return thisContainedValue == null ?
            otherContainedValue == null :
            thisContainedValue.equals(otherContainedValue);
    }


    public int hashCode()
    {
        Value containedValue  = getContainedValue();
        return this.getClass().hashCode() ^
               (containedValue == null ? 0 : containedValue.hashCode());
    }


    public String toString()
    {
        return containedValue == null ? "none" : containedValue.toString();
    }


    // Small utility methods.

    public MutableValue lastMutableValue()
    {
        MutableValue mutableValue = this;

        while (mutableValue.containedValue instanceof MutableValue)
        {
            mutableValue = (MutableValue)mutableValue.containedValue;
        }

        return mutableValue;
    }
}
