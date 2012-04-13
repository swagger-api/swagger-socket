/**
 *  Copyright 2012 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wordnik.swaggersocket.samples

import org.slf4j.LoggerFactory

import javax.ws.rs._
import com.wordnik.swagger.core._
import core.{Context, Response}
import javax.xml.bind.annotation.XmlRootElement
import org.codehaus.jackson.annotate.JsonProperty
import com.wordnik.api.client.model.{Definition, ExampleSearchResults}
import reflect.BeanProperty
import com.wordnik.swagger.runtime.common.{ApiKeyAuthTokenBasedSecurityHandler, APIInvoker}
import javax.servlet.{ServletConfig, ServletContext}

@Path("admin/api/word.json")
@Produces(Array("application/json"))
class WordnikResourceProxy extends RestResourceUtil {

  @Context
  private val sc: ServletConfig = null

  private var initialized = false

  val logger = LoggerFactory.getLogger(classOf[WordnikResourceProxy])

  @GET
  @Path("/{word}/examples")
  @ApiOperation(value = "Returns examples for a word", responseClass = "String")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid word supplied.")))
  def getExamples(
    @ApiParam(value = "Word to return examples for", required = true)@PathParam("word") word: String,
    @ApiParam(value = "Show duplicate examples from different sources", allowableValues = "false,true")@QueryParam("includeDuplicates") showDuplicates: String,
    @ApiParam("Return results from a specific ContentProvider")@QueryParam("contentProvider") source: String,
    @ApiParam(value = "If true will try to return the correct word root ('cats' -> 'cat'). If false returns exactly what was requested.", allowableValues = "false,true")@QueryParam("useCanonical") useCanonical: String,
    @ApiParam(value = "Specify the internal storage engine.", access = "internal")@QueryParam("internalDataStore") internalDataStore: String,
    @ApiParam("Results to skip")@QueryParam("skip") skip: String,
    @ApiParam("Maximum number of results to return") @QueryParam("limit") limit: String): Response = {

    if (!initialized) {
      initialized = true
      val key = sc.getInitParameter("com.wordnik.swagger.key")
      APIInvoker.initialize(new ApiKeyAuthTokenBasedSecurityHandler(key, null), "http://api.wordnik.com/v4", false);
    }

    val data: ExampleSearchResults = com.wordnik.api.client.api.WordAPI.getExamples(word,
        null,
        useCanonical,
        null,
        skip,
        limit)
      Response.ok.entity(APIInvoker.mapper.writeValueAsString(data)).build
    }


  @GET
  @Path("/{word}/definitions")
  @ApiOperation(value = "Returns definitions for a word", responseClass = "String")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid word supplied.")))
  def getDefinitions(
    @ApiParam(value = "Word to return definitions for", required = true)@PathParam("word") word: String,
    @ApiParam("Maximum number of results to return")@QueryParam("limit") limit: String,
    @ApiParam(value = "CSV list of part-of-speech types", allowableValues = "noun,adjective,verb,adverb,interjection,pronoun,preposition,abbreviation,affix,article,auxiliary-verb,conjunction,definite-article,family-name,given-name,idiom,imperative,noun-plural,noun-posessive,past-participle,phrasal-prefix,proper-noun,proper-noun-plural,proper-noun-posessive,suffix,verb-intransitive,verb-transitive")@QueryParam("partOfSpeech") partOfSpeech: String,
    @ApiParam(value = "Return related words with definitions", defaultValue = "false", allowableValues = "true,false")@QueryParam("includeRelated") includeRelated: String,
    @ApiParam(value = "If 'all' is received, results are returned from all sources. If multiple values are received (e.g. 'century,wiktionary'), results are returned " +
      "from the first specified dictionary that has definitions. If left blank, results are returned from the first dictionary that has definitions. " +
      "By default, dictionaries are searched in this order: ahd, wiktionary, webster, century, wordnet", allowableValues = "all,ahd,century,wiktionary,webster,wordnet", allowMultiple = true)@QueryParam("sourceDictionaries") sourcePriority: String,
    @ApiParam(value = "If true will try to return the correct word root ('cats' -> 'cat'). If false returns exactly what was requested.", defaultValue = "false", allowableValues = "false,true")@QueryParam("useCanonical") selectCanonical: String,
    @ApiParam(value = "Return a closed set of XML tags in response", defaultValue = "false", allowableValues = "false,true")@QueryParam("includeTags") includeTags: String) = {
      val data = com.wordnik.api.client.api.WordAPI.getDefinitions(
        word,
        limit,
        null,
        null,
        null,
        null,
        null)
      Response.ok.entity(data.toString).build
  }


  @GET
  @Path("/{word}/details")
  @ApiOperation(value = "Returns the definitions/examples for a given word", responseClass = "com.wordnik.async.WordDetails")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid word supplied.")))
  def getWordDetails(
    @ApiParam(value = "Word to return details for", required = true)@PathParam("word") word: String,
    @ApiParam(value = "If true will try to return the correct word root ('cats' -> 'cat'). If false returns exactly what was requested.", allowableValues = "false,true")@QueryParam("useCanonical") useCanonical: String,
    @ApiParam("Results to skip")@QueryParam("skip") skip: String,
    @ApiParam("Maximum number of results to return")@QueryParam("limit") limit: String): Response = {
      val examples = com.wordnik.api.client.api.WordAPI.getExamples(word,
        null,
        useCanonical,
        null,
        skip,
        limit)
      val definitions = com.wordnik.api.client.api.WordAPI.getDefinitions(
        word,
        limit,
        null,
        "true",
        null,
        useCanonical,
        null)
      val data = WordDetails(examples)
      data.setDefinitions(definitions)
      Response.ok.entity(data).build
  }

}

@XmlRootElement(name="wordDetails")
case class WordDetails(@BeanProperty var examples:ExampleSearchResults) {
  def this() = this(null)

  @JsonProperty(value ="definitions")
  var definitions:List[Definition]=_
  def getDefinitions():List[Definition] = this.definitions
  def setDefinitions(definitions:List[Definition]) {
    this.definitions = definitions
  }
}

object MethodType {
  val EXAMPLES = "examples"
  val DEFINITIONS = "definitions"
}

