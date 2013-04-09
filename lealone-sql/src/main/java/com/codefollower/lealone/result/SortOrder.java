/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.codefollower.lealone.constant.SysProperties;
import com.codefollower.lealone.engine.Database;
import com.codefollower.lealone.expression.Expression;
import com.codefollower.lealone.util.StatementBuilder;
import com.codefollower.lealone.util.StringUtils;
import com.codefollower.lealone.util.Utils;
import com.codefollower.lealone.value.Value;
import com.codefollower.lealone.value.ValueNull;

/**
 * A sort order represents an ORDER BY clause in a query.
 */
public class SortOrder implements Comparator<Value[]> {

    /**
     * This bit mask means the values should be sorted in ascending order.
     */
    public static final int ASCENDING = 0;

    /**
     * This bit mask means the values should be sorted in descending order.
     */
    public static final int DESCENDING = 1;

    /**
     * This bit mask means NULLs should be sorted before other data, no matter
     * if ascending or descending order is used.
     */
    public static final int NULLS_FIRST = 2;

    /**
     * This bit mask means NULLs should be sorted after other data, no matter
     * if ascending or descending order is used.
     */
    public static final int NULLS_LAST = 4;

    /**
     * The default sort order for NULL.
     */
    private static final int DEFAULT_NULL_SORT = SysProperties.SORT_NULLS_HIGH ? 1 : -1;

    private final Database database;
    private final int[] indexes;
    private final int[] sortTypes;

    /**
     * Construct a new sort order object.
     *
     * @param database the database
     * @param index the column index list
     * @param sortType the sort order bit masks
     */
    public SortOrder(Database database, int[] index, int[] sortType) {
        this.database = database;
        this.indexes = index;
        this.sortTypes = sortType;
    }

    /**
     * Create the SQL snippet that describes this sort order.
     * This is the SQL snippet that usually appears after the ORDER BY clause.
     *
     * @param list the expression list
     * @param visible the number of columns in the select list
     * @return the SQL snippet
     */
    public String getSQL(Expression[] list, int visible) {
        StatementBuilder buff = new StatementBuilder();
        int i = 0;
        for (int idx : indexes) {
            buff.appendExceptFirst(", ");
            if (idx < visible) {
                buff.append(idx + 1);
            } else {
                buff.append('=').append(StringUtils.unEnclose(list[idx].getSQL()));
            }
            int type = sortTypes[i++];
            if ((type & DESCENDING) != 0) {
                buff.append(" DESC");
            }
            if ((type & NULLS_FIRST) != 0) {
                buff.append(" NULLS FIRST");
            } else if ((type & NULLS_LAST) != 0) {
                buff.append(" NULLS LAST");
            }
        }
        return buff.toString();
    }

    /**
     * Compare two expressions where one of them is NULL.
     *
     * @param aNull whether the first expression is null
     * @param sortType the sort bit mask to use
     * @return the result of the comparison (-1 meaning the first expression
     *         should appear before the second, 0 if they are equal)
     */
    public static int compareNull(boolean aNull, int sortType) {
        if ((sortType & NULLS_FIRST) != 0) {
            return aNull ? -1 : 1;
        } else if ((sortType & NULLS_LAST) != 0) {
            return aNull ? 1 : -1;
        } else {
            // see also JdbcDatabaseMetaData.nullsAreSorted*
            int comp = aNull ? DEFAULT_NULL_SORT : -DEFAULT_NULL_SORT;
            return (sortType & DESCENDING) == 0 ? comp : -comp;
        }
    }

    /**
     * Compare two expression lists.
     *
     * @param a the first expression list
     * @param b the second expression list
     * @return the result of the comparison
     */
    public int compare(Value[] a, Value[] b) {
        for (int i = 0, len = indexes.length; i < len; i++) {
            int idx = indexes[i];
            int type = sortTypes[i];
            Value ao = a[idx];
            Value bo = b[idx];
            boolean aNull = ao == ValueNull.INSTANCE, bNull = bo == ValueNull.INSTANCE;
            if (aNull || bNull) {
                if (aNull == bNull) {
                    continue;
                }
                return compareNull(aNull, type);
            }
            int comp = database.compare(ao, bo);
            if (comp != 0) {
                return (type & DESCENDING) == 0 ? comp : -comp;
            }
        }
        return 0;
    }

    /**
     * Sort a list of rows.
     *
     * @param rows the list of rows
     */
    public void sort(ArrayList<Value[]> rows) {
        Collections.sort(rows, this);
    }

    /**
     * Sort a list of rows using offset and limit.
     *
     * @param rows the list of rows
     * @param offset the offset
     * @param limit the limit
     */
    public void sort(ArrayList<Value[]> rows, int offset, int limit) {
        if (rows.isEmpty()) {
            return;
        }
        if (limit == 1 && offset == 0) {
             rows.set(0, Collections.min(rows, this));
             return;
        }
        Value[][] arr = rows.toArray(new Value[rows.size()][]);
        Utils.sortTopN(arr, offset, limit, this);
        for (int i = 0, end = Math.min(offset + limit, arr.length); i < end; i++) {
            rows.set(i, arr[i]);
        }
    }

    /**
     * Get the column index list.
     *
     * @return the list
     */
    public int[] getIndexes() {
        return indexes;
    }

    /**
     * Get the sort order bit masks.
     *
     * @return the list
     */
    public int[] getSortTypes() {
        return sortTypes;
    }

}
