///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010 by Peter Spirtes, Richard Scheines, Joseph Ramsey, //
// and Clark Glymour.                                                        //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.util.ChoiceGenerator;

import java.util.*;

/**
 * A clean-up of Ricardo's tetrad-based purify.
 *
 * @author Joe Ramsey
 */
public class PurifyTetradBasedJ implements IPurify {
    private TetradTest tetradTest;

    private List<Node> nodes;
    boolean listTetrads = false;
    private HashMap<Node, Integer> nodeMap;
    private Graph mim;
    private Tetrad foundTetrad;

    public PurifyTetradBasedJ(TetradTest tetradTest) {
        this.tetradTest = tetradTest;
        this.nodes = tetradTest.getVariables();

        this.nodeMap = new HashMap<Node, Integer>();

        for (int index = 0; index < nodes.size(); index++) {
            nodeMap.put(nodes.get(index), index);
        }
    }

    @Override
	public List<List<Node>> purify(List<List<Node>> clustering) {

        // The inputs nodes may not be object identical to the ones from the tetrad test, so we map them over then
        // back by their names.
        List<Node> originalNodes = new ArrayList<Node>();

        for (List<Node> cluster : clustering) {
            originalNodes.addAll(cluster);
        }

        List<List<Node>> _clustering = new ArrayList<List<Node>>();

        for (List<Node> cluster : clustering) {
            List<Node> converted = GraphUtils.replaceNodes(cluster, nodes);
            _clustering.add(converted);
        }

        List<List<Node>> result = combinedSearch(_clustering);
        List<List<Node>> convertedResult = new ArrayList<List<Node>>();

        for (List<Node> cluster : result) {
            List<Node> converted = GraphUtils.replaceNodes(cluster, originalNodes);
            convertedResult.add(converted);
        }

        return convertedResult;
    }


    private List<List<Node>> combinedSearch(List<List<Node>> clustering) {
        double cutoff = tetradTest.getSignificance();

        List<List<Node>> _clustering = initializeZeroClusters(clustering.size());
        Set<Node> allNodes = getAllNodesInClusters(clustering);
        addPureNodes(clustering, _clustering, allNodes, cutoff);

        System.out.println("i-clustring: " + _clustering);

        return _clustering;
    }

    private void addNodesToSubclusters(List<List<Node>> clustering, List<List<Node>> subclustering, int maxSize) {
        for (int i = 0; i < clustering.size(); i++) {
            List<Node> cluster = clustering.get(i);
            List<Node> subcluster = subclustering.get(i);
            Collections.shuffle(cluster);

            for (Node node : cluster) {
                if (subcluster.size() >= maxSize) break;
                if (subcluster.contains(node)) continue;
                subcluster.add(node);
            }
        }
    }

    private List<List<Node>> initializeZeroClusters(int numClusters) {
        List<List<Node>> clustering = new ArrayList<List<Node>>();

        for (int i = 0; i < numClusters; i++) {
            clustering.add(new ArrayList<Node>());
        }
        return clustering;
    }

    private Set<Node> getAllNodesInClusters(List<List<Node>> clustering) {
        Set<Node> allNodes = new HashSet<Node>();

        for (List<Node> cluster : clustering) {
            allNodes.addAll(cluster);
        }
        return allNodes;
    }

    private void addPureNodes(List<List<Node>> clustering, List<List<Node>> _clustering, Set<Node> allNodes, double cutoff) {
        List<Node> _allNodes = new ArrayList<Node>(allNodes);
        Collections.shuffle(_allNodes);

        List<Node> nodesToTry = new ArrayList<Node>(_allNodes);
        int index = 0;

        while (!nodesToTry.isEmpty()) {

            NODE:
            for (Node node : new ArrayList<Node>(nodesToTry)) {

                for (int i = 0; i < _clustering.size(); i++) {
                    List<Node> cluster = _clustering.get(i);
                    List<Node> originalCluster = clustering.get(i);

                    if (!originalCluster.contains(node)) {
                        continue;
                    }

                    if (cluster.contains(node)) {
                        nodesToTry.remove(node);
                        continue NODE;
                    }

                    cluster.add(node);
                    Tetrad tetrad;
                    Tetrad _tetrad = null;

                    List<Node> _node = Collections.singletonList(node);

                    tetrad = findImpurity(_node, _clustering, cutoff, new ArrayList<Set<Node>>());

                    if (tetrad != null) {
                        Set<Node> tetradNodes = tetrad.getNodes();
                        List<Node> _tetradNodes = new ArrayList<Node>(tetradNodes);

                        ChoiceGenerator gen = new ChoiceGenerator(tetradNodes.size(), 2);
                        int[] choice;
                        boolean removed = false;

                        while ((choice = gen.next()) != null) {
                            List<Node> nodes = GraphUtils.asList(choice, _tetradNodes);

                            _tetrad = findImpurity(nodes, _clustering, cutoff, Collections.singletonList(tetradNodes));

                            if (_tetrad != null) {


                                for (List<Node> __cluster : _clustering) {
                                    __cluster.remove(nodes.get(0));
                                }

                                System.out.println("Removed " + nodes.get(0) + " " + tetrad + " " + _tetrad);

                                if (mim != null) {
                                    printSubgraph(node, tetrad);
                                }

                                removed = true;
                                break;
                            }
                        }

                        if (!removed) {
                            for (List<Node> __cluster : _clustering) {
                                __cluster.remove(node);
                            }

                            System.out.println("Removed " + node + " " + tetrad);

                            if (mim != null) {
                                printSubgraph(node, tetrad);
                            }
                        }
                    } else {
                        System.out.println("Added " + node + " index = " + (++index));
                    }


//                    if (tetrad != null) {
//                        for (List<Node> __cluster : _clustering) {
//                            __cluster.remove(node);
//                        }
//
//                        if (mim != null) {
//                            printSubgraph(node);                                node
//                        }
//                    } else {
//                        System.out.println("Added " + node + " index = " + (++index));
//                    }

                    nodesToTry.remove(node);
                    continue NODE;
                }
            }
        }
    }

    private void printSubgraph(Node node, Tetrad tetrad) {
        System.out.println("Subgraph for node " + node + ", tetrad " + getFoundTetrad() + ":");

        List<Node> _nodes = new ArrayList<Node>();

        for (Node node2 : tetrad.getNodes()) {
            _nodes.add(mim.getNode(node2.getName()));
        }

//                        Graph subgraph = mim.subgraph(_nodes);
        List<Edge> edges = mim.getEdges();

        for (int t = 0; t < edges.size(); t++) {
            Edge edge = edges.get(t);

            if (_nodes.contains(edge.getNode1()) || _nodes.contains(edge.getNode2())) {
                System.out.println((t + 1) + ". " + edges.get(t));
            }
        }
    }

    private boolean existsImpurity(Node node, List<List<Node>> clustering, double cutoff) {
        for (List<Node> cluster : clustering) {
//            if (!cluster.contains(node)) {
//                continue;
//            }

            boolean exists = existsWithinClusterImpurity(node, cluster, cutoff);
            if (exists) return true;
        }

        return existsCrossConstructImpurity(node, clustering, cutoff);
    }

    private boolean existsWithinClusterImpurity(Node node, List<Node> cluster, double cutoff) {
        if (cluster.size() < 4) return false;
        ChoiceGenerator gen = new ChoiceGenerator(cluster.size(), 4);
        int[] choice;

        while ((choice = gen.next()) != null) {
            List<Node> _cluster = GraphUtils.asList(choice, cluster);
            boolean exists = existsThreeTetradImpurity(node, _cluster, cutoff);
            if (exists) return true;
        }

        return false;
    }

    private boolean existsCrossConstructImpurity(Node node, List<List<Node>> clustering, double cutoff) {
        for (int p1 = 0; p1 < clustering.size(); p1++) {
            for (int p2 = p1 + 1; p2 < clustering.size(); p2++) {
                List<Node> cluster1 = clustering.get(p1);
                List<Node> cluster2 = clustering.get(p2);

//                if (!(cluster1.contains(node) || cluster2.contains(node))) {
//                    continue;
//                }

                if (cluster1.size() >= 3 && cluster2.size() >= 1) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster1.size(), 3);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster2.size(), 1);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster1.get(i));
                            for (int i : choice2) crossCluster.add(cluster2.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            boolean exists = existsThreeTetradImpurity(node, crossCluster, cutoff);
                            if (exists) return true;
                        }
                    }
                }

                if (cluster2.size() >= 3 && cluster1.size() >= 1) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster2.size(), 3);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster1.size(), 1);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster2.get(i));
                            for (int i : choice2) crossCluster.add(cluster1.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            boolean exists = existsThreeTetradImpurity(node, crossCluster, cutoff);
                            if (exists) return true;
                        }
                    }
                }

                if (cluster1.size() >= 2 && cluster2.size() >= 2) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster1.size(), 2);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster2.size(), 2);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster1.get(i));
                            for (int i : choice2) crossCluster.add(cluster2.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            boolean exists = existsTetrads2By2Impurity(node, crossCluster, cutoff);
                            if (exists) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean existsThreeTetradImpurity(Node node, List<Node> cluster, double cutoff) {
        if (cluster.size() != 4) throw new IllegalStateException("Expected a 4-node cluster: " + cluster);

        Node ci = cluster.get(0);
        Node cj = cluster.get(1);
        Node ck = cluster.get(2);
        Node cl = cluster.get(3);

        if (ci != node && cj != node && ck != node && cl != node) {
            return false;
        }

        double p1 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(cj), nodeMap.get(ck), nodeMap.get(cl));

        if (p1 < cutoff) {
            Tetrad tetrad = new Tetrad(ci, cj, ck, cl, p1);
            this.foundTetrad = tetrad;
            System.out.println("Found " + tetrad);
            return true;
        }

        double p2 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(cj), nodeMap.get(cl), nodeMap.get(ck));

        if (p2 < cutoff) {
            Tetrad tetrad = new Tetrad(ci, cj, cl, ck, p2);
            this.foundTetrad = tetrad;
            System.out.println("Found " + tetrad);
            return true;
        }

        double p3 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(ck), nodeMap.get(cl), nodeMap.get(cj));

        if (p3 < cutoff) {
            Tetrad tetrad = new Tetrad(ci, ck, cl, cj, p3);
            this.foundTetrad = tetrad;
            System.out.println("Found " + tetrad);
            return true;
        }

        return false;
    }

    private boolean existsTetrads2By2Impurity(Node node, List<Node> cluster, double cutoff) {
        if (cluster.size() != 4) throw new IllegalStateException("Expected a 4-node cluster: " + cluster);

        Node ci = cluster.get(0);
        Node cj = cluster.get(1);
        Node ck = cluster.get(2);
        Node cl = cluster.get(3);

        if (ci != node && cj != node && ck != node && cl != node) {
            return false;
        }

        double p1 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(ck), nodeMap.get(cl), nodeMap.get(cj));

        if (p1 < cutoff) {
            Tetrad tetrad = new Tetrad(ci, ck, cl, cj, p1);
            this.foundTetrad = tetrad;
            System.out.println("Found " + tetrad);
            return true;
        }

        return false;
    }

    private Tetrad findImpurity(List<Node> nodes, List<List<Node>> clustering, double cutoff, List<Set<Node>> excluded) {
        for (List<Node> cluster : clustering) {
            if (!cluster.containsAll(nodes)) {
                continue;
            }

            Tetrad tetrad = findWithinClusterImpurity(nodes, cluster, cutoff, excluded);
            if (tetrad != null) return tetrad;
        }

        return findCrossConstructImpurity(nodes, clustering, cutoff, excluded);
    }

    private Tetrad findWithinClusterImpurity(List<Node> nodes, List<Node> cluster, double cutoff, List<Set<Node>> excluded) {
        if (cluster.size() < 4) return null;
        ChoiceGenerator gen = new ChoiceGenerator(cluster.size(), 4);
        int[] choice;

        while ((choice = gen.next()) != null) {
            List<Node> _cluster = GraphUtils.asList(choice, cluster);
            Tetrad tetrad = findThreeTetradImpurity(nodes, _cluster, cutoff, excluded);
            if (tetrad != null) return tetrad;
        }

        return null;
    }

    private Tetrad findCrossConstructImpurity(List<Node> nodes, List<List<Node>> clustering, double cutoff, List<Set<Node>> excluded) {
        for (int p1 = 0; p1 < clustering.size(); p1++) {
            for (int p2 = p1 + 1; p2 < clustering.size(); p2++) {
                List<Node> cluster1 = clustering.get(p1);
                List<Node> cluster2 = clustering.get(p2);

//                if (!(cluster1.contains(node) || cluster2.contains(node))) {
//                    continue;
//                }

                if (cluster1.size() >= 3 && cluster2.size() >= 1) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster1.size(), 3);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster2.size(), 1);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster1.get(i));
                            for (int i : choice2) crossCluster.add(cluster2.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            Tetrad tetrad = findThreeTetradImpurity(nodes, crossCluster, cutoff, excluded);
                            if (tetrad != null) return tetrad;
                        }
                    }
                }

                if (cluster2.size() >= 3 && cluster1.size() >= 1) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster2.size(), 3);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster1.size(), 1);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster2.get(i));
                            for (int i : choice2) crossCluster.add(cluster1.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            Tetrad tetrad = findThreeTetradImpurity(nodes, crossCluster, cutoff, excluded);
                            if (tetrad != null) return tetrad;
                        }
                    }
                }

                if (cluster1.size() >= 2 && cluster2.size() >= 2) {
                    ChoiceGenerator gen1 = new ChoiceGenerator(cluster1.size(), 2);
                    int[] choice1;

                    while ((choice1 = gen1.next()) != null) {
                        ChoiceGenerator gen2 = new ChoiceGenerator(cluster2.size(), 2);
                        int[] choice2;

                        while ((choice2 = gen2.next()) != null) {
                            List<Node> crossCluster = new ArrayList<Node>();
                            for (int i : choice1) crossCluster.add(cluster1.get(i));
                            for (int i : choice2) crossCluster.add(cluster2.get(i));

//                            if (!crossCluster.contains(node)) {
//                                continue;
//                            }

                            Tetrad tetrad = findTetrads2By2Impurity(nodes, crossCluster, cutoff, excluded);
                            if (tetrad != null) return tetrad;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Tetrad findThreeTetradImpurity(List<Node> nodes, List<Node> cluster, double cutoff, List<Set<Node>> excluded) {
        if (cluster.size() != 4) throw new IllegalStateException("Expected a 4-node cluster: " + cluster);

        Node ci = cluster.get(0);
        Node cj = cluster.get(1);
        Node ck = cluster.get(2);
        Node cl = cluster.get(3);

        if (!cluster.containsAll(nodes)) {
            return null;
        }

//        if (ci != node && cj != node && ck != node && cl != node) {
//            return null;
//        }

        double p1 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(cj), nodeMap.get(ck), nodeMap.get(cl));

        Tetrad tetrad = new Tetrad(ci, cj, ck, cl, p1);

        if (p1 < cutoff && checksOut(tetrad, excluded, nodes)) {
            return tetrad;
        }

        double p2 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(cj), nodeMap.get(cl), nodeMap.get(ck));

        Tetrad tetrad2 = new Tetrad(ci, cj, cl, ck, p2);

        if (p2 < cutoff && checksOut(tetrad, excluded, nodes)) {
            return tetrad2;
        }

        double p3 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(ck), nodeMap.get(cl), nodeMap.get(cj));

        Tetrad tetrad3 = new Tetrad(ci, ck, cl, cj, p3);

        if (p3 < cutoff && checksOut(tetrad, excluded, nodes)) {
            return tetrad3;
        }

        return null;
    }

    private Tetrad findTetrads2By2Impurity(List<Node> nodes, List<Node> cluster, double cutoff, List<Set<Node>> excluded) {
        if (cluster.size() != 4) throw new IllegalStateException("Expected a 4-node cluster: " + cluster);

        Node ci = cluster.get(0);
        Node cj = cluster.get(1);
        Node ck = cluster.get(2);
        Node cl = cluster.get(3);

        if (!cluster.containsAll(nodes)) {
            return null;
        }

//        if (ci != node && cj != node && ck != node && cl != node) {
//            return null;
//        }

        double p1 = tetradTest.tetradPValue(nodeMap.get(ci), nodeMap.get(ck), nodeMap.get(cl), nodeMap.get(cj));

        Tetrad tetrad = new Tetrad(ci, ck, cl, cj, p1);

        if (p1 < cutoff && tetrad != null && checksOut(tetrad, excluded, nodes)) {
            return tetrad;
        }

        return null;
    }

    private boolean checksOut(Tetrad tetrad, List<Set<Node>> excluded, List<Node> mustInclude) {
        boolean allNew = true;

        for (Set<Node> _excluded : excluded) {
            Set<Node> set1 = new HashSet<Node>(_excluded);
            set1.removeAll(mustInclude);
            allNew = set1.retainAll(tetrad.getNodes());
            if (!set1.isEmpty()) {
                allNew = false;
            }
        }

        return allNew;
    }

    @Override
	public void setTrueGraph(Graph mim) {
        this.mim = mim;
    }

    public Tetrad getFoundTetrad() {
        return foundTetrad;
    }
}
