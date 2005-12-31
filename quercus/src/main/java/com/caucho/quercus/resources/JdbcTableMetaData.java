/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.resources;

import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.*;

import com.caucho.util.Log;

import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.env.*;

/**
 * Represents a JDBC column metadata
 */
public class JdbcTableMetaData {
  private final String _catalog;
  private final String _schema;
  private final String _name;

  private final HashMap<String,JdbcColumnMetaData> _columnMap
    = new HashMap<String,JdbcColumnMetaData>();

  public JdbcTableMetaData(String catalog,
			   String schema,
			   String name,
			   DatabaseMetaData md)
    throws SQLException
  {
    _catalog = catalog;
    _schema = schema;
    _name = name;

    ResultSet rs = md.getColumns(_catalog, _schema, _name, null);
    try {
      while (rs.next()) {
	String columnName = rs.getString("COLUMN_NAME");

	JdbcColumnMetaData column = new JdbcColumnMetaData(this, rs);

	_columnMap.put(columnName, column);
      }

      rs.close();

      rs = md.getPrimaryKeys(_catalog, _schema, _name);
      while (rs.next()) {
	String columnName = rs.getString("COLUMN_NAME");

	JdbcColumnMetaData column = _columnMap.get(columnName);

	column.setPrimaryKey(true);
      }
      rs.close();

      rs = md.getIndexInfo(_catalog, _schema, _name, false, true);
      while (rs.next()) {
	String columnName = rs.getString("COLUMN_NAME");

	JdbcColumnMetaData column = _columnMap.get(columnName);

	column.setIndex(true);
      }
    } finally {
      rs.close();
    }
  }

  /**
   * Returns the table's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the table's catalog
   */
  public String getCatalog()
  {
    return _catalog;
  }

  /**
   * Returns the matching column.
   */
  public JdbcColumnMetaData getColumn(String name)
  {
    return _columnMap.get(name);
  }

  public String toString()
  {
    return "JdbcTableMetaData[" + getName() + "]";
  }
}

