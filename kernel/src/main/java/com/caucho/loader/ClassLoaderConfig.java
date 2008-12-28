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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.loader.enhancer.EnhancerManager;
import com.caucho.loader.ivy.IvyLoader;
import com.caucho.loader.ivy.IvyModule;
import com.caucho.osgi.OsgiLoader;
import com.caucho.osgi.OsgiBundleConfig;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

/**
 * Class for configuration.
 */
public class ClassLoaderConfig {
  private final static L10N L = new L10N(ClassLoaderConfig.class);

  private EnvironmentClassLoader _classLoader;
  private EnvironmentBean _owner;

  private Path _source;
  private boolean _servletHack;

  private int _index;

  private ArrayList<String> _priorityPackages;

  public ClassLoaderConfig()
    throws ConfigException
  {
    Thread thread = Thread.currentThread();

    ClassLoader loader = thread.getContextClassLoader();

    if (! (loader instanceof EnvironmentClassLoader)) {
      throw new ConfigException(L.l("<class-loader> requires an EnvironmentClassLoader."));
    }

    _classLoader = (EnvironmentClassLoader) loader;

    /*
    _owner = _classLoader.getOwner();

    if (_owner == null)
      throw new ConfigException(L.l("<class-loader> requires an environment with an EnvironmentBean owner."));
    */
  }

  /**
   * Sets the servlet classloader hack.
   */
  public void setServletHack(boolean hack)
  {
    _classLoader.setServletHack(hack);
  }

  /**
   * Adds a simple class loader.
   */
  public void addSimpleLoader(SimpleLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Creates an ivy class loader.
   */
  public IvyLoader createIvyLoader()
  {
    IvyLoader loader = new IvyLoader();
    
    loader.setLoader(_classLoader);

    return loader;
  }

  /**
   * Adds a jar dependency, to be loaded from the repository
   */
  public ModuleConfig createDependency()
  {
    ModuleConfig module = new ModuleConfig();

    return module;
  }

  /**
   * Adds a jar dependency, to be loaded from the repository
   */
  public void addDependency(ModuleConfig config)
  {
  }

  /**
   * Adds an ivy class loader.
   */
  public void addIvyLoader(IvyLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Adds a library loader, e.g. WEB-INF/lib
   */
  public LibraryLoader createLibraryLoader()
  {
    LibraryLoader loader = new LibraryLoader();
    loader.setLoader(_classLoader);

    return loader;
  }

  /**
   * Adds a directory class loader.
   */
  public void addLibraryLoader(LibraryLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Creates an ivy class loader.
   */
  public ModuleConfig createModule()
  {
    ModuleConfig module = new ModuleConfig();

    return module;
  }

  /**
   * Install an osgi bundle
   */
  public OsgiBundleConfig createBundle()
  {
    OsgiBundleConfig module = new OsgiBundleConfig();

    return module;
  }

  /**
   * Adds a compiling class loader.
   */
  public void addCompilingLoader(CompilingLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Creates an osgi class loader.
   */
  public OsgiLoader createOsgiLoader()
  {
    OsgiLoader loader = new OsgiLoader();
    
    loader.setLoader(_classLoader);

    return loader;
  }

  /**
   * Adds an osig class loader.
   */
  public void addOsgiLoader(OsgiLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Adds a tree loader.
   */
  public TreeLoader createTreeLoader()
  {
    TreeLoader loader = new TreeLoader();
    loader.setLoader(_classLoader);

    return loader;
  }

  public void addTreeLoader(TreeLoader loader)
  {
    _classLoader.addLoader(loader, _index++);
  }

  /**
   * Adds an enhancing loader.
   */
  public EnhancerManager createEnhancer()
    throws ConfigException
  {
    return EnhancerManager.create();
  }

  /**
   * Creates the aop.
   */
  /*
  public AopClassEnhancer createAop()
    throws ConfigException
  {
    return AopClassEnhancer.create();
  }
  */

  /**
   * Add a package for which this class loader will
   * take precendence over the parent. Any class that
   * has a qualified name that starts with the passed value
   * will be loaded from this classloader instead of the
   * parent classloader.
   */
  public void addPriorityPackage(String priorityPackage)
  {
    _classLoader.addPriorityPackage(priorityPackage);
  }

  /**
   * init
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    _classLoader.init();

    _classLoader.validate();
  }

  public String toString()
  {
    return "ClassLoaderConfig[]";
  }
}

