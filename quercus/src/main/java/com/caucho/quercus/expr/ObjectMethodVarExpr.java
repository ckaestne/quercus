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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ValueOrVar;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * A "$foo->$bar(...)" method call
 */
public class ObjectMethodVarExpr extends Expr {
  private static final L10N L = new L10N(ObjectMethodVarExpr.class);

  protected final Expr _objExpr;

  protected final Expr _name;
  protected final Expr []_args;

  protected Expr []_fullArgs;

  public ObjectMethodVarExpr(Location location,
                             Expr objExpr,
                             Expr name,
                             ArrayList<Expr> args)
  {
    super(location);
    _objExpr = objExpr;

    _name = name;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public ObjectMethodVarExpr(Location location,
                             Expr objExpr,
                             Expr name,
                             Expr []args)
  {
    super(location);
    _objExpr = objExpr;

    _name = name;

    _args = args;
  }

  public ObjectMethodVarExpr(Expr objExpr, Expr name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, objExpr, name, args);
  }

  public ObjectMethodVarExpr(Expr objExpr, Expr name, Expr []args)
  {
    this(Location.UNKNOWN, objExpr, name, args);
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
    V<?extends ValueOrVar> []values = new V[_args.length];

    for (int i = 0; i < values.length; i++) {
      values[i] = _args[i].evalArg(env, ctx, true);
    }

    StringValue methodName = _name.eval(env, ctx).getOne().toStringValue(env);

    Value obj = _objExpr.eval(env, ctx).getOne();

    env.pushCall(this, obj, values);

    try {
      env.checkTimeout();

      return obj.callMethod(env, ctx, methodName, values).map((a) -> a.toValue());
    } finally {
      env.popCall();
    }
  }

  public String toString()
  {
    return _objExpr + "->" + _name + "()";
  }
}

