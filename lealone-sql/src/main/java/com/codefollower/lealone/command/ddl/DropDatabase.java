/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.command.ddl;

import java.util.ArrayList;

import com.codefollower.lealone.command.CommandInterface;
import com.codefollower.lealone.dbobject.DbObject;
import com.codefollower.lealone.dbobject.Role;
import com.codefollower.lealone.dbobject.Schema;
import com.codefollower.lealone.dbobject.SchemaObject;
import com.codefollower.lealone.dbobject.User;
import com.codefollower.lealone.dbobject.table.Table;
import com.codefollower.lealone.engine.Database;
import com.codefollower.lealone.engine.Session;
import com.codefollower.lealone.util.New;

/**
 * This class represents the statement
 * DROP ALL OBJECTS
 */
public class DropDatabase extends DefineCommand {

    private boolean dropAllObjects;
    private boolean deleteFiles;

    public DropDatabase(Session session) {
        super(session);
    }

    public int update() {
        if (dropAllObjects) {
            dropAllObjects();
        }
        if (deleteFiles) {
            session.getDatabase().setDeleteFilesOnDisconnect(true);
        }
        return 0;
    }

    private void dropAllObjects() {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        db.lockMeta(session);
        // TODO local temp tables are not removed
        for (Schema schema : db.getAllSchemas()) {
            if (schema.canDrop()) {
                db.removeDatabaseObject(session, schema);
            }
        }
        ArrayList<Table> tables = db.getAllTablesAndViews(false);
        for (Table t : tables) {
            if (t.getName() != null && Table.VIEW.equals(t.getTableType())) {
                db.removeSchemaObject(session, t);
            }
        }
        for (Table t : tables) {
            if (t.getName() != null && Table.TABLE_LINK.equals(t.getTableType())) {
                db.removeSchemaObject(session, t);
            }
        }
        for (Table t : tables) {
            if (t.getName() != null && Table.TABLE.equals(t.getTableType()) && !t.isHidden()) {
                db.removeSchemaObject(session, t);
            }
        }
        for (Table t : tables) {
            if (t.getName() != null && Table.EXTERNAL_TABLE_ENGINE.equals(t.getTableType()) && !t.isHidden()) {
                db.removeSchemaObject(session, t);
            }
        }
        session.findLocalTempTable(null);
        ArrayList<SchemaObject> list = New.arrayList();
        list.addAll(db.getAllSchemaObjects(DbObject.SEQUENCE));
        // maybe constraints and triggers on system tables will be allowed in
        // the future
        list.addAll(db.getAllSchemaObjects(DbObject.CONSTRAINT));
        list.addAll(db.getAllSchemaObjects(DbObject.TRIGGER));
        list.addAll(db.getAllSchemaObjects(DbObject.CONSTANT));
        list.addAll(db.getAllSchemaObjects(DbObject.FUNCTION_ALIAS));
        for (SchemaObject obj : list) {
            if (obj.isHidden()) {
                continue;
            }
            db.removeSchemaObject(session, obj);
        }
        for (User user : db.getAllUsers()) {
            if (user != session.getUser()) {
                db.removeDatabaseObject(session, user);
            }
        }
        for (Role role : db.getAllRoles()) {
            String sql = role.getCreateSQL();
            // the role PUBLIC must not be dropped
            if (sql != null) {
                db.removeDatabaseObject(session, role);
            }
        }
        ArrayList<DbObject> dbObjects = New.arrayList();
        dbObjects.addAll(db.getAllRights());
        dbObjects.addAll(db.getAllAggregates());
        dbObjects.addAll(db.getAllUserDataTypes());
        for (DbObject obj : dbObjects) {
            String sql = obj.getCreateSQL();
            // the role PUBLIC must not be dropped
            if (sql != null) {
                db.removeDatabaseObject(session, obj);
            }
        }
    }

    public void setDropAllObjects(boolean b) {
        this.dropAllObjects = b;
    }

    public void setDeleteFiles(boolean b) {
        this.deleteFiles = b;
    }

    public int getType() {
        return CommandInterface.DROP_ALL_OBJECTS;
    }

}
