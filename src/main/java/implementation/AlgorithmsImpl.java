package implementation;

import json_impl.fromJsonToGraph;
import api.DirectedWeightedGraph;
import api.DirectedWeightedGraphAlgorithms;
import api.EdgeData;
import api.NodeData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.nio.file.Paths;
import java.sql.Array;
import java.util.*;
import java.io.*;

public class AlgorithmsImpl implements DirectedWeightedGraphAlgorithms {
    static final int WHITE = 1, GRAY = -1, BLACK = 0;

    private DirectedWeightedGraph g;
    private DirectedWeightedGraph reverseGraph;

    @Override
    public void init(DirectedWeightedGraph g) {
        this.g = g;

    }

    @Override
    public DirectedWeightedGraph getGraph() {

        return this.g;
    }

    @Override
    public DirectedWeightedGraph copy() {


        DirectedWeightedGraph new_graph = new DirectedWeightedGraphImpl();
        for (Iterator<NodeData> it = this.g.nodeIter(); it.hasNext(); ) {
            NodeData NodeCopy = it.next();

            for (Iterator<EdgeData> iter = this.g.edgeIter(NodeCopy.getKey()); iter.hasNext(); ) {
                EdgeData EdgeCopy = iter.next();
                new_graph.connect(NodeCopy.getKey(), EdgeCopy.getDest(), EdgeCopy.getWeight());

            }


        }
        for (Iterator<NodeData> it = this.g.nodeIter(); it.hasNext(); ) {
            NodeData nodeCopy = it.next();
            NodeData newNode = this.g.getNode(nodeCopy.getKey());
            new_graph.addNode(newNode);
            String newInfo = nodeCopy.getInfo();
            new_graph.getNode(nodeCopy.getKey()).setInfo(newInfo);

        }

        return new_graph;

    }

    @Override
    public boolean isConnected() {

        System.out.println("SCC ---->" +SCC());
        if (SCC().size() == 1)
            return true;


        return false;
    }


    public List<Set<NodeData>> SCC() {

        Deque<NodeData> stack = new ArrayDeque<>();
        Set<NodeData> visited = new HashSet<>();


        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData node = it.next();
            if (visited.contains(node)) {
                continue;
            }

            DFS(node, visited, stack);
        }

        this.reverseGraph = reverse();
        visited.clear();
        List<Set<NodeData>> components = new ArrayList<>();
        while (!stack.isEmpty()) {

            NodeData n = reverseGraph.getNode(stack.poll().getKey());
            if (visited.contains(n)) {
                continue;
            }
            //storing the one scc
            Set<NodeData> set = new HashSet<>();

            DFSForReverseGraph(n, visited, set);
            components.add(set);


        }


        return components;
    }

    //second DFS on reversed Graph
    private void DFSForReverseGraph(NodeData n, Set<NodeData> visited, Set<NodeData> set) {
        visited.add(n);
        set.add(n);
        for (Iterator<EdgeData> it = reverseGraph.edgeIter(n.getKey()); it.hasNext(); ) {
            EdgeData e = it.next();
            NodeData curr = reverseGraph.getNode(e.getDest());
            if (visited.contains(curr)) {
                continue;
            }
            DFSForReverseGraph(curr, visited, set);
        }


    }

    private DirectedWeightedGraph reverse() {
        DirectedWeightedGraph reverseGraph = new DirectedWeightedGraphImpl();

        for (Iterator<EdgeData> it = g.edgeIter(); it.hasNext(); ) {
            EdgeData edge = it.next();
//            System.out.println("No reverse: " + edge);

            reverseGraph.addNode(g.getNode(edge.getSrc()));
            reverseGraph.addNode(g.getNode(edge.getDest()));
            reverseGraph.connect(edge.getDest(), edge.getSrc(), edge.getWeight());

        }
        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData nodeData = it.next();
            reverseGraph.addNode(nodeData);

        }

        return reverseGraph;
    }
    //First DFS
    private void DFS(NodeData node, Set<NodeData> visited, Deque<NodeData> stack) {

        visited.add(node);
        for (Iterator<EdgeData> it = g.edgeIter(node.getKey()); it.hasNext(); ) {

            EdgeData e = it.next();

            NodeData curr = g.getNode(e.getDest());
            if (visited.contains(curr)) {
                continue;
            }
            DFS(curr, visited, stack);
        }

        stack.offerFirst(node);

    }

    @Override
    public double shortestPathDist(int src, int dest) {

        double ans = 0;

        List<NodeData> shortestPathList = shortestPath(src, dest);

        if (shortestPathList == null) {
            return -1;
        }


        return shortestPathList.get(shortestPathList.size() - 1).getWeight();

    }

    @Override
    public List<NodeData> shortestPath(int src, int dest) {
        HashMap<Integer, NodeData> dist = new HashMap<Integer, NodeData>();
        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData nodeData = it.next();
            nodeData.setWeight(Integer.MAX_VALUE);
        }
        HashMap<Integer, ArrayList<NodeData>> shortestListsMap = new HashMap<>();
        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData nodeData = it.next();
            shortestListsMap.put(nodeData.getKey(), new ArrayList<>());
        }
        NodeData start = g.getNode(src);
        start.setWeight(0);
        dist.put(src, start);
        while (!dist.isEmpty()) {
            NodeData currentNode = this.MinWeightedNode(dist);
            if (currentNode.getKey() == dest) {
                break;
            }

            int currentNodeKey = currentNode.getKey();
            double currentNodeDistance = currentNode.getWeight();
            dist.remove(currentNodeKey);
            ArrayList<NodeData> currPath = shortestListsMap.get(currentNodeKey);
            for (Iterator<EdgeData> it = g.edgeIter(currentNodeKey); it.hasNext(); ) {
                EdgeData e = it.next();
                int neighbor = e.getDest();
                double CurrNodeAndEdgeWeight = g.getEdge(currentNodeKey, neighbor).getWeight() + currentNodeDistance;
                NodeData neighborNode = g.getNode(neighbor);
                if (CurrNodeAndEdgeWeight < neighborNode.getWeight()) {
                    ArrayList<NodeData> currNeighborPath = new ArrayList<>(currPath);
                    currNeighborPath.add(neighborNode);
                    shortestListsMap.put(neighborNode.getKey(), currNeighborPath);
                    neighborNode.setWeight(CurrNodeAndEdgeWeight);
                    dist.put(neighborNode.getKey(), neighborNode);
                }
            }

        }

        return shortestListsMap.get(dest);

    }

    private NodeData MinWeightedNode(HashMap<Integer, NodeData> nodes_Map) {
        NodeData minNode = null;
        double minDis = Integer.MAX_VALUE;
        for (NodeData node : nodes_Map.values()) {
            if (node.getWeight() <= minDis) {
                minNode = node;
                minDis = node.getWeight();
            }
        }

        return minNode;
    }

    public double maxInThePath(NodeData nodeData) {
        double max = Integer.MIN_VALUE;

        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData node = it.next();
            if (nodeData.getKey() != node.getKey()) {
                double shortestP = shortestPathDist(nodeData.getKey(), node.getKey());
                if (max < shortestP) {
                    max = shortestP;


                }

            }
        }
        return max;
    }

    @Override
    public NodeData center() {
        if (g.nodeSize() == 0) {
            return null;
        }

        double min = Integer.MAX_VALUE;
        NodeData ans = null;

        for (Iterator<NodeData> it = g.nodeIter(); it.hasNext(); ) {
            NodeData node = it.next();
            double temp = maxInThePath(node);
            if (temp < min) {
                min = temp;
                ans = node;

            }

        }
        return ans;
    }

    @Override
    public List<NodeData> tsp(List<NodeData> cities) {
        return null;


    }

    @Override
    public boolean save(String file) {
        boolean state = true;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(new Gson().toJson(new fromJsonToGraph(this.g)));
        String prettyJsonString = gson.toJson(je);
        try {
            FileWriter fr = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fr);
            bw.write(prettyJsonString);
            bw.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
            state = false;
        }
        return state;
    }

    @Override
    public boolean load(String file) {
        this.g = DirectedWeightedGraphImpl.load(file);
        if (this.g != null)
            return true;
        return false;
    }

    public AlgorithmsImpl(DirectedWeightedGraph g){
        this.g = g;
    }

    public static void main(String[] args) {
        DirectedWeightedGraph g = new DirectedWeightedGraphImpl();
        g.addNode(new NodeDataImpl(1, null));
        g.addNode(new NodeDataImpl(2, null));
        g.addNode(new NodeDataImpl(3, null));
        g.addNode(new NodeDataImpl(4, null));
        g.addNode(new NodeDataImpl(5, null));
        g.addNode(new NodeDataImpl(6, null));
        g.addNode(new NodeDataImpl(7, null));
        g.connect(1,2,0);
        g.connect(2,3,0);
        g.connect(3,1,0);
        g.connect(3,5,0);
        g.connect(5,6,0);
        g.connect(6,4,0);
        g.connect(4,5,0);
        g.connect(7,7,0);
        AlgorithmsImpl ag = new AlgorithmsImpl(g);

        System.out.println("ifConnected: " + ag.isConnected());
        ag.getGraph().connect(6,7 , 0);
        ag.getGraph().connect(5,3, 0);
        System.out.println("ifConnected: " + ag.isConnected());

//        AlgorithmsImpl ag = new AlgorithmsImpl();
//
//        System.out.println(ag.getGraph());
//        ag.load("G1.json");
//        ag.getGraph().removeEdge(2,3);
//        System.out.println(ag.getGraph());
//        System.out.println("Shortest Path -> " + ag.shortestPathDist(0, 7));
//        System.out.println("Shortest PathList -> " + ag.shortestPath(0, 7));
//        System.out.println("Center: ->" + ag.center());
//        System.out.println("isConnected: " + ag.isConnected());
    }

}

