/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.store;

import com.codefollower.lealone.engine.InDoubtTransaction;


/**
 * The session state contains information about when was the last commit of a
 * session. It is only used during recovery.
 */
class SessionState {

    /**
     * The session id
     */
    public int sessionId;

    /**
     * The last log id where a commit for this session is found.
     */
    public int lastCommitLog;

    /**
     * The position where a commit for this session is found.
     */
    public int lastCommitPos;

    /**
     * The in-doubt transaction if there is one.
     */
    public InDoubtTransaction inDoubtTransaction;

    /**
     * Check if this session state is already committed at this point.
     *
     * @param logId the log id
     * @param pos the position in the log
     * @return true if it is committed
     */
    public boolean isCommitted(int logId, int pos) {
        if (logId != lastCommitLog) {
            return lastCommitLog > logId;
        }
        return lastCommitPos >= pos;
    }

    public String toString() {
        return "sessionId:" + sessionId + " log:" + lastCommitLog + " pos:" + lastCommitPos + " inDoubt:" + inDoubtTransaction;
    }
}
