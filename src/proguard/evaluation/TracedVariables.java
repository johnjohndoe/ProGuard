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

import proguard.evaluation.value.Value;

/**
 * This Variables class saves additional information with variables, to keep track
 * of their origins.
 * <p>
 * The Variables class stores a given producer Value along with each Value it
 * stores. It then generalizes a given collected Value with the producer Value
 * of each Value it loads. The producer Value and the initial collected Value
 * can be set; the generalized collected Value can be retrieved.
 * <p>
 * In addition, an initialization index can be reset and retrieved, pointing
 * to the most recent variable that has been initialized by a store operation.
 *
 * @author Eric Lafortune
 */
public class TracedVariables extends Variables
{
    public static final int NONE = -1;


    private Value     producerValue;
    private Value     collectedProducerValue;
    private final Variables producerVariables;
    private final Variables consumerVariables;
    private int       initializationIndex;


    public TracedVariables(int size)
    {
        super(size);

        producerVariables = new Variables(size);
        consumerVariables = new Variables(size);
    }


    public TracedVariables(TracedVariables tracedVariables)
    {
        super(tracedVariables);

        producerVariables = new Variables(tracedVariables.producerVariables);
        consumerVariables = new Variables(tracedVariables.consumerVariables);
    }


    /**
     * Sets the Value that will be stored along with all store instructions.
     */
    public void setProducerValue(Value producerValue)
    {
        this.producerValue = producerValue;
    }


    /**
     * Sets the initial Value with which all values stored along with load
     * instructions will be generalized.
     */
    public void setCollectedProducerValue(Value collectedProducerValue)
    {
        this.collectedProducerValue = collectedProducerValue;
    }

    public Value getCollectedProducerValue()
    {
        return collectedProducerValue;
    }


    /**
     * Resets the initialization index.
     */
    public void resetInitialization()
    {
        initializationIndex = NONE;
    }

    public int getInitializationIndex()
    {
        return initializationIndex;
    }


    /**
     * Gets the producer Value for the specified variable, without disturbing it.
     * @param index the variable index.
     * @return the producer value of the given variable.
     */
    public Value getProducerValue(int index)
    {
        return producerVariables.getValue(index);
    }


    /**
     * Sets the given producer Value for the specified variable, without
     * disturbing it.
     * @param index the variable index.
     * @param value the producer value to set.
     */
    public void setProducerValue(int index, Value value)
    {
        producerVariables.store(index, value);
    }


    /**
     * Gets the consumer Value for the specified variable, without disturbing it.
     * @param index the variable index.
     * @return the producer value of the given variable.
     */
    public Value getConsumerValue(int index)
    {
        return ((MutableValue)consumerVariables.getValue(index)).getContainedValue();
    }


    /**
     * Sets the specified consumer Value for the given variable, without
     * disturbing it.
     * @param index the variable index.
     * @param value the consumer value to set.
     */
    public void setConsumerValue(int index, Value value)
    {
        ((MutableValue)consumerVariables.getValue(index)).setContainedValue(value);
        consumerVariables.store(index, new MutableValue());
    }


    // Implementations for Variables.

    public void reset(int size)
    {
        super.reset(size);

        producerVariables.reset(size);
        consumerVariables.reset(size);
    }

    public void initialize(TracedVariables other)
    {
        super.initialize(other);

        producerVariables.initialize(other.producerVariables);
        consumerVariables.initialize(other.consumerVariables);
    }

    public boolean generalize(TracedVariables other,
                              boolean         clearConflictingOtherVariables)
    {
        boolean variablesChanged = super.generalize(other, clearConflictingOtherVariables);
        boolean producersChanged = producerVariables.generalize(other.producerVariables, clearConflictingOtherVariables);
        /* consumerVariables.generalize(other.consumerVariables)*/

        // Clear any traces if a variable has become null.
        if (variablesChanged)
        {
            for (int index = 0; index < size; index++)
            {
                if (values[index] == null)
                {
                    producerVariables.values[index] = null;
                    consumerVariables.values[index] = null;

                    if (clearConflictingOtherVariables)
                    {
                        other.producerVariables.values[index] = null;
                        other.consumerVariables.values[index] = null;
                    }
                }
            }
        }

        return variablesChanged || producersChanged;
    }


    public void store(int index, Value value)
    {
        // Is this store operation an initialization of the variable?
        Value previousValue = super.load(index);
        if (previousValue == null ||
            previousValue.computationalType() != value.computationalType())
        {
            initializationIndex = index;
        }

        // Store the value itself in the variable.
        super.store(index, value);

        // Store the producer value in its producer variable.
        producerVariables.store(index, producerValue);

        // Reserve a space for the consumer value.
        MutableValue mutableValue = new MutableValue();
        consumerVariables.store(index, mutableValue);

        // Account for the extra space required by Category 2 values.
        if (value.isCategory2())
        {
            producerVariables.store(index+1, producerValue);
            consumerVariables.store(index+1, mutableValue);
        }
    }

    public Value load(int index)
    {
        // Load and accumulate the producer value of the variable.
        if (collectedProducerValue != null)
        {
            collectedProducerValue = collectedProducerValue.generalize(producerVariables.load(index));
        }

        // Generalize the consumer value of the variable.
        ((MutableValue)consumerVariables.load(index)).generalizeContainedValue(producerValue);

        // Return the value itself.
        return super.load(index);
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        if (object == null ||
            this.getClass() != object.getClass())
        {
            return false;
        }

        TracedVariables other = (TracedVariables)object;

        return super.equals(object) &&
               this.producerVariables.equals(other.producerVariables) /*&&
               this.consumerVariables.equals(other.consumerVariables)*/;
    }


    public int hashCode()
    {
        return super.hashCode() ^
               producerVariables.hashCode() /*^
               consumerVariables.hashCode()*/;
    }


    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        for (int index = 0; index < this.size(); index++)
        {
            Value value         = this.values[index];
            Value producerValue = producerVariables.getValue(index);
            Value consumerValue = consumerVariables.getValue(index);
            buffer = buffer.append('[')
                           .append(producerValue == null ? "empty" : producerValue.toString())
                           .append('>')
                           .append(value         == null ? "empty" : value.toString())
                           .append('>')
                           .append(consumerValue == null ? "empty" : consumerValue.toString())
                           .append(']');
        }

        return buffer.toString();
    }
}
