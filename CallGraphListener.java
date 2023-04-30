import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.misc.*;
import java.util.*;

public class CallGraphListener extends Java8BaseListener {
	public static void main(String[] args) throws Exception {
            ANTLRInputStream input = new ANTLRInputStream(System.in);
            Java8Lexer lexer = new Java8Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Java8Parser parser = new Java8Parser(tokens);
            ParseTree tree = parser.compilationUnit();
            ParseTreeWalker walker = new ParseTreeWalker();
            CallGraphListener listener = new CallGraphListener();
            walker.walk(listener, tree);

            StringBuilder buf = new StringBuilder();
            System.out.println(listener.graph.toDOT());
    }   
    
    static Set<String> declaredMethods = new HashSet<>();

    static class Graph {

        Set<String> nodes = new OrderedHashSet<String>();
        MultiMap<String, String> edges = new MultiMap<String, String>();

        public void add(String node) {
            boolean isInEdges = edges.entrySet().stream().anyMatch(entry -> entry.getKey().equals(node) || entry.getValue().contains(node));
            if (!isInEdges) {
                nodes.add(node);
            }
        }

        public void edge(String source, String target) {
            edges.putIfAbsent(source, target);
        }

        public String toDOT() {
            StringBuilder buf = new StringBuilder();
            buf.append("digraph G {\n");
            buf.append("    ranksep=.25;\n");
            buf.append("    edge [arrowsize=.5]\n");
            buf.append("    node [shape=circle, fontname=\"ArialNarrow\",\n" + "            fontsize=12, width=1.0, height=1.0];\n");
            buf.append("    ");

            for (String node : nodes) {
                buf.append(node);
                if (declaredMethods.contains(node)) {
                    buf.append(" [style=filled, fillcolor=green]");
                }
                buf.append("; ");
            }

            for (String src : edges.keySet()) {
                for (String trg : edges.get(src)) {
                    buf.append(trg);
                    if (declaredMethods.contains(trg)) {
                        buf.append(" [style=filled, fillcolor=green]");
                    }
                    buf.append("; ");
                }
                    buf.append(src);
                    if (declaredMethods.contains(src)) {
                        buf.append(" [style=filled, fillcolor=green]");
                    }
                    buf.append("; ");
                
            }

            buf.append("\n");
            for (String src : edges.keySet()) {
                for (String trg : edges.get(src)) {
                    buf.append(" ");
                    buf.append(src);
                    buf.append(" -> ");
                    buf.append(trg);
                    buf.append(";\n");
                }
            }
            buf.append("}\n");
            return buf.toString();
        }
    }

    static Graph graph = new Graph();
    static String mainMethodNew = "";

    static String packageNameFirst ="";
    static String packageNameSecond ="";
    static String currentClass = "";
    static String currentMethod = "";
    static String currentClassOLD = "";


    @Override
    public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
        String packageName ="";
        List<TerminalNode> identifiers = ctx.Identifier();
        packageName = identifiers.stream().map(TerminalNode::getText).collect(Collectors.joining("."));

        String[] packageParts = packageName.split("\\.");

        packageNameFirst = packageParts[0];
        packageNameSecond = packageParts[1];
    }

    @Override
    public void enterClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        currentClassOLD = currentClass;
        currentClass = ctx.normalClassDeclaration().Identifier().getText();
    }
    
    @Override public void enterClassBodyDeclaration(Java8Parser.ClassBodyDeclarationContext ctx) {

        String things = ctx.getText();
        currentMethod = mainMethodNew;
        mainMethodNew = things.substring(10,12);

        if(things.length() > 16){
            String currentTempClass = "";
            String currentTempMethod = "";

            if(currentMethod.equals("")){
                graph.add(packageNameFirst + packageNameSecond + currentClass + mainMethodNew);
            }
            
            currentMethod = mainMethodNew;

            things = things.substring(15,things.length()-1);
            String input = things;
            String[] parts = input.split(";");

            for (String part : parts) {

                if (!part.isEmpty()) {
                    int dotIndex = part.indexOf(".");
                    if (dotIndex != -1) {
                        currentTempClass = part.substring(0, dotIndex);
                        currentTempMethod = part.substring(dotIndex + 1, part.indexOf("(", dotIndex));
                    } else {
                        currentTempMethod = part.substring(0, part.indexOf("("));
                    }
                }

                if((!currentTempClass.equals(""))&&(!currentTempMethod.equals(""))){
                    graph.edge(packageNameFirst + packageNameSecond + currentClass + currentMethod, packageNameFirst + packageNameSecond + currentTempClass + currentTempMethod);
                }
                else if((currentTempClass.equals(""))){
                    graph.edge(packageNameFirst + packageNameSecond + currentClass + currentMethod, packageNameFirst + packageNameSecond + currentClass + currentTempMethod);
                }
                
            }
        }
        else{
                graph.add(packageNameFirst + packageNameSecond + currentClass + mainMethodNew);
        }
    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        String methodName = ctx.methodHeader().methodDeclarator().Identifier().getText();
        declaredMethods.add(packageNameFirst + packageNameSecond + currentClass + methodName);
    }
   
    static class MultiMap<K, V>{
        private Map<K, Collection<V>> map = new HashMap<>();

        /**
        * Add the specified value with the specified key in this multimap.
        */
        public void put(K key, V value) {
            if (map.get(key) == null)
                map.put(key, new ArrayList<V>());

            map.get(key).add(value);
        }

        /**
        * Associate the specified key with the given value if not
        * already associated with a value
        */
        public void putIfAbsent(K key, V value) {
            if (map.get(key) == null)
                map.put(key, new ArrayList<>());

            // if value is absent, insert it
            if (!map.get(key).contains(value)) {
                map.get(key).add(value);
            }
        }

        /**
        * Returns the Collection of values to which the specified key is mapped,
        * or null if this multimap contains no mapping for the key.
        */
        public Collection<V> get(Object key) {
            return map.get(key);
        }

        /**
        * Returns a Set view of the keys contained in this multimap.
        */
        public Set<K> keySet() {
            return map.keySet();
        }

        /**
        * Returns a Set view of the mappings contained in this multimap.
        */
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            return map.entrySet();
        }

        /**
        * Returns a Collection view of Collection of the values present in
        * this multimap.
        */
        public Collection<Collection<V>> values() {
            return map.values();
        }

        /**
        * Returns true if this multimap contains a mapping for the specified key.
        */
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        /**
        * Removes the mapping for the specified key from this multimap if present
        * and returns the Collection of previous values associated with key, or
        * null if there was no mapping for key.
        */
        public Collection<V> remove(Object key) {
            return map.remove(key);
        }

        /**
        * Returns the number of key-value mappings in this multimap.
        */
        public int size() {
            int size = 0;
            for (Collection<V> value: map.values()) {
                size += value.size();
            }
            return size;
        }

        /**
        * Returns true if this multimap contains no key-value mappings.
        */
        public boolean isEmpty() {
            return map.isEmpty();
        }

        /**
        * Removes all of the mappings from this multimap.
        */
        public void clear() {
            map.clear();
        }

        /**
        * Removes the entry for the specified key only if it is currently
        * mapped to the specified value and return true if removed
        */
        public boolean remove(K key, V value) {
            if (map.get(key) != null) // key exists
                return map.get(key).remove(value);

            return false;
        }

        /**
        * Replaces the entry for the specified key only if currently
        * mapped to the specified value and return true if replaced
        */
        public boolean replace(K key, V oldValue, V newValue) {

            if (map.get(key) != null) {
                if (map.get(key).remove(oldValue))
                    return map.get(key).add(newValue);
            }
            return false;
        }
    }
}   