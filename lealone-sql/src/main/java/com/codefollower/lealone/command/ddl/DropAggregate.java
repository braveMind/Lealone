/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.command.ddl;

import com.codefollower.lealone.command.CommandInterface;
import com.codefollower.lealone.constant.ErrorCode;
import com.codefollower.lealone.dbobject.UserAggregate;
import com.codefollower.lealone.engine.Database;
import com.codefollower.lealone.engine.Session;
import com.codefollower.lealone.message.DbException;

/**
 * This class represents the statement
 * DROP AGGREGATE
 */
public class DropAggregate extends DefineCommand {

    private String name;
    private boolean ifExists;

    public DropAggregate(Session session) {
        super(session);
    }

    public int update() {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        UserAggregate aggregate = db.findAggregate(name);
        if (aggregate == null) {
            if (!ifExists) {
                throw DbException.get(ErrorCode.AGGREGATE_NOT_FOUND_1, name);
            }
        } else {
            db.removeDatabaseObject(session, aggregate);
        }
        return 0;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }

    public int getType() {
        return CommandInterface.DROP_AGGREGATE;
    }

}
