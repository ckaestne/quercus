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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.program.ClassField;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.UnimplementedVException;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import edu.cmu.cs.varex.VWriteStream;
import edu.cmu.cs.varex.annotation.VDeprecated;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Represents a PHP expression value.
 */
@SuppressWarnings("serial")
abstract public class Value implements java.io.Serializable, ValueOrVar {
  protected static final L10N L = new L10N(Value.class);

  private static final V<? extends ValueOrVar>[] NULL_ARG_VALUES = new V[0];

  public static final Value[] NULL_VALUE_ARRAY = new Value[0];
  public static final Value[] NULL_ARGS = new Value[0];
  public static final V<? extends ValueOrVar>[] VNULL_ARGS = new V[0];

  //
  // Properties
  //

  /**
   * Returns the value's class name.
   */
  public String getClassName() {
    return getType();
  }

  /**
   * Returns the backing QuercusClass.
   */
  public QuercusClass getQuercusClass() {
    return null;
  }

  /**
   * Returns the Quercus class for this obj/name.
   */
  public QuercusClass findQuercusClass(Env env) {
    QuercusClass cls = getQuercusClass();

    if (cls != null) {
      return cls;
    }

    String name = toString();

    return env.getClass(name);
  }

  /**
   * Returns the called class
   */
  public Value getCalledClass(Env env) {
    QuercusClass qClass = getQuercusClass();

    if (qClass != null)
      return env.createString(qClass.getName());
    else {
      env.warning(L.l("get_called_class() must be called in a class context"));

      return BooleanValue.FALSE;
    }
  }

  //
  // Predicates and Relations
  //

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(Env env, String name) {
    return false;
  }

  /**
   * Returns true for an implementation of a class
   */
  final public boolean isA(Env env, Value value) {
    if (value.isObject()) {
      // php/03p7
      return isA(env, value.getClassName());
    } else if (value instanceof QuercusClass) {
      // php/1277
      return isA(env, value.getClassName());
    } else {
      return isA(env, value.toJavaString());
    }
  }

  /**
   * Checks if 'this' is a valid protected call for 'className'
   */
  public void checkProtected(Env env, String className) {
  }

  /**
   * Checks if 'this' is a valid private call for 'className'
   */
  public void checkPrivate(Env env, String className) {
  }

  /**
   * Returns the ValueType.
   */
  public ValueType getValueType() {
    return ValueType.VALUE;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray() {
    return false;
  }

  /**
   * Returns true for a double-value.
   */
  public boolean isDoubleConvertible() {
    return false;
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isLongConvertible() {
    return false;
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isLong() {
    return false;
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isDouble() {
    return false;
  }

  /**
   * Returns true for a null.
   */
  public boolean isNull() {
    return false;
  }

  /**
   * Returns true for a number.
   */
  public boolean isNumberConvertible() {
    return isLongConvertible() || isDoubleConvertible();
  }

  /**
   * Matches is_numeric
   */
  public boolean isNumeric() {
    return false;
  }

  /**
   * Returns true for an object.
   */
  public boolean isObject() {
    return false;
  }

  /**
   * Returns true for a resource.
   */
  public boolean isResource() {
    return false;
  }

  /**
   * Returns true for a StringValue.
   */
  public boolean isString() {
    return false;
  }

  /**
   * Returns true for a BinaryValue.
   */
  public boolean isBinary() {
    return false;
  }

  /**
   * Returns true for a UnicodeValue.
   */
  public boolean isUnicode() {
    return false;
  }

  /**
   * Returns true for a BooleanValue
   */
  public boolean isBoolean() {
    return false;
  }

  /**
   * Returns true for a DefaultValue
   */
  public boolean isDefault() {
    return false;
  }

  //
  // marshal costs
  //

  /**
   * Cost to convert to a boolean
   */
  public int toBooleanMarshalCost() {
    return Marshal.COST_TO_BOOLEAN;
  }

  /**
   * Cost to convert to a byte
   */
  public int toByteMarshalCost() {
    return Marshal.COST_INCOMPATIBLE;
  }

  /**
   * Cost to convert to a short
   */
  public int toShortMarshalCost() {
    return Marshal.COST_INCOMPATIBLE;
  }

  /**
   * Cost to convert to an integer
   */
  public int toIntegerMarshalCost() {
    return Marshal.COST_INCOMPATIBLE;
  }

  /**
   * Cost to convert to a long
   */
  public int toLongMarshalCost() {
    return Marshal.COST_INCOMPATIBLE;
  }

  /**
   * Cost to convert to a double
   */
  public int toDoubleMarshalCost() {
    return Marshal.COST_INCOMPATIBLE;
  }

  /**
   * Cost to convert to a float
   */
  public int toFloatMarshalCost() {
    return toDoubleMarshalCost() + 10;
  }

  /**
   * Cost to convert to a character
   */
  public int toCharMarshalCost() {
    return Marshal.COST_TO_CHAR;
  }

  /**
   * Cost to convert to a string
   */
  public int toStringMarshalCost() {
    return Marshal.COST_TO_STRING;
  }

  /**
   * Cost to convert to a byte[]
   */
  public int toByteArrayMarshalCost() {
    return Marshal.COST_TO_BYTE_ARRAY;
  }

  /**
   * Cost to convert to a char[]
   */
  public int toCharArrayMarshalCost() {
    return Marshal.COST_TO_CHAR_ARRAY;
  }

  /**
   * Cost to convert to a Java object
   */
  public int toJavaObjectMarshalCost() {
    return Marshal.COST_TO_JAVA_OBJECT;
  }

  /**
   * Cost to convert to a binary value
   */
  public int toBinaryValueMarshalCost() {
    return Marshal.COST_TO_STRING + 1;
  }

  /**
   * Cost to convert to a StringValue
   */
  public int toStringValueMarshalCost() {
    return Marshal.COST_TO_STRING + 1;
  }

  /**
   * Cost to convert to a UnicodeValue
   */
  public int toUnicodeValueMarshalCost() {
    return Marshal.COST_TO_STRING + 1;
  }

  //
  // predicates
  //

  /**
   * Returns true if the value is set.
   */
  public boolean isset() {
    return true;
  }

  /**
   * Returns true if the value is empty
   */
  public V<? extends Boolean> isEmpty() {
    return V.one(false);
  }

  /**
   * Returns true if the value is empty
   */
  public V<? extends Boolean> isEmpty(Env env, Value index) {
    return isEmpty();
  }

  /**
   * Returns true if there are more elements.
   */
  public V<? extends Boolean> hasCurrent() {
    return V.one(false);
  }

  /**
   * Returns true for equality
   */
  public Value eqValue(Value rValue) {
    return eq(rValue) ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue) {
    if (rValue.isArray())
      return false;
    else if (rValue.isObject()) {
      return rValue.eq(this);
    } else if (rValue.isBoolean())
      return toBoolean() == rValue.toBoolean();
    else if (isLongConvertible() && rValue.isLongConvertible())
      return toLong() == rValue.toLong();
    else if (isNumberConvertible() || rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue) {
    return this == rValue.toValue();
  }

  /**
   * Returns a negative/positive integer if this Value is
   * lessthan/greaterthan rValue.
   */
  public int cmp(Value rValue) {
    // This is tricky: implemented according to Table 15-5 of
    // http://us2.php.net/manual/en/language.operators.comparison.php

    Value lVal = toValue();
    Value rVal = rValue.toValue();

    if (lVal instanceof StringValue && rVal instanceof NullValue)
      return ((StringValue) lVal).cmpString(StringValue.EMPTY);

    if (lVal instanceof NullValue && rVal instanceof StringValue)
      return StringValue.EMPTY.cmpString((StringValue) rVal);

    if (lVal instanceof StringValue && rVal instanceof StringValue)
      return ((StringValue) lVal).cmpString((StringValue) rVal);

    if (lVal instanceof NullValue
            || lVal instanceof BooleanValue
            || rVal instanceof NullValue
            || rVal instanceof BooleanValue) {
      boolean lBool = toBoolean();
      boolean rBool = rValue.toBoolean();

      if (!lBool && rBool) return -1;
      if (lBool && !rBool) return 1;
      return 0;
    }

    if (lVal.isObject() && rVal.isObject())
      return ((ObjectValue) lVal).cmpObject((ObjectValue) rVal);

    if ((lVal instanceof StringValue
            || lVal instanceof NumberValue
            || lVal instanceof ResourceValue)
            && (rVal instanceof StringValue
            || rVal instanceof NumberValue
            || rVal instanceof ResourceValue))
      return NumberValue.compareNum(lVal, rVal);

    if (lVal instanceof ArrayValue) return 1;
    if (rVal instanceof ArrayValue) return -1;
    if (lVal instanceof ObjectValue) return 1;
    if (rVal instanceof ObjectValue) return -1;

    // XXX: proper default case?
    throw new RuntimeException(
            "values are incomparable: " + lVal + " <=> " + rVal);
  }

  /**
   * Returns true for less than
   */
  public boolean lt(Value rValue) {
    return cmp(rValue) < 0;
  }

  /**
   * Returns true for less than or equal to
   */
  public boolean leq(Value rValue) {
    return cmp(rValue) <= 0;
  }

  /**
   * Returns true for greater than
   */
  public boolean gt(Value rValue) {
    return cmp(rValue) > 0;
  }

  /**
   * Returns true for greater than or equal to
   */
  public boolean geq(Value rValue) {
    return cmp(rValue) >= 0;
  }

  //
  // Conversions
  //

  public static long toLong(boolean b) {
    return b ? 1 : 0;
  }

  public static long toLong(double d) {
    return (long) d;
  }

  public static long toLong(long l) {
    return l;
  }

  public static long toLong(CharSequence s) {
    return StringValue.parseLong(s);
  }

  public static double toDouble(boolean b) {
    return b ? 1.0 : 0.0;
  }

  public static double toDouble(double d) {
    return d;
  }

  public static double toDouble(long l) {
    return l;
  }

  public static double toDouble(CharSequence s) {
    return StringValue.toDouble(s.toString());
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean() {
    return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong() {
    return toBoolean() ? 1 : 0;
  }

  /**
   * Converts to an int
   */
  public int toInt() {
    return (int) toLong();
  }

  /**
   * Converts to a double.
   */
  public double toDouble() {
    return 0;
  }

  /**
   * Converts to a char
   */
  public char toChar() {
    String s = toString();

    if (s == null || s.length() < 1)
      return 0;
    else
      return s.charAt(0);
  }

  /**
   * Converts to a string.
   *
   * @param env
   */
  public StringValue toString(Env env) {
    return toStringValue(env);
  }

  /**
   * Converts to an array.
   */
  public ArrayValue toArray() {
    return new ArrayValueImpl().append(EnvVar._gen(this));
  }

  /**
   * Converts to an array if null.
   */
  public Value toAutoArray() {
    Env.getCurrent().warning(L.l("'{0}' cannot be used as an array.",
            toDebugString()));

    return this;
  }

  /**
   * Casts to an array.
   */
  public ArrayValue toArrayValue(Env env) {
    env.warning(L.l("'{0}' ({1}) is not assignable to ArrayValue",
            this, getType()));

    return null;
  }

  /**
   * Converts to an object if null.
   */
  public Value toAutoObject(Env env) {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env) {
    ObjectValue obj = env.createObject();

    obj.putField(env, VHelper.noCtx(), env.createString("scalar"), V.one(this));

    return obj;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject() {
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject(Env env, Class<?> type) {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));

    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObjectNotNull(Env env, Class<?> type) {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));

    return null;
  }

  /**
   * Converts to a java boolean object.
   */
  public Boolean toJavaBoolean() {
    return toBoolean() ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Converts to a java byte object.
   */
  public Byte toJavaByte() {
    return new Byte((byte) toLong());
  }

  /**
   * Converts to a java short object.
   */
  public Short toJavaShort() {
    return new Short((short) toLong());
  }

  /**
   * Converts to a java Integer object.
   */
  public Integer toJavaInteger() {
    return new Integer((int) toLong());
  }

  /**
   * Converts to a java Long object.
   */
  public Long toJavaLong() {
    return new Long((int) toLong());
  }

  /**
   * Converts to a java Float object.
   */
  public Float toJavaFloat() {
    return new Float((float) toDouble());
  }

  /**
   * Converts to a java Double object.
   */
  public Double toJavaDouble() {
    return new Double(toDouble());
  }

  /**
   * Converts to a java Character object.
   */
  public Character toJavaCharacter() {
    return new Character(toChar());
  }

  /**
   * Converts to a java String object.
   */
  public String toJavaString() {
    return toString();
  }

  /**
   * Converts to a java Collection object.
   */
  public Collection<?> toJavaCollection(Env env, Class<?> type) {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));

    return null;
  }

  /**
   * Converts to a java List object.
   */
  public List<?> toJavaList(Env env, Class<?> type) {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));

    return null;
  }

  /**
   * Converts to a java Map object.
   */
  public Map<?, ?> toJavaMap(Env env, Class<?> type) {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));

    return null;
  }

  /**
   * Converts to a Java Calendar.
   */
  public Calendar toJavaCalendar() {
    Calendar cal = Calendar.getInstance();

    cal.setTimeInMillis(toLong());

    return cal;
  }

  /**
   * Converts to a Java Date.
   */
  public Date toJavaDate() {
    return new Date(toLong());
  }

  /**
   * Converts to a Java URL.
   */
  public URL toJavaURL(Env env) {
    try {
      return new URL(toString());
    } catch (MalformedURLException e) {
      env.warning(e.getMessage());
      return null;
    }
  }

  /**
   * Converts to a Java Enum.
   */
  public Enum toJavaEnum(Env env, Class cls) {
    String s = toString();

    if (s == null) {
      return null;
    }

    try {
      return Enum.valueOf(cls, s);
    } catch (IllegalArgumentException e) {
      env.warning(e);

      return null;
    }
  }

  /**
   * Converts to a Java BigDecimal.
   */
  public BigDecimal toBigDecimal() {
    return new BigDecimal(toString());
  }

  /**
   * Converts to a Java BigInteger.
   */
  public BigInteger toBigInteger() {
    return new BigInteger(toString());
  }

  /**
   * Converts to an exception.
   */
  public QuercusException toException(Env env, String file, int line) {
    putField(env, VHelper.noCtx(), env.createString("file"), V.one(env.createString(file)));
    putField(env, VHelper.noCtx(), env.createString("line"), V.one(LongValue.create(line)));

    return new QuercusLanguageException(this);
  }

//  /**
//   * Converts to a raw value.
//   */
//  final public Value toValue() {
//    return this;
//  }

  /**
   * Converts to a key.
   */
  public Value toKey() {
    throw new QuercusRuntimeException(L.l("{0} is not a valid key", this));
  }

  /**
   * Convert to a ref.
   */
  public Value toRef() {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   * <p>
   * function foo($a)
   * <p>
   * where $a is never assigned or modified
   */
  public Value toLocalValueReadOnly() {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   * <p>
   * function foo($a)
   * <p>
   * where $a is never assigned, but might be modified, e.g. $a[3] = 9
   */
  public Value toLocalValue() {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   * <p>
   * function foo($a)
   * <p>
   * where $a may be assigned.
   */
  public V<? extends Value> toLocalRef() {
    return V.one(this);
  }

  /**
   * Convert to a function argument value, e.g. for
   * <p>
   * function foo($a)
   * <p>
   * where $a is used as a variable in the function
   */
  @Override
  public Var toLocalVar() {
    return new VarImpl(toLocalRef());
  }

  /**
   * Convert to a function argument reference value, e.g. for
   * <p>
   * function foo(&$a)
   * <p>
   * where $a is used as a variable in the function
   */
  @Override
  public V<? extends Var> toLocalVarDeclAsRef() {
    return V.one(new VarImpl(V.one(this)));
  }

  /**
   * Converts to a local $this, which can depend on the calling class
   */
  public Value toLocalThis(QuercusClass qClass) {
    return this;
  }

  /**
   * Convert to a function argument reference value, e.g. for
   * <p>
   * function foo(&$a)
   * <p>
   * where $a is never assigned in the function
   */
  public V<? extends ValueOrVar> toRefValue() {
    return V.one(this);
  }

  /**
   * Converts to a Var.
   */
  @Override
  public Var toVar() {
    return new VarImpl(V.one(this));
  }

  /**
   * Convert to a function argument reference value, e.g. for
   * <p>
   * function foo(&$a)
   * <p>
   * where $a is used as a variable in the function
   */
  public Value toArgRef() {
    Env.getCurrent()
            .warning(L.l(
                    "'{0}' is an invalid reference, because only "
                            + "variables may be passed by reference.",
                    this));

    return NullValue.NULL;
  }

  /**
   * Converts to a StringValue.
   */
  public StringValue toStringValue() {
    return toStringValue(Env.getInstance());
  }

  /**
   * Converts to a StringValue.
   */
  public StringValue toStringValue(Env env) {
    return toStringBuilder(env);
  }

  /**
   * Converts to a Unicode string.  For unicode.semantics=false, this will
   * still return a StringValue. For unicode.semantics=true, this will
   * return a UnicodeStringValue.
   */
  public StringValue toUnicode(Env env) {
    return toUnicodeValue(env);
  }

  /**
   * Converts to a UnicodeValue for marshaling, so it will create a
   * UnicodeValue event when unicode.semantics=false.
   */
  public StringValue toUnicodeValue() {
    return toUnicodeValue(Env.getInstance());
  }

  /**
   * Converts to a UnicodeValue for marshaling, so it will create a
   * UnicodeValue event when unicode.semantics=false.
   */
  public StringValue toUnicodeValue(Env env) {
    // php/0ci0
    return new UnicodeBuilderValue(env.createString(toString()));
  }

  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue() {
    return toBinaryValue(Env.getInstance());
  }

  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue(String charset) {
    return toBinaryValue();
  }

  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue(Env env) {
    StringValue bb = env.createBinaryBuilder();

    bb.append(VHelper.noCtx(), this);

    return bb;

      /*
    try {
      int length = 0;
      while (true) {
        bb.ensureCapacity(bb.getLength() + 256);

        int sublen = is.read(bb.getBuffer(),
                             bb.getOffset(),
                             bb.getLength() - bb.getOffset());

        if (sublen <= 0)
          return bb;
        else {
          length += sublen;
          bb.setOffset(length);
        }
      }
    } catch (IOException e) {
      throw new QuercusException(e);
    }
      */
  }

  /**
   * Returns a byteArrayInputStream for the value.
   * See TempBufferStringValue for how this can be overriden
   *
   * @return InputStream
   */
  public InputStream toInputStream() {
    return new CharSequenceInputStream(toString());
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder() {
    return toStringBuilder(Env.getInstance());
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env) {
    return env.createUnicodeBuilder().appendUnicode(this);
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, Value value) {
    return toStringBuilder(env).appendUnicode(value);
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value) {
    return toStringBuilder(env).appendUnicode(value);
  }

  /**
   * Converts to a string builder
   */
  public StringValue copyStringBuilder() {
    return toStringBuilder();
  }

  /**
   * Converts to a long vaule
   */
  public LongValue toLongValue() {
    return LongValue.create(toLong());
  }

  /**
   * Converts to a double vaule
   */
  public DoubleValue toDoubleValue() {
    return new DoubleValue(toDouble());
  }

  /**
   * Returns true for a callable object.
   */
  public boolean isCallable(Env env, boolean isCheckSyntaxOnly, Var nameRef) {
    return false;
  }

  /**
   * Returns the callable's name for is_callable()
   */
  public String getCallableName() {
    return null;
  }

  /**
   * Converts to a callable
   */
  public Callable toCallable(Env env, FeatureExpr ctx, boolean isOptional) {
    if (!isOptional) {
      env.warning(L.l("Callable: '{0}' is not a valid callable argument",
              toString()));

      return new CallbackError(toString());
    } else {
      return null;
    }

  }

  //
  // Operations
  //

  /**
   * Append to a string builder.
   */
  public StringValue appendTo(UnicodeBuilderValue sb) {
    return sb.append(toString());
  }

  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(FeatureExpr ctx, StringBuilderValue sb) {
    return sb.append(ctx, toString());
  }

  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(BinaryBuilderValue sb) {
    return sb.appendBytes(toString());
  }

  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(LargeStringBuilderValue sb) {
    return sb.append(toString());
  }

  /**
   * Copy for assignment.
   */
  public Value copy() {
    return this;
  }

  /**
   * Copy as an array item
   */
  public Value copyArrayItem() {
    return copy();
  }

  /**
   * Copy as a return value
   */
  public Value copyReturn() {
    // php/3a5d

    return this;
  }

  /**
   * Copy for serialization
   */
  public final Value copy(Env env) {
    return copy(env, new IdentityHashMap<Value, EnvVar>());
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value, EnvVar> map) {
    return this;
  }

  /**
   * Copy for serialization
   */
  public Value copyTree(Env env, CopyRoot root) {
    return this;
  }

  /**
   * Clone for the clone keyword
   */
  public Value clone(Env env) {
    return this;
  }

  /**
   * Copy for saving a method's arguments.
   */
  public Value copySaveFunArg() {
    return copy();
  }

  /**
   * Returns the type.
   */
  public String getType() {
    return "value";
  }

  /**
   * Returns the SPL object hash.
   */
  public StringValue getObjectHash(Env env) {
    return env.getEmptyString();
  }

  /**
   * Returns the resource type.
   */
  public String getResourceType() {
    return null;
  }

  /**
   * Returns the current key
   */
  public V<? extends Value> key() {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the current value
   */
  public V<? extends Value> current() {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the next value
   * @param ctx
   */
  public V<? extends Value> next(FeatureExpr ctx) {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the previous value
   * @param ctx
   */
  public V<? extends Value> prev(FeatureExpr ctx) {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the end value.
   * @param ctx
   */
  public V<? extends Value> end(FeatureExpr ctx) {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the array pointer.
   * @param ctx
   */
  public V<? extends Value> reset(FeatureExpr ctx) {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Shuffles the array.
   */
  public Value shuffle() {
    return BooleanValue.FALSE;
  }

//  /**
//   * Pops the top array element.
//   */
//  public V<? extends Value> pop(Env env, FeatureExpr ctx) {
//    env.warning("cannot pop a non-array");
//
//    return V.one(NullValue.NULL);
//  }

  /**
   * Finds the method name.
   */
  public AbstractFunction findFunction(StringValue methodName) {
    return null;
  }

  //
  // function invocation
  //

  /**
   * Evaluates the function.
   */
  public V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args) {
    Callable call = toCallable(env, ctx, false);

    if (call != null)
      return call.call(env, ctx, args);
    else
      return VHelper.toV(env.warning(L.l("{0} is not a valid function",
              this)));
  }

  /**
   * Evaluates the function, returning a reference.
   */
  public V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args) {
    AbstractFunction fun = env.getFunction(this, ctx).getOne();

    if (fun != null)
      return fun.callRef(env, ctx, args);
    else
      return VHelper.toV(env.warning(L.l("{0} is not a valid function",
              this)));
  }

  /**
   * Evaluates the function, returning a copy
   */
  public V<? extends Value> callCopy(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args) {
    AbstractFunction fun = env.getFunction(this, ctx).getOne();

    if (fun != null)
      return fun.callCopy(env, ctx, args);
    else
      return VHelper.toV(env.warning(L.l("{0} is not a valid function",
              this)));
  }


  /**
   * Evaluates the function.
   */
  public final @Nonnull
  V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx) {
    return callRef(env, ctx, Callable.NULL_ARG_VALUES);
  }



  @Deprecated@VDeprecated
  public final V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, Value... args) {
    return call(env, ctx, VHelper.toVArray(args));
  }
  /**
   * Evaluates the function with an argument .
   */
  public final V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1) {
    return call(env, ctx, new V[]{a1});
  }

  /**
   * Evaluates the function with an argument .
   */
  public final @Nonnull
  V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1) {
    return callRef(env, ctx, new V[]{a1});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull
  V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2) {
    return call(env, ctx, new V[]{a1, a2});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull
  V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2) {
    return callRef(env, ctx, new V[]{a1, a2});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3) {
    return call(env, ctx, new V[]{a1, a2, a3});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3) {
    return callRef(env, ctx, new V[]{a1, a2, a3});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3, V<? extends ValueOrVar> a4) {
    return call(env, ctx, new V[]{a1, a2, a3, a4});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3, V<? extends ValueOrVar> a4) {
    return callRef(env, ctx, new V[]{a1, a2, a3, a4});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3, V<? extends ValueOrVar> a4, V<? extends ValueOrVar> a5) {
    return call(env, ctx, new V[]{a1, a2, a3, a4, a5});
  }

  /**
   * Evaluates the function with arguments
   */
  public final @Nonnull V<? extends ValueOrVar> callRef(Env env, FeatureExpr ctx,
                                                   V<? extends ValueOrVar> a1, V<? extends ValueOrVar> a2, V<? extends ValueOrVar> a3, V<? extends ValueOrVar> a4, V<? extends ValueOrVar> a5) {
    return callRef(env, ctx, new V[]{a1, a2, a3, a4, a5});
  }

  //
  // Methods invocation
  //

  /**
   * Evaluates a method.
   */
  public V<? extends ValueOrVar> callMethod(Env env,
                                            FeatureExpr ctx, StringValue methodName, int hash,
                                            V<? extends ValueOrVar>[] args) {
    if (isNull()) {
      return VHelper.toV(env.error(L.l("Method call '{0}' is not allowed for a null value.",
              methodName)));
    } else {
      return VHelper.toV(env.error(L.l("'{0}' is an unknown method of {1}.",
              methodName,
              toDebugString())));
    }
  }


  @Deprecated@VDeprecated
  public final V<? extends ValueOrVar> callMethod(Env env,
                                             FeatureExpr ctx, StringValue methodName,
                                             Value... args)

  { return this.callMethod(env, ctx, methodName, VHelper.toVArray(args));}
  /**
   * Evaluates a method.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                   FeatureExpr ctx, StringValue methodName,
                                    V<? extends ValueOrVar>[] args)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash, args);
  }


  /**
   * Evaluates a method.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                 V<? extends ValueOrVar>[] args)
  {
    return callMethod(env, ctx, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                      FeatureExpr ctx, StringValue methodName,
                                       V<? extends ValueOrVar>[] args)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash, args);
  }

  /**
   * Evaluates a method with 0 args.
   */
  public @Nonnull
  V<? extends ValueOrVar> callMethod(Env env, FeatureExpr ctx, StringValue methodName, int hash)
  {
    return callMethod(env, ctx, methodName, hash, NULL_ARG_VALUES);
  }

  /**
   * Evaluates a method with 0 args.
   */
  public final V<? extends ValueOrVar> callMethod(Env env, FeatureExpr ctx, StringValue methodName)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash);
  }

  /**
   * Evaluates a method with 0 args.
   */
  public @Nonnull
  V<? extends ValueOrVar> callMethodRef(Env env, FeatureExpr ctx, StringValue methodName, int hash)
  {
    return callMethodRef(env, ctx, methodName, hash, NULL_ARG_VALUES);
  }

  /**
   * Evaluates a method with 0 args.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env, FeatureExpr ctx, StringValue methodName)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public final @Nonnull V<? extends ValueOrVar> callMethod(Env env,
                             FeatureExpr ctx, StringValue methodName, int hash,
                                                V<? extends ValueOrVar> a1)
  {
    return callMethod(env, ctx, methodName, hash, new V[] { a1 });
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                   FeatureExpr ctx, StringValue methodName,
                                             V<? extends ValueOrVar> a1)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash, a1);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                 V<? extends ValueOrVar> a1)
  {
    return callMethodRef(env, ctx, methodName, hash, new V[] { a1 });
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                      FeatureExpr ctx, StringValue methodName,
                                       V<? extends ValueOrVar> a1)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash, a1);
  }

  /**
   * Evaluates a method with 2 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethod(Env env,
                             FeatureExpr ctx, StringValue methodName, int hash,
                              V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2)
  {
    return callMethod(env, ctx, methodName, hash, new V[] { a1, a2 });
  }




  /**
   * Evaluates a method with 2 args.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                   FeatureExpr ctx, StringValue methodName,
                                    V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash,
                      a1, a2);
  }

  /**
   * Evaluates a method with 2 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                 V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2)
  {
    return callMethodRef(env, ctx, methodName, hash, new V[] { a1, a2 });
  }

  /**
   * Evaluates a method with 2 args.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                      FeatureExpr ctx, StringValue methodName,
                                       V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash,
                         a1, a2);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethod(Env env,
                             FeatureExpr ctx, StringValue methodName, int hash,
                              V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3)
  {
    return callMethod(env, ctx, methodName, hash, new V[] { a1, a2, a3 });
  }

  /**
   * Evaluates a method with 3 args.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                   FeatureExpr ctx, StringValue methodName,
                                    V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash,
                      a1, a2, a3);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                 V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3)
  {
    return callMethodRef(env, ctx, methodName, hash, new V[] { a1, a2, a3 });
  }

  /**
   * Evaluates a method with 3 args.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                      FeatureExpr ctx, StringValue methodName,
                                       V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash,
                         a1, a2, a3);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethod(Env env,
                             FeatureExpr ctx, StringValue methodName, int hash,
                              V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4)
  {
    return callMethod(env, ctx, methodName, hash,
                      new V[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates a method with 4 args.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                   FeatureExpr ctx, StringValue methodName,
                                    V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash,
                      a1, a2, a3, a4);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                 V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4)
  {
    return callMethodRef(env, ctx, methodName, hash,
                         new V[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates a method with 4 args.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                      FeatureExpr ctx, StringValue methodName,
                                       V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash,
                         a1, a2, a3, a4);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethod(Env env,
                             FeatureExpr ctx, StringValue methodName, int hash,
                              V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4,  V<? extends ValueOrVar> a5)
  {
    return callMethod(env, ctx, methodName, hash,
                      new V[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates a method with 5 args.
   */
  public final V<? extends ValueOrVar> callMethod(Env env,
                                             FeatureExpr ctx, StringValue methodName,
                                              V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4,  V<? extends ValueOrVar> a5)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethod(env, ctx, methodName, hash,
                         a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                          FeatureExpr ctx, StringValue methodName, int hash,
                                           V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4,  V<? extends ValueOrVar> a5)
  {
    return callMethodRef(env, ctx, methodName, hash,
                         new V[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates a method with 5 args.
   */
  public final V<? extends ValueOrVar> callMethodRef(Env env,
                                                FeatureExpr ctx, StringValue methodName,
                                                 V<? extends ValueOrVar> a1,  V<? extends ValueOrVar> a2,  V<? extends ValueOrVar> a3,  V<? extends ValueOrVar> a4,  V<? extends ValueOrVar> a5)
  {
    int hash = methodName.hashCodeCaseInsensitive();

    return callMethodRef(env, ctx, methodName, hash,
                         a1, a2, a3, a4, a5);
  }

  //
  // Arithmetic operations
  //

  /**
   * Negates the value.
   */
  public Value neg()
  {
    return LongValue.create(- toLong());
  }

  /**
   * Negates the value.
   */
  public Value pos()
  {
    return LongValue.create(toLong());
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() + rValue.toLong());

    return DoubleValue.create(toDouble() + rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value add(long lLong)
  {
    return new DoubleValue(lLong + toDouble());
  }

//  /**
//   * Pre-increment the following value.
//   */
//  public Value preincr(int incr)
//  {
//    return increment(incr);
//  }
//
//  /**
//   * Post-increment the following value.
//   */
//  public Value postincr(int incr)
//  {
//    return increment(incr);
//  }

  /**
   * Return the next integer
   */
  public Value addOne()
  {
    return add(1);
  }

  /**
   * Return the previous integer
   */
  public Value subOne()
  {
    return sub(1);
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr()
  {
    return increment(1);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr()
  {
    return increment(1);
  }

  /**
   * Pre-increment the following value.
   */
  public Value predecr()
  {
    return increment(-1);
  }

  /**
   * Post-increment the following value.
   */
  public Value postdecr()
  {
    return increment(-1);
  }

  /**
   * Increment the following value.
   */
  public Value increment(int incr)
  {
    long lValue = toLong();

    return LongValue.create(lValue + incr);
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() - rValue.toLong());

    return DoubleValue.create(toDouble() - rValue.toDouble());
  }

  /**
   * Subtracts
   */
  public Value sub(long rLong)
  {
    return new DoubleValue(toDouble() - rLong);
  }


  /**
   * Substracts from the previous value.
   */
  public Value sub_rev(long lLong)
  {
    if (getValueType().isLongAdd())
      return LongValue.create(lLong - toLong());
    else
      return new DoubleValue(lLong - toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() * rValue.toLong());
    else
      return new DoubleValue(toDouble() * rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long r)
  {
    if (isLongConvertible())
      return LongValue.create(toLong() * r);
    else
      return new DoubleValue(toDouble() * r);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd()) {
      long l = toLong();
      long r = rValue.toLong();

      if (r != 0 && l % r == 0)
        return LongValue.create(l / r);
      else
        return new DoubleValue(toDouble() / rValue.toDouble());
    }
    else
      return new DoubleValue(toDouble() / rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value div(long r)
  {
    long l = toLong();

    if (r != 0 && l % r == 0)
      return LongValue.create(l / r);
    else
      return new DoubleValue(toDouble() / r);
  }

  /**
   * modulo the following value.
   */
  public Value mod(Value rValue)
  {
    double lDouble = toDouble();
    double rDouble = rValue.toDouble();

    return LongValue.create((long) lDouble % rDouble);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return LongValue.create(lLong << rLong);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return LongValue.create(lLong >> rLong);
  }

  /*
   * Binary And.
   */
  public Value bitAnd(Value rValue)
  {
    return LongValue.create(toLong() & rValue.toLong());
  }

  /*
   * Binary or.
   */
  public Value bitOr(Value rValue)
  {
    return LongValue.create(toLong() | rValue.toLong());
  }

  /**
   * Binary xor.
   */
  public Value bitXor(Value rValue)
  {
    return LongValue.create(toLong() ^ rValue.toLong());
  }

  /**
   * Absolute value.
   */
  public Value abs()
  {
    if (getValueType().isDoubleCmp())
      return new DoubleValue(Math.abs(toDouble()));
    else
      return LongValue.create(Math.abs(toLong()));
  }

  /**
   * Returns the next array index based on this value.
   */
  public long nextIndex(long oldIndex)
  {
    return oldIndex;
  }

  //
  // string functions
  //

  /**
   * Returns the length as a string.
   */
  public int length()
  {
    return toStringValue().length();
  }

  //
  // Array functions
  //

  /**
   * Returns the array size.
   */
  public V<? extends Integer> getSize()
  {
    return V.one(1);
  }

  /**
   * Returns the count, as returned by the global php count() function
   */
  public V<? extends Integer> getCount(Env env)
  {
    return getSize();
  }

  /**
   * Returns the count, as returned by the global php count() function
   */
  public V<? extends Integer> getCountRecursive(Env env)
  {
    return getCount(env);
  }

  /**
   * Returns an iterator for the key => value pairs.
   */
  public Iterator<VEntry> getIterator(Env env)
  {
    return getBaseIterator(env);
  }

  /**
   * Returns an iterator for the key => value pairs.
   */
  public Iterator<VEntry> getBaseIterator(Env env)
  {
    Set<VEntry> emptySet = Collections.emptySet();

    return emptySet.iterator();
  }

  /**
   * Returns an iterator for the field keys.
   * The default implementation uses the Iterator returned
   * by {@link #getIterator(Env)}; derived classes may override and
   * provide a more efficient implementation.
   */
  public Iterator<Value> getKeyIterator(Env env)
  {
    final Iterator<VEntry> iter = getIterator(env);

    return new Iterator<Value>() {
      public boolean hasNext() { return iter.hasNext(); }
      public Value next()      { return iter.next().getKey(); }
      public void remove()     { iter.remove(); }
    };
  }

  /**
   * Returns the field keys.
   */
  public Value []getKeyArray(Env env)
  {
    return NULL_VALUE_ARRAY;
  }

  /**
   * Returns the field values.
   */
  public Value []getValueArray(Env env)
  {
    return NULL_VALUE_ARRAY;
  }

  /**
   * Returns an iterator for the field values.
   * The default implementation uses the Iterator returned
   * by {@link #getIterator(Env)}; derived classes may override and
   * provide a more efficient implementation.
   */
  public Iterator<EnvVar> getValueIterator(Env env)
  {
    final Iterator<VEntry> iter = getIterator(env);

    return new Iterator<EnvVar>() {
      public boolean hasNext() { return iter.hasNext(); }

      public EnvVar next() {
        return iter.next().getEnvVar();
      }
      public void remove()     { iter.remove(); }
    };
  }

  //
  // Object field references
  //

  /**
   * Returns the field value
   */
  public V<? extends Value> getField(Env env, StringValue name)
  {
    return V.one(NullValue.NULL);
  }

  /**
   * Returns the field ref.
   */
  public V<? extends Var> getFieldVar(Env env, StringValue name)
  {
    return getField(env, name).map((a)->a.toVar());
  }

  /**
   * Returns the field used as a method argument
   */
  public V<? extends Var> getFieldArg(Env env, StringValue name, boolean isTop)
  {
    return getFieldVar(env, name);
  }

  /**
   * Returns the field ref for an argument.
   */
  public V<? extends Var> getFieldArgRef(Env env, StringValue name)
  {
    return getFieldVar(env, name);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldObject(Env env, StringValue name)
  {
    Value v = getField(env, name).getOne();

    if (! v.isset()) {
      v = env.createObject();

      putField(env, VHelper.noCtx(), name, v);
    }

    return v;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldArray(Env env, StringValue name)
  {
    Value v = getField(env, name).getOne();

    Value array = v.toAutoArray();

    if (v != array) {
      putField(env, VHelper.noCtx(), name, array);

      return array;
    }
    else if (array.isString()) {
      // php/0484
      return getFieldVar(env, name).getOne().getValue().getOne();
    }
    else {
      return v;
    }
  }

  @Deprecated@VDeprecated    //for V transformation only
  final public V<? extends Value> putField(Env env, FeatureExpr ctx, StringValue name, ValueOrVar object) {
    return putField(env, ctx, name, V.one(object));
  }
  /**
   * Returns the field ref.
   */
  public V<? extends Value> putField(Env env, FeatureExpr ctx, StringValue name, V<? extends ValueOrVar> object)
  {
    return V.one(NullValue.NULL);
  }

  public final V<? extends Value> putField(Env env, FeatureExpr ctx, StringValue name, Value value,
                                           Value innerIndex, V<? extends ValueOrVar> innerValue)
  {
    Value result = value.append(ctx, innerIndex, innerValue);

    return putField(env, ctx, name, V.one(result));
  }

  public void setFieldInit(boolean isInit)
  {
  }

  /**
   * Returns true if the object is in a __set() method call.
   * Prevents infinite recursion.
   */
  public boolean isFieldInit()
  {
    return false;
  }

  /**
   * Returns true if the object has this field.
   */
  public boolean isFieldExists(Env env, StringValue name) {
    return getField(env, name) == UnsetValue.UNSET;
  }

  /**
   * Returns true if the field is set
   */
  public V<? extends Boolean> issetField(Env env, StringValue name)
  {
    return V.one(false);
  }

  /**
   * Removes the field ref.
   */
  public void unsetField(FeatureExpr ctx, StringValue name)
  {
  }

  /**
   * Removes the field ref.
   */
  public void unsetArray(Env env, FeatureExpr ctx, StringValue name, Value index)
  {
  }

  /**
   * Removes the field ref.
   */
  public void unsetThisArray(Env env, FeatureExpr ctx, StringValue name, Value index)
  {
  }

  /**
   * Appends a value to an array that is a field of an object.
   */
  public Value putThisFieldArray(Env env,
                                   Value obj,
                                   StringValue fieldName,
                                   Value index,
                                   Value value)
  {
    // php/03mm
    return put(index, value);
  }

  /**
   * Returns the field as a Var or Value.
   */
  public V<? extends Value> getThisField(Env env, StringValue name)
  {
    return getField(env, name);
  }

  /**
   * Returns the field as a Var.
   */
  public V<? extends Var> getThisFieldVar(Env env, StringValue name)
  {
    return getThisField(env, name).map((a) -> a.toVar());
  }

  /**
   * Returns the field used as a method argument
   */
  public V<? extends Var> getThisFieldArg(Env env, StringValue name)
  {
    return getThisFieldVar(env, name);
  }

  /**
   * Returns the field ref for an argument.
   */
  public V<? extends Var> getThisFieldArgRef(Env env, StringValue name)
  {
    return getThisFieldVar(env, name);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public V<? extends Value> getThisFieldObject(Env env, FeatureExpr ctx, StringValue name)
  {
    V<? extends Value> v = getThisField(env, name);

    v.sforeach(ctx, (c, vv) -> {
      if (!vv.isset()) {
        putThisField(env, c, name, V.one(env.createObject()));
      }
    });

    return getThisField(env, name);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public V<? extends Value> getThisFieldArray(Env env, FeatureExpr ctx, StringValue name)
  {
    V<? extends Value> vv = getThisField(env, name);

    return vv.smap(ctx, (c, v) -> {
      Value array = v.toAutoArray();

      if (v == array)
        return v;
      else {
        putField(env, c, name, V.one(array));

        return array;
      }
    });
  }

  public final void initField(Env env,
                              FeatureExpr ctx, ClassField field,
                              boolean isInitFieldValues)
  {
    Value value = NullValue.NULL;

    if (isInitFieldValues) {
      value = field.evalInitExpr(env);
    }

    initField(env, ctx, field.getName(), field.getCanonicalName(), V.one(value));
  }

  public final void initField(Env env,
                              FeatureExpr ctx, StringValue canonicalName,
                              V<? extends Value>  value)
  {
    StringValue name = ClassField.getOrdinaryName(canonicalName);

    initField(env, ctx, name, canonicalName, value);
  }

  public void initField(Env env,
                        FeatureExpr ctx, StringValue name,
                        StringValue canonicalName,
                        V<? extends Value> value)
  {
    putThisField(env, ctx, canonicalName, value);
  }

  public void initIncompleteField(Env env,
                                  StringValue name,
                                  Value value,
                                  FieldVisibility visibility)
  {
    putThisField(env, VHelper.noCtx(), name, V.one(value));
  }

  @Deprecated@VDeprecated//V transformation
  final public V<? extends Value> putThisField(Env env, FeatureExpr ctx, StringValue name, ValueOrVar value) {
    return putThisField(env, ctx, name, V.one(value));
  }


  /**
   * Returns the field ref.
   */
  public V<? extends Value> putThisField(Env env, FeatureExpr ctx, StringValue name, V<? extends ValueOrVar> value)
  {
    return putField(env, ctx, name, value);
  }

//  /**
//   * Sets an array field ref.
//   */
//  public Value putThisField(Env env,
//                            StringValue name,
//                            Value array,
//                            Value index,
//                            Value value)
//  {
//    Value result = array.append(index, value);
//
//    putThisField(env, name, result);
//
//    return value;
//  }

//  /**
//   * Returns true if the field is set
//   */
//  public boolean issetThisField(Env env, StringValue name)
//  {
//    return issetField(env, name);
//  }

  /**
   * Removes the field ref.
   */
  public void unsetThisField(StringValue name)
  {
    unsetField(VHelper.noCtx(), name);
  }

//  /**
//   * Removes the field ref.
//   */
//  public void unsetThisPrivateField(String className, StringValue name)
//  {
//    unsetField(name);
//  }

  /**
   * Returns the static field.
   */
  public V<? extends Value> getStaticFieldValue(Env env, StringValue name)
  {
    env.error(L.l("No calling class found for '{0}'", this));

    return V.one(NullValue.NULL);
  }

  /**
  * Returns the static field reference.
  */
  public V<? extends Var> getStaticFieldVar(Env env, StringValue name)
  {
    env.error(L.l("No calling class found for '{0}'", this));

    throw new IllegalStateException();
  }

  /**
   * Sets the static field.
   */
  public V<? extends Var> setStaticFieldRef(Env env, FeatureExpr ctx, StringValue name, V<? extends ValueOrVar> value)
  {
    env.error(L.l("No calling class found for '{0}'", this));

    throw new IllegalStateException();
  }

  //
  // field convenience
  //


  @Deprecated@VDeprecated // V transformation
  final public Value putField(Env env, String name, Value value)
  {
    return putThisField(env, VHelper.noCtx(), env.createString(name), V.one(value)).getOne();
  }

  public V<? extends Value> putField(Env env, FeatureExpr ctx, String name, V<? extends ValueOrVar> value) {
    return putThisField(env, ctx, env.createString(name), value);
  }



  /**
   * Returns the array ref.
   */
  public EnvVar get(Value index)
  {
    return EnvVar.fromValue(UnsetValue.UNSET);
  }

  /**
   * Helper method that calls get(Value).
   */
  final public EnvVar get(long index) {
    return get(LongValue.create(index));
  }

  /**
   * Returns a reference to the array value.
   */
  public EnvVar getVar(FeatureExpr ctx, Value index)
  {
    return get(index);
  }

  /**
   * Returns a reference to the array value.
   */
  public EnvVar getRef(FeatureExpr ctx, Value index)
  {
    return get(index);
  }

  /**
   * Returns the array ref as a function argument.
   */
  public EnvVar getArg(Value index, boolean isTop)
  {
    return get(index);
  }

  /**
   * Returns the array value, copying on write if necessary.
   */
  public V<? extends Value> getDirty(Value index)
  {
    return get(index).getValue();
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public Value getArray()
  {
    return this;
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public V<? extends ValueOrVar> getArray(FeatureExpr ctx, Value index)
  {
    EnvVar var = getVar(ctx, index);

    return var.getVar().map((a) -> a.toAutoArray());
  }

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  public Value getObject(Env env)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public V<? extends Value> getObject(Env env, FeatureExpr ctx, Value index)
  {
    final EnvVar var = getVar(ctx, index);

    return var.getValue().sflatMap(ctx, (c, v) -> {
      if (v.isset())
        return V.one(v.toValue());
    else {
        V<ObjectValue> r = V.one(env.createObject());
        var.set(c, r);

        return r;
      }
    });
  }

  @Override
  public boolean isVar()
  {
    return false;
  }

  /**
   * Sets the value ref.
   */
  public Value set(Value value)
  {
    return value;
  }

  @Deprecated@VDeprecated("V<? extends ValueOrVar> put(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value)")//for V transformation only
  final public Value put(Value index, Value value) {
    put(index,EnvVar._gen(value));
    return value;
  }

  @Deprecated@VDeprecated("V<? extends ValueOrVar> put(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value)")//for V transformation only
  final public ValueOrVar put(Value index, ValueOrVar value) {
    if (value.isVar())
      put(index,new EnvVarImpl(V.one(value._var())));
    else
      put(index,EnvVar._gen(value._value()));
    return value;
  }

  public V<? extends ValueOrVar> put(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value) {
    Env.getCurrent().warning(L.l("{0} cannot be used as an array",
            toDebugString()));
    return value;
  }

  /**
   * Sets the array ref and returns the value
   */
  @Deprecated@VDeprecated("use V<? extends ValueOrVar> put(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value) instead")
  final public EnvVar put(Value index, EnvVar value)
  {
    return EnvVar.fromValuesOrVar(put(VHelper.noCtx(), index, value.getValue()));
  }

  /**
   * Sets the array ref.
   */
  @Deprecated@VDeprecated
  public final Value put(Value index, EnvVar value,
                         Value innerIndex, Value innerValue)
  {
    V<? extends Value> result = value.getValue().map((a)->a.append(innerIndex, innerValue));

    throw new UnimplementedVException();
//    put(index, result);

//    return innerValue;
  }

  /**
   * Appends an array value
   */
  public V<? extends ValueOrVar> put(FeatureExpr ctx, V<? extends ValueOrVar> value)
  {
    /*
    Env.getCurrent().warning(L.l("{0} cannot be used as an array",
                                 toDebugString()));
                                 */


    return value;
  }

  @Deprecated@VDeprecated //for transition to V implementation only
  final public V<? extends ValueOrVar> put(FeatureExpr ctx, ValueOrVar value) {
    return this.put(ctx, V.one(value));
  }


  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  @Deprecated@VDeprecated("Value append(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value)") //for transition to V implementation only
  final public Value append(Value index, ValueOrVar value)
  {
    return append(VHelper.noCtx(), index, V.one(value));
  }

  public Value append(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value) {
    Value array = toAutoArray();

    if (array.isArray())
      return array.append(ctx, index, value);
    else
      return array;
  }

  /**
   * Sets the array tail, returning the Var of the tail.
   * @param ctx
   */
  public V<? extends Var> putVar(FeatureExpr ctx)
  {
    return V.one(new VarImpl());
  }

  /**
   * Appends a new array.
   */
  @Deprecated@VDeprecated //for transition to V implementation only
  final public Value putArray(Env env)
  {
    Value value = new ArrayValueImpl();

    put(VHelper.noCtx(), V.one(value));

    return value;
  }

  /**
   * Sets the array tail, returning a reference to the tail.
   */
  public V<? extends Var> getArgTail(Env env, FeatureExpr ctx, boolean isTop)
  {
    return putVar(ctx);
  }

  /**
   * Appends a new object
   */
  public Value putObject(Env env)
  {
    Value value = env.createObject();

    put(VHelper.noCtx(), V.one(value));

    return value;
  }

  /**
   * Return true if the array value is set
   */
  public boolean isset(Value index)
  {
    return false;
  }

  /**
   * Returns true if the key exists in the array.
   */
  public boolean keyExists(Value key)
  {
    return isset(key);
  }

  /**
   * Returns the corresponding value if this array contains the given key
   *
   * @param key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   */
  public V<? extends Value> containsKey(Value key)
  {
    return null;
  }

  /**
   * Return unset the value.
   */
  public V<? extends Value> remove(FeatureExpr ctx, Value index)
  {
    return V.one(UnsetValue.UNSET);
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  public Object valuesToArray(Env env, FeatureExpr ctx, Class<?> elementType)
  {
    env.error(L.l("Can't assign {0} with type {1} to {2}[]",
                  this,
                  this.getClass(),
                  elementType));
    return null;
  }

  /**
   * Returns the character at the named index.
   */
  public Value charValueAt(long index)
  {
    return NullValue.NULL;
  }

  /**
   * Sets the character at the named index.
   */
  public Value setCharValueAt(long index, Value value)
  {
    return NullValue.NULL;
  }

  /**
   * Prints the value.
   * @param env
   * @param ctx
   */
  public void print(Env env, FeatureExpr ctx)
  {
    env.print(ctx, toString(env));
  }

  /**
   * Prints the value.
   * @param env
   * @param out
   */
  public void print(Env env, VWriteStream out)
  {
      out.print(VHelper.noCtx(), toString(env));
  }

  /**
   * nonvariational print method
     */
  @Deprecated@VDeprecated
  final public void print(Env env, WriteStream out)
  {
    try {
      out.print(toString(env));
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Serializes the value.
   *
   * @param env
   * @param sb holds result of serialization
   * @param serializeMap holds reference indexes
   */
  public void serialize(Env env,
                        StringBuilder sb,
                        SerializeMap serializeMap)
  {
    serializeMap.incrementIndex();

    serialize(env, sb);
  }

  /**
   * Encodes the value in JSON.
   */
  public void jsonEncode(Env env, JsonEncodeContext context, StringValue sb)
  {
    env.warning(L.l("type is unsupported; json encoded as null"));

    sb.append("null");
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Exports the value.
   */
  public StringValue varExport(Env env)
  {
    StringValue sb = env.createStringBuilder();

    varExportImpl(sb, 0);

    return sb;
  }

  /**
   * Exports the value.
   */
  protected void varExportImpl(StringValue sb, int level)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Binds a Java object to this object.
   */
  public void setJavaObject(Object obj)
  {
    // XXX: nam: do nothing for now, need to refactor the PHP-Java interface
    //throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PrintWriter out)
    throws IOException
  {
  }

  protected static void printJavaChar(PrintWriter out, char ch)
  {
    switch (ch) {
      case '\r':
        out.print("\\r");
        break;
      case '\n':
        out.print("\\n");
        break;
      //case '\"':
      //  out.print("\\\"");
      //  break;
      case '\'':
        out.print("\\\'");
        break;
      case '\\':
        out.print("\\\\");
        break;
      default:
        out.print(ch);
        break;
    }
  }

  protected static void printJavaString(PrintWriter out, StringValue s)
  {
    if (s == null) {
      out.print("");
      return;
    }

    int len = s.length();
    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      switch (ch) {
      case '\r':
        out.print("\\r");
        break;
      case '\n':
        out.print("\\n");
        break;
      case '\"':
        out.print("\\\"");
        break;
      case '\'':
        out.print("\\\'");
        break;
      case '\\':
        out.print("\\\\");
        break;
      default:
        out.print(ch);
        break;
      }
    }
  }

  public String toInternString()
  {
    return toString().intern();
  }

  public String toDebugString()
  {
    return toString();
  }

  public final void varDump(Env env, FeatureExpr ctx,
                            VWriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
  {
    if (valueSet.get(this) != null) {
      out.print(ctx, "*recursion*");
      return;
    }

    valueSet.put(this, "printing");

    try {
      varDumpImpl(env, ctx, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  public void varDumpImpl(Env env, FeatureExpr ctx,
                          VWriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet) {
    out.print(ctx, "resource(" + toString() + ")");
  }

  public final void printR(Env env, FeatureExpr ctx,
                           VWriteStream out,
                           int depth,
                           IdentityHashMap<Value, String> valueSet)
  {
    if (valueSet.get(this) != null) {
      out.print(ctx, "*recursion*");
      return;
    }

    valueSet.put(this, "printing");

    try {
      printRImpl(env, ctx, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  protected void printRImpl(Env env,
                            FeatureExpr ctx, VWriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet) {
    out.print(ctx, toString());
  }

  protected void printDepth(FeatureExpr ctx, VWriteStream out, int depth) {
    for (int i = 0; i < depth; i++)
      out.print(ctx, ' ');
  }

  public int getHashCode()
  {
    return hashCode();
  }

  @Override
  public int hashCode()
  {
    return 1021;
  }

  @Override
  public Var _var() {
    throw new UnsupportedOperationException("called _var on a Value");
  }

  @Override
  public Value _value() {
    return this;
  }

  @Deprecated@VDeprecated //introduced for V transformation as warning
  final public Var makeVar() {
    return toVar();
  }
}

