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
package net.sf.maven.plugins.propertyenum;

/**
 * This exception is thrown if a invalid property key was found.
 * 
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 */
public class InvalidPropertyKeyException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -6420973612923244279L;

  /**
   * 
   */
  public InvalidPropertyKeyException() {
    super();
  }

  /**
   * @param message
   *          the error message
   */
  public InvalidPropertyKeyException(final String message) {
    super(message);
  }

  /**
   * @param message
   *          the error message
   * @param cause
   *          the causing exception
   */
  public InvalidPropertyKeyException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * @param cause
   *          the causing exception
   */
  public InvalidPropertyKeyException(final Throwable cause) {
    super(cause);
  }
}
