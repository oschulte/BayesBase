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

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.ResolveSepsets;
import edu.cmu.tetrad.util.TetradLogger;

import java.util.*;

/**
 * Pools together a set of independence tests using a specified methods
 *
 * @author Robert Tillman
 */
public final class IndTestMulti implements IndependenceTest {


    /**
     * The variables of the covariance matrix, in order. (Unmodifiable list.)
     */
    private final List<Node> variables;

    /**
     * The independence test associated with each data set.
     */
    private List<IndependenceTest> independenceTests;

    /**
     * Pooling method
     */
    private ResolveSepsets.Method method;

    //==========================CONSTRUCTORS=============================//

    public IndTestMulti(List<IndependenceTest> independenceTests, ResolveSepsets.Method method) {
        Set<String> nodeNames = new HashSet<String>();
        for (IndependenceTest independenceTest : independenceTests) {
            nodeNames.addAll(independenceTest.getVariableNames());
        }
        if (independenceTests.get(0).getVariables().size() != nodeNames.size()) {
            throw new IllegalArgumentException("Data sets must have same variables.");
        }
        this.variables = independenceTests.get(0).getVariables();
        this.independenceTests = independenceTests;
        this.method = method;
    }

    //==========================PUBLIC METHODS=============================//

    @Override
	public IndependenceTest indTestSubset(List<Node> vars) {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines whether variable x is independent of variable y given a list of conditioning variables z.
     *
     * @param x the one variable being compared.
     * @param y the second variable being compared.
     * @param z the list of conditioning variables.
     * @return true iff x _||_ y | z.
     * @throws RuntimeException if a matrix singularity is encountered.
     */
    @Override
	public boolean isIndependent(Node x, Node y, List<Node> z) {
        boolean independent = ResolveSepsets.isIndependentPooled(method, independenceTests, x, y, z);

        if (independent) {
            TetradLogger.getInstance().log("independencies", "In aggregate independent: " + SearchLogUtils.independenceFact(x, y, z));
        } else {
            TetradLogger.getInstance().log("dependencies", "In aggregate dependent: " + SearchLogUtils.independenceFact(x, y, z));
        }

        return independent;
    }

    @Override
	public boolean isIndependent(Node x, Node y, Node... z) {
        List<Node> zList = Arrays.asList(z);
        boolean independent = isIndependent(x, y, zList);

        return independent;
    }

    @Override
	public boolean isDependent(Node x, Node y, List<Node> z) {
        return !isIndependent(x, y, z);
    }

    @Override
	public boolean isDependent(Node x, Node y, Node... z) {
        List<Node> zList = Arrays.asList(z);
        return isDependent(x, y, zList);
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
	public double getPValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
	public void setAlpha(double alpha) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the current significance level.
     */
    @Override
	public double getAlpha() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the list of variables over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    @Override
	public List<Node> getVariables() {
        return this.variables;
    }

    /**
     * Returns the variable with the given name.
     */
    @Override
	public Node getVariable(String name) {
        for (int i = 0; i < getVariables().size(); i++) {
            Node variable = getVariables().get(i);
            if (variable.getName().equals(name)) {
                return variable;
            }
        }

        return null;
    }

    /**
     * Returns the list of variable varNames.
     */
    @Override
	public List<String> getVariableNames() {
        List<Node> variables = getVariables();
        List<String> variableNames = new ArrayList<String>();
        for (Node variable1 : variables) {
            variableNames.add(variable1.getName());
        }
        return variableNames;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
	public boolean determines(List z, Node x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @throw UnsupportedOperationException
     */
    @Override
	public DataSet getData() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if determinism is allowed.
     */
    public boolean isDeterminismAllowed() {
        return false;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void setDeterminismAllowed(boolean determinismAllowed) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a string representation of this test.
     */
    @Override
	public String toString() {
        return "Pooled Independence Test:  alpha = " + independenceTests.get(0).getAlpha();
    }
}
