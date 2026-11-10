package jairo.investviewapi.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import jairo.investviewapi.dto.GraphEdge;
import jairo.investviewapi.dto.GraphNode;
import jairo.investviewapi.entity.AssetType;

public class AdjacencyListGraph implements Graph {

    private final Map<String, GraphNode> nodesById = new LinkedHashMap<>();
    private final Map<String, Set<String>> adjacency = new LinkedHashMap<>();
    private final Map<EdgeKey, Integer> weights = new LinkedHashMap<>();

    @Override
    public void addNode(String id, String symbol, AssetType type, String name) {
        GraphNode existing = nodesById.get(id);
        if (existing == null) {
            nodesById.put(id, new GraphNode(id, symbol, type, name));
            adjacency.computeIfAbsent(id, key -> new LinkedHashSet<>());
        } else if (existing.name() == null && name != null) {
            nodesById.put(id, new GraphNode(id, existing.symbol(), existing.type(), name));
        }
    }

    @Override
    public void addEdge(String source, String target) {
        adjacency.computeIfAbsent(source, key -> new LinkedHashSet<>()).add(target);
        adjacency.computeIfAbsent(target, key -> new LinkedHashSet<>()).add(source);
        weights.merge(EdgeKey.of(source, target), 1, Integer::sum);
    }

    @Override
    public boolean contains(String id) {
        return nodesById.containsKey(id);
    }

    @Override
    public List<GraphNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    @Override
    public List<GraphNode> neighbors(String id) {
        return adjacency.getOrDefault(id, Set.of()).stream()
                .map(nodesById::get)
                .toList();
    }

    @Override
    public List<GraphEdge> edges() {
        return weights.entrySet().stream()
                .map(entry -> new GraphEdge(
                        entry.getKey().first(),
                        entry.getKey().second(),
                        entry.getValue()))
                .toList();
    }

    @Override
    public int weight(String source, String target) {
        return weights.getOrDefault(EdgeKey.of(source, target), 0);
    }

    @Override
    public Optional<List<String>> shortestPath(String from, String to) {
        if (!nodesById.containsKey(from) || !nodesById.containsKey(to)) {
            return Optional.empty();
        }
        if (from.equals(to)) {
            return Optional.of(List.of(from));
        }
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        visited.add(from);
        queue.add(from);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : adjacency.get(current)) {
                if (visited.add(neighbor)) {
                    predecessors.put(neighbor, current);
                    if (neighbor.equals(to)) {
                        return Optional.of(buildPath(predecessors, to));
                    }
                    queue.add(neighbor);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Set<String>> components() {
        Set<String> visited = new HashSet<>();
        List<Set<String>> components = new ArrayList<>();
        for (String start : nodesById.keySet()) {
            if (visited.add(start)) {
                components.add(componentFrom(start, visited));
            }
        }
        return components;
    }

    private List<String> buildPath(Map<String, String> predecessors, String to) {
        LinkedList<String> path = new LinkedList<>();
        for (String current = to; current != null; current = predecessors.get(current)) {
            path.addFirst(current);
        }
        return List.copyOf(path);
    }

    private Set<String> componentFrom(String start, Set<String> visited) {
        Set<String> component = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            component.add(current);
            for (String neighbor : adjacency.get(current)) {
                if (visited.add(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return component;
    }

    private record EdgeKey(String first, String second) {

        static EdgeKey of(String source, String target) {
            return source.compareTo(target) <= 0
                    ? new EdgeKey(source, target)
                    : new EdgeKey(target, source);
        }
    }
}
