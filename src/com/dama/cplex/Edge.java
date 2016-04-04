package com.dama.cplex;

/**
 * Created by pero on 18/02/16.
 */
public class Edge {
    int edgeIdx;
    int startNode;
    int endNode;
    double score;
    double cost;

    Edge(int edgeIdx, int startNodeIdx, int endNodeIdx, double cost, double score) {
        this.edgeIdx = edgeIdx;
        this.startNode = startNodeIdx;
        this.endNode = endNodeIdx;
        this.score = score;
        this.cost = cost;
    }
}
