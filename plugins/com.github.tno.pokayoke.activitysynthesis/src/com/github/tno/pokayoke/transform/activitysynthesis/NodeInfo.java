/**
 *
 */

package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;

/**
 * Helper class containing information about the minimum sub graph for a location.
 */
public class NodeInfo {
    private Location location;

    private Set<NodeInfo> children;

    private Set<NodeInfo> parents;

    private Map<NodeInfo, Set<Edge>> childrenToEdges;

    private List<Set<Object>> minSubGraphs;

    public NodeInfo(Location loc, Set<NodeInfo> children, Set<NodeInfo> parents,
            Map<NodeInfo, Set<Edge>> childrenToEdges, List<Set<Object>> minSubGraphs)
    {
        this.location = loc;
        this.children = children;
        this.parents = parents;
        this.childrenToEdges = childrenToEdges;
        this.setMinSubGraphs(minSubGraphs);
    }

    /**
     * Return the location field.
     *
     * @return The location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Return the set of children of the current location.
     *
     * @return The children set.
     */
    public Set<NodeInfo> getChildren() {
        return children;
    }

    /**
     * Return the map from children to the edges connecting the location to them.
     *
     * @return The map from children to edges.
     */
    public Map<NodeInfo, Set<Edge>> getChildrenToEdges() {
        return childrenToEdges;
    }

    /**
     * Return the set of parents of the current location.
     *
     * @return The parents set.
     */
    public Set<NodeInfo> getParents() {
        return parents;
    }

    /**
     * Return the (list of) minimum sub graph of the current location, as a set of graph elements.
     *
     * @return The list of sub graphs.
     */
    public List<Set<Object>> getMinSubGraphs() {
        return minSubGraphs;
    }

    /**
     * Sets the (list of) minimum sub graph of the current location, as a set of graph elements.
     *
     * @param minSubGraphs The list of minimum sub graphs to set.
     */
    public void setMinSubGraphs(List<Set<Object>> minSubGraphs) {
        this.minSubGraphs = minSubGraphs;
    }
}
