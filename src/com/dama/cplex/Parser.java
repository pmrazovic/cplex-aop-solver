package com.dama.cplex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pero on 18/02/16.
 */
public class Parser {
    String filePath;
    int vertexCount;
    double budget;
    List<Edge> edges;

    Parser(String filePath){
        this.filePath = filePath;
    }

    public void parse() {
        // Parsing nodes
        File file = new File(this.filePath);
        List<String> fileLines = new ArrayList<String>();
        if(file.exists()) {
            try {
                fileLines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.edges = new ArrayList<>();
        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            if (line.equals("")) continue;
            if (i == 0) {
                this.vertexCount = Integer.parseInt(line);
            } else if (i == 1) {
                this.budget = Double.parseDouble(line);
            } else {
                String[] separtedLine = line.split(";");
                Edge newEdge = new Edge(Integer.parseInt(separtedLine[0]),
                        Integer.parseInt(separtedLine[1]),
                        Integer.parseInt(separtedLine[2]),
                        Double.parseDouble(separtedLine[3]),
                        Double.parseDouble(separtedLine[4]));
                this.edges.add(newEdge);
            }
        }

    }
}
