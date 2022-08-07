package com.madewithlove.anmeldung

interface CheckResult

object Success : CheckResult

data class Fail(val message: String) : CheckResult
