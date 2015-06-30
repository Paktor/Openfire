package org.jivesoftware.util;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class ImmutableProperties extends Properties {
    private ImmutableMap<Object, Object> map;
    private int size = 0;

    public ImmutableProperties(Properties properties) {
        //copy to avoid sync with backed object calls
        map = ImmutableMap.copyOf(properties);
        size = map.size();
    }

    @Override
    public void list(PrintWriter out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list(PrintStream out) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> stringPropertyNames() {
        Set<String> names = new HashSet<>();
        for (Object name : map.keySet()) { // java 7 ((
            if (name instanceof String) {
                names.add((String) name);
            }
        }
        return names;
    }

    @Override
    public Enumeration<?> propertyNames() {
        Vector<Object> vector = new Vector<>(size);
        vector.addAll(map.keySet());
        return vector.elements();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return (String) map.getOrDefault(key, defaultValue);
    }

    @Override
    public String getProperty(String key) {
        return (String) map.get(key);
    }

    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void save(OutputStream out, String comments) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void load(InputStream inStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void load(Reader reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setProperty(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size <= 0;
    }

    @Override
    public Enumeration<Object> keys() {
        Vector<Object> vector = new Vector<>(size);
        vector.addAll(map.keySet());
        return vector.elements();
    }

    @Override
    public Enumeration<Object> elements() {
        Vector<Object> vector = new Vector<>(size);
        vector.addAll(map.values());
        return vector.elements();
    }

    @Override
    public boolean contains(Object value) {
        return map.containsValue(value);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<?, ?> t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public Set<Object> keySet() {
        return map.keySet();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public void forEach(BiConsumer<? super Object, ? super Object> action) {
        map.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replace(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }
}
