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

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JCaughtExceptionRef;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class CFGAnalyzer {

    private Collection<Loop> loops;
    private Map<Loop,Integer> loopedApiCalls = new HashMap<Loop, Integer>();
    private Map<Loop,Integer> loopNestingLevels = new HashMap<Loop, Integer>();
    private List<Integer> pathApiCalls = new ArrayList<Integer>();
    private List<Integer> pathAllocations = new ArrayList<Integer>();
    private Set<SootMethod> userMethodCalls = new LinkedHashSet<SootMethod>();
    private Set<InvokeExpr> apiCalls = new LinkedHashSet<InvokeExpr>();

    private final UnitGraph graph;
    private final SootMethod method;
    private final XMansion xmansion;

    private static final String[] GAE_API_CALLS = new String[] {
        "com.google.appengine.api.datastore.Cursor#fromWebSafeString()",
        "com.google.appengine.api.datastore.Cursor#toWebSafeString()",
        "com.google.appengine.api.datastore.DatastoreService#delete()",
        "com.google.appengine.api.datastore.DatastoreService#beginTransaction()",
        "!com.google.appengine.api.datastore.Transaction#isActive()",
        "com.google.appengine.api.datastore.Transaction#commit()",
        "com.google.appengine.api.datastore.Transaction#rollback()",
        "!com.google.appengine.api.datastore.DatastoreService#prepare()",
        "com.google.appengine.api.datastore.DatastoreService#get()",
        "com.google.appengine.api.datastore.DatastoreService#put()",
        "!com.google.appengine.api.datastore.DatastoreServiceFactory#getDatastoreService()",
        "!com.google.appengine.api.datastore.FetchOptions$Builder#withDefaults()",
        "!com.google.appengine.api.datastore.Key#getId()",
        "!com.google.appengine.api.datastore.Key#getName()",
        "com.google.appengine.api.datastore.KeyFactory#createKey()",
        "com.google.appengine.api.datastore.PreparedQuery#asIterable()",
        "com.google.appengine.api.datastore.PreparedQuery#asList()",
        "!com.google.appengine.api.datastore.Query#<init>()",
        "!com.google.appengine.api.datastore.Query#addFilter()",
        "!com.google.appengine.api.datastore.Query#setAncestor()",
        "!com.google.appengine.api.datastore.Entity#<init>()",
        "!com.google.appengine.api.datastore.Entity#getKey()",
        "!com.google.appengine.api.datastore.Entity#setProperty()",
        "!com.google.appengine.api.datastore.Entity#getProperties()",
        "!com.google.appengine.api.datastore.Entity#getProperty()",
        "!com.google.appengine.api.datastore.Blob#<init>()",
        "!com.google.appengine.api.datastore.Blob#getBytes()",
        "!com.google.appengine.api.datastore.Text#<init>()",
        "!com.google.appengine.api.datastore.Text#getValue()",
        "!com.google.appengine.api.datastore.Query#setKeysOnly()",
        "!com.google.appengine.api.datastore.KeyFactory#keyToString()",
        "!com.google.appengine.api.datastore.KeyFactory#stringToKey()",
        "!com.google.appengine.api.datastore.Query#addSort()",
        "!com.google.appengine.api.datastore.Query$FilterOperator#of()",
        "!com.google.appengine.api.datastore.Query#setFilter()",
        "!com.google.appengine.api.datastore.Query$CompositeFilterOperator#and()",
        "com.google.appengine.api.datastore.PreparedQuery#asSingleEntity()",
        "!com.google.appengine.api.datastore.FetchOptions$Builder#withLimit()",
        "!com.google.appengine.api.datastore.KeyFactory$Builder#<init>()",
        "!com.google.appengine.api.datastore.KeyFactory$Builder#addChild()",
        "!com.google.appengine.api.datastore.KeyFactory$Builder#getKey()",
        "!com.google.appengine.api.datastore.TransactionOptions$Builder#withXG()",

        "com.google.appengine.datanucleus.query.JPACursorHelper#getCursor()",
        "com.google.appengine.datanucleus.query.JDOCursorHelper#getCursor()",

        "com.google.appengine.api.files.FileService#createNewGSFile()",
        "com.google.appengine.api.files.FileService#openWriteChannel()",
        "!com.google.appengine.api.files.FileServiceFactory#getFileService()",
        "com.google.appengine.api.files.FileWriteChannel#closeFinally()",
        "!com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#<init>()",
        "com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#build()",
        "!com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#setAcl()",
        "!com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#setBucket()",
        "!com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#setKey()",
        "!com.google.appengine.api.files.GSFileOptions$GSFileOptionsBuilder#setMimeType()",

        "com.google.appengine.api.taskqueue.Queue#add()",
        "com.google.appengine.api.taskqueue.QueueFactory#getDefaultQueue()",
        "com.google.appengine.api.taskqueue.QueueFactory#getQueue()",
        "!com.google.appengine.api.taskqueue.TaskOptions$Builder#withUrl()",
        "!com.google.appengine.api.taskqueue.TaskOptions#retryOptions()",
        "!com.google.appengine.api.taskqueue.RetryOptions$Builder#withTaskAgeLimitSeconds()",
        "!com.google.appengine.api.taskqueue.TaskOptions#param()",
        "!com.google.appengine.api.taskqueue.TaskOptions#method()",

        "com.google.appengine.api.urlfetch.HTTPResponse#getContent()",
        "com.google.appengine.api.urlfetch.URLFetchService#fetch()",
        "!com.google.appengine.api.urlfetch.URLFetchServiceFactory#getURLFetchService()",
        "!com.google.appengine.api.urlfetch.HTTPResponse#getResponseCode()",
        "!com.google.appengine.api.urlfetch.HTTPRequest#<init>()",

        "!com.google.appengine.api.users.User#getEmail()",
        "!com.google.appengine.api.users.User#getNickname()",
        "!com.google.appengine.api.users.User#getUserId()",
        "!com.google.appengine.api.users.User#toString()",
        "com.google.appengine.api.users.UserService#createLoginURL()",
        "com.google.appengine.api.users.UserService#createLogoutURL()",
        "com.google.appengine.api.users.UserService#getCurrentUser()",
        "com.google.appengine.api.users.UserService#isUserLoggedIn()",
        "!com.google.appengine.api.users.UserServiceFactory#getUserService()",

        "!javax.jdo.JDODetachedFieldAccessException#<init>()",
        "javax.jdo.PersistenceManager#close()",
        "javax.jdo.PersistenceManager#deletePersistent()",
        "javax.jdo.PersistenceManager#getObjectById()",
        "javax.jdo.PersistenceManager#getObjectsById()",
        "javax.jdo.PersistenceManager#makePersistent()",
        "javax.jdo.PersistenceManager#newObjectIdInstance()",
        "!javax.jdo.PersistenceManager#newQuery()",
        "!javax.jdo.PersistenceManagerFactory#getPersistenceManager()",
        "javax.jdo.Query#closeAll()",
        "javax.jdo.Query#declareParameters()",
        "javax.jdo.Query#execute()",
        "!javax.jdo.Query#setExtensions()",
        "!javax.jdo.Query#setFilter()",
        "!javax.jdo.Query#setOrdering()",
        "!javax.jdo.Query#setRange()",
        "!javax.persistence.EntityExistsException#<init>()",
        "javax.persistence.EntityManager#clear()",
        "javax.persistence.EntityManager#close()",
        "!javax.persistence.EntityManager#createQuery()",
        "javax.persistence.EntityManager#find()",
        "javax.persistence.EntityManager#getTransaction()",
        "javax.persistence.EntityManager#persist()",
        "javax.persistence.EntityManager#remove()",
        "javax.persistence.EntityManagerFactory#createEntityManager()",
        "!javax.persistence.EntityNotFoundException#<init>()",
        "javax.persistence.EntityTransaction#begin()",
        "javax.persistence.EntityTransaction#commit()",
        "javax.persistence.Query#getResultList()",
        "!javax.persistence.Query#setFirstResult()",
        "!javax.persistence.Query#setHint()",
        "!javax.persistence.Query#setMaxResults()",
        "!javax.persistence.Query#setParameter()",

        "!com.google.appengine.api.memcache.MemcacheServiceFactory#getMemcacheService()",
        "com.google.appengine.api.memcache.MemcacheService#get()",
        "com.google.appengine.api.memcache.MemcacheService#contains()",
        "com.google.appengine.api.memcache.MemcacheService#put()",
        "com.google.appengine.api.memcache.MemcacheService#delete()",
        "com.google.appengine.api.memcache.MemcacheService#getAll()",
        "com.google.appengine.api.memcache.MemcacheService#deleteAll()",
        "!com.google.appengine.api.memcache.Expiration#byDeltaSeconds()",

        "com.google.appengine.api.xmpp.XMPPService#parseMessage()",
        "com.google.appengine.api.xmpp.XMPPService#sendPresence()",
        "com.google.appengine.api.xmpp.Presence#getFromJid()",
        "!com.google.appengine.api.xmpp.JID#<init>()",
        "!com.google.appengine.api.xmpp.JID#getId()",

        "!com.google.appengine.api.channel.ChannelServiceFactory#getChannelService()",
        "!com.google.appengine.api.channel.ChannelMessage#<init>()",
        "com.google.appengine.api.channel.ChannelService#sendMessage()",

        "edu.ucsb.cs.eager.gae.DataStore#query1()",
        "edu.ucsb.cs.eager.gae.DataStore#query2()",
        "edu.ucsb.cs.eager.gae.DataStore#query3()",
        "edu.ucsb.cs.eager.gae.DataStore#query4()",
    };

    public CFGAnalyzer(SootMethod method, XMansion xmansion) {
        this.method = method;
        this.xmansion = xmansion;
        Body b = method.retrieveActiveBody();
        this.graph = new BriefUnitGraph(b);
        doAnalyze();
    }

    private void doAnalyze() {
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(graph.getBody());
        loops = loopFinder.loops();

        Stmt stmt = (Stmt) graph.getHeads().get(0);
        visit(stmt, graph, 0, 0);
    }

    public Map<Loop, Integer> getLoopedApiCalls() {
        return Collections.unmodifiableMap(loopedApiCalls);
    }

    public Map<Loop, Integer> getLoopNestingLevels() {
        return Collections.unmodifiableMap(loopNestingLevels);
    }

    public Collection<Integer> getPathApiCalls() {
        return Collections.unmodifiableList(pathApiCalls);
    }

    public Collection<Integer> getPathAllocations() {
        return Collections.unmodifiableList(pathAllocations);
    }

    public Collection<SootMethod> getUserMethodCalls() {
        return Collections.unmodifiableSet(userMethodCalls);
    }

    public Collection<InvokeExpr> getApiCalls() {
        return Collections.unmodifiableSet(apiCalls);
    }

    public int getMaxApiCalls() {
        int max = 0;
        for (int calls : pathApiCalls) {
            if (calls > max) {
                max = calls;
            }
        }
        return max;
    }

    public int getMaxAllocations() {
        int max = 0;
        for (int calls : pathAllocations) {
            if (calls > max) {
                max = calls;
            }
        }
        return max;
    }

    private void analyzeLoop(Loop loop, int nestingLevel) {
        if (loopedApiCalls.containsKey(loop)) {
            return;
        }

        Set<Loop> nestedLoops = new HashSet<Loop>();
        for (Stmt stmt : loop.getLoopStatements()) {
            Loop nestedLoop = findLoop(stmt);
            if (nestedLoop != null && !nestedLoop.equals(loop)) {
                nestedLoops.add(nestedLoop);
            }
        }

        int apiCallCount = 0;
        for (Stmt stmt : loop.getLoopStatements()) {
            Loop nestedLoop = findLoop(stmt);
            if (nestedLoop != null && !nestedLoop.equals(loop)) {
                analyzeLoop(nestedLoop, nestingLevel + 1);
            }

            if (isStmtInNestedLoopBody(stmt, nestedLoops)) {
                continue;
            }

            if (stmt.containsInvokeExpr()) {
                InvokeExpr invocation = stmt.getInvokeExpr();
                if (isApiCall(invocation)) {
                    apiCallCount++;
                    apiCalls.add(invocation);
                } else if (isUserMethodCall(invocation.getMethod())) {
                    userMethodCalls.add(invocation.getMethod());
                    CFGAnalyzer analyzer = xmansion.getAnalyzer(invocation.getMethod());
                    if (analyzer != null) {
                        apiCallCount += analyzer.getMaxApiCalls();
                    }
                }
            }
        }
        loopedApiCalls.put(loop, apiCallCount);
        loopNestingLevels.put(loop, nestingLevel);
    }

    private void visit(Stmt stmt, UnitGraph graph, int apiCallCount, int allocationCount) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            if (isApiCall(invocation)) {
                apiCallCount++;
                apiCalls.add(invocation);
            } else if (isUserMethodCall(invocation.getMethod())) {
                userMethodCalls.add(invocation.getMethod());
                CFGAnalyzer analyzer = xmansion.getAnalyzer(invocation.getMethod());
                if (analyzer != null) {
                    apiCallCount += analyzer.getMaxApiCalls();
                    allocationCount += analyzer.getMaxAllocations();
                }
            }
        } else if (stmt instanceof NewExpr || stmt instanceof NewArrayExpr) {
            allocationCount++;
        } else if (stmt instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) stmt).getRightOp();
            if (rightOp instanceof NewExpr || rightOp instanceof NewArrayExpr) {
                allocationCount++;
            }
        }

        Collection<Unit> children = graph.getSuccsOf(stmt);

        Loop loop = findLoop(stmt);
        if (loop != null) {
            analyzeLoop(loop, 1);
            children = new HashSet<Unit>();
            for (Stmt exit : loop.getLoopExits()) {
                for (Stmt exitTarget : loop.targetsOfLoopExit(exit)) {
                    if (exitTarget instanceof JIdentityStmt) {
                        if (((JIdentityStmt) exitTarget).getRightOp() instanceof JCaughtExceptionRef) {
                            continue;
                        }
                    }
                    children.add(exitTarget);
                }
            }
        }

        for (Unit child : children) {
            visit((Stmt) child, graph, apiCallCount, allocationCount);
        }
        if (children.isEmpty()) {
            pathApiCalls.add(apiCallCount);
            pathAllocations.add(allocationCount);
        }
    }

    private boolean isApiCall(InvokeExpr invocation) {
        String signature = getSignature(invocation.getMethod());
        for (String apiCall : GAE_API_CALLS) {
            if (apiCall.equals(signature)) {
                return true;
            } else if (apiCall.equals("!" + signature)) {
                return false;
            }
        }


        String pkg = invocation.getMethod().getDeclaringClass().getPackageName();
        if (pkg.startsWith("com.google.appengine.") && !pkg.contains(".codelab")) {
            System.out.println("[GCALL] " + signature);
        }

        return false;
    }

    private String getSignature(SootMethod method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "()";
    }

    private boolean isUserMethodCall(SootMethod target) {
        String userPackage = method.getDeclaringClass().getJavaPackageName();
        String targetPackage = target.getDeclaringClass().getJavaPackageName();
        return targetPackage.startsWith(userPackage);
    }

    private Loop findLoop(Stmt stmt) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return loop;
            }
        }
        return null;
    }

    private boolean isStmtInNestedLoopBody(Stmt stmt, Set<Loop> nestedLoops) {
        for (Loop nestedLoop : nestedLoops) {
            if (nestedLoop.getLoopStatements().contains(stmt)) {
                return true;
            }
        }
        return false;
    }
}
