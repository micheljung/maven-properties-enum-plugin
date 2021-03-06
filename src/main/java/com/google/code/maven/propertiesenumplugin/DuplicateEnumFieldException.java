/*
 * Copyright 2010 Michel Jung
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, softwaredistributed under the License is distributed on an
 * "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the License for the
 * specific language governing permissions andlimitations under the License.
 */
package com.google.code.maven.propertiesenumplugin;

/**
 * This exception is thrown if a duplicate enum field name was detected.
 * 
 * @author <a href="mailto:michel_jung@hotmail.com">Michel Jung</a>
 */
public class DuplicateEnumFieldException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 8038494599790703227L;

  /**
   * @param message
   *          the message
   */
  public DuplicateEnumFieldException(final String message) {
    super(message);
  }
}
