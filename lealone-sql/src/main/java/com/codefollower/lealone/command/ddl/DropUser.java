/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.command.ddl;

import com.codefollower.lealone.command.CommandInterface;
import com.codefollower.lealone.constant.ErrorCode;
import com.codefollower.lealone.dbobject.User;
import com.codefollower.lealone.engine.Database;
import com.codefollower.lealone.engine.Session;
import com.codefollower.lealone.message.DbException;

/**
 * This class represents the statement
 * DROP USER
 */
public class DropUser extends DefineCommand {

    private boolean ifExists;
    private String userName;

    public DropUser(Session session) {
        super(session);
    }

    public void setIfExists(boolean b) {
        ifExists = b;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int update() {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        User user = db.findUser(userName);
        if (user == null) {
            if (!ifExists) {
                throw DbException.get(ErrorCode.USER_NOT_FOUND_1, userName);
            }
        } else {
            if (user == session.getUser()) {
                int adminUserCount = 0;
                for (User u : db.getAllUsers()) {
                    if (u.isAdmin()) {
                        adminUserCount++;
                    }
                }
                if (adminUserCount == 1) {
                    throw DbException.get(ErrorCode.CANNOT_DROP_CURRENT_USER);
                }
            }
            user.checkOwnsNoSchemas();
            db.removeDatabaseObject(session, user);
        }
        return 0;
    }

    public boolean isTransactional() {
        return false;
    }

    public int getType() {
        return CommandInterface.DROP_USER;
    }

}
