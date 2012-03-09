package com.wordnik.realtime

import java.text.SimpleDateFormat

import org.apache.commons.lang.StringUtils
import java.util.Date


trait RestResourceUtil {
  def asOption(s: String): Option[String] = {
    StringUtils.isBlank(s) match {
      case true => None
      case _ => Some(s)
    }
  }

  def getInt(minVal: Int, maxVal: Int, defaultValue: Int, inputString: String): Int = {
    var output: Int = {
      if (null == inputString)
        defaultValue
      else {
        try inputString.toInt
        catch {
          case _ => defaultValue
        }
      }
    }

    if (output < minVal) output = minVal
    if (maxVal == -1) { if (output < minVal) output = minVal }
    else if (output > maxVal) output = maxVal
    output
  }

  def getLong(minVal: Long, maxVal: Long, defaultValue: Long, inputString: String): Long = {
    var output: Long = defaultValue;
    try output = inputString.toLong
    catch {
      case _ => output = defaultValue
    }

    if (output < minVal) output = minVal
    if (maxVal == -1) { if (output < minVal) output = minVal }
    else if (output > maxVal) output = maxVal
    output
  }

  def getDouble(minVal: Double, maxVal: Double, defaultValue: Double, inputString: String): Double = {
    var output: Double = defaultValue;
    try output = inputString.toDouble
    catch {
      case _ => output = defaultValue
    }

    if (output < minVal) output = minVal
    if (maxVal == -1) { if (output < minVal) output = minVal }
    else if (output > maxVal) output = maxVal
    output
  }

  def getBoolean(defaultValue: Boolean, booleanString: String): Boolean = {
    var output: Boolean = defaultValue
    if (booleanString == null) output = defaultValue

    //	treat "", "YES" as "true"
    if ("".equals(booleanString)) output = true
    else if ("YES".equalsIgnoreCase(booleanString)) output = true
    else if ("NO".equalsIgnoreCase(booleanString)) output = false
    else {
      try output = booleanString.toBoolean
      catch {
        case _ => output = defaultValue
      }
    }
    output
  }

  def getDate(defaultValue: Date, dateString: String): Date = {
    try new SimpleDateFormat("yyyy-MM-dd").parse(dateString)
    catch {
      case _ => defaultValue
    }
  }
}