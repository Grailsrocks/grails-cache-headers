package com.grailsrocks.cacheheaders

class TestAnnotationController {
    @NoCaching
    def annotations = {
        println 'bar'
    }
}