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
 *
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
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return the children
     */
    public Set<NodeInfo> getChildren() {
        return children;
    }

    /**
     * @return the childrenToEdges
     */
    public Map<NodeInfo, Set<Edge>> getChildrenToEdges() {
        return childrenToEdges;
    }

    /**
     * @return the parents
     */
    public Set<NodeInfo> getParents() {
        return parents;
    }

    /**
     * @return the minSubGraphs
     */
    public List<Set<Object>> getMinSubGraphs() {
        return minSubGraphs;
    }

    /**
     * @param minSubGraphs the minSubGraphs to set
     */
    public void setMinSubGraphs(List<Set<Object>> minSubGraphs) {
        this.minSubGraphs = minSubGraphs;
    }
}
