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

package com.caucho.quercus.env;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;

/**
 * Represents the introspected static function information.
 */
abstract public class AbstractJavaMethod extends AbstractFunction
{
  private static final L10N L = new L10N(AbstractJavaMethod.class);

  /**
   * Returns the minimally required number of arguments.
   */
  abstract public int getMinArgLength();

  /**
   * Returns the maximum number of arguments allowed.
   */
  abstract public int getMaxArgLength();

  /**
   * Returns true if the function can take in unlimited number of args.
   */
  abstract public boolean getHasRestArgs();

  abstract public int getMarshalingCost(Value []args);
   public int getMarshalingCost(V<? extends Value> []args) {
     return getMarshalingCost(VHelper.arrayToOne(args));
   }

  abstract public int getMarshalingCost(Expr []args);

  @Override
  public boolean isJavaMethod()
  {
    return true;
  }

  public Class<?> getJavaDeclaringClass()
  {
    return null;
  }

  public Class<?> []getJavaParameterTypes()
  {
    return null;
  }

  /**
   * Returns an overloaded java method.
   */
  public AbstractJavaMethod overload(AbstractJavaMethod fun)
  {
    // same method can occur for interfaces and overrides
    if (isSameMethod(this, fun)) {
      // php/5220
      if (getJavaDeclaringClass().isAssignableFrom(fun.getJavaDeclaringClass()))
        return fun;
      else
        return this;
    }

    AbstractJavaMethod method = new JavaOverloadMethod(this);

    method = method.overload(fun);

    return method;
  }

  /**
   * Checks for the same method, e.g. for multiple interfaces declaring
   * the same method.
   */
  private boolean isSameMethod(AbstractJavaMethod funA,
                               AbstractJavaMethod funB)
  {
    Class<?> []paramTypesA = funA.getJavaParameterTypes();
    Class<?> []paramTypesB = funB.getJavaParameterTypes();

    if (paramTypesA == null || paramTypesB == null)
      return false;

    if (paramTypesA.length != paramTypesB.length)
      return false;

    for (int i = 0; i < paramTypesA.length; i++) {
      if (! paramTypesA[i].equals(paramTypesB[i]))
        return false;
    }

    return true;
  }

  @Override
  abstract public V<? extends ValueOrVar> callMethod(Env env,
                                                     FeatureExpr ctx,
                                                     QuercusClass qClass,
                                                     Value qThis,
                                                     V<? extends ValueOrVar>[] args);

  /**
   * Evaluates the function, returning a copy
   */
  @Override
  public V<? extends Value> callCopy(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args)
  {
    return call(env, ctx, args).map((a) -> a.toValue());
  }

  @Override
  public V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args)
  {
    return callMethod(env, ctx, getQuercusClass(), (Value) null, args);
  }

}
