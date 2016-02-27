/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.*;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * Represents the array function
 */
public class FunArrayExpr extends Expr {
  protected final Expr []_keys;
  protected final Expr []_values;

  public FunArrayExpr(Location location,
                      ArrayList<Expr> keyList,
                      ArrayList<Expr> valueList)
  {
    super(location);

    _keys = new Expr[keyList.size()];
    keyList.toArray(_keys);

    _values = new Expr[valueList.size()];
    valueList.toArray(_values);
  }

  public FunArrayExpr(Location location, Expr []keys, Expr []values)
  {
    super(location);
    _keys = keys;
    _values = values;
  }

  public FunArrayExpr(ArrayList<Expr> keyList, ArrayList<Expr> valueList)
  {
    this(Location.UNKNOWN, keyList, valueList);
  }

  public FunArrayExpr(Expr []keys, Expr []values)
  {
    this(Location.UNKNOWN, keys, values);
  }

  /**
   * Returns true if the expression evaluates to an array.
   */
  @Override
  public boolean isArray()
  {
    return true;
  }

  /**
   * Returns true for a constant array.
   */
  @Override
  public boolean isConstant()
  {
    for (int i = 0; i < _keys.length; i++) {
      if (_keys[i] != null && ! _keys[i].isConstant())
        return false;
    }

    for (int i = 0; i < _values.length; i++) {
      if (_values[i] != null && ! _values[i].isConstant())
        return false;
    }

    return true;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  @Nonnull protected V<? extends ValueOrVar> _eval(Env env, FeatureExpr ctx)
  {
    ArrayValue array = new ArrayValueImpl();

    for (int i = 0; i < _values.length; i++) {
      Expr keyExpr = _keys[i];

      V<? extends ValueOrVar> value =
              _values[i].evalArg(env, ctx, true).
                      flatMap((a)->a.toRefValue());

      if (keyExpr != null) {
        V<? extends Value> key = VHelper.getValues(keyExpr.evalArg(env, ctx, true));

        key.sforeach(ctx, (c, k) ->
          array.put(c, k.toLocalValue(), value));
      }
      else
        array.put(ctx, value);
    }

    return VHelper.toV(array);
  }

  public String toString()
  {
    return "array()";
  }
}

