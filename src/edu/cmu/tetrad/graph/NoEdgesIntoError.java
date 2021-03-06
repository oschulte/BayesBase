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

package edu.cmu.tetrad.graph;


/**
 * A graph constraint prohibiting directed edges from pointing to (being into)
 * error nodes.
 *
 * @author Joseph Ramsey
 */
public final class NoEdgesIntoError implements GraphConstraint {
    static final long serialVersionUID = 23L;

    //=============================CONSTRUCTORS===========================//

    public NoEdgesIntoError() {

    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see edu.cmu.TestSerialization
     * @see edu.cmu.tetradapp.util.TetradSerializableUtils
     */
    public static NoEdgesIntoError serializableInstance() {
        return new NoEdgesIntoError();
    }

    //=============================PUBLIC METHODS=========================//

    /**
     * Checks whether a new edge satisfies the constraint in this graph.
     *
     * @param edge  the edge to check.
     * @param graph the graph to check.
     * @return true if the new edge may be added, false if not.
     */
    @Override
	public boolean isEdgeAddable(Edge edge, Graph graph) {

        if (Edges.isDirectedEdge(edge)) {
            Node into = (edge.getEndpoint1() == Endpoint
                    .ARROW) ? edge.getNode1() : edge.getNode2();

            return !(into.getNodeType() == NodeType.ERROR);
        }

        return true;
    }

    /**
     * @param node  the node to check.
     * @param graph the graph to check.
     * @return true.
     */
    @Override
	public boolean isNodeAddable(Node node, Graph graph) {
        return true;
    }

    /**
     * @param edge  the edge to check.
     * @param graph the graph to check.
     * @return true.
     */
    @Override
	public boolean isEdgeRemovable(Edge edge, Graph graph) {
        return true;
    }

    /**
     * @param node
     * @param graph the graph to check.
     * @return true.
     */
    @Override
	public boolean isNodeRemovable(Node node, Graph graph) {
        return true;
    }

    /**
     * Returns a string representation of the constraint.
     *
     * @return this representation.
     */
    @Override
	public String toString() {
        return "<Directed edges must be into non-error nodes.>";
    }
}



