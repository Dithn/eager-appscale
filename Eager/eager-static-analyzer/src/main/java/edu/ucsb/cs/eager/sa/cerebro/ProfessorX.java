/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package edu.ucsb.cs.eager.sa.cerebro;

import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import soot.SootMethod;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * ProfessorX performs static analysis on a collection of Java applications,
 * using Cerebro. All application metadata are read from an XML file.
 */
public class ProfessorX {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", "input-file", true, "Path to input xml file");
        options.addOption("r", "root-path", true, "Root path of all Git repositories");

        CommandLine cmd;
        try {
            CommandLineParser parser = new BasicParser();
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Cerebro", options);
            return;
        }

        String inputFileName = cmd.getOptionValue("i");
        if (inputFileName == null) {
            System.err.println("input file path is required");
            return;
        }

        String rootPath = cmd.getOptionValue("r");
        if (rootPath == null) {
            System.err.println("root path is required");
            return;
        }

        File inputFile = new File(inputFileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(inputFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        NodeList repoList = doc.getElementsByTagName("repo");
        for (int i = 0; i < repoList.getLength(); i++) {
            Element repo = (Element) repoList.item(i);
            String name = repo.getElementsByTagName("name").item(0).getTextContent();
            String classPath = repo.getElementsByTagName("classpath").item(0).getTextContent();
            Set<String> classes = new LinkedHashSet<>();
            NodeList classesList = repo.getElementsByTagName("classes").item(0).getChildNodes();
            for (int j = 0; j < classesList.getLength(); j++) {
                if (!(classesList.item(j) instanceof Element)) {
                    continue;
                }
                classes.add(classesList.item(j).getTextContent());
            }
            analyzeRepo(rootPath, name, classPath, classes);
        }
    }

    private static void analyzeRepo(String root, String name, String classPath,
                                    Collection<String> classes) {

        System.out.println("----------------- PROJECT: " + name + " -----------------\n");
        File repoDir = new File(root, name);
        File classPathDir = new File(repoDir, classPath);
        Set<String> analyzedMethods = new HashSet<>();
        for (String className : classes) {
            Cerebro cerebro = new Cerebro(classPathDir.getAbsolutePath(), className);
            cerebro.setWholeProgramMode(true);
            Map<SootMethod,CFGAnalyzer> results = cerebro.analyze();
            cerebro.setVerbose(true);
            for (Map.Entry<SootMethod,CFGAnalyzer> entry : results.entrySet()) {
                String method = toString(entry.getKey());
                if (analyzedMethods.contains(method)) {
                    continue;
                }
                analyzedMethods.add(method);
                cerebro.printResult(entry.getKey(), entry.getValue());
            }
            cerebro.cleanup();
        }
        System.out.println();
    }

    private static String toString(SootMethod method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "()";
    }
}
