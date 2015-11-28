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
 * @author Nam Nguyen
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a PHP instanceof expression in which the right side is a variable
 * holding the string class name.
 */
public class BinaryInstanceOfVarExpr extends AbstractBinaryExpr {

  public BinaryInstanceOfVarExpr(Location location, Expr left, Expr right)
  {
    super(location, left, right);
  }

  public BinaryInstanceOfVarExpr(Expr left, Expr right)
  {
    super(left, right);
  }

  /**
   * Returns true for a boolean.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Evaluates the equality as a boolean.
   */
  public @NonNull V<? extends Value> eval(Env env, FeatureExpr ctx)
  {
    return evalBoolean(env, VHelper.noCtx()).map((a)->a ? BooleanValue.TRUE : BooleanValue.FALSE);
  }

  /**
   * Evaluates the equality as a boolean.
   */
  @Override
  public V<Boolean> evalBoolean(Env env, FeatureExpr ctx)
  {
    V<? extends Value> lValue = _left.eval(env, VHelper.noCtx());
    V<? extends Value> rValue = _right.eval(env, VHelper.noCtx());

    return VHelper.mapAll(lValue,rValue,(l,r)-> l.isA(env,r));
  }

  public String toString()
  {
    return "(" + _left + " instanceof " + _right + ")";
  }
}
