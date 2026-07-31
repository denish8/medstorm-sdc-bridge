package org.jvnet.jaxb.lang;

import org.jvnet.jaxb2_commons.lang.DefaultCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ItemObjectLocator;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.PropertyObjectLocator;

import jakarta.xml.bind.ValidationEventLocator;
import org.w3c.dom.Node;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Bridges our shim org.jvnet.jaxb.locator.* to jaxb2-commons locator types,
 * and implements the legacy jaxb-basics CopyStrategy. It also guards against
 * binary-incompatible CopyTo implementations (avoids AbstractMethodError).
 */
public final class JAXBCopyStrategy extends DefaultCopyStrategy implements CopyStrategy {

    private static final JAXBCopyStrategy INSTANCE = new JAXBCopyStrategy();
    public static JAXBCopyStrategy getInstance() { return INSTANCE; }
    private JAXBCopyStrategy() {}

    /* ========= Legacy jaxb-basics CopyStrategy ========= */

    /** Old signature; commons doesn't have the locator overload. */
    public Boolean shouldBeCopiedAndSet(org.jvnet.jaxb.locator.ObjectLocator locator, boolean value) {
        return value ? Boolean.TRUE : Boolean.FALSE;
    }

    /** Old-locator overload bridged to our commons override. */
    public Object copy(org.jvnet.jaxb.locator.ObjectLocator locator, Object value) {
        return copy(toCommons(locator), value);
    }

    /** No-locator legacy overload. */
    public Object copy(Object value) {
        return copy((ObjectLocator) null, value);
    }

    /** Some jaxb-basics variants declare this tri-arg method. */
    public Object copy(org.jvnet.jaxb.locator.ObjectLocator locator, Object value, boolean valueSet) {
        if (!Boolean.TRUE.equals(shouldBeCopiedAndSet(locator, valueSet))) {
            return null;
        }
        return copy(locator, value);
    }

    /* ========= jaxb2-commons override with HARD guard ========= */

    /**
     * If the value truly supports the current CopyTo ABI, delegate to DefaultCopyStrategy (deep copy).
     * Otherwise deep-copy arrays/collections/maps ourselves and return POJOs as-is.
     */
    @Override
    public Object copy(ObjectLocator locator, Object value) {
        if (value == null) return null;

        // Try deep copy via CopyTo *only if* the resolved method exists at runtime.
        if (value instanceof org.jvnet.jaxb2_commons.lang.CopyTo) {
            if (hasCopyToCurrentAbi(value.getClass())) {
                try {
                    return super.copy(locator, value);
                } catch (AbstractMethodError | NoSuchMethodError e) {
                    // Binary-incompatible CopyTo – fall through to safe copy path.
                }
            }
        }

        // Arrays (primitive or reference)
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int len = Array.getLength(value);
            Object copy = Array.newInstance(type.getComponentType(), len);
            for (int i = 0; i < len; i++) {
                Object elem = Array.get(value, i);
                Array.set(copy, i, copy(locator, elem));
            }
            return copy;
        }

        // Collections
        if (value instanceof Collection<?> col) {
            Collection<Object> out = instantiateCollection(col);
            for (Object e : col) out.add(copy(locator, e));
            return out;
        }

        // Maps
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> out = instantiateMap(map);
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object k = copy(locator, e.getKey());
                Object v = copy(locator, e.getValue());
                out.put(k, v);
            }
            return out;
        }

        // Known “safe” immutables → return as-is
        if (isKnownImmutable(type)) return value;

        // Mutable-but-common leafs – shallow duplicates where reasonable
        if (value instanceof Date d) return new Date(d.getTime());

        // JAXB/JDK beans without compatible CopyTo: treat as opaque.
        return value;
    }

    /** Verify the *current* jaxb2-commons ABI exists on this class to avoid AbstractMethodError. */
    private static boolean hasCopyToCurrentAbi(Class<?> c) {
        try {
            // Expected signature since jaxb2-basics 0.6.x
            c.getMethod("copyTo",
                    ObjectLocator.class,
                    Object.class,
                    org.jvnet.jaxb2_commons.lang.CopyStrategy.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean isKnownImmutable(Class<?> c) {
        return c.isEnum()
                || Number.class.isAssignableFrom(c)
                || CharSequence.class.isAssignableFrom(c)
                || UUID.class.equals(c)
                || Boolean.class.equals(c)
                || Character.class.equals(c)
                || Class.class.equals(c);
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> instantiateCollection(Collection<?> src) {
        try {
            return (Collection<Object>) src.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            return new ArrayList<>(Math.max(16, src.size()));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> instantiateMap(Map<?, ?> src) {
        try {
            return (Map<Object, Object>) src.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            return new LinkedHashMap<>(Math.max(16, (int)(src.size() / 0.75f) + 1));
        }
    }

    /* ========= Locator adaptation (old -> commons) ========= */

    public static ObjectLocator toCommons(org.jvnet.jaxb.locator.ObjectLocator l) {
        if (l == null) return null;
        ObjectLocator parent = toCommons(l.getParentLocator());
        if (l.getIndex() >= 0) {
            return new CommonsItemLocator(parent, l.getIndex(), l.getObject());
        }
        return new CommonsPropertyLocator(parent, l.getPropertyName(), l.getObject());
    }

    /** Base adapter for jaxb2-commons ObjectLocator (and Jakarta ValidationEventLocator). */
    private static abstract class CommonsLocatorAdapter
            implements ObjectLocator, ValidationEventLocator {

        protected final ObjectLocator parent;
        protected final String propertyName; // null for item
        protected final int index;           // -1 for property
        protected final Object object;

        private CommonsLocatorAdapter(ObjectLocator parent, String propertyName, int index, Object object) {
            this.parent = parent;
            this.propertyName = propertyName;
            this.index = index;
            this.object = object;
        }

        /* ---- ObjectLocator (jaxb2-commons) ---- */
        public ObjectLocator getParentLocator() { return parent; }
        public Object getObject() { return object; }

        // jaxb2-commons expects an array here.
        public ObjectLocator[] getPath() {
            LinkedList<ObjectLocator> path = new LinkedList<>();
            ObjectLocator cur = this;
            while (cur != null) {
                path.addFirst(cur);
                cur = cur.getParentLocator();
            }
            return path.toArray(new ObjectLocator[0]);
        }

        public String getPathAsString() {
            String base = (parent != null) ? parent.getPathAsString() : "";
            if (propertyName != null) {
                return base.isEmpty() ? propertyName : base + "." + propertyName;
            } else {
                return base + "[" + index + "]";
            }
        }

        /* ---- “Reportable”-ish hooks (some jaxb2-commons variants require these) ---- */
        public String getMessage() { return getPathAsString(); }
        public String getMessage(ResourceBundle bundle) { return getPathAsString(); }
        public Object[] getMessageParameters() { return new Object[] { getPathAsString() }; }
        public String getMessageCode() { return "locator.path"; }

        /* ---- ValidationEventLocator (Jakarta) ---- */
        public URL  getURL()          { return null; }
        public int  getLineNumber()   { return -1; }
        public int  getColumnNumber() { return -1; }
        public int  getOffset()       { return -1; }
        public Node getNode()         { return null; }
    }

    /** Item variant (implements ItemObjectLocator). */
    private static final class CommonsItemLocator extends CommonsLocatorAdapter implements ItemObjectLocator {
        private CommonsItemLocator(ObjectLocator parent, int index, Object object) {
            super(parent, null, index, object);
        }
        public int getIndex() { return index; }
        public ItemObjectLocator item(int idx, Object val) { return new CommonsItemLocator(this, idx, val); }
        public PropertyObjectLocator property(String name, Object val) { return new CommonsPropertyLocator(this, name, val); }
        public String toString() { return getPathAsString(); }
    }

    /** Property variant (implements PropertyObjectLocator). */
    private static final class CommonsPropertyLocator extends CommonsLocatorAdapter implements PropertyObjectLocator {
        private CommonsPropertyLocator(ObjectLocator parent, String name, Object object) {
            super(parent, name, -1, object);
        }
        public String getPropertyName() { return propertyName; }
        public ItemObjectLocator item(int idx, Object val) { return new CommonsItemLocator(this, idx, val); }
        public PropertyObjectLocator property(String name, Object val) { return new CommonsPropertyLocator(this, name, val); }
        public String toString() { return getPathAsString(); }
    }
}
