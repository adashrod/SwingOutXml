package com.adashrod.swingoutxml.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Given a varargs list of Classes in the constructor, this iterates over all of the permutations of all of those Classes'
 * superclasses, interfaces, and superinterfaces.
 * E.g. iterating over new MultiClassInheritanceIterator(Integer.class, Long.class) yields:
 * [class java.lang.Integer,        class java.lang.Long]
 * [class java.lang.Integer,        interface java.lang.Comparable]
 * [class java.lang.Integer,        long]
 * [class java.lang.Integer,        class java.lang.Number]
 * [class java.lang.Integer,        interface java.io.Serializable]
 * [class java.lang.Integer,        class java.lang.Object]
 * [interface java.lang.Comparable, class java.lang.Long]
 * [interface java.lang.Comparable, interface java.lang.Comparable]
 * [interface java.lang.Comparable, long]
 * [interface java.lang.Comparable, class java.lang.Number]
 * [interface java.lang.Comparable, interface java.io.Serializable]
 * [interface java.lang.Comparable, class java.lang.Object]
 * [int,                            class java.lang.Long]
 * [int,                            interface java.lang.Comparable]
 * [int,                            long]
 * [int,                            class java.lang.Number]
 * [int,                            interface java.io.Serializable]
 * [int,                            class java.lang.Object]
 * [class java.lang.Number,         class java.lang.Long]
 * [class java.lang.Number,         interface java.lang.Comparable]
 * [class java.lang.Number,         long]
 * [class java.lang.Number,         class java.lang.Number]
 * [class java.lang.Number,         interface java.io.Serializable]
 * [class java.lang.Number,         class java.lang.Object]
 * [interface java.io.Serializable, class java.lang.Long]
 * [interface java.io.Serializable, interface java.lang.Comparable]
 * [interface java.io.Serializable, long]
 * [interface java.io.Serializable, class java.lang.Number]
 * [interface java.io.Serializable, interface java.io.Serializable]
 * [interface java.io.Serializable, class java.lang.Object]
 * [class java.lang.Object,         class java.lang.Long]
 * [class java.lang.Object,         interface java.lang.Comparable]
 * [class java.lang.Object,         long]
 * [class java.lang.Object,         class java.lang.Number]
 * [class java.lang.Object,         interface java.io.Serializable]
 * [class java.lang.Object,         class java.lang.Object]
 * Like incrementing digits in a number: 000, 001, 002, ... 010, 011, 012, etc, each class is "incremented" (replaced
 * with a superclass, interface, or superinterface, until it rolls over, then the next class is "incremented". This
 * repeats until the entire sequence has rolled over to the original state
 */
public class MultiClassInheritanceIterator implements Iterator<Class<?>[]>{
    private Class<?>[] currentArray;
    private final Class<?>[] nextArray;
    private final List<InheritanceIterator> iterators = new ArrayList<>();
    private boolean firstIteration;
    private boolean done;

    public MultiClassInheritanceIterator(final Class<?>... classes) {
        currentArray = new Class<?>[classes.length];
        nextArray = new Class<?>[classes.length];
        for (int i = 0; i < classes.length; i++) {
            final Class<?> c = classes[i];
            final InheritanceIterator iterator = new InheritanceIterator(c);
            iterators.add(iterator);
            nextArray[i] = iterator.next();
        }
        firstIteration = true;
        done = false;
    }

    @Override
    public boolean hasNext() {
        return firstIteration || !done;
    }

    @Override
    public Class<?>[] next() {
        if (done) {
            throw new NoSuchElementException("Iteration is done");
        }
        if (firstIteration) {
            firstIteration = false;
        }
        increment();
        return currentArray;
    }

    private void increment() {
        currentArray = Arrays.copyOf(nextArray, nextArray.length);
        for (int i = iterators.size() - 1; i >= 0; i--) {
            if (iterators.get(i).hasNext()) {
                nextArray[i] = iterators.get(i).next();
                done = false;
                return;
            } else {
                iterators.get(i).reset();
                nextArray[i] = iterators.get(i).next();
            }
        }
        // reached when the "most significant class" (the last one to be incremented) no longer hasNext(), i.e. all
        // iteration has completed and the list of classes overflowed to the original state
        done = true;
    }
}
