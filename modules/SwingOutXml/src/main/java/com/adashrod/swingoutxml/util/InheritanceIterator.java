package com.adashrod.swingoutxml.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An {@link java.util.Iterator} that, given a class, will sequentially return all superclass, interfaces, and
 * superinterfaces of the class, effectively everything that the class can be cast to. The one exception to this behavior
 * is for the primitive wrapper types.
 *
 * Note for primitive wrappers:
 * For primitive wrappers, repeatedly calling {@link Class#getSuperclass()} (and iterating over {@link Class#getInterfaces()}}
 * will yield a sequence of Integer -> Comparable -> Number -> Serializable -> Object. The behavior of this class
 * is to instead do Integer -> Comparable -> int -> Number -> Serializable -> Object. This is for consumers of the
 * iteration that want the primitive types included in the iteration.
 *
 * It is guaranteed at least one iteration because the first iteration is always the same Class that was passed to the
 * constructor
 */
public class InheritanceIterator implements Iterator<Class<?>> {
    private static final Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
    private static final Map<Class<?>, Class<?>> primitiveInverseMap = new HashMap<>();

    static {
        primitiveMap.put(Boolean.class, boolean.class);
        primitiveMap.put(Character.class, char.class);
        primitiveMap.put(Byte.class, byte.class);
        primitiveMap.put(Short.class, short.class);
        primitiveMap.put(Integer.class, int.class);
        primitiveMap.put(Long.class, long.class);
        primitiveMap.put(Float.class, float.class);
        primitiveMap.put(Double.class, double.class);

        primitiveInverseMap.put(boolean.class, Boolean.class);
        primitiveInverseMap.put(char.class, Character.class);
        primitiveInverseMap.put(byte.class, Byte.class);
        primitiveInverseMap.put(short.class, Short.class);
        primitiveInverseMap.put(int.class, Integer.class);
        primitiveInverseMap.put(long.class, Long.class);
        primitiveInverseMap.put(float.class, Float.class);
        primitiveInverseMap.put(double.class, Double.class);
    }

    private final Collection<Class<?>> returnedClasses = new HashSet<>();
    private final Class<?> startingClass;
    private Class<?> currentClass;
    private final List<Class<?>> currentInterfaces = new LinkedList<>();
    private boolean firstIteration;
    private boolean interfacesChecked;

    public InheritanceIterator(final Class<?> startingClass) {
        this.startingClass = startingClass;
        reset();
    }

    @Override
    public boolean hasNext() {
        return firstIteration || currentClass.getSuperclass() != null || primitiveMap.values().contains(currentClass);
    }

    @Override
    public Class<?> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Iteration is done");
        }
        if (firstIteration) {
            firstIteration = false;
            return doNext(currentClass);
        }
        if (!interfacesChecked && currentInterfaces.isEmpty()) {
            currentInterfaces.addAll(getAllInterfaces(currentClass));
            currentInterfaces.removeAll(returnedClasses);
        }
        if (!currentInterfaces.isEmpty()) {
            // about to empty the queue, i.e. all interfaces of currentClass will have been returned after this exits
            if (currentInterfaces.size() == 1) {
                interfacesChecked = true;
            }
            return doNext(currentInterfaces.remove(0));
        }
        // already returned all interfaces of the currentClass, set to false to possibly return interfaces of the
        // superClass, which is about to be returned here
        interfacesChecked = false;
        currentClass = getSuperClass(currentClass);
        return doNext(currentClass);
    }

    /**
     * Resets the iterator to its initial state
     */
    public void reset() {
        currentClass = startingClass;
        firstIteration = true;
        interfacesChecked = false;
        currentInterfaces.clear();
        returnedClasses.clear();
    }

    private Class<?> doNext(final Class<?> toReturn) {
        returnedClasses.add(toReturn);
        return toReturn;
    }

    /**
     * Returns the superclass of klass with special behavior for primitives and primitive wrappers. In the case
     * where a primitive is auto-boxed to its wrapper class, repeatedly calling {@link Class#getSuperclass()} would
     * go Integer -> Number -> Object -> null, when it should have gone int -> null. This function changes that path
     * to be Integer -> int -> Number -> Object -> null so that construction signatures with primitive formal types
     * will not be missed.
     * @param klass a class to get a superclass of
     * @return the superclass, corresponding primitive, or corresponding wrapper's superclass
     */
    private Class<?> getSuperClass(final Class<?> klass) {
        if (primitiveMap.keySet().contains(klass)) {
            return primitiveMap.get(klass);
        } else if (primitiveMap.values().contains(klass)) {
            return primitiveInverseMap.get(klass).getSuperclass();
        } else {
            return klass.getSuperclass();
        }
    }

    private static Collection<Class<?>> getAllInterfaces(final Class<?> c) {
        final Collection<Class<?>> result = new LinkedHashSet<>();
        final Collection<Class<?>> directInterfaces = Arrays.asList(c.getInterfaces());
        result.addAll(directInterfaces);

        for (final Class<?> directInterface: directInterfaces) {
            result.addAll(getAllInterfaces(directInterface));
        }

        return result;
    }
}
