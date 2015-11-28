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

package com.caucho.quercus.lib.pdf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * pdf object oriented API facade
 */
public class PDFOutline 
{
  private int _id;
  
  private Map<Integer, PDFDestination> _allDestinations =
    new LinkedHashMap<Integer, PDFDestination>();
  
  private List<PDFDestination> _rootDestinations =
    new ArrayList<PDFDestination>();
  

  PDFOutline(int id)
  {
    _id = id;
  }
  
  public int getId()
  {
    return _id;
  }

  public void addDestination(PDFDestination dest, int parentId)
  {
    PDFDestination parent = null;
    if (parentId > 0)
      parent = _allDestinations.get(parentId);
    
    if (parent != null) {
      parent.addChild(dest);
    } else {
      dest.setParentId(_id);
      _rootDestinations.add(dest);
    }
    
    _allDestinations.put(dest.getId(), dest);
  }
  
  public List<PDFDestination> getRootDestinations()
  {
    return _rootDestinations;
  }
}
