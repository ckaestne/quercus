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
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ValueOrVar;
import com.caucho.quercus.env.Var;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;

import javax.annotation.Nonnull;

/**
 * Represents a PHP array reference expression.
 */
public class ArrayGetExpr extends AbstractVarExpr {
  protected final Expr _expr;
  protected final Expr _index;

  public ArrayGetExpr(Location location, Expr expr, Expr index)
  {
    super(location);
    _expr = expr;
    _index = index;
  }

  public ArrayGetExpr(Expr expr, Expr index)
  {
    _expr = expr;
    _index = index;
  }

  /**
   * Returns the expr.
   */
  public Expr getExpr()
  {
    return _expr;
  }

  /**
   * Returns the index.
   */
  public Expr getIndex()
  {
    return _index;
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
  protected @Nonnull
  V<? extends ValueOrVar> _eval(Env env, FeatureExpr ctx)
  {
    V<? extends Value> array = _expr.eval(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.flatMapAll(array, index,(a,i)-> a.get(i).getValue());
  }

  /**
   * Evaluates the expression as a copyable result.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public @Nonnull V<? extends Value> evalCopy(Env env, FeatureExpr ctx)
  {
    V<? extends Value> array = _expr.eval(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.smapAll(ctx, array, index, (a, i) -> a.get(i).getOne().copy());
  }

  /**
   * Evaluates the expression, creating an array if the value is unset.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public @Nonnull
  V<? extends ValueOrVar> evalArray(Env env, FeatureExpr ctx)
  {
    V<? extends ValueOrVar> array = _expr.evalArray(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.sflatMapAll(ctx, VHelper.getValues(array), index, (c, a, i) -> a.getArray(c, i));
  }

  /**
   * Evaluates the expression, marking as dirty.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public @Nonnull V<? extends Value> evalDirty(Env env, FeatureExpr ctx)
  {
    V<? extends Value> array = _expr.eval(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.smapAll(ctx, array, index, (a, i) -> a.getDirty(i).getOne());
  }

  /**
   * Evaluates the expression, creating an object if the value is unset.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public @Nonnull V<? extends Value> evalObject(Env env, FeatureExpr ctx)
  {
    V<? extends ValueOrVar> array = _expr.evalArray(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.sflatMapAll(ctx, VHelper.getValues(array), index, (c, a, i) -> a.getObject(env, c, i));
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
  public V<? extends ValueOrVar> evalArg(Env env, FeatureExpr ctx, boolean isTop)
  {
    // php/0d2t
    // php/0d1c
    V<? extends ValueOrVar> array = _expr.evalArg(env, ctx, false);
    V<? extends Value> index = _index.eval(env, ctx);

    V<? extends Var> result = VHelper.flatMapAll(VHelper.getValues(array), index,(a,i)-> a.getArg(i, isTop).getVar());

    return result;
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
  public V<? extends Var> evalVar(Env env, FeatureExpr ctx)
  {
    V<? extends ValueOrVar> array = _expr.evalArray(env, ctx);
    V<? extends Value> index = _index.eval(env, ctx);

    return VHelper.flatMapAll(VHelper.getValues(array), index, (a, i) -> a.getVar(ctx, i).getVar());
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
  public @Nonnull V<? extends Value> evalAssignValue(Env env, FeatureExpr ctx, Expr valueExpr)
  {
    // php/03mk, php/03mm, php/03mn, php/04b3
    // php/04ah
    V<? extends Value> array = _expr.evalArrayAssign(env, ctx, _index, valueExpr);

    return array;
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
  public V<? extends ValueOrVar> evalAssignRef(Env env, FeatureExpr ctx, Expr valueExpr)
  {
    // php/03mk
    // php/04ai
    return _expr.evalArrayAssignRef(env, ctx, _index, valueExpr);
  }

  @Override
  public V<? extends ValueOrVar> evalAssignRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> value)
  {
    return _expr.evalArrayAssignRef(env, ctx, _index, value);
  }

  /**
   * Evaluates the expression as an isset().
   */
  @Override
  public V<? extends Boolean> evalIsset(Env env, FeatureExpr ctx)
  {
    V<? extends Value> array = _expr.evalIssetValue(env, ctx);
    V<? extends Value> index = _index.evalIssetValue(env, ctx);

    return VHelper.smapAll(ctx, array, index, (a, i) -> a.isset(i));
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
  public void evalUnset(Env env, FeatureExpr ctx)
  {
    _expr.evalUnsetArray(env, ctx, _index);
  }

  /**
   * Evaluates as an empty() expression.
   */
  @Override
  public V<? extends Boolean> evalEmpty(Env env, FeatureExpr ctx)
  {
    V<? extends Value> array = _expr.evalIssetValue(env, ctx);
    V<? extends Value> index = _index.evalIssetValue(env, ctx);

    return VHelper.flatMapAll(array, index,(a,i)-> a.isEmpty(env, i));
  }

  @Override
  public String toString()
  {
    return _expr + "[" + _index + "]";
  }
}

