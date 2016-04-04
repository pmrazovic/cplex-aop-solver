package com.dama.cplex;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        String path="CPLEX/";
        String[] dirListing = null;
        File dir = new File(path);
        dirListing = dir.list();
        Arrays.sort(dirListing);

        for (String filePath : dirListing) {

            Parser parser = new Parser("CPLEX/" + filePath);
            parser.parse();
            int n = parser.vertexCount;
            int v = parser.edges.size();
            List<Edge> edges = parser.edges;
            int startNodeIdx = 0;
            int endNodeIdx = n - 1;
            double maxCost = parser.budget;

            try {
                IloCplex cplex = new IloCplex();

                // variables ----------------------------------------------------------------
                IloNumVar[][] x = new IloNumVar[n][];
                for (int i = 0; i < n; i++) {
                    x[i] = cplex.boolVarArray(n);
                }
                IloNumVar[] edgeVar = cplex.boolVarArray(v);
                IloIntVar[] u = cplex.intVarArray(n, 0, n);

                // objective ----------------------------------------------------------------
                IloLinearNumExpr obj = cplex.linearNumExpr();
                for (int i = 0; i < v; i++) {
                    obj.addTerm(edges.get(i).score, edgeVar[i]);
                }
                cplex.addMaximize(obj);

                // constraints ----------------------------------------------------------------

                // edgeVar
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        if (i == j) continue;
                        IloLinearNumExpr exprIJ = cplex.linearNumExpr();
                        for (int k = 0; k < v; k++) {
                            if (edges.get(k).startNode == i && edges.get(k).endNode == j) {
                                exprIJ.addTerm(edgeVar[k], 1.0);
                            }
                        }
                        cplex.addEq(exprIJ, x[i][j]);
                    }
                }


                // guarantees that the route starts in vertex with index startNodeIdx
                IloLinearNumExpr expr1 = cplex.linearNumExpr();
                for (int j = 0; j < n; j++) {
                    if (j != startNodeIdx)
                        expr1.addTerm(1.0, x[startNodeIdx][j]);
                }
                cplex.addEq(expr1, 1.0);

                // guarantees that the route ends in vertex with index endNodeIdx
                IloLinearNumExpr expr2 = cplex.linearNumExpr();
                for (int i = 0; i < n; i++) {
                    if (i != endNodeIdx)
                        expr2.addTerm(1.0, x[i][endNodeIdx]);
                }
                cplex.addEq(expr2, 1.0);

                // ensures the connectivity of the route and guarantees that each vertex and each arc is visited at most once
                for (int k = 0; k < n; k++) {
                    if (k != startNodeIdx && k != endNodeIdx) {

                        IloLinearNumExpr expr3 = cplex.linearNumExpr();
                        for (int i = 0; i < n; i++) {
                            expr3.addTerm(1.0, x[i][k]);
                        }
                        IloLinearNumExpr expr4 = cplex.linearNumExpr();
                        for (int j = 0; j < n; j++) {
                            expr4.addTerm(1.0, x[k][j]);
                        }
                        cplex.addEq(expr3, expr4);
                        cplex.addLe(expr3, 1.0);
                        cplex.addLe(expr4, 1.0);

                    }

                }

                // limits the cost of the route
                IloLinearNumExpr expr5 = cplex.linearNumExpr();
                for (int i = 0; i < v; i++) {
                    expr5.addTerm(edges.get(i).cost, edgeVar[i]);
                }
                cplex.addLe(expr5, maxCost);

                // eliminates sub-tours
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        if (i != j) {
                            IloLinearNumExpr expr6 = cplex.linearNumExpr();
                            expr6.addTerm(1.0, u[i]);
                            expr6.addTerm(-1.0, u[j]);
                            expr6.addTerm(n - 1, x[i][j]);
                            cplex.addLe(expr6, n - 2);
                        }
                    }
                }

                cplex.setParam(IloCplex.Param.TimeLimit,10800);

                // solving model ----------------------------------------------------------------
                DecimalFormat df = new DecimalFormat("#0.000");
                long start = System.currentTimeMillis();
                if (cplex.solve()) {
                    boolean aborted = false;
                    if (cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
                        aborted = true;
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    String CPUtime = "CPU time = " + df.format(elapsed / 1000.0);
                    if (aborted) {
                        CPUtime += " (ABORTED DUE TIME LIMIT)\n";
                    } else {
                        CPUtime += "\n";
                    }
                    String totalScore = "Total score = " + cplex.getObjValue() + "\n";
                    List<Edge> routeEdges = new ArrayList<>();
                    double cost = 0.0;
                    for (int k = 0; k < v; k++) {
                        double a = cplex.getValue(edgeVar[k]);
                        if (a > 0.9999) {
                            routeEdges.add(edges.get(k));
                            cost += edges.get(k).cost;
                        }
                    }
                    String consumedBudget = "Total consumed budget = " + cost + "\n";
                    String route = "";
                    int currentIdx = startNodeIdx;
                    boolean finish = false;
                    while (!finish) {
                        for (Edge edge : routeEdges) {
                            if (edge.startNode == currentIdx) {
                                currentIdx = edge.endNode;
                                route += edge.startNode + " --(" + edge.edgeIdx + ")--> ";
                                if (currentIdx == endNodeIdx) {
                                    finish = true;
                                    break;
                                }
                                break;
                            }
                        }

                    }
                    route += endNodeIdx + "\n";
                    String delimiter = "---------------------------\n";
                    String fileName = filePath + "\n";
                    String printString = delimiter + fileName + CPUtime + totalScore + consumedBudget + route;

                    try {
                        Files.write(Paths.get("solutions_details.txt"), printString.getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        //exception handling left as an exercise for the reader
                    }

                    String shortSolutionString = filePath + ";" + cplex.getObjValue() + ";" + df.format(elapsed / 1000.0);
                    if (aborted) {
                        shortSolutionString += ";ABORTED\n";
                    } else {
                        shortSolutionString += "\n";
                    }
                    try {
                        Files.write(Paths.get("solutions.txt"), shortSolutionString.getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        //exception handling left as an exercise for the reader
                    }


                } else {
                    System.out.print("Problem not solved!");
                }

                cplex.end();

            } catch (IloException e) {
                e.printStackTrace();
            }
        }


    }
}
