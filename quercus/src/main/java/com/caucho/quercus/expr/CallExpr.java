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
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

//import java.io.FileWriter;
//import java.io.IOException;

/**
 * A "foo(...)" function call.
 */
public class CallExpr extends Expr {
  private static final L10N L = new L10N(CallExpr.class);

  protected final StringValue _name;
  protected final StringValue _nsName;
  protected final Expr []_args;

  private V<? extends @NonNull Integer> _funId;

  protected boolean _isRef;

  public CallExpr(Location location, StringValue name, ArrayList<Expr> args)
  {
    // quercus/120o
    super(location);
    _name = name;

    int ns = _name.lastIndexOf('\\');

    if (ns > 0) {
      _nsName = _name.substring(ns + 1);
    }
    else {
      _nsName = null;
    }

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public CallExpr(Location location, StringValue name, Expr []args)
  {
    // quercus/120o
    super(location);
    _name = name;

    int ns = _name.lastIndexOf('\\');

    if (ns > 0) {
      _nsName = _name.substring(ns + 1);
    }
    else
      _nsName = null;

    _args = args;
  }

  /**
   * Returns the name.
   */
  public StringValue getName()
  {
    return _name;
  }

  /**
   * Returns the location if known.
   */
  public String getFunctionLocation()
  {
    return " [" + _name + "]";
  }

  /**
   * Returns the reference of the value.
   * @param location
   */
  /*
  @Override
  public Expr createRef(QuercusParser parser)
  {
    return parser.getExprFactory().createCallRef(this);
  }
  */

  /**
   * Returns the copy of the value.
   * @param location
   */
  @Override
  public Expr createCopy(ExprFactory factory)
  {
    return this;
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
  public @NonNull V<? extends Value> eval(Env env, FeatureExpr ctx)
  {
    return evalImpl(env, ctx, false, false);
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
  public @NonNull V<? extends Value> evalCopy(Env env, FeatureExpr ctx)
  {
    return evalImpl(env, ctx, false, true);
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
  public V<? extends ValueOrVar> evalRef(Env env, FeatureExpr ctx)
  {
    return evalImpl(env, ctx, true, true);
  }


//  static FileWriter w;
//  static {
//  try {
//    w=new FileWriter("c:\\php\\trace\\t.txt");
//  } catch (IOException e) {
//  }
//  };

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  private V<? extends Value> evalImpl(Env env, FeatureExpr _ctx, boolean isRef, boolean isCopy)
  {
//    try {
//      w.write(_name+" - "+this.getLocation().toString()+"\n");
//      w.flush();
//    } catch (IOException e) {
//    }
    if (_funId==null)
      _funId=lookupFunId(env);

    return _funId.vflatMap(_ctx, (ctx,funId)-> {

      if (funId <= 0) {
        env.error(ctx, L.l("'{0}' is an unknown function.", _name), getLocation());
        return V.one(NullValue.NULL);
      }

      AbstractFunction fun = env.getFunction(funId);

      if (fun == null) {
        env.error(ctx, L.l("'{0}' is an unknown function.", _name), getLocation());

        return VHelper.toV(NullValue.NULL);
      }

      Value[] args = evalArgs(env, _args, ctx).getOne();

      env.pushCall(this, NullValue.NULL, args);

      // php/0249
      QuercusClass oldCallingClass = env.setCallingClass(null);

      // XXX: qa/1d14 Value oldThis = env.setThis(UnsetValue.NULL);
      try {
        env.checkTimeout();

        /*
        if (isRef)
          return fun.callRef(env, args);
        else if (isCopy)
          return fun.callCopy(env, args);
        else
          return fun.call(env, args);
          */

        if (isRef)
          return fun.callRef(env, ctx, args);
        else if (isCopy)
          return fun.call(env, ctx, args).map((a) -> a.copyReturn());
        else {
          return fun.call(env, ctx, args).map((a) -> a.toValue());
        }
        //} catch (Exception e) {
        //  throw QuercusException.create(e, env.getStackTrace());
      } finally {
        env.popCall();
        env.setCallingClass(oldCallingClass);
        // XXX: qa/1d14 env.setThis(oldThis);
      }
    });
  }

  private @NonNull V<? extends @NonNull Integer> lookupFunId(Env env) {
    return VHelper.<Integer>vifTry(
            () -> env.findFunctionId(_name),
            () -> (_nsName != null) ? env.findFunctionId(_nsName) : V.one(0),
            (id) -> id > 0);
  }

  // Return an array containing the Values to be
  // passed in to this function.

  public Value []evalArguments(Env env)
  {
    AbstractFunction fun = env.findFunction(_name).getOne();

    if (fun == null) {
      return null;
    }

    return fun.evalArguments(env, this, _args);
  }

  public String toString()
  {
    return _name + "()";
  }
}

