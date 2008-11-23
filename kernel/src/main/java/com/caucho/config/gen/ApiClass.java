/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.gen;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import com.caucho.util.*;

/**
 * Represents an interface class, either Local or Remote
 */
public class ApiClass {
  public static final ApiClass OBJECT = new ApiClass(Object.class);
  public static final ApiClass ENTITY_BEAN
    = new ApiClass(javax.ejb.EntityBean.class);

  private Class _apiClass;
  private HashMap<String,Type> _typeMap;
  private ArrayList<Type> _typeParam;
    
  private ArrayList<ApiMethod> _methods = new ArrayList<ApiMethod>();

  private ArrayList<ApiClass> _interfaces = new ArrayList<ApiClass>();
  
  /**
   * Creates a new api class
   *
   * @param topClass the api class
   */
  public ApiClass(Class apiClass)
  {
    this(apiClass, null);
  }
  
  /**
   * Creates a new api class
   *
   * @param topClass the api class
   */
  public ApiClass(Class apiClass, HashMap<String,Type> parentTypeMap)
  {
    if (apiClass == null)
      throw new NullPointerException();
    
    _apiClass = apiClass;

    _typeMap = new HashMap<String,Type>();

    if (parentTypeMap != null) {
      _typeMap.putAll(parentTypeMap);
    }

    introspectClass(apiClass, _typeMap);
  }
  
  /**
   * Creates a new api class
   *
   * @param topClass the api class
   */
  public ApiClass(Class apiClass,
		  HashMap<String,Type> parentTypeMap,
		  ArrayList<Type> param)
  {
    this(apiClass, parentTypeMap);

    _typeParam = param;
  }

  /**
   * Returns the classname.
   */
  public String getName()
  {
    return _apiClass.getName();
  }

  /**
   * Returns the short classname.
   */
  public String getSimpleName()
  {
    return _apiClass.getSimpleName();
  }

  /**
   * Returns the declaration name
   */
  public String getDeclarationName()
  {
    if (_typeParam == null || _typeParam.size() == 0)
      return _apiClass.getName();
    
    StringBuilder sb = new StringBuilder();
    sb.append(_apiClass.getName());
    sb.append("<");

    for (int i = 0; i < _typeParam.size(); i++) {
      if (i != 0)
	sb.append(",");

      Type type = (Type) _typeParam.get(i);

      if (type instanceof Class) {
	sb.append(((Class) type).getName());
      }
      else if (type instanceof ParameterizedType) {
	ParameterizedType pType = (ParameterizedType) type;
	Class rawType = (Class) pType.getRawType();

	sb.append(rawType.getName());
      }
      else
	throw new UnsupportedOperationException();
    }
    
    sb.append(">");

    return sb.toString();
  }

  /**
   * Returns the type map
   */
  public HashMap<String,Type> getTypeMap()
  {
    return _typeMap;
  }

  public Class getJavaClass()
  {
    return _apiClass;
  }

  /**
   * Returns true for a public class.
   */
  public boolean isPublic()
  {
    return Modifier.isPublic(_apiClass.getModifiers());
  }

  /**
   * Returns true for an interface
   */
  public boolean isInterface()
  {
    return Modifier.isInterface(_apiClass.getModifiers());
  }

  /**
   * Returns true for an abstract class
   */
  public boolean isAbstract()
  {
    return Modifier.isAbstract(_apiClass.getModifiers());
  }

  /**
   * Returns true for a final class
   */
  public boolean isFinal()
  {
    return Modifier.isFinal(_apiClass.getModifiers());
  }

  /**
   * Returns true for a primitive class
   */
  public boolean isPrimitive()
  {
    return _apiClass.isPrimitive();
  }

  /**
   * Returns the fields.
   */
  public Field []getFields()
  {
    return _apiClass.getFields();
  }

  /**
   * Returns the interfaces (should be ApiClass?)
   */
  public ArrayList<ApiClass> getInterfaces()
  {
    // return _apiClass.getInterfaces();
    return _interfaces;
  }

  public Constructor getConstructor(Class []param)
    throws NoSuchMethodException
  {
    return _apiClass.getConstructor(param);
  }

  /**
   * Returns the methods.
   */
  public ArrayList<ApiMethod> getMethods()
  {
    return _methods;
  }

  /**
   * Returns the matching method.
   */
  public ApiMethod getMethod(Method method)
  {
    return getMethod(method.getName(), method.getParameterTypes());
  }

  /**
   * Returns the matching method.
   */
  public ApiMethod getMethod(ApiMethod method)
  {
    return getMethod(method.getName(), method.getParameterTypes());
  }
  
  /**
   * Returns true if the method exists
   */
  public boolean hasMethod(String name, Class []args)
  {
    return getMethod(name, args) != null;
  }

  /**
   * Returns the matching method.
   */
  public ApiMethod getMethod(String name, Class []param)
  {
    for (int i = 0; i < _methods.size(); i++) {
      ApiMethod method = _methods.get(i);

      if (method.isMatch(name, param))
	return method;
    }

    return null;
  }

  /**
   * Returns true if the annotation exists.
   */
  public boolean isAnnotationPresent(Class annType)
  {
    return _apiClass.isAnnotationPresent(annType);
  }

  /**
   * Returns true if the annotation exists.
   */
  public <A extends Annotation> A getAnnotation(Class<A> annType)
  {
    return (A) _apiClass.getAnnotation(annType);
  }

  private void introspectClass(Class cl, HashMap<String,Type> typeMap)
  {
    if (cl == null)
      return;

    HashSet<ApiMethod> methodSet = new HashSet<ApiMethod>();
    
    for (Method method : cl.getDeclaredMethods()) {
      ApiMethod apiMethod = new ApiMethod(this, method, typeMap);

      methodSet.add(apiMethod);
    }

    _methods.addAll(methodSet);

    introspectGenericClass(cl.getGenericSuperclass(), typeMap);

    for (Type subClass : cl.getGenericInterfaces()) {
      ApiClass iface = introspectGenericClass(subClass, typeMap);

      if (cl == _apiClass && iface != null)
	_interfaces.add(iface);
    }
  }

  private ApiClass introspectGenericClass(Type type,
					  HashMap<String,Type> typeMap)
  {
    if (type == null)
      return null;

    if (type instanceof Class) {
      introspectClass((Class) type, typeMap);

      return new ApiClass((Class) type);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      Type []args = pType.getActualTypeArguments();
      
      Class rawType = (Class) pType.getRawType();
      TypeVariable []params = rawType.getTypeParameters();

      HashMap<String,Type> subMap = new HashMap<String,Type>(typeMap);
      ArrayList<Type> paramList = new ArrayList<Type>();

      for (int i = 0; i < params.length; i++) {
	TypeVariable param = params[i];
	Type arg = args[i];

	if (arg instanceof TypeVariable) {
	  TypeVariable argVar = (TypeVariable) arg;

	  if (typeMap.get(argVar.getName()) != null)
	    arg = typeMap.get(argVar.getName());
	}

	subMap.put(param.getName(), arg);

	paramList.add(arg);
      }

      introspectClass(rawType, subMap);

      return new ApiClass(rawType, subMap, paramList);
    }
    else
      return null;
  }

  public String toString()
  {
    return "ApiClass[" + _apiClass.getName() + "]";
  }
}
