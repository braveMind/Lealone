/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.command.dml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.codefollower.lealone.command.CommandInterface;
import com.codefollower.lealone.command.Prepared;
import com.codefollower.lealone.constant.Constants;
import com.codefollower.lealone.engine.Session;
import com.codefollower.lealone.message.DbException;
import com.codefollower.lealone.result.ResultInterface;
import com.codefollower.lealone.util.ScriptReader;

/**
 * This class represents the statement
 * RUNSCRIPT
 */
public class RunScriptCommand extends ScriptBase {

    /**
     * The byte order mark.
     * 0xfeff because this is the Unicode char
     * represented by the UTF-8 byte order mark (EF BB BF).
     */
    private static final char UTF8_BOM = '\uFEFF';

    private String charset = Constants.UTF8;

    public RunScriptCommand(Session session) {
        super(session);
    }

    public int update() {
        session.getUser().checkAdmin();
        int count = 0;
        try {
            openInput();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            // if necessary, strip the BOM from the front of the file
            reader.mark(1);
            if (reader.read() != UTF8_BOM) {
                reader.reset();
            }
            ScriptReader r = new ScriptReader(reader);
            while (true) {
                String sql = r.readStatement();
                if (sql == null) {
                    break;
                }
                execute(sql);
                count++;
                if ((count & 127) == 0) {
                    checkCanceled();
                }
            }
            reader.close();
        } catch (IOException e) {
            throw DbException.convertIOException(e, null);
        } finally {
            closeIO();
        }
        return count;
    }

    private void execute(String sql) {
        try {
            Prepared command = session.prepare(sql);
            if (command.isQuery()) {
                command.query(0);
            } else {
                command.update();
            }
            if (session.getAutoCommit()) {
                session.commit(false);
            }
        } catch (DbException e) {
            throw e.addSQL(sql);
        }
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public ResultInterface queryMeta() {
        return null;
    }

    public int getType() {
        return CommandInterface.RUNSCRIPT;
    }

}
