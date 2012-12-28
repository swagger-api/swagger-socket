package com.wordnik.swagger.sample.exception

class ApiException(code:Int, msg:String) extends Exception(msg:String)
class BadRequestException(code:Int, msg:String) extends ApiException(code:Int, msg:String)
class NotFoundException(code:Int, msg:String) extends ApiException(code:Int, msg:String)